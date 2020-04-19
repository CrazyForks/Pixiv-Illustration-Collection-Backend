package dev.cheerfun.pixivic.biz.web.artist.service;

import dev.cheerfun.pixivic.biz.crawler.pixiv.service.ArtistService;
import dev.cheerfun.pixivic.biz.userInfo.dto.ArtistWithIsFollowedInfo;
import dev.cheerfun.pixivic.biz.web.artist.dto.ArtistSearchDTO;
import dev.cheerfun.pixivic.biz.web.artist.mapper.ArtistBizMapper;
import dev.cheerfun.pixivic.biz.web.artist.util.ArtistSearchUtil;
import dev.cheerfun.pixivic.biz.web.common.exception.BusinessException;
import dev.cheerfun.pixivic.biz.web.illust.service.IllustrationBizService;
import dev.cheerfun.pixivic.biz.web.user.dto.ArtistWithRecentlyIllusts;
import dev.cheerfun.pixivic.biz.web.user.dto.UserListDTO;
import dev.cheerfun.pixivic.common.constant.AuthConstant;
import dev.cheerfun.pixivic.common.constant.RedisKeyConstant;
import dev.cheerfun.pixivic.common.context.AppContext;
import dev.cheerfun.pixivic.common.po.Artist;
import dev.cheerfun.pixivic.common.po.ArtistSummary;
import dev.cheerfun.pixivic.common.po.Illustration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author OysterQAQ
 * @version 1.0
 * @date 2020/4/19 10:46 上午
 * @description ArtistBizService
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ArtistBizService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ArtistBizMapper artistBizMapper;
    private final ArtistService artistService;
    private final IllustrationBizService illustrationBizService;
    private final ArtistSearchUtil artistSearchUtil;
    private volatile String today;
    private volatile String yesterday;

    {
        LocalDate now = LocalDate.now();
        today = now.toString();
        yesterday = now.plusDays(-1).toString();
    }

    @Scheduled(cron = "0 1 0 * * ?")
    public void clearArtistLatestIllustsMap() {
        stringRedisTemplate.delete(RedisKeyConstant.ARTIST_LATEST_ILLUSTS_PULL_FLAG + yesterday);
        yesterday = today;
        today = LocalDate.now().toString();
    }

    public Artist queryArtistDetail(Integer artistId) {
        Artist artist = queryArtistById(artistId);
        dealArtist(artist);
        Map<String, Object> context = AppContext.get();
        if (context != null && context.get(AuthConstant.USER_ID) != null) {
            int userId = (int) context.get(AuthConstant.USER_ID);
            Boolean isFollowed = stringRedisTemplate.opsForSet().isMember(RedisKeyConstant.ARTIST_FOLLOW_REDIS_PRE + artistId, String.valueOf(userId));
            return new ArtistWithIsFollowedInfo(artist, isFollowed);
        }
        return artist;
    }

    public Artist queryArtistDetail(Integer artistId, Integer userId) {
        Artist artist = queryArtistById(artistId);
        dealArtist(artist);
        if (userId != null) {
            Boolean isFollowed = stringRedisTemplate.opsForSet().isMember(RedisKeyConstant.ARTIST_FOLLOW_REDIS_PRE + artistId, String.valueOf(userId));
            return new ArtistWithIsFollowedInfo(artist, isFollowed);
        }
        return artist;
    }

    public void dealArtist(Artist artist) {
        //更改关注数
        artist.setTotalFollowUsers(String.valueOf(stringRedisTemplate.opsForSet().size(RedisKeyConstant.ARTIST_FOLLOW_REDIS_PRE + artist.getId())));
    }

    @Cacheable(value = "artist")
    public Artist queryArtistById(Integer artistId) {
        Artist artist = artistBizMapper.queryArtistById(artistId);
        if (artist == null) {
            artist = artistService.pullArtistsInfo(artistId);
            if (artist == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "画师不存在");
            }
        }

        return artist;
    }

    @Cacheable("artist_followed")
    public List<UserListDTO> queryUserListFollowedArtist(Integer artistId, Integer page, Integer pageSize) {
        return artistBizMapper.queryUserListFollowedArtist(artistId, (page - 1) * pageSize, pageSize);
    }

    @Cacheable(value = "artist_illusts")
    public List<Illustration> queryIllustrationsByArtistId(Integer artistId, String type, int currIndex, int pageSize) {
        //如果是近日首次则进行拉取
        String key = artistId + ":" + type;
        Boolean todayCheck = stringRedisTemplate.opsForSet().isMember(RedisKeyConstant.ARTIST_LATEST_ILLUSTS_PULL_FLAG + today, key);
        Boolean yesterdayCheck = stringRedisTemplate.opsForSet().isMember(RedisKeyConstant.ARTIST_LATEST_ILLUSTS_PULL_FLAG + yesterday, key);
        if (currIndex == 0 && pageSize == 30 && !(todayCheck || yesterdayCheck)) {
            System.out.println("近日首次，将从Pixiv拉取");
            stringRedisTemplate.opsForSet().add(RedisKeyConstant.ARTIST_LATEST_ILLUSTS_PULL_FLAG + today, key);
            artistService.pullArtistLatestIllust(artistId, type);
        }
        List<Illustration> illustrations = artistBizMapper.queryIllustrationsByArtistId(artistId, type, currIndex, pageSize);
        return illustrations;
    }

    @Cacheable(value = "artistSummarys")
    public ArtistSummary querySummaryByArtistId(Integer artistId) {
        return artistBizMapper.querySummaryByArtistId(artistId);
    }

    public CompletableFuture<List<Artist>> queryArtistByName(String artistName, Integer page, Integer pageSize) {
        Map<String, Object> context = AppContext.get();
        Integer userId = null;
        if (context != null && context.get(AuthConstant.USER_ID) != null) {
            userId = (int) context.get(AuthConstant.USER_ID);
        }
        CompletableFuture<List<ArtistSearchDTO>> request = artistSearchUtil.search(artistName, page, pageSize);
        Integer finalUserId = userId;
        return request.thenApply(e -> e.stream().parallel().map(artistSearchDTO ->
        {
            List<Illustration> illustrations = queryIllustrationsByArtistId(artistSearchDTO.getId(), "illust", 0, 3);
            return new ArtistWithRecentlyIllusts(queryArtistDetail(artistSearchDTO.getId(), finalUserId), illustrations);

        }).collect(Collectors.toList()));
    }
}
