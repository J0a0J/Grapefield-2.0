package com.example.grapefield2.dto;

import com.example.grapefield2.entity.BoxOffice;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "boxofs")
public class BoxOfficeListResponse {

    @JacksonXmlProperty(localName = "boxof")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<BoxOfficeDto> boxOffices;
}
