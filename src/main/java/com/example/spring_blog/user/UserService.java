package com.example.spring_blog.user;

import com.example.spring_blog.error.Exception400;
import com.example.spring_blog.error.Exception404;
import com.example.spring_blog.user.apiLogin.OauthProvider;
import com.example.spring_blog.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리
     *
     * @param joinDTO 회원가입 정보
     * @return 저장된 사용자 정보
     */
    @Transactional
    public UserResponse.joinDTO join(UserRequest.joinDTO joinDTO, String verifiedEmail) {
        log.info("회원가입 서비스 처리시작 사용자명 : {}", joinDTO.getUsername());

//        인증하지 않았거나 인증한 이메일과 가입 폼의 이메일이 다르면 거름
        if (verifiedEmail == null || !verifiedEmail.equals(joinDTO.getEmail())) {
            throw new Exception400("이메일 인증을 먼저 완료해주세요");
        }

        checkEmailAvailable(joinDTO.getEmail());

        userJpaRepository.findByUsernameWithRoles(joinDTO.getUsername())
                .ifPresent(user -> {
                    log.warn("회원가입 실패 - 중복된 사용자명 : {}", joinDTO.getUsername());
                    throw new Exception400("중복된 이름입니다.");
                });

//        프로필 이미지 저장
        String profileImage = FileUtil.saveImage(joinDTO.getProfileImage());

        String encodePassword = passwordEncoder.encode(joinDTO.getPassword());

//        엔티티 생성 후 기본역할 USER
        User user = joinDTO.toEntity(profileImage, encodePassword);
        user.addRole(Role.USER);

//        저장. cascade로 UserRole로 함께 insert
        User savedUser = userJpaRepository.save(user);
        log.info("회원가입 서비스 처리시작 사용자명 : {}, ID : {}", joinDTO.getUsername(), savedUser.getId());
        return new UserResponse.joinDTO(savedUser);
    }

    /**
     * 로그인 처리
     *
     * @param loginDto 로그인 정보
     * @return 로그인된 사용자 정보
     */
    @Transactional
    public SessionUserDTO login(UserRequest.loginDto loginDto) {
        log.info("로그인 서비스 처리 시작 - 사용자명: {}", loginDto.getUsername());

        User user = userJpaRepository.findByUsername(
                loginDto.getUsername()).orElseThrow(() -> {
            log.warn("로그인 실패 사용자명: {} - 원인 : 인증정보 불일치", loginDto.getUsername());
            return new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다.");
        });

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("로그인 실패");
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다.");
        }

        log.info("로그인 성공 - 사용자명 : {}", loginDto.getUsername());
        return new SessionUserDTO(user);
    }

    public UserResponse.UpdateFormDTO findUpdateForm(Long userId) {
        User user = findById(userId);
        return new UserResponse.UpdateFormDTO(user);
    }

    @Transactional
    public SessionUserDTO updateById(Long id, UserRequest.updateDTO updateDTO) {
        User user = findById(id);

        if (updateDTO.hasPassword()) {
            user.update(passwordEncoder.encode(updateDTO.getPassword()));
        }

        if (updateDTO.hasProfileImage()) {
            String newImage = FileUtil.saveImage(updateDTO.getProfileImage());
            String oldImage = user.getProfileImage();
            user.updateProfileImage(newImage);
            FileUtil.deleteFile(oldImage);
        }

        return new SessionUserDTO(user);
    }

    private User findById(Long id) {
        log.info("사용자 정보 조회 - id: {}", id);
        return userJpaRepository.findById(id).orElseThrow(() -> {

            log.warn("회원 조회 실패 id: {} - 회원정보를 찾을 수 없음", id);
            throw new Exception404("사용자 정보를 찾을 수 없습니다.");

        });
    }

    public UserResponse.DetailDTO findByDetail(Long userId) {
        User user = findById(userId);
        return new UserResponse.DetailDTO(user);
    }

    public String findByPhoneNumber(Long userId) {

        User user = findById(userId);

        if (user.getPhoneNumber() == null ||
                user.getPhoneNumber().isBlank()) {
            throw new Exception400("등록된 전화번호가 없습니다.");
        }

        return user.getPhoneNumber();
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = findById(userId);
        String oldImage = user.getProfileImage();
        user.updateProfileImage(null); //db에 파일 비우기
        FileUtil.deleteFile(oldImage); //폴더 실제파일 삭제
    }

    @Transactional
    public SessionUserDTO kakaoLogin(UserResponse.KakaoUserInfo kakaoUserInfo) {
//        카카오 회원 번호로 우리 서비스의 사용자명 만들기
        String username = "kakao_" + kakaoUserInfo.getId();

//        이미 가입했는지 확인, 없으면 자동 회원 가입
        User user = userJpaRepository.findByUsername(username)
                .orElseGet(() -> joinKakaoUser(username, kakaoUserInfo));

        if (user.getProvider() != OauthProvider.KAKAO) {
            throw new Exception400("이미 사용중인 사용자명입니다.");
        }

        return new SessionUserDTO(user);
    }

    @Transactional
    private User joinKakaoUser(String username, UserResponse.KakaoUserInfo kakaoUserInfo) {
        User newUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // 소셜 회원가입은 비밀번호가 통일됨.
                .profileImage(kakaoUserInfo.getProperties().getProfileImage())
                .provider(OauthProvider.KAKAO)
                .email(username + "@kakao.com")
                .build();

        newUser.addRole(Role.USER);
        return userJpaRepository.save(newUser);
    }

    @Transactional
    public SessionUserDTO googleLogin(UserResponse.GoogleUserInfo googleUserInfo) {
        String username = "google_" + googleUserInfo.getSub();

        User user = userJpaRepository.findByUsername(username)
                .orElseGet(() -> joinGoogleUser(username, googleUserInfo));

        if (user.getProvider() != OauthProvider.GOOGLE) {
            throw new Exception400("이미 사용중인 사용자명입니다.");
        }

        return new SessionUserDTO(user);
    }

    @Transactional
    private User joinGoogleUser(String username, UserResponse.GoogleUserInfo googleUserInfo) {
        User newUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .profileImage(googleUserInfo.getPicture())
                .provider(OauthProvider.GOOGLE)
                .email(username + "@gmail.com")
                .build();

        newUser.addRole(Role.USER);
        return userJpaRepository.save(newUser);
    }

    //    인증요청 전, 사용가능한 이메일인지 검사
    public void checkEmailAvailable(String email) {
        userJpaRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new Exception400("이미 등록된 이메일입니다.");
                });
    }

    public void checkPhoneNumberAvailable(String phoneNumber) {
        userJpaRepository.findByPhoneNumber(phoneNumber)
                .ifPresent(user -> {
                    throw new Exception400("이미 사용중인 전화번호입니다.");
                });
    }

    @Transactional
    public void completePhoneVerification(Long userId, String phoneNumber) {
        User user = findById(userId);
        user.verifyPhone();
    }

    @Transactional
    public int chargePoint(Long userId, Integer amount) {
        User user = findById(userId);
        user.chargePoint(amount);
        log.info("포인트 충전 완료 - 사용자 Id : {}, 충전 : {}, 보유포인트 : {}", userId, amount, user.getPoint());
        return user.getPoint();
    }
}
