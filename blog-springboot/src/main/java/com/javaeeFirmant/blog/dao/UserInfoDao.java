package com.javaeeFirmant.blog.dao;

import com.javaeeFirmant.blog.entity.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;


/**
 * 用户信息 */
@Repository
public interface UserInfoDao extends BaseMapper<UserInfo> {

}
