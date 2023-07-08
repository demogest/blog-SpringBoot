package com.javaeeFirmant.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.dao.UserRoleDao;
import com.javaeeFirmant.blog.entity.UserRole;
import com.javaeeFirmant.blog.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 用户角色服务 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRole> implements UserRoleService {

    private final UserRoleDao userRoleDao;

    @Autowired
    public UserRoleServiceImpl (UserRoleDao userRoleDao) {
        this.userRoleDao = userRoleDao;
    }

    /**
     * 根据用户信息id删除用户角色关联信息
     * @param userInfoId 用户信息id
     */
    @Override
    public void removeByUserInfoId(Integer userInfoId) {
        userRoleDao.deleteById(userInfoId);
    }
}
