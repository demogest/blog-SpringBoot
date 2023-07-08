package com.javaeeFirmant.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.constant.CommonConst;
import com.javaeeFirmant.blog.constant.RedisPrefixConst;
import com.javaeeFirmant.blog.dao.UserAuthDao;
import com.javaeeFirmant.blog.dao.UserInfoDao;
import com.javaeeFirmant.blog.dao.UserRoleDao;
import com.javaeeFirmant.blog.dto.UserAreaDTO;
import com.javaeeFirmant.blog.dto.UserBackDTO;
import com.javaeeFirmant.blog.dto.UserDetailDTO;
import com.javaeeFirmant.blog.entity.UserAuth;
import com.javaeeFirmant.blog.entity.UserInfo;
import com.javaeeFirmant.blog.entity.UserRole;
import com.javaeeFirmant.blog.enums.LoginTypeEnum;
import com.javaeeFirmant.blog.enums.RoleEnum;
import com.javaeeFirmant.blog.exception.BizException;
import com.javaeeFirmant.blog.service.BlogInfoService;
import com.javaeeFirmant.blog.service.RedisService;
import com.javaeeFirmant.blog.service.UserAuthService;
import com.javaeeFirmant.blog.util.PageUtils;
import com.javaeeFirmant.blog.util.UserUtils;
import com.javaeeFirmant.blog.vo.ConditionVO;
import com.javaeeFirmant.blog.vo.PageResult;
import com.javaeeFirmant.blog.vo.PasswordVO;
import com.javaeeFirmant.blog.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.javaeeFirmant.blog.enums.UserAreaTypeEnum.getUserAreaType;


/**
 * 用户账号服务 */
@Service
public class UserAuthServiceImpl extends ServiceImpl<UserAuthDao, UserAuth> implements UserAuthService {
    private RedisService redisService;
    private UserAuthDao userAuthDao;
    private UserRoleDao userRoleDao;
    private UserInfoDao userInfoDao;
    private BlogInfoService blogInfoService;

    @Autowired
    public UserAuthServiceImpl(UserAuthDao userAuthDao, UserRoleDao userRoleDao, UserInfoDao userInfoDao, RedisService redisService, BlogInfoService blogInfoService) {
        this.userAuthDao = userAuthDao;
        this.userRoleDao = userRoleDao;
        this.userInfoDao = userInfoDao;
        this.redisService = redisService;
        this.blogInfoService = blogInfoService;
    }

    @Override
    public List<UserAreaDTO> listUserAreas(ConditionVO conditionVO) {
        List<UserAreaDTO> userAreaDTOList = new ArrayList<>();
        switch (Objects.requireNonNull(getUserAreaType(conditionVO.getType()))) {
            case USER:
                // 查询注册用户区域分布
                Object userArea = redisService.get(RedisPrefixConst.USER_AREA);
                if (Objects.nonNull(userArea)) {
                    userAreaDTOList = JSON.parseObject(userArea.toString(), List.class);
                }
                return userAreaDTOList;
            case VISITOR:
                // 查询游客区域分布
                Map<String, Object> visitorArea = redisService.hGetAll(RedisPrefixConst.VISITOR_AREA);
                if (Objects.nonNull(visitorArea)) {
                    userAreaDTOList = visitorArea.entrySet().stream()
                            .map(item -> UserAreaDTO.builder()
                                    .name(item.getKey())
                                    .value(Long.valueOf(item.getValue().toString()))
                                    .build())
                            .collect(Collectors.toList());
                }
                return userAreaDTOList;
            default:
                break;
        }
        return userAreaDTOList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserVO user) {
        // 校验账号是否合法
        if (checkUser(user)) {
            throw new BizException("邮箱已被注册！");
        }
        // 新增用户信息
        UserInfo userInfo = UserInfo.builder()
                .email(user.getUsername())
                .nickname(CommonConst.DEFAULT_NICKNAME + IdWorker.getId())
                .avatar(blogInfoService.getWebsiteConfig().getUserAvatar())
                .build();
        userInfoDao.insert(userInfo);
        // 绑定用户角色
        UserRole userRole = UserRole.builder()
                .userId(userInfo.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleDao.insert(userRole);
        // 新增用户账号
        UserAuth userAuth = UserAuth.builder()
                .userInfoId(userInfo.getId())
                .username(user.getUsername())
                .password(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()))
                .loginType(LoginTypeEnum.EMAIL.getType())
                .build();
        userAuthDao.insert(userAuth);
    }

