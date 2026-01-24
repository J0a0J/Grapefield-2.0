    package com.example.grapefield2.entity;

    import com.example.grapefield2.enums.LoginType;
    import jakarta.persistence.*;
    import lombok.*;
    import org.hibernate.annotations.CollectionId;
    import org.hibernate.annotations.DynamicInsert;
    import org.hibernate.annotations.DynamicUpdate;

    import java.time.LocalDateTime;

    @Entity
    @Table(name="users")
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @DynamicUpdate
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long idx;

        @Column(unique = true, nullable = false)
        private String email;

        @Column(nullable = true)
        private String password; // 카카오 로그인은 null

        @Column(unique = true, nullable = false)
        private String username;

        private String profileImg;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private LoginType loginType;

        private String providerId; // 카카오 고유 ID

        @Column(nullable = false)
        @Builder.Default
        private boolean isEmailVerified = false;

        @Builder.Default
        private LocalDateTime createdAt = LocalDateTime.now();

        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();

        @PreUpdate
        protected void onUpdate() {
            this.updatedAt = LocalDateTime.now();
        }

        public String getProfileImg() {
            if (this.profileImg == null || this.profileImg.isEmpty()) {
                return "https://api.dicebear.com/7.x/initials/svg?seed=" + this.username;
            }
            return this.profileImg;
        }
    }
