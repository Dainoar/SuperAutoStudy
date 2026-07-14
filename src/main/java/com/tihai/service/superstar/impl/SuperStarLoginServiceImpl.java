package com.tihai.service.superstar.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihai.constant.GlobalConstant;
import com.tihai.domain.chaoxing.SuperStarLog;
import com.tihai.domain.chaoxing.WkUser;
import com.tihai.mapper.SuperStarUserMapper;
import com.tihai.service.superstar.SuperStarLogService;
import com.tihai.service.superstar.SuperStarLoginService;
import com.tihai.service.superstar.SuperStarUserService;
import com.tihai.utils.AESCipher;
import com.tihai.utils.JsonParser;
import com.tihai.utils.CredentialCipher;
import ma.glasnost.orika.MapperFacade;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Copyright : DuanInnovator
 * @Description : 超星学习通-登录服务实现类
 * @Author : DuanInnovator
 * @CreateTime : 2025/3/1
 * @Link : <a href="https://github.com/DuanInnovator/SuperAutotudy">...</a>
 **/
@SuppressWarnings("all")
@Service
public class SuperStarLoginServiceImpl extends ServiceImpl<SuperStarUserMapper, WkUser> implements SuperStarLoginService {

    @Autowired
    private AESCipher cipher;

    @Autowired
    private SuperStarLogService superStarLogService;

    @Autowired
    private SuperStarUserService userService;
    @Autowired
    private MapperFacade mapperFacade;

    @Autowired
    private CredentialCipher credentialCipher;

    /**
     * 超星登录
     *
     * @param wkUser 用户信息
     * @param isUpdate 是否更新用户信息  false->不执行重新登录请求  true->cookies已过期,执行重新登录请求
     */
    @Override
    public String login(WkUser wkUser,boolean isUpdate) {

        WkUser userByAccount = userService.getUserByAccount(wkUser.getAccount());
        if(!isUpdate && userByAccount!=null && StringUtils.hasLength(userByAccount.getCookies())){
            return credentialCipher.decrypt(userByAccount.getCookies());
        }
        String password = credentialCipher.decrypt(wkUser.getPassword());
        SuperStarLog startLog = mapperFacade.map(wkUser, SuperStarLog.class);

        CookieJar cookieJar = new CookieJar() {
            private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<>();
            }
        };

        // 初始化 OkHttpClient 并设置 CookieJar
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();

        String url = "https://passport2.chaoxing.com/fanyalogin";

        FormBody formBody = new FormBody.Builder()
                .add("fid", "-1")
                .add("uname", cipher.encrypt(wkUser.getAccount()))
                .add("password", cipher.encrypt(password))
                .add("refer", "https%3A%2F%2Fi.chaoxing.com")
                .add("t", "true")
                .add("forbidotherlogin", "0")
                .add("validate", "")
                .add("doubleFactorLogin", "0")
                .add("independentId", "0")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            Map<String, Object> result = JsonParser.parse(body); // 解析 JSON 响应
            Map<String, Object> ret = new HashMap<>();

            if (result != null && Boolean.TRUE.equals(result.get("status"))) {
                List<Cookie> cookies = cookieJar.loadForRequest(HttpUrl.parse(url));
                String cookieStr = cookies.stream()
                        .map(Cookie::toString)
                        .collect(Collectors.joining(", "));

                if (userByAccount != null) {
                    userByAccount.setCookies(credentialCipher.encrypt(cookieStr));
                    userByAccount.setPassword(credentialCipher.encrypt(password));
                    this.baseMapper.updateById(userByAccount);
                } else {
                    wkUser.setPassword(credentialCipher.encrypt(password));
                    wkUser.setCookies(credentialCipher.encrypt(cookieStr));
                    this.save(wkUser);
                }
                startLog.setRemark(GlobalConstant.LOGIN_SUCCESS);

                return cookieStr;


            } else {
                startLog.setRemark(result != null && result.get("msg2") != null ? result.get("msg2").toString() : GlobalConstant.LOGIN_FAIL);
            }

        } catch (IOException e) {
            startLog.setRemark(GlobalConstant.LOGIN_FAIL + e.getMessage());
        }
        superStarLogService.saveLog(startLog);
        return null;
    }


}

