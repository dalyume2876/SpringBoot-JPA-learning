package com.example.spring_blog.user;

import com.example.spring_blog.error.Exception400;
import com.example.spring_blog.error.Exception401;
import com.example.spring_blog.error.Exception500;
import com.example.spring_blog.user.sms.SmsService;
import com.example.spring_blog.user.smtp.MailService;
import com.example.spring_blog.util.Define;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final MailService mailService;
    private final SmsService smsService;

//    POST /api/email/send
//    요청바디 : {"email": "aaa@naver.com"}
    @PostMapping("/api/email/send")
    public ResponseEntity<?> sendCode(@RequestBody UserRequest.EmailCheckDTO emailCheckDTO, HttpSession session) {

        emailCheckDTO.validate();
//        이미 가입된 이메일이 있으면 400떨구기
        userService.checkEmailAvailable(emailCheckDTO.getEmail());

//        SMTP를 사용해서 인증번호 발송 (구글 메일 서버) -> 사용자 이메일 수신.
        mailService.sendVerificationCode(emailCheckDTO.getEmail(), session);


        return ResponseEntity.ok(Map.of("message", "인증번호 발송. " + Define.CODE_EXPIRE_IN_MINUTES + "분 안에 입력해주세요."));
    }

    @PostMapping("/api/point/charge")
//    json 바디에 데이터를 추출하려면
    public ResponseEntity<?> chargePoint(@RequestBody UserRequest.PointChargeDTO pointChargeDTO, HttpSession session) {
        SessionUserDTO sessionUserDTO = (SessionUserDTO) session.getAttribute(Define.SESSION_USER);

        if (sessionUserDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message" , "로그인이 필요합니다."));
        }

        pointChargeDTO.validate();

        int point = userService.chargePoint(sessionUserDTO.getId(), pointChargeDTO.getAmount());

        return ResponseEntity.ok(Map.of("message", "포인트가 충전되었습니다.", "point", point));
    }

    @PostMapping("/api/email/verify")
    public ResponseEntity<?> verifyCode(@RequestBody UserRequest.EmailCheckDTO emailCheckDTO, HttpSession session) {
        emailCheckDTO.validateCode();
        boolean verified =  mailService.verifyCode(emailCheckDTO.getEmail(), emailCheckDTO.getCode(), session);

        if (!verified) {
            return ResponseEntity.badRequest().body(Map.of("message", "인증번호가 일치하지 않습니다."));
        }

        return ResponseEntity.ok().body(Map.of("message", "인증되었습니다."));
    }


    @PostMapping("/api/sms/send")
    public ResponseEntity<?> sendSmsCode(HttpSession session){
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("sessionUser");

        String phoneNumber =
                userService.findByPhoneNumber(sessionUser.getId());

        smsService.sendVerificationCode(phoneNumber, session);

        return ResponseEntity.ok().body(Map.of("message", "인증번호가 전송되었습니다."));
    }

    @PostMapping("/api/sms/verify")
    public ResponseEntity<?> verifyCode(@RequestBody UserRequest.PhoneNumberCheckDTO phoneNumberCheckDTO, HttpSession session) {
        phoneNumberCheckDTO.validateCode();

        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute(Define.SESSION_USER);

        String phoneNumber = userService.findByPhoneNumber(sessionUser.getId());
        smsService.verifyCode(phoneNumber, phoneNumberCheckDTO.getCode(), session);

        userService.completePhoneVerification(sessionUser.getId(), phoneNumber);

        return ResponseEntity.ok().body(Map.of("message", "인증되었습니다."));
    }

//    이 컨트롤러에서 발생한 예외는 JSON으로 별도 응답 처리함
    @ExceptionHandler(Exception400.class)
    public ResponseEntity<Map<String, String>> handleEx400(Exception400 e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(Exception500.class)
    public ResponseEntity<Map<String, String>> handleEx500(Exception500 e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "SMS 발송 중 오류가 발생했습니다."));
    }
}
