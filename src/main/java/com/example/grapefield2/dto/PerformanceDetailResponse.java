package com.example.grapefield2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JacksonXmlRootElement(localName = "dbs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerformanceDetailResponse {
    @JacksonXmlProperty(localName = "db")
    private PerformanceDetailDto detailInfo;
}
