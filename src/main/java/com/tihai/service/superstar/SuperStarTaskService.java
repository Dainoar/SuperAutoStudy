package com.tihai.service.superstar;

import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihai.domain.chaoxing.SuperStarTask;
import com.tihai.dubbo.dto.CourseSubmitTaskDTO;

import java.io.IOException;
import java.util.List;

/**
 * @Copyright : DuanInnovator
 * @Description : 超星学习通=服务接口
 * @Author : DuanInnovator
 * @CreateTime : 2025/3/1
 * @Link : <a href="https://github.com/DuanInnovator/SuperAutotudy">...</a>
 **/
@SuppressWarnings("all")
public interface SuperStarTaskService extends IService<SuperStarTask> {


    /**
     * 根据账号和课程id查询超星学习任务
     *
     * @param loginAccount
     * @param courseId
     * @return
     */
    SuperStarTask getSuperStarTask(String loginAccount, String courseId);

    /**
     * 根据课程名称查询超星学习任务
     *
     * @param loginAccount
     * @param courseName
     * @return
     */
    SuperStarTask getSuperStarTaskByCourseName(String loginAccount, String courseName);

    /**
     * 添加超星学习任务
     *
     * @param courseSubmitTaskDTO
     */
    void addChaoXingTask(CourseSubmitTaskDTO courseSubmitTaskDTO) throws NacosException;


    /**
     * 从任务队列中取出未开始的超星学习任务，开始执行
     */
    void startChaoxingTask();

    /**
     * 执行超星学习任务
     *
     * @param task
     */
    void executeCourseTask(SuperStarTask task) throws IOException;

    List<SuperStarTask> listTasks(String loginAccount);

    boolean pauseTask(String taskId);
}

