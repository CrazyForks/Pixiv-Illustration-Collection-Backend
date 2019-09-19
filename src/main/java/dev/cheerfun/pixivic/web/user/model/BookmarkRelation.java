package dev.cheerfun.pixivic.web.user.model;

import lombok.Data;

/**
 * @author OysterQAQ
 * @version 1.0
 * @date 2019/09/19 9:29
 * @description Bookmark
 */
@Data
public class BookmarkRelation {
    private int userId;
    private int illustId;
}
