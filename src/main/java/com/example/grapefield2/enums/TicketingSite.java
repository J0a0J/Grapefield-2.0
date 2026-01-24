package com.example.grapefield2.enums;

import lombok.Getter;

@Getter
public enum TicketingSite {
    INTERPARK("인터파크"),
    NAVER("네이버N예약"),
    YES24("YES24"),
    TICKETLINK("티켓링크"),
    Naver("네이버N예약"),
    UNKNOWN("기타");

    private final String displayName;

    TicketingSite(String displayName) {
        this.displayName = displayName;
    }

    public static TicketingSite fromDisplayName(String name) {
        for (TicketingSite site : values()) {
            if (site.displayName.equals(name)) {
                return site;
            }
        }
        return UNKNOWN;
    }
}
