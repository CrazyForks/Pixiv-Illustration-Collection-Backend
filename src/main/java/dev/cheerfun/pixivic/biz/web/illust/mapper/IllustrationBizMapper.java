package dev.cheerfun.pixivic.biz.web.illust.mapper;

import dev.cheerfun.pixivic.biz.web.illust.po.IllustRelated;
import dev.cheerfun.pixivic.biz.web.user.dto.UserListDTO;
import dev.cheerfun.pixivic.common.po.Illustration;
import dev.cheerfun.pixivic.common.po.illust.ArtistPreView;
import dev.cheerfun.pixivic.common.util.json.JsonTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IllustrationBizMapper {
    @Select("select * from illusts where illust_id = #{illustId}")
    @Results({
            @Result(property = "id", column = "illust_id"),
            @Result(property = "artistPreView", column = "artist", javaType = ArtistPreView.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "tools", column = "tools", javaType = List.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "tags", column = "tags", javaType = List.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "imageUrls", column = "image_urls", javaType = List.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "tags", column = "tags", javaType = List.class, typeHandler = JsonTypeHandler.class)
    })
    Illustration queryIllustrationByIllustId(Integer illustId);

    @Select("  SELECT\n" +
            "          * \n" +
            "     FROM\n" +
            "         `illusts` AS t1\n" +
            "    JOIN (\n" +
            "             SELECT\n" +
            "                   ROUND(\n" +
            "                          RAND( ) * (\n" +
            "                           ( SELECT MIN( illust_id ) FROM (SELECT illust_id FROM `illusts` WHERE illust_id > 1000 AND total_bookmarks > 1000 ORDER BY illust_id desc LIMIT 100) tt ) - ( SELECT MIN( illust_id ) FROM `illusts` WHERE illust_id > 1000 ) \n" +
            "                           ) + ( SELECT MIN( illust_id ) FROM `illusts` WHERE illust_id > 1000 ) \n" +
            "                      ) AS illust_id \n" +
            "         ) AS t2 \n" +
            "WHERE\n" +
            "   t1.illust_id >= t2.illust_id \n" +
            "ORDER BY\n" +
            "    t1.illust_id \n" +
            "LIMIT 2000")
    @Results({
            @Result(property = "id", column = "illust_id"),
            @Result(property = "artistPreView", column = "artist", javaType = ArtistPreView.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "tools", column = "tools", javaType = List.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "tags", column = "tags", javaType = List.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "imageUrls", column = "image_urls", javaType = List.class, typeHandler = JsonTypeHandler.class),
            @Result(property = "tags", column = "tags", javaType = List.class, typeHandler = JsonTypeHandler.class)
    })
    List<Illustration> queryRandomIllustration();

    @Insert({
            "<script>",
            "replace into illust_related (`illust_id`, `related_illust_id`, `order_num`) values ",
            "<foreach collection='illustRelatedList' item='illustRelated' index='index' separator=','>",
            "(#{illustRelated.illustId}, #{illustRelated.relatedIllustId}, #{illustRelated.orderNum})",
            "</foreach>",
            "</script>"
    })
    int insertIllustRelated(@Param("illustRelatedList") List<IllustRelated> illustRelatedList);

    @Select("select user_id,username,create_date from user_illust_bookmarked where illust_id=#{illustId} order by id desc  limit #{currIndex} , #{pageSize}")
    @Results({
            @Result(property = "illustId", column = "illust_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "createDate", column = "create_Date", typeHandler = org.apache.ibatis.type.LocalDateTimeTypeHandler.class)
    })
    List<UserListDTO> queryUserListBookmarkedIllust(Integer illustId, int currIndex, int pageSize);
}