    @Override
    public void updatePassword(PasswordVO passwordVO, Integer userInfoId) {
        UserDetailDTO user = UserUtils.getLoginUser();
        // 校验身份
        if (!Objects.equals(user.getUserInfoId(), userInfoId)) {
            throw new BizException("无权限修改！");
        }
        // 查询旧密码是否正确
        UserAuth userAuth = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getId, user.getId()));
        // 正确则修改密码，错误则提示不正确
        if (Objects.nonNull(userAuth) && BCrypt.checkpw(passwordVO.getOldPassword(), userAuth.getPassword())) {
            userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                    .set(UserAuth::getPassword, BCrypt.hashpw(passwordVO.getNewPassword(), BCrypt.gensalt()))
                    .eq(UserAuth::getId, user.getId()));
        } else {
            throw new BizException("旧密码不正确！");
        }
//        // 根据用户名修改密码
//        userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
//                .set(UserAuth::getPassword, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()))
//                .eq(UserAuth::getUsername, user.getUsername()));
    }

    @Override
    public void updateAdminPassword(PasswordVO passwordVO) {
        // 查询旧密码是否正确
        UserAuth user = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getId, UserUtils.getLoginUser().getId()));
        // 正确则修改密码，错误则提示不正确
        if (Objects.nonNull(user) && BCrypt.checkpw(passwordVO.getOldPassword(), user.getPassword())) {
            UserAuth userAuth = UserAuth.builder()
                    .id(UserUtils.getLoginUser().getId())
                    .password(BCrypt.hashpw(passwordVO.getNewPassword(), BCrypt.gensalt()))
                    .build();
            userAuthDao.updateById(userAuth);
        } else {
            throw new BizException("旧密码不正确");
        }
    }

    @Override
    public PageResult<UserBackDTO> listUserBackDTO(ConditionVO condition) {
        // 获取后台用户数量
        Integer count = userAuthDao.countUser(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // 获取后台用户列表
        List<UserBackDTO> userBackDTOList = userAuthDao.listUsers(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        return new PageResult<>(userBackDTOList, count);
    }

    /**
     * 校验用户数据是否合法
     *
     * @param user 用户数据
     * @return 结果
     */
    private Boolean checkUser(UserVO user) {
        //查询用户名是否存在
        UserAuth userAuth = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getUsername)
                .eq(UserAuth::getUsername, user.getUsername()));
        return Objects.nonNull(userAuth);
    }
    private Boolean checkUser(UserDetailDTO user) {
        //查询用户名是否存在
        UserAuth userAuth = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getUserInfoId)
                .eq(UserAuth::getUserInfoId, user.getUserInfoId()));
        return Objects.nonNull(userAuth);
    }
    /**
     * 统计用户地区
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void statisticalUserArea() {
        // 统计用户地域分布
        Map<String, Long> userAreaMap = userAuthDao.selectList(new LambdaQueryWrapper<UserAuth>().select(UserAuth::getIpSource))
                .stream()
                .map(item -> {
                    if (StringUtils.isNotBlank(item.getIpSource())) {
                        return item.getIpSource().substring(0, 2)
                                .replaceAll(CommonConst.PROVINCE, "")
                                .replaceAll(CommonConst.CITY, "");
                    }
                    return CommonConst.UNKNOWN;
                })
                .collect(Collectors.groupingBy(item -> item, Collectors.counting()));
        // 转换格式
        List<UserAreaDTO> userAreaList = userAreaMap.entrySet().stream()
                .map(item -> UserAreaDTO.builder()
                        .name(item.getKey())
                        .value(item.getValue())
                        .build())
                .collect(Collectors.toList());
        redisService.set(RedisPrefixConst.USER_AREA, JSON.toJSONString(userAreaList));
    }

    /**
     * 删除用户
     *
     * @param userInfoId 用户信息id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteUser(Integer userInfoId) {
        // 删除用户账号
        userAuthDao.delete(new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserInfoId, userInfoId));
        // 删除用户角色
        userRoleDao.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userInfoId));
    }

    /**
     * 重置用户密码
     *
     * @param userInfoId 用户信息id
     */
    @Override
    public void resetPassword(Integer userInfoId) {
        // 重置用户密码
        userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getPassword, BCrypt.hashpw(CommonConst.INIT_PASSWORD, BCrypt.gensalt()))
                .eq(UserAuth::getUserInfoId, userInfoId));
    }
}
