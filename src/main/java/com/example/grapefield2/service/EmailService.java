package com.example.grapefield2.service;

import com.example.grapefield2.entity.EmailVerify;
import com.example.grapefield2.entity.User;
import com.example.grapefield2.repository.EmailVerifyRepository;
import com.example.grapefield2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerifyRepository emailVerifyRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 회원가입 전 임시 인증번호 발송
     */
    @Transactional
    public void sendVerificationCode(String email, String verifyCode) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("[Grapefield2] 이메일 인증을 완료해주세요");
        message.setText("GrapeField 회원가입을 환영합니다.\n"
                + "아래 인증번호를 입력해주세요.\n\n"
                + "인증번호: " + verifyCode + "\n\n"
                + "이 인증번호는 10분 동안 유효합니다.\n\n"
                + "본인이 요청하지 않았다면 이 메일을 무시하세요."
        );

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("이메일 발송 실패: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 회원가입 중 인증번호 확인
     */
    public boolean verifyCode(String email, String verifyCode) {
        EmailVerify emailVerify = emailVerifyRepository
                .findByEmailAndVerifyCode(email, verifyCode)
                .orElse(null);

        if (emailVerify == null) return false;
        if (emailVerify.isVerified()) return false;
        if (emailVerify.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        emailVerify.setVerified(true);
        emailVerifyRepository.saveAndFlush(emailVerify);

        return true;
    }
}
