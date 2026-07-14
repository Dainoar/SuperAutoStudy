package com.tihai.service.superstar.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihai.domain.chaoxing.WkUser;
import com.tihai.mapper.SuperStarUserMapper;
import com.tihai.service.superstar.SuperStarUserService;
import org.springframework.stereotype.Service;

/**
 * @Copyright : DuanInnovator
 * @Description : 超星学习通-用户信息实现类
 * @Author : DuanInnovator
 * @CreateTime : 2025/3/1
 * @Link : <a href="https://github.com/DuanInnovator/SuperAutotudy">...</a>
 **/
@Service
public class SuperStarUserServiceImpl extends ServiceImpl<SuperStarUserMapper, WkUser> implements SuperStarUserService {

    /**
     * 根据账号获取用户信息
     *
     * @param account 账号
     */
    @Override
    public WkUser getUserByAccount(String account) {
        LambdaQueryWrapper<WkUser> wkUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        wkUserLambdaQueryWrapper.eq(WkUser::getAccount, account);
        return this.getOne(wkUserLambdaQueryWrapper, false);

    }
}

