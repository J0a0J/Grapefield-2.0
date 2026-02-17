package com.example.grapefield2.config;

import com.example.grapefield2.service.DiscordNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupNotifier {
    
    private final DiscordNotifier discordNotifier;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("서버 시작 완료 - Discord 알림 전송");
        
        String environment = System.getenv("ENV") != null ? "PRODUCTION" : "LOCAL";
        
        discordNotifier.sendSystemMessage(
            "🚀", 
            "서버 시작 완료",
            String.format(
                "Grapefield 백엔드 서버가 정상적으로 시작되었습니다.\n" +
                "• 환경: %s\n" +
                "• 시간: %s",
                environment,
                java.time.LocalDateTime.now()
            )
        );
    }
}
