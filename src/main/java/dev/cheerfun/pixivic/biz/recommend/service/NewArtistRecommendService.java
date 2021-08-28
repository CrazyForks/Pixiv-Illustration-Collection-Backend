package dev.cheerfun.pixivic.biz.recommend.service;

import dev.cheerfun.pixivic.biz.recommend.domain.URRec;
import dev.cheerfun.pixivic.biz.recommend.mapper.RecommendMapper;
import dev.cheerfun.pixivic.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author OysterQAQ
 * @version 1.0
 * @date 2021/8/27 11:11 下午
 * @description NewArtistRecommendService
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NewArtistRecommendService {

    private final RecommendSyncService recommendSyncService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RecommendMapper recommendMapper;

    public void dealPerUser(List<Integer> userList, Integer size) {
        userList.stream().parallel().forEach(e -> {
            try {
                List<URRec> urRecList = recommendSyncService.queryRecommendArtistByUser(e, size);
                //重置分数
                Set<ZSetOperations.TypedTuple<String>> typedTuples = urRecList.stream()
                        //过滤已经收藏的
                        .filter(r -> Boolean.FALSE.equals(stringRedisTemplate.opsForSet().isMember(RedisKeyConstant.ARTIST_FOLLOW_REDIS_PRE + r.getItem(), e)))
                        .map(recommendedItem -> {
                                    Double score = stringRedisTemplate.opsForZSet().score(RedisKeyConstant.USER_RECOMMEND_ARTIST + e, String.valueOf(recommendedItem.getItem()));
                                    if (score == null) {
                                        score = (double) recommendedItem.getScore();
                                    }
                                    return new DefaultTypedTuple<>(String.valueOf(recommendedItem.getItem()), score);
                                }
                        ).collect(Collectors.toSet());
                if (typedTuples.size() > 0) {
                    //清空
                    stringRedisTemplate.delete(RedisKeyConstant.USER_RECOMMEND_ARTIST + e);
                    //新增
                    stringRedisTemplate.opsForZSet().add(RedisKeyConstant.USER_RECOMMEND_ARTIST + e, typedTuples);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        });
    }
}
