package com.javaeeFirmant.blog.service;

import com.javaeeFirmant.blog.dto.UserAreaDTO;
import com.javaeeFirmant.blog.vo.*;
import com.javaeeFirmant.blog.dto.UserInfoDTO;
import com.javaeeFirmant.blog.dto.UserBackDTO;
import com.javaeeFirmant.blog.entity.UserAuth;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * 用户账号服务
 */
public interface UserAuthService extends IService<UserAuth> {


    /**
     * 获取用户区域分布
     *
     * @param conditionVO 条件签证官
     * @return {@link List< UserAreaDTO >} 用户区域分布
     */
    List<UserAreaDTO> listUserAreas(ConditionVO conditionVO);

    /**
     * 用户注册
     *
     * @param user 用户对象
     */
    void register(UserVO user);

    /**
     * 修改密码
     *
     * @param passwordVO 密码对象
     */
    void updatePassword(PasswordVO passwordVO, Integer userInfoId);

    /**
     * 修改管理员密码
     *
     * @param passwordVO 密码对象
     */
    void updateAdminPassword(PasswordVO passwordVO);

    /**
     * 查询后台用户列表
     *
     * @param condition 条件
     * @return 用户列表
     */
    PageResult<UserBackDTO> listUserBackDTO(ConditionVO condition);

    /**
     * 删除用户
     *
     * @param userInfoId 用户信息id
     */
    void deleteUser(Integer userInfoId);

    /**
     * 重置密码
     *
     * @param userInfoId 用户信息id
     */
    void resetPassword(Integer userInfoId);

}
