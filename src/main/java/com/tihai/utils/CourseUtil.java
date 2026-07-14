package com.tihai.utils;

import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tihai.common.*;
import com.tihai.common.GlobalConst;
import com.tihai.domain.chaoxing.SuperStarLog;
import com.tihai.domain.chaoxing.WkUser;
import com.tihai.dubbo.pojo.course.Course;
import com.tihai.service.superstar.SuperStarCookieService;
import com.tihai.service.superstar.SuperStarLogService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Copyright : DuanInnovator
 * @Description : 课程工具
 * @Author : DuanInnovator
 * @CreateTime : 2025/3/1
 * @Link : <a href="https://github.com/DuanInnovator/SuperAutotudy">...</a>
 **/
@Component
public class CourseUtil {


    private final ThreadLocal<SessionContext> sessionContext =
            ThreadLocal.withInitial(SessionContext::new);

    @Autowired
    private SuperStarLogService superStarLogService;

    @Autowired
    private SuperStarCookieService cookieService;

    @Autowired
    private ServerInfoUtil serverInfoUtil;

    @Autowired
    private Query query;

    public void setAccount(String account) {
        sessionContext.get().account = account;
    }


    public void setCookies(String cookies) {
        SessionContext session = sessionContext.get();
        session.cookies = convertCookieStringToMap(cookies);
        session.cookieList = convertCookieStringToList(cookies);
    }

    /**
     * Clears request-scoped session state before a pooled worker is reused.
     */
    public void clearSession() {
        sessionContext.remove();
    }

    /**
     * 将 Cookie 字符串转换为 Cookie 列表
     *
     * @param cookieStr Cookie 字符串
     * @return Cookie 列表
     */
    public static List<Cookie> convertCookieStringToList(String cookieStr) {
        List<Cookie> cookies = new ArrayList<>();

        if (cookieStr == null || cookieStr.isEmpty()) {
            return cookies;
        }
        String[] cookieArray = cookieStr.split(", ");
        for (String cookieEntry : cookieArray) {
            String[] parts = cookieEntry.split(";");
            if (parts.length > 0) {
                String[] keyValue = parts[0].split("=", 2);
                if (keyValue.length == 2) {
                    Cookie cookie = new Cookie.Builder()
                            .name(keyValue[0].trim())
                            .value(keyValue[1].trim())
                            .domain("chaoxing.com")
                            .path("/")
                            .build();
                    cookies.add(cookie);
                }
            }
        }
        return cookies;
    }

    public static Map<String, String> convertCookieStringToMap(String cookieStr) {
        if (cookieStr == null || cookieStr.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        // 去掉前后的方括号
        if (cookieStr.startsWith("[") && cookieStr.endsWith("]")) {
            cookieStr = cookieStr.substring(1, cookieStr.length() - 1);
        }
        Map<String, String> cookieMap = new LinkedHashMap<>();
        String[] cookies = cookieStr.split(", ");


        for (String cookie : cookies) {
            String[] attributes = cookie.split(";");
            String[] keyValue = attributes[0].split("=", 2); // 只取第一个键值对，防止 value 中带 "=" 导致错误

            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // 如果 key 已存在，合并值，确保所有相同 key 的值都能保留
                if (cookieMap.containsKey(key)) {
                    cookieMap.put(key, cookieMap.get(key) + "," + value);
                } else {
                    cookieMap.put(key, value);
                }
            }
        }
        return cookieMap;
    }

    public String getValue(String key) {
        return sessionContext.get().cookies.get(key);
    }

    private String getAccount() {
        return sessionContext.get().account;
    }

    private List<Cookie> getCookieList() {
        return sessionContext.get().cookieList;
    }

    private String getCookieHeader() {
        return getCookieList().stream()
                .map(cookie -> cookie.name() + "=" + cookie.value())
                .collect(Collectors.joining("; "));
    }

    private static final class SessionContext {
        private String account;
        private Map<String, String> cookies = new LinkedHashMap<>();
        private List<Cookie> cookieList = new ArrayList<>();
    }


