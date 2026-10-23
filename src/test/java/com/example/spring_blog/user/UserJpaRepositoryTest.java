package com.example.spring_blog.user; // 확인 필요: UserService와 관련 클래스가 user 패키지에 있다고 가정

import com.example.spring_blog.error.Exception400; // 확인 필요: Exception400 패키지 경로 추정
import com.example.spring_blog.error.Exception404; // 확인 필요: Exception404 패키지 및 클래스 존재 추정
import com.example.spring_blog.util.FileUtil; // 확인 필요: FileUtil 패키지 경로 추정
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserJpaRepository userJpaRepository; // 확인 필요: Repository 타입을 UserJpaRepository로 추정

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService; // 확인 필요: 서비스 클래스 이름을 UserService로 추정

    @Test
    void login_로그인성공_테스트() {
        // given - 사용자가 존재하고 입력한 비밀번호가 일치하도록 설정한다.
        UserRequest.loginDto loginDto = mock(UserRequest.loginDto.class);
        User user = mock(User.class);
        given(loginDto.getUsername()).willReturn("ssar");
        given(loginDto.getPassword()).willReturn("1234");
        given(userJpaRepository.findByUsername("ssar")).willReturn(Optional.of(user));
        given(user.getPassword()).willReturn("stored-hash");
        given(passwordEncoder.matches("1234", "stored-hash")).willReturn(true);

        try (MockedConstruction<SessionUserDTO> createdDtos = mockConstruction(SessionUserDTO.class)) {
            // when - 로그인 서비스를 실행한다.
            SessionUserDTO result = userService.login(loginDto);

            // then - 새로 만든 SessionUserDTO를 결과로 반환했는지 확인한다.
            assertThat(createdDtos.constructed()).containsExactly(result);
        }
    }

    @Test
    void login_사용자없음_테스트() {
        // given - 조회한 사용자명이 데이터베이스에 없도록 설정한다.
        UserRequest.loginDto loginDto = mock(UserRequest.loginDto.class);
        given(loginDto.getUsername()).willReturn("unknown");
        given(userJpaRepository.findByUsername("unknown")).willReturn(Optional.empty());

        // when - 존재하지 않는 계정으로 로그인을 시도한다.
        // then - 400 예외와 안내 메시지가 발생하는지 확인한다.
        assertThatThrownBy(() -> userService.login(loginDto))
                .isInstanceOf(Exception400.class)
                .hasMessage("사용자명 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void login_비밀번호불일치_테스트() {
        // given - 사용자는 존재하지만 비밀번호 검사가 실패하도록 설정한다.
        UserRequest.loginDto loginDto = mock(UserRequest.loginDto.class);
        User user = mock(User.class);
        given(loginDto.getUsername()).willReturn("ssar");
        given(loginDto.getPassword()).willReturn("wrong");
        given(userJpaRepository.findByUsername("ssar")).willReturn(Optional.of(user));
        given(user.getPassword()).willReturn("stored-hash");
        given(passwordEncoder.matches("wrong", "stored-hash")).willReturn(false);

        // when - 잘못된 비밀번호로 로그인을 시도한다.
        // then - 400 예외와 안내 메시지가 발생하는지 확인한다.
        assertThatThrownBy(() -> userService.login(loginDto))
                .isInstanceOf(Exception400.class)
                .hasMessage("사용자명 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void findUpdateForm_조회성공_테스트() {
        // given - 요청한 ID에 해당하는 사용자가 존재하도록 설정한다.
        User user = mock(User.class);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(user)); // 확인 필요: findById가 Repository의 findById에 위임한다고 추정

        try (MockedConstruction<UserResponse.UpdateFormDTO> createdDtos =
                     mockConstruction(UserResponse.UpdateFormDTO.class)) {
            // when - 회원정보 수정 화면에 필요한 데이터를 조회한다.
            UserResponse.UpdateFormDTO result = userService.findUpdateForm(1L);

            // then - UpdateFormDTO가 생성되어 반환됐는지 확인한다.
            assertThat(createdDtos.constructed()).containsExactly(result);
        }
    }

    @Test
    void findUpdateForm_사용자없음_테스트() {
        // given - 요청한 ID의 사용자가 존재하지 않도록 설정한다.
        given(userJpaRepository.findById(999L)).willReturn(Optional.empty()); // 확인 필요: findById의 Repository 호출을 추정

        // when - 없는 사용자의 수정 정보를 조회한다.
        // then - 사용자 없음 예외가 발생하는지 확인한다.
        assertThatThrownBy(() -> userService.findUpdateForm(999L))
                .isInstanceOf(Exception404.class); // 확인 필요: findById가 Exception404를 던진다고 추정
    }

    @Test
    void updateById_비밀번호변경_테스트() {
        // given - 변경할 비밀번호와 BCrypt 인코딩 결과를 준비한다.
        UserRequest.updateDTO updateDto = mock(UserRequest.updateDTO.class);
        User user = mock(User.class);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(user)); // 확인 필요: findById의 Repository 호출을 추정
        given(updateDto.hasPassword()).willReturn(true);
        given(updateDto.getPassword()).willReturn("new-password");
        given(passwordEncoder.encode("new-password")).willReturn("encoded-password");

        try (MockedConstruction<SessionUserDTO> ignored = mockConstruction(SessionUserDTO.class)) {
            // when - 회원정보 수정 서비스를 실행한다.
            userService.updateById(1L, updateDto);

            // then - 인코딩된 비밀번호로 User.update가 호출됐는지 확인한다.
            verify(user).update("encoded-password");
        }
    }

    @Test
    void updateById_비밀번호미입력_테스트() {
        // given - 새 비밀번호와 새 이미지가 없는 수정 요청을 준비한다.
        UserRequest.updateDTO updateDto = mock(UserRequest.updateDTO.class);
        User user = mock(User.class);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(user)); // 확인 필요: findById의 Repository 호출을 추정

        try (MockedConstruction<SessionUserDTO> ignored = mockConstruction(SessionUserDTO.class)) {
            // when - 비밀번호 없이 수정 서비스를 실행한다.
            userService.updateById(1L, updateDto);

            // then - 비밀번호 변경 메서드가 호출되지 않았는지 확인한다.
            verify(user, never()).update(anyString());
        }
    }

    @Test
    void updateById_프로필이미지변경_테스트() {
        // given - 새 프로필 이미지가 저장되면 새 파일명이 반환되도록 설정한다.
        UserRequest.updateDTO updateDto = mock(UserRequest.updateDTO.class);
        User user = mock(User.class);
        MultipartFile image = mock(MultipartFile.class); // 확인 필요: getProfileImage 반환 타입을 MultipartFile로 추정
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(user)); // 확인 필요: findById의 Repository 호출을 추정
        given(updateDto.hasProfileImage()).willReturn(true);
        given(updateDto.getProfileImage()).willReturn(image); // 확인 필요: getProfileImage 반환 타입 추정
        given(user.getProfileImage()).willReturn("old.png");

        try (MockedStatic<FileUtil> files = mockStatic(FileUtil.class);
             MockedConstruction<SessionUserDTO> ignored = mockConstruction(SessionUserDTO.class)) {
            files.when(() -> FileUtil.saveImage(image)).thenReturn("new.png");

            // when - 새 이미지로 회원정보를 수정한다.
            userService.updateById(1L, updateDto);

            // then - 사용자의 프로필 이미지 경로를 새 파일명으로 바꾸는지 확인한다.
            verify(user).updateProfileImage("new.png");
        }
    }

    @Test
    void updateById_기존이미지삭제_테스트() {
        // given - 기존 이미지와 새 이미지가 모두 있는 상황을 준비한다.
        UserRequest.updateDTO updateDto = mock(UserRequest.updateDTO.class);
        User user = mock(User.class);
        MultipartFile image = mock(MultipartFile.class); // 확인 필요: getProfileImage 반환 타입을 MultipartFile로 추정
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(user)); // 확인 필요: findById의 Repository 호출을 추정
        given(updateDto.hasProfileImage()).willReturn(true);
        given(updateDto.getProfileImage()).willReturn(image); // 확인 필요: getProfileImage 반환 타입 추정
        given(user.getProfileImage()).willReturn("old.png");

        try (MockedStatic<FileUtil> files = mockStatic(FileUtil.class);
             MockedConstruction<SessionUserDTO> ignored = mockConstruction(SessionUserDTO.class)) {
            files.when(() -> FileUtil.saveImage(image)).thenReturn("new.png");

            // when - 프로필 이미지를 교체한다.
            userService.updateById(1L, updateDto);

            // then - 기존 이미지 파일 삭제가 한 번 호출됐는지 확인한다.
            files.verify(() -> FileUtil.deleteFile("old.png"), times(1));
        }
    }

    @Test
    void updateById_사용자없음_테스트() {
        // given - 수정하려는 사용자가 존재하지 않도록 설정한다.
        UserRequest.updateDTO updateDto = mock(UserRequest.updateDTO.class);
        given(userJpaRepository.findById(999L)).willReturn(Optional.empty()); // 확인 필요: findById의 Repository 호출을 추정

        // when - 없는 사용자의 정보를 수정한다.
        // then - 사용자 없음 예외가 발생하는지 확인한다.
        assertThatThrownBy(() -> userService.updateById(999L, updateDto))
                .isInstanceOf(Exception404.class); // 확인 필요: findById가 Exception404를 던진다고 추정
    }
}