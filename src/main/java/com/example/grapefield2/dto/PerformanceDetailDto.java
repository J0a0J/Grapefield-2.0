package com.example.grapefield2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerformanceDetailDto {

    @JacksonXmlProperty(localName = "mt20id")
    private String performanceId;

    @JacksonXmlProperty(localName = "prfcast")
    private String castInfo;

    @JacksonXmlProperty(localName = "prfcrew")
    private String crewInfo;

    @JacksonXmlProperty(localName = "prfruntime")
    private String runTime;

    @JacksonXmlProperty(localName = "prfage")
    private String ageLimit;

    @JacksonXmlProperty(localName = "pcseguidance")
    private String ticketPrice;

    @JacksonXmlProperty(localName = "dtguidance")
    private String showTimes;

    @JacksonXmlProperty(localName = "mt10id")
    private String venueId;

    @JacksonXmlProperty(localName = "entrpsnmP")
    private String producer;

    @JacksonXmlProperty(localName = "entrpsnmA")
    private String agency;

    @JacksonXmlProperty(localName = "area")
    private String area;

    @JacksonXmlProperty(localName = "sty")
    private String story;

    @JacksonXmlProperty(localName = "daehakro")
    private String isDaehakro;

    @JacksonXmlElementWrapper(localName = "styurls")
    @JacksonXmlProperty(localName = "styurl")
    private List<String> introImageUrls;

    @JacksonXmlElementWrapper(localName = "relates")
    @JacksonXmlProperty(localName = "relate")
    private List<RelateInfo> ticketSites;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RelateInfo {
        // 사이트와 링크 모두 필요하기 때문에 메서드 생성
        @JacksonXmlProperty(localName = "relatenm")
        private String siteName;

        @JacksonXmlProperty(localName = "relateurl")
        private String url;
    }
}
