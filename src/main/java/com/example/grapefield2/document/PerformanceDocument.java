package com.example.grapefield2.document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Setting(settingPath="opensearch/performance-settings.json")
@Document(indexName = "performances")
@Getter
@Setter
@ToString
public class PerformanceDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String venue;

    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Text)
    private String cast;

    @Field(type = FieldType.Text)
    private String producer;

    @Field(type = FieldType.Keyword)
    private String startDate;

    @Field(type = FieldType.Keyword)
    private String endDate;

    @Field(type = FieldType.Keyword)
    private String state;

    public PerformanceDocument() {}

    public PerformanceDocument(String id, String title, String venue, String genre,
                               String cast, String producer, String startDate, String endDate, String state) {
        this.id = id;
        this.title = title;
        this.venue = venue;
        this.genre = genre;
        this.cast = cast;
        this.producer = producer;
        this.startDate = startDate;
        this.endDate = endDate;
        this.state = state;
    }
}
