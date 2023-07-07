package com.javaeeFirmant.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javaeeFirmant.blog.entity.UserRole;

/**
 * 用户角色服务
 */
public interface UserRoleService extends IService<UserRole> {

    /**
     * 删除用户角色
     *
     * @param userInfoId 用户信息id
     */
    void removeByUserInfoId(Integer userInfoId);

}
