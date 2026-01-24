package com.example.grapefield2.enums;

import lombok.Getter;

@Getter
public enum GenreCode {
    THEATER("AAAA", "연극"),
    MUSICAL("GGGA", "뮤지컬"),
    DANCE_WESTERN("BBBC", "무용(서양/한국무용)"),
    DANCE_POPULAR("BBBE", "대중무용"),
    CLASSIC("CCCA", "서양음악(클래식)"),
    KOREAN_MUSIC("CCCC", "한국음악(국악)"),
    POPULAR_MUSIC("CCCD", "대중음악"),
    COMPLEX("EEEA", "복합"),
    CIRCUS("EEEB", "서커스/마술");

    private final String code;
    private final String name;

    GenreCode(String code, String name) {
        this.code = code;
        this.name = name;
    }
}