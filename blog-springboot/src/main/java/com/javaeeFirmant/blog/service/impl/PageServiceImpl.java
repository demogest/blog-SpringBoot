package com.javaeeFirmant.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.constant.RedisPrefixConst;
import com.javaeeFirmant.blog.dao.PageDao;
import com.javaeeFirmant.blog.vo.PageVO;
import com.javaeeFirmant.blog.entity.Page;
import com.javaeeFirmant.blog.service.PageService;
import com.javaeeFirmant.blog.service.RedisService;
import com.javaeeFirmant.blog.util.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Objects;

/**
 * 页面服务 */
@Service
public class PageServiceImpl extends ServiceImpl<PageDao, Page> implements PageService {
    private RedisService redisService;
    private PageDao pageDao;

    @Autowired
    public PageServiceImpl(RedisService redisService, PageDao pageDao) {
        this.redisService = redisService;
        this.pageDao = pageDao;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdatePage(PageVO pageVO) {
        Page page = BeanCopyUtils.copyObject(pageVO, Page.class);
        this.saveOrUpdate(page);
        // 删除缓存
        redisService.del(RedisPrefixConst.PAGE_COVER);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePage(Integer pageId) {
        pageDao.deleteById(pageId);
        // 删除缓存
        redisService.del(RedisPrefixConst.PAGE_COVER);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<PageVO> listPages() {
        List<PageVO> pageVOList;
        // 查找缓存信息，不存在则从mysql读取，更新缓存
        Object pageList = redisService.get(RedisPrefixConst.PAGE_COVER);
        if (Objects.nonNull(pageList)) {
            pageVOList = JSON.parseObject(pageList.toString(), List.class);
        } else {
            pageVOList = BeanCopyUtils.copyList(pageDao.selectList(null), PageVO.class);
            redisService.set(RedisPrefixConst.PAGE_COVER, JSON.toJSONString(pageVOList));
        }
        return pageVOList;
    }

}




