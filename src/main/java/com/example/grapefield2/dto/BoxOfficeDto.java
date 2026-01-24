package com.example.grapefield2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxOfficeDto {
    @JacksonXmlProperty(localName = "rnum")
    private Integer rnum;

    @JacksonXmlProperty(localName = "mt20id")
    private String performanceId;

    private LocalDateTime updatedAt;
}
