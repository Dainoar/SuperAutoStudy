package com.tihai.service.superstar.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihai.common.*;
import com.tihai.constant.GlobalConstant;
import com.tihai.constant.JobTypeConstant;
import com.tihai.constant.RetryConstant;
import com.tihai.domain.chaoxing.SuperStarLog;
import com.tihai.domain.chaoxing.SuperStarTask;
import com.tihai.domain.chaoxing.WkUser;
import com.tihai.dubbo.dto.CourseSubmitTaskDTO;
import com.tihai.dubbo.pojo.course.Course;
import com.tihai.enums.BizCodeEnum;
import com.tihai.enums.WkTaskStatusEnum;
import com.tihai.exception.BusinessException;
import com.tihai.factory.CustomThreadFactory;
import com.tihai.factory.PriorityRejectPolicy;
import com.tihai.manager.RollBackManager;
import com.tihai.manager.CoursePointNavigator;
import com.tihai.manager.TaskLifecycle;
import com.tihai.mapper.SuperStarMapper;
import com.tihai.properties.StudyProperties;
import com.tihai.properties.ThreadPoolProperties;
import com.tihai.queue.PriorityTaskWrapper;
import com.tihai.queue.BoundedPriorityBlockingQueue;
import com.tihai.service.superstar.SuperStarLogService;
import com.tihai.service.superstar.SuperStarLoginService;
import com.tihai.service.superstar.SuperStarTaskService;
import com.tihai.service.superstar.SuperStarUserService;
import com.tihai.utils.CourseUtil;
import com.tihai.utils.ServerInfoUtil;
import com.tihai.utils.CredentialCipher;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;


/**
 * @Copyright : DuanInnovator
 * @Description : 超星学习通-任务启动和实现
 * @Author : DuanInnovator
 * @CreateTime : 2025/4/26
 * @Link : <a href="https://github.com/DuanInnovator/TiHaiWuYou-Admin/tree/mine-admin">...</a>
 **/
@Service
@Slf4j
public class SuperStarTaskServiceImpl extends ServiceImpl<SuperStarMapper, SuperStarTask> implements SuperStarTaskService {

    @Autowired
    private MapperFacade mapperFacade;

    @Autowired
    private ThreadPoolExecutor taskExecutor;

    @Autowired
    private SuperStarUserService userService;

    @Autowired
    private CourseUtil courseUtil;

    @Autowired
    private SuperStarLogService superStarLogService;

    @Autowired
    private SuperStarLoginService loginService;

    @Autowired
    private RollBackManager rb;

    @Autowired
    private ServerInfoUtil serverInfoUtil;

    @Autowired
    private ThreadPoolProperties config;

    @Autowired
    private StudyProperties studyProperties;

    @Autowired
    private CredentialCipher credentialCipher;


    private volatile boolean isShuttingDown = false;
    private final Object shutdownLock = new Object();


    // 日志批量保存的缓冲队列
    private final BlockingQueue<SuperStarLog> logQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Long, SuperStarLog> logMap = new ConcurrentHashMap<>();
    private Thread logWriterThread;

    public SuperStarTaskServiceImpl() throws NacosException {
    }

    public void shutdownThreadPoolGracefully() throws NacosException {
        // 提前检查减少同步块竞争
        if (isShuttingDown || taskExecutor.isShutdown()) {
            return;
        }

        synchronized (shutdownLock) {
            // 再次检查（双重检查锁模式）
            if (isShuttingDown || taskExecutor.isShutdown()) {
                return;
            }
            isShuttingDown = true;

            try {
                // 1. 停止接受新任务
                taskExecutor.shutdown();
                log.info("线程池开始关闭，等待现有任务完成...");

                // 2. 分阶段等待
                // 第一阶段：等待正常完成
                if (!taskExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                    log.warn("等待超时，尝试强制停止剩余任务...");

                    // 第二阶段：强制停止
                    List<Runnable> unfinishedTasks = taskExecutor.shutdownNow();

                    // 3. 处理未完成任务
                    if (!unfinishedTasks.isEmpty()) {
                        log.warn("有{}个任务被强制停止，开始回滚状态...", unfinishedTasks.size());
                        processUnfinishedTasks(unfinishedTasks);
                    }

                    // 最后检查
                    if (!taskExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                        log.error("线程池最终未能完全关闭");
                    }
                }
            } catch (InterruptedException e) {
                // 处理中断
                List<Runnable> unfinishedTasks = taskExecutor.shutdownNow();
                if (!unfinishedTasks.isEmpty()) {
                    processUnfinishedTasks(unfinishedTasks);
                }
                Thread.currentThread().interrupt();
                throw new NacosException(NacosException.SERVER_ERROR, "线程池关闭被中断");
            } finally {
                log.info("线程池关闭完成，当前状态: isShutdown={}, isTerminated={}",
                        taskExecutor.isShutdown(), taskExecutor.isTerminated());

            }
        }
    }

