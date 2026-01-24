package com.example.grapefield2.repository;

import com.example.grapefield2.entity.EmailVerify;
import com.example.grapefield2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {

    Optional<EmailVerify> findByVerifyCode(String verifyCode);

    Optional<EmailVerify> findByUserAndIsVerifiedFalse(User user);

    Optional <EmailVerify> findByEmailAndVerifyCode(String email, String verifyCode);
}
