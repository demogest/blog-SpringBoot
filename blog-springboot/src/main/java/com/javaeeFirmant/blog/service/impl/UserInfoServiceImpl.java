package com.javaeeFirmant.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.dao.UserInfoDao;
import com.javaeeFirmant.blog.dto.UserDetailDTO;
import com.javaeeFirmant.blog.dto.UserOnlineDTO;
import com.javaeeFirmant.blog.entity.UserInfo;
import com.javaeeFirmant.blog.entity.UserRole;
import com.javaeeFirmant.blog.enums.FilePathEnum;
import com.javaeeFirmant.blog.service.UserInfoService;
import com.javaeeFirmant.blog.service.UserRoleService;
import com.javaeeFirmant.blog.strategy.context.UploadStrategyContext;
import com.javaeeFirmant.blog.util.UserUtils;
import com.javaeeFirmant.blog.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.javaeeFirmant.blog.util.PageUtils.getLimitCurrent;
import static com.javaeeFirmant.blog.util.PageUtils.getSize;


/**
 * 用户信息服务 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoDao, UserInfo> implements UserInfoService {
    private UserInfoDao userInfoDao;
    private UserRoleService userRoleService;
    private SessionRegistry sessionRegistry;
    private UploadStrategyContext uploadStrategyContext;

    @Autowired
    public UserInfoServiceImpl(UserInfoDao userInfoDao, UserRoleService userRoleService, SessionRegistry sessionRegistry, UploadStrategyContext uploadStrategyContext) {
        this.userInfoDao = userInfoDao;
        this.userRoleService = userRoleService;
        this.sessionRegistry = sessionRegistry;
        this.uploadStrategyContext = uploadStrategyContext;
    }


    /**
     * 更新用户信息
     *
     * @param userInfoVO 用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserInfo(UserInfoVO userInfoVO) {
        // 封装用户信息
        UserInfo userInfo = UserInfo.builder()
                .id(UserUtils.getLoginUser().getUserInfoId())
                .nickname(userInfoVO.getNickname())
                .intro(userInfoVO.getIntro())
                .webSite(userInfoVO.getWebSite())
                .build();
        userInfoDao.updateById(userInfo);
    }

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String updateUserAvatar(MultipartFile file) {
        // 头像上传
        String avatar = uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.AVATAR.getPath());
        // 更新用户信息
        UserInfo userInfo = UserInfo.builder()
                .id(UserUtils.getLoginUser().getUserInfoId())
                .avatar(avatar)
                .build();
        userInfoDao.updateById(userInfo);
        return avatar;
    }

    /**
     * 更新用户角色
     *
     * @param userRoleVO 更新用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserRole(UserRoleVO userRoleVO) {
        // 更新用户角色和昵称
        UserInfo userInfo = UserInfo.builder()
                .id(userRoleVO.getUserInfoId())
                .nickname(userRoleVO.getNickname())
                .build();
        userInfoDao.updateById(userInfo);
        // 删除用户角色重新添加
        userRoleService.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userRoleVO.getUserInfoId()));
        List<UserRole> userRoleList = userRoleVO.getRoleIdList().stream()
                .map(roleId -> UserRole.builder()
                        .roleId(roleId)
                        .userId(userRoleVO.getUserInfoId())
                        .build())
                .collect(Collectors.toList());
        userRoleService.saveBatch(userRoleList);
    }

    /**
     * 更新用户禁用状态
     *
     * @param userDisableVO 用户禁用信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserDisable(UserDisableVO userDisableVO) {
        // 更新用户禁用状态
        UserInfo userInfo = UserInfo.builder()
                .id(userDisableVO.getId())
                .isDisable(userDisableVO.getIsDisable())
                .build();
        userInfoDao.updateById(userInfo);
    }

    /**
     * 获取在线用户
     *
     * @param conditionVO 条件
     * @return 在线用户列表
     */
    @Override
    public PageResult<UserOnlineDTO> listOnlineUsers(ConditionVO conditionVO) {
        // 获取security在线session
        List<UserOnlineDTO> userOnlineDTOList = sessionRegistry.getAllPrincipals().stream()
                .filter(item -> sessionRegistry.getAllSessions(item, false).size() > 0)
                .map(item -> JSON.parseObject(JSON.toJSONString(item), UserOnlineDTO.class))
                .filter(item -> StringUtils.isBlank(conditionVO.getKeywords()) || item.getNickname().contains(conditionVO.getKeywords()))
                .sorted(Comparator.comparing(UserOnlineDTO::getLastLoginTime).reversed())
                .collect(Collectors.toList());
        // 执行分页
        int fromIndex = getLimitCurrent().intValue();
        int size = getSize().intValue();
        int toIndex = userOnlineDTOList.size() - fromIndex > size ? fromIndex + size : userOnlineDTOList.size();
        List<UserOnlineDTO> userOnlineList = userOnlineDTOList.subList(fromIndex, toIndex);
        return new PageResult<>(userOnlineList, userOnlineDTOList.size());
    }

    /**
     * 强制用户下线
     *
     * @param userInfoId 用户信息id
     */
    @Override
    public void removeOnlineUser(Integer userInfoId) {
        // 获取用户session
        List<Object> userInfoList = sessionRegistry.getAllPrincipals().stream().filter(item -> {
            UserDetailDTO userDetailDTO = (UserDetailDTO) item;
            return userDetailDTO.getUserInfoId().equals(userInfoId);
        }).collect(Collectors.toList());
        List<SessionInformation> allSessions = new ArrayList<>();
        userInfoList.forEach(item -> allSessions.addAll(sessionRegistry.getAllSessions(item, false)));
        // 注销session
        allSessions.forEach(SessionInformation::expireNow);
    }

    /**
     * 删除用户
     *
     * @param userInfoId 用户信息id
     */
    @Override
    public void deleteUser(Integer userInfoId) {
        // 删除用户
        userInfoDao.deleteById(userInfoId);
    }

}
