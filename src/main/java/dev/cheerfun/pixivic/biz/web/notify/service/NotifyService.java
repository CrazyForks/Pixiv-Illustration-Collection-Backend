package dev.cheerfun.pixivic.biz.web.notify.service;

import dev.cheerfun.pixivic.biz.notify.po.NotifyRemind;
import dev.cheerfun.pixivic.biz.notify.service.NotifyRemindService;
import dev.cheerfun.pixivic.biz.web.notify.mapper.NotifyBIZMapper;
import dev.cheerfun.pixivic.biz.web.notify.po.NotifyRemindSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * @author OysterQAQ
 * @version 1.0
 * @date 2020/10/9 8:15 PM
 * @description NotifyService
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NotifyService {
    private final NotifyBIZMapper notifyMapper;
    private final NotifyRemindService notifyRemindService;

    @Transactional
    public List<NotifyRemind> queryRemind(int userId, Integer type, long offset, int pageSize) {
        List<Integer> remindIdList = notifyMapper.queryRemind(userId, type, offset, pageSize);
        List<NotifyRemind> notifyReminds = notifyRemindService.queryRemindList(remindIdList);
        updateRemindSummary(userId, type);
        //将id加入移步刷新队列
        return notifyReminds;
    }

    public Integer queryRemindCount(int userId, Integer type) {
        Optional<NotifyRemindSummary> any = queryRemindSummary(userId).stream().filter(e -> type.compareTo(e.getType()) == 0).findAny();
        if (any.isPresent()) {
            return any.get().getTotal();
        }
        return 0;
    }

    @CacheEvict("remind")
    public void readRemind(Integer remindId) {
        notifyMapper.readRemind(remindId);
    }

    @CacheEvict(value = "remindSummary", key = "#userId")
    public void updateRemindSummary(Integer userId, Integer type) {
        notifyMapper.updateRemindSummary(userId, type);

    }

    @Cacheable("remindSummary")
    public List<NotifyRemindSummary> queryRemindSummary(int userId) {
        return notifyMapper.queryRemindSummary(userId);
    }

    public Integer queryUnreadRemindsCount(int userId) {
        return queryRemindSummary(userId).stream().mapToInt(NotifyRemindSummary::getUnread).sum();
    }
}
