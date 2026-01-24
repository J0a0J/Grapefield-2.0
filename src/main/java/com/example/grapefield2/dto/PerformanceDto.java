package com.example.grapefield2.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PerformanceDto {
    @JacksonXmlProperty(localName = "mt20id")
    private String performanceId;

    @JacksonXmlProperty(localName = "prfnm")
    private String title;

    @JacksonXmlProperty(localName = "prfpdfrom")
    private String startDate;

    @JacksonXmlProperty(localName = "prfpdto")
    private String endDate;

    @JacksonXmlProperty(localName = "fcltynm")
    private String venue;

    @JacksonXmlProperty(localName = "area")
    private String area;

    @JacksonXmlProperty(localName = "genrenm")
    private String genre;

    @JacksonXmlProperty(localName = "openrun")
    private String openRun;

    @JacksonXmlProperty(localName = "prfstate")
    private String state;

    @JacksonXmlProperty(localName = "pcseguidance")
    private String ticketPrice;

    @JacksonXmlProperty(localName = "poster")
    private String posterUrl;
}