    private void processUnfinishedTasks(List<Runnable> unfinishedTasks) {
        unfinishedTasks.forEach(task -> {
            if (task instanceof PriorityTaskWrapper) {
                String taskId = ((PriorityTaskWrapper) task).getTaskId();
                try {
                    SuperStarTask dbTask = this.getById(taskId);
                    if (dbTask != null &&
                            (WkTaskStatusEnum.PROCESSING.getCode().equals(dbTask.getStatus()) ||
                                    WkTaskStatusEnum.EXAM.getCode().equals(dbTask.getStatus()))) {
                        dbTask.setStatus(WkTaskStatusEnum.PAUSED.getCode());
                        this.updateById(dbTask);
                    }
                } catch (Exception e) {
                    log.error("回滚任务状态失败, taskId={}", taskId, e);
                }
            }
        });
    }

    public void restartThreadPoolAndTasks() {
        synchronized (shutdownLock) {
            if (!taskExecutor.isShutdown()) {
                return;
            }

            BlockingQueue<Runnable> queue = new BoundedPriorityBlockingQueue<>(config.getQueueCapacity());

            // 重新构建线程池
            taskExecutor = new ThreadPoolExecutor(
                    config.getCoreSize(),
                    config.getMaxSize(),
                    config.getKeepAlive(),
                    TimeUnit.SECONDS,
                    queue,
                    new CustomThreadFactory(config.getThreadNamePrefix()),
                    new PriorityRejectPolicy()
            );
            taskExecutor.allowCoreThreadTimeOut(config.isAllowCoreThreadTimeout());
            this.startChaoxingTask();
        }
    }

    @PostConstruct
    public void init() {
        recoverInterruptedTasks();
        logWriterThread = new Thread(this::batchSaveLogs, "super-star-log-writer");
        logWriterThread.setDaemon(true);
        logWriterThread.start();
        startChaoxingTask();
    }

    @PreDestroy
    public void stopLogWriter() {
        if (logWriterThread != null) {
            logWriterThread.interrupt();
        }
    }

    private void recoverInterruptedTasks() {
        List<SuperStarTask> interruptedTasks = list(new LambdaQueryWrapper<SuperStarTask>()
                .in(SuperStarTask::getStatus,
                        WkTaskStatusEnum.QUEUE.getCode(),
                        WkTaskStatusEnum.PROCESSING.getCode()));
        interruptedTasks.stream()
                .filter(task -> TaskLifecycle.shouldRecover(task.getStatus()))
                .forEach(task -> task.setStatus(TaskLifecycle.recoveredStatus()));
        if (!interruptedTasks.isEmpty()) {
            updateBatchById(interruptedTasks);
            log.info("已恢复 {} 个因服务重启中断的任务", interruptedTasks.size());
        }
    }

