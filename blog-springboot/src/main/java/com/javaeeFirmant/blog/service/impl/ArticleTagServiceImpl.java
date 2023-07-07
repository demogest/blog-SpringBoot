package com.javaeeFirmant.blog.service.impl;

import com.javaeeFirmant.blog.entity.ArticleTag;
import com.javaeeFirmant.blog.dao.ArticleTagDao;
import com.javaeeFirmant.blog.service.ArticleTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 文章标签服务 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagDao, ArticleTag> implements ArticleTagService {

}
