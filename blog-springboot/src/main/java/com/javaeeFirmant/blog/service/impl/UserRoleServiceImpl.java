package com.javaeeFirmant.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.dao.UserRoleDao;
import com.javaeeFirmant.blog.entity.UserRole;
import com.javaeeFirmant.blog.service.UserRoleService;
import org.springframework.stereotype.Service;


/**
 * 用户角色服务 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRole> implements UserRoleService {


    @Override
    public void removeByUserInfoId(Integer userInfoId) {

    }
}