    /**
     * 获取随机秒数
     *
     * @return 随机秒数
     */
    public static int getRandomSeconds() {
        return ThreadLocalRandom.current().nextInt(100, 120);
    }

    /**
     * 获取当前时间戳
     *
     * @return 时间戳字符串
     */
    public static String getTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }


    /**
     * 初始化 OkHttpClient，并根据 isVideo / isAudio 设置请求头
     */

    public OkHttpClient initSession(boolean isVideo, boolean isAudio) {
        // 创建信任所有证书的 TrustManager
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        // 创建 SSLContext 并使用 TrustManager 初始化
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建 OkHttpClient.Builder
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true); // 启用重试机制
//                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]) // 禁用 SSL 验证
//                .hostnameVerifier((hostname, session) -> true); // 禁用主机名验证

        // 配置 CookieJar
        builder.cookieJar(new CookieJar() {
            private final Map<String, List<Cookie>> cookieCache = new ConcurrentHashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                String domain = url.host();
                String account = getAccount();
                if (account == null || cookieService == null) {
                    return;
                }

                // 获取现有Cookie（从内存或数据库）
                List<Cookie> existingCookies = cookieCache.computeIfAbsent(domain, k -> {
                    String cookieStr = cookieService.getWkUserCookie(account);
                    return parseCookieString(cookieStr);
                });

                // 检测是否有变更
                boolean changed = false;
                for (Cookie newCookie : cookies) {
                    boolean exists = existingCookies.stream()
                            .anyMatch(c -> c.name().equals(newCookie.name()) &&
                                    c.value().equals(newCookie.value()));

                    if (!exists) {
                        existingCookies.removeIf(c -> c.name().equals(newCookie.name()));
                        existingCookies.add(newCookie);
                        changed = true;
                    }
                }

                // 如果有变更则更新数据库
                if (changed) {
                    String formattedCookies = formatCookies(existingCookies);
                    WkUser user = new WkUser();
                    user.setAccount(account);
                    user.setCookies(formattedCookies);
                    cookieService.updateWkUserCookies(user);
                }
            }

            // 格式化Cookie为[ ]包裹的字符串
            // 格式化Cookie为字符串（去除[ ]）
            private String formatCookies(List<Cookie> cookies) {
                if (cookies == null || cookies.isEmpty()) {
                    return "";  // 返回空字符串
                }

                return cookies.stream()
                        .map(c -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append(c.name()).append("=").append(c.value());

                            if (c.expiresAt() != Long.MAX_VALUE) {
                                sb.append("; expires=").append(formatDate(c.expiresAt()));
                            }
                            if (c.domain() != null) {
                                sb.append("; domain=").append(c.domain());
                            }
                            if (c.path() != null) {
                                sb.append("; path=").append(c.path());
                            }
                            if (c.secure()) {
                                sb.append("; secure");
                            }
                            if (c.httpOnly()) {
                                sb.append("; httponly");
                            }
                            return sb.toString();
                        })
                        .collect(Collectors.joining(", "));  // 直接拼接字符串，不加方括号
            }

            // 日期格式化（RFC 1123格式）
            private String formatDate(long timestamp) {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                return sdf.format(new Date(timestamp));
            }

            // 从字符串解析Cookie列表
            // 从字符串解析Cookie列表
            private List<Cookie> parseCookieString(String cookieStr) {
                if (cookieStr == null || cookieStr.trim().isEmpty()) {
                    return new ArrayList<>();
                }

                // 处理空内容
                String content = cookieStr.trim();
                if (content.isEmpty()) {
                    return new ArrayList<>();
                }

                // 处理包含逗号的cookie值
                return Arrays.stream(content.split(",(?=\\s*[^=]+=)"))
                        .map(String::trim)
                        .map(this::parseSingleCookie)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }


            private Cookie parseSingleCookie(String cookieStr) {
                try {
                    String[] parts = cookieStr.split(";\\s*");
                    String[] nameValue = parts[0].split("=", 2);
                    if (nameValue.length != 2) return null;

                    Cookie.Builder builder = new Cookie.Builder()
                            .name(nameValue[0].trim())
                            .value(nameValue[1].trim());

                    for (int i = 1; i < parts.length; i++) {
                        String attr = parts[i].trim().toLowerCase();
                        if (attr.startsWith("domain=")) {
                            builder.domain(attr.substring(7));
                        } else if (attr.startsWith("path=")) {
                            builder.path(attr.substring(5));
                        } else if (attr.startsWith("expires=")) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat(
                                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                                Date date = sdf.parse(attr.substring(8));
                                builder.expiresAt(date.getTime());
                            } catch (ParseException e) {
                                // 忽略日期解析错误
                            }
                        } else if (attr.equals("secure")) {
                            builder.secure();
                        } else if (attr.equals("httponly")) {
                            builder.httpOnly();
                        }
                    }
                    return builder.build();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {

                try {
                    String account = getAccount();
                    // 1. 检查基础条件
                    if (url == null || account == null || cookieService == null) {
                        System.out.println("获取Cookie异常: 参数错误 -> " +
                                "url=" + url + ", " +
                                "account=" + account + ", " +
                                "cookieService=" + cookieService);
                        return Collections.emptyList();
                    }

                    // 2. 内存缓存检查
                    String domain = url.host();
                    if (cookieCache.containsKey(domain)) {
                        return new ArrayList<>(cookieCache.get(domain)); // 返回副本避免并发修改
                    }

                    // 3. 安全获取Cookie
                    String wkUserCookie = Optional.ofNullable(cookieService)
                            .map(service -> service.getWkUserCookie(account))
                            .orElse("");

                    if (!wkUserCookie.isEmpty()) {
                        List<Cookie> cookies = convertCookieStringToList(wkUserCookie);
                        cookieCache.put(domain, new CopyOnWriteArrayList<>(cookies));
                        return cookies;
                    }
                } catch (Exception e) {
                    System.out.println("获取Cookie异常: " + e.getMessage());
                }
                return Collections.emptyList();
            }

        });

        // 使用拦截器添加统一请求头
        builder.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder reqBuilder = original.newBuilder();
            Map<String, String> headers;
            if (isVideo) {
                headers = GlobalConst.VIDEO_HEADERS;
            } else if (isAudio) {
                headers = GlobalConst.AUDIO_HEADERS;
            } else {
                headers = GlobalConst.HEADERS;
            }
            headers.forEach(reqBuilder::header);
            Request req = reqBuilder.build();
            return chain.proceed(req);
        });

        return builder.build();
    }


    /**
     * 获取课程列表请求头
     *
     * @return 请求头
     */
    private Map<String, String> getCourseListHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "mooc2-ans.chaoxing.com");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...");
        headers.put("Accept", "text/html, */*; q=0.01");
        headers.put("sec-ch-ua", "\"Microsoft Edge\";v=\"129\", \"Not=A?Brand\";v=\"8\", \"Chromium\";v=\"129\"");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("Origin", "https://mooc2-ans.chaoxing.com");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Referer", "https://mooc2-ans.chaoxing.com/mooc2-ans/visit/interaction?moocDomain=https://mooc1-1.chaoxing.com/mooc-ans");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,ja;q=0.5");
        return headers;
    }


    /**
     * 获取课程列表
     */
    public List<Course> getCourseList() throws IOException {
        OkHttpClient client = initSession(false, false);
        String url = "https://mooc2-ans.chaoxing.com/mooc2-ans/visit/courselistdata";
        FormBody formBody = new FormBody.Builder()
                .add("courseType", "1")
                .add("courseFolderId", "0")
                .add("Query", "")
                .add("superstarClass", "0")
                .build();
        // 此处定义专用的请求头
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .headers(Headers.of(getCourseListHeaders()))
                .build();


        Response response = client.newCall(request).execute();

        String respText = response.body().string();
        List<Course> courseList = Decode.decodeCourseList(respText);

        // 处理课程文件夹
        String interactionUrl = "https://mooc2-ans.chaoxing.com/mooc2-ans/visit/interaction";
        Request interRequest = new Request.Builder().url(interactionUrl).build();
        Response interResponse = client.newCall(interRequest).execute();
        String interText = interResponse.body().string();
        List<CourseFolder> courseFolder = Decode.decodeCourseFolder(interText);
        for (CourseFolder folder : courseFolder) {
            FormBody folderForm = new FormBody.Builder()
                    .add("courseType", "1")
                    .add("courseFolderId", folder.getId())
                    .add("Query", "")
                    .add("superstarClass", "0")
                    .build();
            Request folderRequest = new Request.Builder()
                    .url(url)
                    .post(folderForm)
                    .build();
            Response folderResponse = client.newCall(folderRequest).execute();
            String folderText = folderResponse.body().string();

            courseList.addAll(Decode.decodeCourseList(folderText));
        }
        return courseList;
    }


    /**
     * 获取课程章节（course point）
     */
    public List<CoursePoint> getCoursePoint(String courseId, String clazzId, String cpi) throws IOException {
        OkHttpClient client = initSession(false, false);
        String url = String.format("https://mooc2-ans.chaoxing.com/mooc2-ans/mycourse/studentcourse?courseid=%s&clazzid=%s&cpi=%s&ut=s",
                courseId, clazzId, cpi);
        Request request = new Request.Builder().url(url).addHeader("Cookie", getCookieHeader()).build();
        Response response = client.newCall(request).execute();
        String respText = response.body().string();
        CoursePoint stringObjectMap = Decode.decodeCoursePoint(respText);
        return Arrays.asList(Decode.decodeCoursePoint(respText));
    }

    /**
     * 获取任务点列表
     */
    public Pair<List<Job>, List<JobInfo>> getJobList(String clazzId, String courseId, String cpi, String knowledgeId) throws IOException {
        OkHttpClient client = initSession(false, false);
        List<Job> jobList = new ArrayList<>();
        List<JobInfo> jobInfo = new ArrayList<>();
        String[] possibleNums = {"0", "1", "2"};
        for (String num : possibleNums) {
            String url = String.format("https://mooc1.chaoxing.com/mooc-ans/knowledge/cards?clazzid=%s&courseid=%s&knowledgeid=%s&num=%s&ut=s&cpi=%s&v=20160407-3&mooc2=1",
                    clazzId, courseId, knowledgeId, num, cpi);
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String respText = response.body().string();
            Pair<List<Job>, JobInfo> result = Decode.decodeCourseCard(respText);
            List<Job> _jobList = result.getFirst();

            List<JobInfo> _jobInfo = Arrays.asList(result.getSecond());
            if (Boolean.TRUE.equals(_jobInfo.get(0).getNotOpen())) {
                return new Pair<>(new ArrayList<>(), _jobInfo);
            }
            jobList.addAll(_jobList);
            jobInfo.addAll(_jobInfo);
        }
        return new Pair<>(jobList, jobInfo);
    }

    /**
     * 生成 MD5 加密串
     */
    public String getEnc(String clazzId, String jobId, String objectId, long playingTime, long duration, String userId)
            throws Exception {
        String str = String.format("[%s][%s][%s][%s][%d][d_yHJ!$pdA~5][%d][0_%s]",
                clazzId, userId, jobId, objectId, playingTime * 1000, duration * 1000, duration);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes());
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 视频播放进度日志
     */
    public Boolean videoProgressLog(OkHttpClient session, Course course,
                                    Job job, JobInfo jobInfo, String dtoken, long duration,
                                    long playingTime, String type) throws Exception {
        String midText;
        String respText = null;
        if (job.getOtherInfo().contains("courseId")) {
            midText = "otherInfo=" + job.getOtherInfo() + "&";
        } else {
            midText = "otherInfo=" + job.getOtherInfo() + "&courseId=" + course.getCourseId() + "&";
        }
        boolean success = false;
        Response response = null;
        for (String possibleRt : new String[]{"0.9", "1"}) {
            String url = String.format("https://mooc1.chaoxing.com/mooc-ans/multimedia/log/a/%s/%s?clazzId=%s&playingTime=%s" +
                            "&duration=%s&clipTime=0_%s&objectId=%s&%sjobid=%s&userid=%s&isdrag=3&view=pc&enc=%s&rt=%s&dtype=%s&_t=%s",
                    course.getCpi(), dtoken, course.getClazzId(), playingTime, duration, duration,
                    job.getObjectId(),
                    midText, job.getJobId(), getValue("_uid"),
                    getEnc(course.getClazzId(), job.getJobId(), job.getObjectId(),
                            playingTime, duration, getValue("_uid")),
                    possibleRt, type, getTimestamp());
            Request request = new Request.Builder().url(url).build();
            response = session.newCall(request).execute();
            respText = response.body().string();
            if (response.code() == 200) {
                success = true;
                break;
            } else if (response.code() == 403) {
                continue;
            }
        }
        System.out.println(respText);
        if (success && respText != null) {
            if (playingTime >= duration) {
                return true;
            }
        } else {
            //logger.error("出现403报错，尝试修复无效，正在跳过当前任务点...");
            return false;
        }
        return false;
    }

    /**
     * 学习视频（模拟观看）
     */
    public void studyVideo(Course course, Job job, JobInfo jobInfo,
                           double speed, String type, SuperStarLog log) throws Exception {

        OkHttpClient session = ("Video".equals(type)) ?
                initSession(true, false) : initSession(false, true);
        // 获取视频信息
        String infoUrl = String.format("https://mooc1.chaoxing.com/ananas/status/%s?k=%s&flag=normal",
                job.getObjectId(), getValue("fid"));
        Request infoRequest = new Request.Builder().url(infoUrl).build();
        Response infoResponse = session.newCall(infoRequest).execute();
        Map<String, Object> videoInfo = JsonParser.parse(infoResponse.body().string());
        if ("success".equals(videoInfo.get("status"))) {
            String dtoken = videoInfo.get("dtoken").toString();
            long duration = Long.parseLong(videoInfo.get("duration").toString());
            boolean isFinished = false;
            long playingTime = 0;
            System.out.println(Thread.currentThread().getName() + "开始学习视频: " + "课程名:" + course.getTitle() + " " + "任务名:" + job.getName());
            log.setStartTime(LocalDateTime.now());
            log.setCurrentJob(job.getName());
            while (!isFinished) {
                Boolean progress = videoProgressLog(session, course, job, jobInfo, dtoken, duration,
                        playingTime, type);
                if (progress == null || progress == true) {
                    break;
                }
                int waitTime = (int) (getRandomSeconds() * speed);
                if (playingTime + waitTime >= duration) {
                    waitTime = (int) (duration - playingTime);
                }

                Thread.sleep(60000);

                playingTime += waitTime;
            }
            log.setErrorMessage(null);
            log.setStatus(1);
            log.setRemark("任务完成: " + job.getName());
            log.setEndTime(LocalDateTime.now());
            log.setMachineNum(serverInfoUtil.getCurrentServerInstance());
            superStarLogService.saveLog(log);

        }
    }

    /**
     * 学习文档
     */
    public void studyDocument(Course course, Job job) throws IOException {
        OkHttpClient client = initSession(false, false);
        String otherInfo = job.getOtherInfo();
        Pattern pattern = Pattern.compile("nodeId_(.*?)-");
        Matcher matcher = pattern.matcher(otherInfo);
        String knowledgeId = "";
        if (matcher.find()) {
            knowledgeId = matcher.group(1);
        }
        String url = String.format("https://mooc1.chaoxing.com/ananas/job/document?jobid=%s&knowledgeid=%s&courseid=%s&clazzid=%s&jtoken=%s&_dc=%s",
                job.getJobId(), knowledgeId, course.getCourseId(), course.getClazzId(), job.getJToken(), getTimestamp());
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).execute();
    }

    /**
     * 阅读任务，只完成任务点，不增加时长
     */
    public void studyRead(Course course, Job job, JobInfo jobInfo, SuperStarLog log)
            throws IOException {
        OkHttpClient client = initSession(false, false);
        HttpUrl url = HttpUrl.parse("https://mooc1.chaoxing.com/ananas/job/readv2").newBuilder()
                .addQueryParameter("jobid", job.getJobId())
                .addQueryParameter("knowledgeid", jobInfo.getKnowledgeid())
                .addQueryParameter("jtoken", job.getJToken())
                .addQueryParameter("courseid", course.getCourseId())
                .addQueryParameter("clazzid", course.getClazzId())
                .build();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        if (response.code() != 200) {
            log.setRemark("阅读任务学习失败 -> [" + response.code() + "]" + response.body().string());
        } else {
            Map<String, Object> respJson = JsonParser.parse(response.body().string());
            log.setRemark("阅读任务学习 -> " + respJson.get("msg"));
        }
        superStarLogService.saveLog(log);
    }

    /**
     * 作业任务
     *
     * @param course  课程信息
     * @param job     任务点信息
     * @param jobInfo 作业信息
     * @throws Exception 异常
     */
    public void studyWork(Course course, Job job, JobInfo jobInfo, SuperStarLog log) throws Exception {

        OkHttpClient client = initSession(false, false);
        String originHtmlContent = "";

        // 获取作业页面
        String url = "https://mooc1.chaoxing.com/mooc-ans/api/work";
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("api", "1")
                .addQueryParameter("workId", job.getJobId().replace("work-", ""))
                .addQueryParameter("jobid", job.getJobId())
                .addQueryParameter("originJobId", job.getJobId())
                .addQueryParameter("needRedirect", "true")
                .addQueryParameter("skipHeader", "true")
                .addQueryParameter("knowledgeid", jobInfo.getKnowledgeid())
                .addQueryParameter("ktoken", jobInfo.getKtoken())
                .addQueryParameter("cpi", jobInfo.getCpi())
                .addQueryParameter("ut", "s")
                .addQueryParameter("clazzId", course.getClazzId())
                .addQueryParameter("enc", job.getEnc())
                .addQueryParameter("mooc2", "1")
                .addQueryParameter("courseid", course.getCourseId());

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0")
                .get()
                .build();

        Response response = client.newCall(request).execute();
        originHtmlContent = response.body().string();

        Map<String, Object> questions = Decode.decodeQuestionsInfo(originHtmlContent);

        processAnswers(questions, client, log, job.getTitle());
    }

    /**
     * 切割选项
     *
     * @param options 选项
     * @return 选项信息
     */
    private static List<String> multiCut(String options) {
        if (options == null) return Collections.emptyList();

        return Pattern.compile("\n+")
                .splitAsStream(options)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 获取并处理答案
     *
     * @param questions 题目信息
     */
    public void processAnswers(Map<String, Object> questions, OkHttpClient client, SuperStarLog log, String jobTitle) throws IOException {
        // 获取题目列表
        List<Question> questionList = new ArrayList<>();
        List<Object> questions1 = (List<Object>) questions.get("questions");
        for (Object question : questions1) {
            if (question instanceof Map) {
                Map<String, Object> questionMap = (Map<String, Object>) question;
                Map<String, Object> answerField = (Map<String, Object>) questionMap.get("answerField");
                String title = (String) questionMap.get("title");
                String options = (String) questionMap.get("options");
                String id = (String) questionMap.get("id");
                String type = (String) questionMap.get("type");
                AnswerField<String> readAnswerField = new AnswerField<>();
                readAnswerField.updateFromMap(answerField);
                Question q = new Question(id, title, options, type, readAnswerField);
                questionList.add(q);
            }
        }

        for (Question q : questionList) {

            String questionType = q.getType();
            List<String> options = multiCut(q.getOptions());
            q.setTKOptions(options);
            String res = query.queryAnswerSync(q);
            StringBuilder answer = new StringBuilder(); //最终答案

            if (res == null || res.isEmpty()) {
                if (questionType.equals("judgement")) {
                    answer = new StringBuilder("true");  //对于判断题来说,如果题库无答案，则默认选对
                } else {
                    answer = new StringBuilder(randomAnswer(options)); //其他题型默认随机选
                }
            } else {
                switch (questionType) {

                    case "single":
                        for (String option : options) {
                            if (option.contains(res)) {
                                answer = new StringBuilder(option.substring(0, 1));
                                break;
                            }
                        }
                        break;
                    case "multiple":
                        String[] resArr = res.split("#");
                        for(String re : resArr){
                            for (String option : options) {
                                if (option.contains(re)) {
                                    answer.append(option.charAt(0));
                                    break;
                                }
                            }
                        }

                        answer = new StringBuilder(answer.chars()
                                .sorted()
                                .mapToObj(c -> String.valueOf((char) c))
                                .collect(Collectors.joining()));
                        break;

                    case "judgement":
                        if (res.equals("正确")) {
                            answer = new StringBuilder("true");
                        } else {
                            answer = new StringBuilder("false");
                        }
                    default:
                }

            }

            AnswerField<String> answerField = q.getAnswerField();
            answerField.setAnswerValue(answer.toString());

        }

        for (Question q : questionList) {
            questions.put("answer" + q.getId(), q.getAnswerField().getAnswerValue());
            String type = q.getType();
            String reaType = "0";
            switch (type) {
                case "single":
                    reaType = "0";
                    break;
                case "multiple":
                    reaType = "1";
                    break;
                case "fill":
                    reaType = "2";
                    break;
                case "judgement":
                    reaType = "3";
                    break;
                case "short":
                    reaType = "4";
                    break;

            }
            questions.put("answertype" + q.getId(), reaType);
        }
        try {
            questions.remove("questions");
            submitAnswers(questions, client, log, jobTitle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 随机选择一个选项
     *
     * @param options 选项
     * @return 答案
     */
    private String randomAnswer(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        Random random = new Random();
        int index = random.nextInt(options.size());
        return options.get(index).substring(0, 1); // 返回选项首字母
    }


    /**
     * 提交章节测验答案
     *
     * @param questions 题目信息
     * @param client    OkHttpClient
     * @throws IOException 异常
     */
    private void submitAnswers(Map<String, Object> questions, OkHttpClient client, SuperStarLog log, String jobTitle) throws IOException, NacosException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder(StandardCharsets.UTF_8);

        for (Map.Entry<String, Object> entry : questions.entrySet()) {
            formBodyBuilder.add(entry.getKey(), entry.getValue().toString());
        }
        Request request = new Request.Builder()
                .url("https://mooc1.chaoxing.com/mooc-ans/work/addStudentWorkNewWeb")  //2025/4/26发现学习通提交题目接口已改变
                .post(formBodyBuilder.build())
                .header("Host", "mooc1.chaoxing.com")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("sec-ch-ua", "\"Microsoft Edge\";v=\"129\", \"Not=A?Brand\";v=\"8\", \"Chromium\";v=\"129\"")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("sec-ch-ua-mobile", "?0")
                .header("Origin", "https://mooc1.chaoxing.com")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,ja;q=0.5")
                .build();

        Response response = client.newCall(request).execute();
        ObjectMapper objectMapper = new ObjectMapper();
        assert response.body() != null;
        Map<String, Object> responseBody = objectMapper.readValue(response.body().string(), Map.class);
        if (responseBody.get("status").equals("true") && responseBody.get("msg").equals("success")) {
            log.setErrorMessage(null);
            log.setStatus(1);
            log.setRemark("章节测验完成: " + jobTitle);
            log.setEndTime(LocalDateTime.now());
            log.setMachineNum(serverInfoUtil.getCurrentServerInstance());
            superStarLogService.saveLog(log);
        } else {
            log.setErrorMessage("章节测验提交失败" + jobTitle);
            log.setStatus(5);
            log.setRemark("章节测验提交失败: " + jobTitle);
            log.setMachineNum(serverInfoUtil.getCurrentServerInstance());
            superStarLogService.saveLog(log);
        }
    }


}

