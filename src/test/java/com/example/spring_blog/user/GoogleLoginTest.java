package com.example.spring_blog.user; // 확인 필요: UserService와 같은 user 패키지라고 추측

import com.example.spring_blog.error.Exception400; // 확인 필요: Exception400의 패키지를 error로 추측
import com.example.spring_blog.user.apiLogin.OauthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceGoogleLoginTest {

    private static final String SUB = "google-user-123";
    private static final String USERNAME = "google_" + SUB;
    private static final String PICTURE = "https://example.com/avatar.png";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserJpaRepository userJpaRepository; // 확인 필요: 실제 Repository 타입명이 UserJpaRepository라고 추측

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserResponse.GoogleUserInfo googleUserInfo;

    @InjectMocks
    private UserService userService; // 확인 필요: googleLogin()이 UserService에 선언되었다고 추측

    @Test
    void googleLogin_기존구글회원_세션반환_테스트() {
        // given: 구글 계정과 연결된 회원이 DB에 존재하도록 설정한다.
        기존회원_준비(OauthProvider.GOOGLE);

        // when: 기존 구글 회원으로 로그인한다.
        SessionUserDTO result = userService.googleLogin(googleUserInfo);

        // then: 세션 DTO에 기존 회원의 사용자명이 들어 있는지 확인한다.
        assertThat(result.getUsername()).isEqualTo(USERNAME); // 확인 필요: SessionUserDTO에 getUsername()이 있다고 추측
    }

    @Test
    void googleLogin_기존구글회원_중복저장안함_테스트() {
        // given: 이미 등록된 구글 회원을 준비한다.
        기존회원_준비(OauthProvider.GOOGLE);

        // when: 기존 회원으로 로그인한다.
        userService.googleLogin(googleUserInfo);

        // then: 신규 회원 저장이 발생하지 않아야 한다.
        verify(userJpaRepository, never()).save(any(User.class));
    }

    @Test
    void googleLogin_다른제공자회원_예외_테스트() {
        // given: 같은 사용자명이지만 카카오 계정인 회원을 준비한다.
        기존회원_준비(OauthProvider.KAKAO);

        // when: 해당 사용자명으로 구글 로그인을 시도한다.
        // then: Exception400과 지정된 메시지가 발생하는지 확인한다.
        assertThatThrownBy(() -> userService.googleLogin(googleUserInfo))
                .isInstanceOf(Exception400.class)
                .hasMessage("이미 사용중인 사용자명입니다.");
    }

    @Test
    void googleLogin_신규구글회원_한번저장_테스트() {
        // given: 기존 회원이 없고 저장이 가능한 상황을 준비한다.
        신규회원_준비();

        // when: 신규 구글 회원으로 로그인한다.
        userService.googleLogin(googleUserInfo);

        // then: 회원을 한 번만 저장하는지 확인한다.
        verify(userJpaRepository, times(1)).save(any(User.class));
    }

    @Test
    void googleLogin_신규구글회원_사용자명생성_테스트() {
        // given: 신규 구글 회원 가입을 준비한다.
        신규회원_준비();

        // when: 구글 로그인을 수행한다.
        userService.googleLogin(googleUserInfo);

        // then: 저장하려던 회원명이 google_ + sub 형태인지 확인한다.
        assertThat(저장요청된회원().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void googleLogin_신규구글회원_프로필사진저장_테스트() {
        // given: 구글 사용자 정보에 프로필 사진 주소가 있는 상황을 준비한다.
        신규회원_준비();

        // when: 신규 구글 회원으로 로그인한다.
        userService.googleLogin(googleUserInfo);

        // then: 구글 프로필 사진 주소가 회원에게 저장되는지 확인한다.
        assertThat(저장요청된회원().getProfileImage()).isEqualTo(PICTURE);
    }

    @Test
    void googleLogin_신규구글회원_제공자설정_테스트() {
        // given: 신규 회원을 생성할 수 있게 준비한다.
        신규회원_준비();

        // when: 구글 로그인을 수행한다.
        userService.googleLogin(googleUserInfo);

        // then: 신규 회원의 소셜 로그인 제공자가 GOOGLE인지 확인한다.
        assertThat(저장요청된회원().getProvider()).isEqualTo(OauthProvider.GOOGLE);
    }

    @Test
    void googleLogin_신규구글회원_비밀번호암호화값저장_테스트() {
        // given: PasswordEncoder가 일정한 암호화 결과를 반환하도록 준비한다.
        신규회원_준비();

        // when: 신규 회원으로 구글 로그인을 수행한다.
        userService.googleLogin(googleUserInfo);

        // then: 평문이 아니라 인코더 반환값이 저장되는지 확인한다.
        assertThat(저장요청된회원().getPassword()).isEqualTo(ENCODED_PASSWORD);
    }

    @Test
    void googleLogin_신규구글회원_USER권한설정_테스트() {
        // given: 새로운 구글 회원이 가입할 상황을 준비한다.
        신규회원_준비();

        // when: 구글 로그인을 수행한다.
        userService.googleLogin(googleUserInfo);

        // then: 생성한 회원의 역할에 USER가 있는지 확인한다.
        assertThat(저장요청된회원().getRoles())
                .anySatisfy(userRole -> assertThat(userRole.getRole()).isEqualTo(Role.USER)); // 확인 필요: getRoles()의 원소에 getRole()이 있다고 추측
    }

    private void 기존회원_준비(OauthProvider provider) {
        given(googleUserInfo.getSub()).willReturn(SUB);
        User existingUser = User.builder()
                .username(USERNAME)
                .password(ENCODED_PASSWORD)
                .profileImage(PICTURE)
                .email("existing@example.com")
                .provider(provider)
                .build();
        existingUser.addRole(Role.USER);
        given(userJpaRepository.findByUsername(USERNAME)).willReturn(Optional.of(existingUser));
    }

    private void 신규회원_준비() {
        given(googleUserInfo.getSub()).willReturn(SUB);
        given(googleUserInfo.getPicture()).willReturn(PICTURE);
        given(userJpaRepository.findByUsername(USERNAME)).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn(ENCODED_PASSWORD);
        given(userJpaRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private User 저장요청된회원() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userJpaRepository).save(captor.capture());
        return captor.getValue();
    }
}