package com.javaeeFirmant.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.javaeeFirmant.blog.constant.CommonConst;
import com.javaeeFirmant.blog.constant.RedisPrefixConst;
import com.javaeeFirmant.blog.dto.*;
import com.javaeeFirmant.blog.vo.*;
import com.javaeeFirmant.blog.entity.Comment;
import com.javaeeFirmant.blog.dao.CommentDao;
import com.javaeeFirmant.blog.service.BlogInfoService;
import com.javaeeFirmant.blog.service.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.service.RedisService;
import com.javaeeFirmant.blog.util.HTMLUtils;
import com.javaeeFirmant.blog.util.PageUtils;
import com.javaeeFirmant.blog.util.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentDao, Comment> implements CommentService {
    @Autowired
    private CommentDao commentDao;
    @Autowired
    private RedisService redisService;
    @Autowired
    private BlogInfoService blogInfoService;

    /**
     * 网站网址
     */
    @Value("${website.url}")
    private String websiteUrl;

    @Override
    public PageResult<CommentDTO> listComments(CommentVO commentVO) {
        // 查询评论量
        Integer commentCount = commentDao.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Objects.nonNull(commentVO.getTopicId()), Comment::getTopicId, commentVO.getTopicId())
                .eq(Comment::getType, commentVO.getType())
                .isNull(Comment::getParentId)
                .eq(Comment::getIsReview, CommonConst.TRUE));
        if (commentCount == 0) {
            return new PageResult<>();
        }
        // 分页查询评论数据
        List<CommentDTO> commentDTOList = commentDao.listComments(PageUtils.getLimitCurrent(), PageUtils.getSize(), commentVO);
        if (CollectionUtils.isEmpty(commentDTOList)) {
            return new PageResult<>();
        }
        // 查询redis的评论点赞数据
        Map<String, Object> likeCountMap = redisService.hGetAll(RedisPrefixConst.COMMENT_LIKE_COUNT);
        // 提取评论id集合
        List<Integer> commentIdList = commentDTOList.stream()
                .map(CommentDTO::getId)
                .collect(Collectors.toList());
        // 根据评论id集合查询回复数据
        List<ReplyDTO> replyDTOList = commentDao.listReplies(commentIdList);
        // 封装回复点赞量
        replyDTOList.forEach(item -> item.setLikeCount((Integer) likeCountMap.get(item.getId().toString())));
        // 根据评论id分组回复数据
        Map<Integer, List<ReplyDTO>> replyMap = replyDTOList.stream()
                .collect(Collectors.groupingBy(ReplyDTO::getParentId));
        // 根据评论id查询回复量
        Map<Integer, Integer> replyCountMap = commentDao.listReplyCountByCommentId(commentIdList)
                .stream().collect(Collectors.toMap(ReplyCountDTO::getCommentId, ReplyCountDTO::getReplyCount));
        // 封装评论数据
        commentDTOList.forEach(item -> {
            item.setLikeCount((Integer) likeCountMap.get(item.getId().toString()));
            item.setReplyDTOList(replyMap.get(item.getId()));
            item.setReplyCount(replyCountMap.get(item.getId()));
        });
        return new PageResult<>(commentDTOList, commentCount);
    }

    @Override
    public List<ReplyDTO> listRepliesByCommentId(Integer commentId) {
        // 转换页码查询评论下的回复
        List<ReplyDTO> replyDTOList = commentDao.listRepliesByCommentId(PageUtils.getLimitCurrent(), PageUtils.getSize(), commentId);
        // 查询redis的评论点赞数据
        Map<String, Object> likeCountMap = redisService.hGetAll(RedisPrefixConst.COMMENT_LIKE_COUNT);
        // 封装点赞数据
        replyDTOList.forEach(item -> item.setLikeCount((Integer) likeCountMap.get(item.getId().toString())));
        return replyDTOList;
    }

    @Override
    public void saveComment(CommentVO commentVO) {
        // 判断是否需要审核
        WebsiteConfigVO websiteConfig = blogInfoService.getWebsiteConfig();
        Integer isReview = websiteConfig.getIsCommentReview();
        // 过滤标签
        commentVO.setCommentContent(HTMLUtils.filter(commentVO.getCommentContent()));
        Comment comment = Comment.builder()
                .userId(UserUtils.getLoginUser().getUserInfoId())
                .replyUserId(commentVO.getReplyUserId())
                .topicId(commentVO.getTopicId())
                .commentContent(commentVO.getCommentContent())
                .parentId(commentVO.getParentId())
                .type(commentVO.getType())
                .isReview(isReview == CommonConst.TRUE ? CommonConst.FALSE : CommonConst.TRUE)
                .build();
        commentDao.insert(comment);
    }

    @Override
    public void saveCommentLike(Integer commentId) {
        // 判断是否点赞
        String commentLikeKey = RedisPrefixConst.COMMENT_USER_LIKE + UserUtils.getLoginUser().getUserInfoId();
        if (redisService.sIsMember(commentLikeKey, commentId)) {
            // 点过赞则删除评论id
            redisService.sRemove(commentLikeKey, commentId);
            // 评论点赞量-1
            redisService.hDecr(RedisPrefixConst.COMMENT_LIKE_COUNT, commentId.toString(), 1L);
        } else {
            // 未点赞则增加评论id
            redisService.sAdd(commentLikeKey, commentId);
            // 评论点赞量+1
            redisService.hIncr(RedisPrefixConst.COMMENT_LIKE_COUNT, commentId.toString(), 1L);
        }
    }

    @Override
    public void updateCommentsReview(ReviewVO reviewVO) {
        // 修改评论审核状态
        List<Comment> commentList = reviewVO.getIdList().stream().map(item -> Comment.builder()
                        .id(item)
                        .isReview(reviewVO.getIsReview())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(commentList);
    }

    @Override
    public PageResult<CommentBackDTO> listCommentBackDTO(ConditionVO condition) {
        // 统计后台评论量
        Integer count = commentDao.countCommentDTO(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询后台评论集合
        List<CommentBackDTO> commentBackDTOList = commentDao.listCommentBackDTO(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        return new PageResult<>(commentBackDTOList, count);
    }

}
