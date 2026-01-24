package com.example.grapefield2.repository;

import com.example.grapefield2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdx(Long idx);

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // 카카오 ID로 유저 찾기
    Optional<User> findByProviderId(String providerId);

    boolean existsByEmail(String email);
}