    private void batchSaveLogs() {
        List<SuperStarLog> batch = new ArrayList<>(100);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SuperStarLog log = logQueue.poll(1, TimeUnit.SECONDS);
                if (log != null) {
                    batch.add(log);
                    logMap.put(log.getId(), log); // 合并更新
                }
                // 批量保存条件：达到100条或超时
                if (batch.size() >= 100 || (log == null && !batch.isEmpty())) {
                    List<SuperStarLog> toSave = new ArrayList<>(logMap.values());

                    if (log != null) {
                        log.setMachineNum(serverInfoUtil.getCurrentServerInstance());
                    }
                    superStarLogService.saveOrUpdateBatch(toSave);
                    logMap.clear();
                    batch.clear();

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SuperStarLog createNewLog(SuperStarTask task) {
        SuperStarLog log = mapperFacade.map(task, SuperStarLog.class);
        log.setStartTime(LocalDateTime.now());
        log.setStatus(WkTaskStatusEnum.PROCESSING.getCode());
        // 首次插入，获取自增ID
        superStarLogService.save(log);
        return log;
    }

    /**
     * 根据账号和课程id获取超星学习任务
     *
     * @param loginAccount 账号
     * @param courseId     课程id
     */
    @Override
    public SuperStarTask getSuperStarTask(String loginAccount, String courseId) {
        LambdaQueryWrapper<SuperStarTask> superStarTaskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        superStarTaskLambdaQueryWrapper.eq(SuperStarTask::getLoginAccount, loginAccount);
        superStarTaskLambdaQueryWrapper.eq(SuperStarTask::getCourseId, courseId);
        return this.getOne(superStarTaskLambdaQueryWrapper, false);
    }

    /**
     * 根据账号和课程id获取超星学习任务
     *
     * @param loginAccount 账号
     * @param courseName   课程名
     */
    @Override
    public SuperStarTask getSuperStarTaskByCourseName(String loginAccount, String courseName) {
        LambdaQueryWrapper<SuperStarTask> superStarTaskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        superStarTaskLambdaQueryWrapper.eq(SuperStarTask::getLoginAccount, loginAccount);
        superStarTaskLambdaQueryWrapper.eq(SuperStarTask::getCourseName, courseName);
        return this.getOne(superStarTaskLambdaQueryWrapper, false);
    }

    /**
     * 添加任务到等待队列
     *
     * @param courseSubmitTaskDTO 任务信息
     * @throws NacosException nacos异常
     */
    public void addChaoXingTask(CourseSubmitTaskDTO courseSubmitTaskDTO) throws NacosException {
        SuperStarTask task = null;
        if (courseSubmitTaskDTO.getCourseId() == null) {
            task = getSuperStarTaskByCourseName(courseSubmitTaskDTO.getLoginAccount(), courseSubmitTaskDTO.getCourseName());
        } else {
            task = getSuperStarTask(courseSubmitTaskDTO.getLoginAccount(), courseSubmitTaskDTO.getCourseId());
        }
        if (task == null) {
            SuperStarTask chaoXingTask = mapperFacade.map(courseSubmitTaskDTO, SuperStarTask.class);
            chaoXingTask.setPassword(credentialCipher.encrypt(chaoXingTask.getPassword()));
            chaoXingTask.setStatus(WkTaskStatusEnum.PENDING.getCode());
            chaoXingTask.setId(UUID.randomUUID().toString());
            chaoXingTask.setPriority(1);
            chaoXingTask.setRetryCount(0);
            chaoXingTask.setCreatTime(LocalDateTime.now());
            chaoXingTask.setMachineNum(serverInfoUtil.getCurrentServerInstance());
            this.baseMapper.insert(chaoXingTask);
        } else {
            throw new BusinessException(BizCodeEnum.TASK_ALREADY_EXIST.getCode(), BizCodeEnum.TASK_ALREADY_EXIST.getMsg());
        }
        //TODO 这里似乎不太合理,等待后续优化.......
        startChaoxingTask();
    }

    /**
     * 超星学习任务启动
     */
    @Override
    public void startChaoxingTask() {
        LambdaQueryWrapper<SuperStarTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuperStarTask::getStatus, WkTaskStatusEnum.PENDING.getCode());
        wrapper.le(SuperStarTask::getRetryCount, RetryConstant.DEFAULT_RETRY_COUNT);
        wrapper.orderByDesc(SuperStarTask::getPriority);
        List<SuperStarTask> tasks = list(wrapper);
//        executeCourseTask(tasks.get(0));

        tasks.forEach(task -> {
            task.setStatus(WkTaskStatusEnum.QUEUE.getCode());
            try {
                task.setMachineNum(serverInfoUtil.getCurrentServerInstance());
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
            boolean isUpdated = this.updateById(task);
            if (!isUpdated) {
                log.warn("任务入队状态更新失败，taskId={}", task.getId());
                return;
            }
            Runnable taskWrapper = new PriorityTaskWrapper(() -> {
                try {
                    executeCourseTask(task);
                } catch (Exception e) {
                    int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
                    if (retryCount >= RetryConstant.DEFAULT_RETRY_COUNT) {
                        task.setStatus(WkTaskStatusEnum.ABNORMAL.getCode());
                    } else {
                        task.setRetryCount(retryCount + 1);
                        task.setStatus(WkTaskStatusEnum.PENDING.getCode());
                    }
                    this.updateById(task);
                    log.warn("任务执行失败，将按状态重试。taskId={}, retryCount={}",
                            task.getId(), task.getRetryCount(), e);
                    if (WkTaskStatusEnum.PENDING.getCode().equals(task.getStatus())) {
                        startChaoxingTask();
                    }
                }
            }, task.getPriority(), task.getId());

            try {
                taskExecutor.execute(taskWrapper);
            } catch (RejectedExecutionException e) {
                task.setStatus(WkTaskStatusEnum.PENDING.getCode());
                this.updateById(task);
                throw e;
            }
        });
    }

    @Override
    public List<SuperStarTask> listTasks(String loginAccount) {
        LambdaQueryWrapper<SuperStarTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(loginAccount), SuperStarTask::getLoginAccount, loginAccount);
        wrapper.orderByDesc(SuperStarTask::getCreatTime);
        return list(wrapper);
    }

    @Override
    public boolean pauseTask(String taskId) {
        SuperStarTask task = getById(taskId);
        if (task == null || !(WkTaskStatusEnum.PENDING.getCode().equals(task.getStatus())
                || WkTaskStatusEnum.QUEUE.getCode().equals(task.getStatus()))) {
            return false;
        }
        task.setStatus(WkTaskStatusEnum.PAUSED.getCode());
        return updateById(task);
    }

    /**
     * 执行超星学习任务
     *
     * @param task 超星学习任务
     */
    public void executeCourseTask(SuperStarTask task) {
        SuperStarTask persistedTask = getById(task.getId());
        if (persistedTask == null || WkTaskStatusEnum.PAUSED.getCode().equals(persistedTask.getStatus())) {
            return;
        }
        task.setStatus(WkTaskStatusEnum.PROCESSING.getCode());
        this.updateById(task);
        WkUser user = userService.getUserByAccount(task.getLoginAccount());
        SuperStarLog log = superStarLogService.getLatestLogByLoginAccount(task.getLoginAccount(), task.getCourseName());

        if (log == null) {
            log = createNewLog(task);
        } else {
            log.setStartTime(LocalDateTime.now());
        }


        try {
            if (user == null || user.getCookies() == null) {
                WkUser wkUser = new WkUser();
                wkUser.setAccount(task.getLoginAccount());
                wkUser.setPassword(task.getPassword());
                String cookies = loginService.login(wkUser, true);
                if (StringUtils.isEmpty(cookies)) {
                    throw new IllegalStateException("用户登录失败");
                }
                user = wkUser;
                user.setCookies(cookies);
            }

            Course readyCourse = new Course();
            courseUtil.setAccount(task.getLoginAccount());
            courseUtil.setCookies(credentialCipher.decrypt(user.getCookies()));
            if (task.getCourseId() != null) {
                readyCourse = courseUtil.getCourseList().stream()
                        .filter(course -> course.getCourseId().equals(task.getCourseId()))
                        .findFirst().orElse(null);
            } else if (task.getCourseName() != null) {
                List<Course> courseList = courseUtil.getCourseList();
                 readyCourse = Optional.ofNullable(courseList)
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(course -> course != null && task != null)
                        .filter(course -> {
                            // 标准化处理
                            String dbTitle = StringUtils.normalizeSpace(course.getTitle());
                            String taskName = StringUtils.normalizeSpace(task.getCourseName());

                            return dbTitle.equalsIgnoreCase(taskName);
                        })
                        .findFirst()
                        .orElse(null);
            }

            if (readyCourse == null) {
                throw new IllegalStateException(GlobalConstant.COURSE_INFO_GET_FAIL);
            }

            List<CoursePoint> pointList = courseUtil.getCoursePoint(
                    readyCourse.getCourseId(), readyCourse.getClazzId(), readyCourse.getCpi());

            if (!processCoursePoints(task, log, readyCourse, pointList)) {
                throw new IllegalStateException("课程章节执行失败");
            }
            task.setStatus(WkTaskStatusEnum.FINISHED.getCode());
            this.updateById(task);
        } catch (Exception e) {
            log.setStatus(WkTaskStatusEnum.ABNORMAL.getCode());
            log.setErrorMessage(e.getMessage());
            logQueue.offer(log);
            throw new IllegalStateException("课程任务执行失败: " + task.getId(), e);
        } finally {
            courseUtil.clearSession();
        }
    }

    private boolean processCoursePoints(SuperStarTask task, SuperStarLog log, Course readyCourse, List<CoursePoint> pointList) {
        if (log == null) {
            log = mapperFacade.map(task, SuperStarLog.class);
            log.setId(Long.valueOf(UUID.randomUUID().toString()));
            log.setStatus(WkTaskStatusEnum.PROCESSING.getCode());
            logQueue.offer(log);
        }

        int pointIndex = log.getCurrentChapterIndex() != null ? log.getCurrentChapterIndex() : 0;

        List<ChapterPoint> chapterPointList = CoursePointNavigator.flatten(pointList);


        while (pointIndex < chapterPointList.size()) {

            try {
                log.setCurrentChapterIndex(pointIndex);
                ChapterPoint chapterPoint = chapterPointList.get(pointIndex);
                Pair<List<Job>, List<JobInfo>> result = courseUtil.getJobList(
                        readyCourse.getClazzId().toString(),
                        readyCourse.getCourseId(),
                        readyCourse.getCpi(),
                        chapterPoint.getId());

                List<Job> jobs = result.getFirst();
                List<JobInfo> jobInfo = result.getSecond();

                if (jobs.isEmpty()) {
                    pointIndex++;
                    continue;
                }

                if (jobInfo != null && jobInfo.stream().anyMatch(info -> Boolean.TRUE.equals(info.getNotOpen()))) {
                    rb.addTimes(chapterPoint.getId());
                    continue;
                }

                for (Job job : jobs) {
                    try {

                        switch (job.getType()) {

                            case JobTypeConstant.VIDEO:
                                boolean isAudio = false;
                                try {
                                courseUtil.studyVideo(readyCourse, job, requiredJobInfo(jobInfo, job), studyProperties.getSpeed(), "Video", log);
                                } catch (Exception e) {
                                    isAudio = true;
                                }
                                if (isAudio) {
                                    try {
                                        courseUtil.studyVideo(readyCourse, job, requiredJobInfo(jobInfo, job), studyProperties.getSpeed(), "Audio", log);
                                    } catch (Exception e) {
                                        log.setErrorMessage("异常任务 -> 章节: " + job.getId() + "，已跳过");
                                    }
                                }
                                break;
                            case JobTypeConstant.DOCUMENT:
                                courseUtil.studyDocument(readyCourse, job);
                                break;
                            case JobTypeConstant.READ:
                                courseUtil.studyRead(readyCourse, job, requiredJobInfo(jobInfo, job), log);
                                break;
                            case JobTypeConstant.QUESTION:
                                courseUtil.studyWork(readyCourse, job, requiredJobInfo(jobInfo, job), log);
                            default:
                                break;

                        }

                    } catch (Exception e) {
                        log.setEndTime(LocalDateTime.now());
                        log.setStatus(WkTaskStatusEnum.ABNORMAL.getCode());
                        log.setErrorMessage("任务异常，任务ID=" + job.getTitle());
                        throw new IllegalStateException("任务点执行失败: " + job.getId(), e);
                    }
                }
                pointIndex++;

                int totalChapters = chapterPointList.size();
                double currentPercentage = (pointIndex * 100.0) / totalChapters;
                log.setCurrentChapterIndex(pointIndex - 1);
                log.setEndTime(LocalDateTime.now());
                log.setCurrentProgress(BigDecimal.valueOf(currentPercentage));
                logQueue.offer(log);


            } catch (Exception e) {
                log.setStatus(WkTaskStatusEnum.ABNORMAL.getCode());
                log.setErrorMessage("章节处理异常");
                logQueue.offer(log);
                return false;
            }
        }

        if (CoursePointNavigator.isComplete(pointIndex, chapterPointList.size())) {
            log.setEndTime(LocalDateTime.now());
            log.setCurrentChapterIndex(pointIndex - 1);
            log.setCurrentProgress(BigDecimal.valueOf(100));
            log.setStatus(WkTaskStatusEnum.FINISHED.getCode());

            logQueue.offer(log);
        }
        return CoursePointNavigator.isComplete(pointIndex, chapterPointList.size());
    }

    private JobInfo requiredJobInfo(List<JobInfo> jobInfo, Job job) {
        if (jobInfo == null || jobInfo.isEmpty()) {
            throw new IllegalStateException("任务点缺少执行信息: " + job.getId());
        }
        return jobInfo.get(0);
    }
}
