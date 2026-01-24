package com.example.grapefield2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnterMessage {
    private String type;
    private Long userId;
    private String username;
    private Long userCount;
}
