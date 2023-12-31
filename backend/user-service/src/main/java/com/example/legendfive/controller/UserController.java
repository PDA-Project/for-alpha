package com.example.legendfive.controller;

import com.example.legendfive.dto.MailDto;
import com.example.legendfive.dto.ResponseDto;
import com.example.legendfive.dto.UserDto;
import com.example.legendfive.entity.User;
import com.example.legendfive.exception.UserErrorResult;
import com.example.legendfive.service.MailService;
import com.example.legendfive.service.UserService;
import com.example.legendfive.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequestMapping("/user-service")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerificationService verificationService;
    private final MailService mailService;
    private final ObjectMapper objectMapper;
    private String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    private String passwordRegex = "^(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,20}$";

    //닉네임 한글 포함 8자리 이하 특수문자X
    private String nicknameRegex = "^[가-힣a-zA-Z0-9]{1,10}$";

    @PostMapping("/login") //@AuthenticationPrincipal UserPrincipal userPrincipal, User user = userPrincipal.getUser();
    public ResponseEntity<ResponseDto> login(@RequestBody UserDto.LoginRequestDto loginRequestDto) {
        try {
            if (loginRequestDto.getEmail() == null || loginRequestDto.getPassword() == null) {
                log.info("필수값 누락");
                UserErrorResult userErrorResult = UserErrorResult.REQUIRED_VALUE;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            if (!loginRequestDto.getEmail().matches(emailRegex)) {
                log.info("유효하지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            if (!userService.userExistsByEmail(loginRequestDto.getEmail())) {
                log.info("존재하지 않는 사용자");
                UserErrorResult userErrorResult = UserErrorResult.NOT_FOUND_USER;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();
            }

            if (!userService.userExistsByEmailAndPassword(loginRequestDto.getEmail(), loginRequestDto.getPassword())) {
                log.info("비밀번호 불일치");
                UserErrorResult userErrorResult = UserErrorResult.NOT_FOUND_USER;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            log.info("로그인 성공");
            UserDto.LoginResponseDto loginResponseDto = userService.login(loginRequestDto);

            ResponseDto responseDto = ResponseDto.builder()
                    .payload(objectMapper.convertValue(loginResponseDto, Map.class))
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDto); //200
        } catch (Exception e) {
            log.info("로그인 실패");
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
    }

    @PostMapping
    public ResponseEntity<ResponseDto> signIn(@RequestBody UserDto.SignInRequestDto signInRequestDto) throws Exception {
        try {
            if (signInRequestDto.getEmail() == null || signInRequestDto.getPassword() == null || signInRequestDto.getNickname() == null) {
                log.info("필수값 누락");
                UserErrorResult userErrorResult = UserErrorResult.REQUIRED_VALUE;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            if (!signInRequestDto.getEmail().matches(emailRegex)) {
                log.info("유효하지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            if (!signInRequestDto.getPassword().matches(passwordRegex)) {
                log.info("유효하지 않은 비밀번호");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_PASSWORD;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            if (!signInRequestDto.getNickname().matches(nicknameRegex)) {
                log.info("유효하지 않은 닉네임");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_NICKNAME;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            if (userService.userExistsByNickname(signInRequestDto.getNickname())) {
                log.info("이미 존재하는 닉네임");
                UserErrorResult userErrorResult = UserErrorResult.DUPLICATED_NICKNAME;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            boolean userHasProvider = userService.userHasProvider(signInRequestDto.getEmail());

            if (userService.userExistsByEmail(signInRequestDto.getEmail()) && !userHasProvider) {
                log.info("이메일 중복");
                UserErrorResult userErrorResult = UserErrorResult.DUPLICATED_EMAIL;

                ResponseDto responseDto = ResponseDto.builder()
                        .error(userErrorResult.getMessage())
                        .build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            if (!verificationService.isVerified(signInRequestDto.getEmail())) {
                log.info("인증되지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.UNVERIFIED_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            log.info("UserHasProvider: " + userHasProvider);

            if (!userHasProvider) {
                UserDto.SignInResponseDto signInResponseDto = userService.signIn(signInRequestDto);

                ResponseDto responseDto = ResponseDto.builder()
                        .payload(objectMapper.convertValue(signInResponseDto, Map.class))
                        .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(responseDto); //201

            } else if (userHasProvider) {

                UserDto.SocialSignInResponseDto socialSignInResponseDto = userService.socialSignIn(signInRequestDto);

                ResponseDto responseDto = ResponseDto.builder()
                        .payload(objectMapper.convertValue(socialSignInResponseDto, Map.class))
                        .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(responseDto); //201
            }
        } catch (Exception e) {
            log.info("회원가입 실패");
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
        return null;
    }

    @PostMapping("/sign-in/email/validation")
    public ResponseEntity<ResponseDto> validationEmail(@RequestBody MailDto.MailRequestDto mailRequestDto) {
        try {
            String email = mailRequestDto.getEmail();

            if (!email.matches(emailRegex)) {
                log.info("유효하지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            String verificationCode = verificationService.makeVerificationCode();

            MailDto.MailSendDto mailSendDto = MailDto.MailSendDto.builder()
                    .email(email)
                    .title("포알파 이메일 인증코드입니다.")
                    .content("인증번호는 [" + verificationCode + "]입니다.")
                    .build();

            mailService.sendMail(mailSendDto);

            verificationService.saveVerificationCode(email, verificationCode);
            verificationService.saveCompletionCode(email, false);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); //204
        } catch (Exception e) {
            log.info("이메일 인증코드 발송 실패");
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
    }

    @PostMapping("/sign-in/email/verification")
    public ResponseEntity<ResponseDto> verificationEmail(@RequestBody MailDto.MailVerifyDto mailVerifyDto) {
        try {
            String email = mailVerifyDto.getEmail();

            if (!email.matches(emailRegex)) {
                log.info("유효하지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            if (verificationService.verifyCode(email, mailVerifyDto.getVerificationCode())) {
                log.info("인증 코드 검증 성공");
                //검증 성공 후 코드 삭제, 완료 코드 생성
                verificationService.deleteVerificationCode(email);
                verificationService.saveCompletionCode(email, true);

                return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); //204
            } else {
                log.info("인증 코드 검증 실패");
                UserErrorResult userErrorResult = UserErrorResult.FAILED_VALIDATING_CODE;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
        } catch (Exception e) {
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
    }

    @PutMapping("/sign-in/password")
    public ResponseEntity<ResponseDto> updatePassword(@RequestBody UserDto.UpdatePasswordRequestDto updatePasswordRequestDto) {
        try {
            String email = updatePasswordRequestDto.getEmail();
            if (!email.matches(emailRegex)) {
                log.info("유효하지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            if (!verificationService.isVerified(email)) {
                log.info("검증되지 않은 이메일");
                UserErrorResult userErrorResult = UserErrorResult.UNVERIFIED_EMAIL;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }

            if (!updatePasswordRequestDto.getNewPassword().matches(passwordRegex)) {
                log.info("유효하지 않은 비밀번호");
                UserErrorResult userErrorResult = UserErrorResult.INVALID_PASSWORD;
                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();

                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
            }
            userService.updatePassword(updatePasswordRequestDto);

            UserDto.UpdatePasswordResponseDto updatePasswordResponseDto = userService.updatePassword(updatePasswordRequestDto);

            ResponseDto responseDto = ResponseDto.builder()
                    .payload(objectMapper.convertValue(updatePasswordResponseDto, Map.class))
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDto); //204

        } catch (Exception e) {
            log.info("비밀번호 변경 실패");
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
    }

//    @GetMapping("/sign-in/email/verification")
//    public ResponseEntity<ResponseDto> isVerified(@RequestBody String email) {
//        try {
//            if (!email.matches(emailRegex)) {
//                log.info("유효하지 않은 이메일");
//                UserErrorResult userErrorResult = UserErrorResult.INVALID_EMAIL;
//                ResponseDto responseDto = ResponseDto.builder().error(userErrorResult.getMessage()).build();
//
//                return ResponseEntity.status(userErrorResult.getHttpStatus()).body(responseDto);
//            }
//            boolean isVerified = verificationService.isVerified(email);
//            if (isVerified) {
//                log.info("인증된 이메일");
//            } else {
//                log.info("인증되지 않은 이메일");
//            }
//            ResponseDto responseDto = ResponseDto.builder()
//                    .payload(objectMapper.convertValue(isVerified, Map.class))
//                    .build();
//            log.info("email: " + String.valueOf(isVerified));
//
//            return ResponseEntity.status(HttpStatus.OK).body(responseDto);
//        } catch (Exception e) {
//            log.info("이메일 인증 여부 확인 실패");
//            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();
//
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
//        }
//    }

    @GetMapping("/test")
    public String test(){
        return "Test";
    }
}