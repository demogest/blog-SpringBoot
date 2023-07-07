package com.javaeeFirmant.blog.controller;


import com.javaeeFirmant.blog.annotation.OptLog;
import com.javaeeFirmant.blog.dto.UserAreaDTO;
import com.javaeeFirmant.blog.dto.UserBackDTO;
import com.javaeeFirmant.blog.service.UserAuthService;
import com.javaeeFirmant.blog.service.UserInfoService;
import com.javaeeFirmant.blog.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.javaeeFirmant.blog.constant.OptTypeConst.REMOVE;
import static com.javaeeFirmant.blog.constant.OptTypeConst.UPDATE;

/**
 * 用户账号控制器
 */
@Api(tags = "用户账号模块")
@RestController
public class UserAuthController {
    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 获取用户区域分布
     *
     * @param conditionVO 条件
     * @return {@link Result< UserAreaDTO >} 用户区域分布
     */
    @ApiOperation(value = "获取用户区域分布")
    @GetMapping("/admin/users/area")
    public Result<List<UserAreaDTO>> listUserAreas(ConditionVO conditionVO) {
        return Result.ok(userAuthService.listUserAreas(conditionVO));
    }

    /**
     * 查询后台用户列表
     *
     * @param condition 条件
     * @return {@link Result<UserBackDTO>} 用户列表
     */
    @ApiOperation(value = "查询后台用户列表")
    @GetMapping("/admin/users")
    public Result<PageResult<UserBackDTO>> listUsers(ConditionVO condition) {
        return Result.ok(userAuthService.listUserBackDTO(condition));
    }

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return {@link Result<>}
     */
    @ApiOperation(value = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserVO user) {
        userAuthService.register(user);
        return Result.ok();
    }

    /**
     * 修改密码
     *
     * @param passwordVO 密码信息
     * @param userId     用户id
     * @return {@link Result<>}
     */
    @ApiOperation(value = "修改密码")
    @PutMapping("/users/password")
    public Result<?> updatePassword(@Valid @RequestBody PasswordVO passwordVO,
                                    @RequestParam("userId") Integer userId) {
        userAuthService.updatePassword(passwordVO, userId);
        return Result.ok();
    }

    /**
     * 修改管理员密码
     *
     * @param passwordVO 密码信息
     * @return {@link Result<>}
     */
    @ApiOperation(value = "修改管理员密码")
    @PutMapping("/admin/users/password")
    public Result<?> updateAdminPassword(@Valid @RequestBody PasswordVO passwordVO) {
        userAuthService.updateAdminPassword(passwordVO);
        return Result.ok();
    }

    /**
     * 重置用户密码
     *
     * @param userInfoId 用户信息id
     * @return {@link Result<>}
     */
    @OptLog(optType = UPDATE)
    @ApiOperation(value = "重置用户密码")
    @PutMapping("/admin/users/password/reset/{userInfoId}")
    public Result<?> resetUserPassword(@PathVariable("userInfoId") Integer userInfoId) {
        userAuthService.resetPassword(userInfoId);
        return Result.ok();
    }

    /**
     * 删除用户
     *
     * @param userId 用户id
     * @return {@link Result<>}
     */
    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除用户")
    @DeleteMapping("/admin/users/delete/{userId}")
    public Result<?> deleteUser(@PathVariable("userId") Integer userId) {
        userAuthService.deleteUser(userId);
        userInfoService.deleteUser(userId);
        return Result.ok();
    }
}

