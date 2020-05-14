package dev.cheerfun.pixivic.biz.web.collection.po;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

/**
 * @author OysterQAQ
 * @version 1.0
 * @date 2020/4/29 5:24 下午
 * @description CollectionTag
 */
@Data
public class CollectionTag {
    @JsonSetter("tag_id")
    private Integer id;
    @JsonSetter("tag_name")
    private String tagName;
}
