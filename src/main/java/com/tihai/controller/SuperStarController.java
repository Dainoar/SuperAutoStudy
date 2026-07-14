package com.tihai.controller;

import com.tihai.common.R;
import com.tihai.dubbo.dto.CourseSubmitTaskDTO;
import com.tihai.service.superstar.impl.SuperStarTaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

/**
 * @Copyright : DuanInnovator
 * @Description : 超星学习通-控制器
 * @Author : DuanInnovator
 * @CreateTime : 2025/2/28
 * @Link : <a href="https://github.com/DuanInnovator/SuperAutotudy">...</a>
 **/
@SuppressWarnings("all")
@RestController
@RequestMapping("/chaoxing")
public class SuperStarController {

    @Autowired
    private SuperStarTaskServiceImpl chaoXingTaskService;

    /**
     * 添加网课代刷任务
     *
     * @return
     */
    @PostMapping("")
    public R addChaxingTask(@RequestBody @Valid CourseSubmitTaskDTO courseSubmitTaskDTO) throws Exception {
        chaoXingTaskService.addChaoXingTask(courseSubmitTaskDTO);
        return R.ok();
    }

    /**
     * 启动任务
     */
    @GetMapping("")
    public void start(){
        chaoXingTaskService.startChaoxingTask();
    }

    @GetMapping("/tasks")
    public R listTasks(@RequestParam(required = false) String loginAccount) {
        List<?> tasks = chaoXingTaskService.listTasks(loginAccount);
        return R.ok().put(tasks);
    }

    @DeleteMapping("/tasks/{taskId}")
    public R pauseTask(@PathVariable String taskId) {
        return chaoXingTaskService.pauseTask(taskId)
                ? R.ok()
                : R.error(404, "任务不存在或已完成");
    }
}

