package com.tihai.exception;


import com.tihai.common.R;
import com.tihai.enums.BizCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Copyright : DuanInnovator
 * @Description : 全局异常处理
 * @Author : DuanInnovator
 * @CreateTime : 2025/2/24
 * @Link : <a href="https://github.com/DuanInnovator/TiHaiWuYou-Admin/tree/mine-admin">...</a>
 **/
@RestControllerAdvice
@Slf4j
public class GlobalException {


    /**
     * 公共错误数据构建方法
     *
     * @param bindingResult 异常
     */
    private Map<String, String> buildErrorDetails(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        bindingResult.getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }


    /**
     * 处理请求体未携带任何数据异常
     *
     * @param ex 异常信息
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleMissingRequestBody(HttpMessageNotReadableException ex) {
        return R.error(BizCodeEnum.METHOD_ARGUMENT_NOT_VALID_CODE.getCode(), BizCodeEnum.METHOD_ARGUMENT_NOT_VALID_CODE.getMsg());  //提示参数不能为空
    }


    /**
     * 处理MethodArgumentNotValidException,这个异常通常与 @RequestBody 或 @ModelAttribute 一起使用，用于验证复杂对象的字段（如DTO）。
     *
     * @param ex 异常信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleValidationExceptions(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        Map<String, String> errors = buildErrorDetails(bindingResult);
        return R.error(BizCodeEnum.METHOD_ARGUMENT_NOT_VALID_CODE.getCode(), BizCodeEnum.METHOD_ARGUMENT_NOT_VALID_CODE.getMsg(), errors);


    }

    /**
     * 参数绑定失败（类型转换、缺失参数）	@RequestParam/@PathVariable 绑定失败（如参数不存在或类型错误）
     *
     * @param ex 异常信息
     */
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException ex) {
        Map<String, String> errors = buildErrorDetails(ex);
        return R.error(BizCodeEnum.METHOD_ARGUMENT_NOT_VALID_CODE.getCode(), BizCodeEnum.METHOD_ARGUMENT_NOT_VALID_CODE.getMsg(), errors);

    }

    @ExceptionHandler(BusinessException.class)
    public R handleBusinessException(BusinessException ex) {
        return R.error(ex.getCode(), ex.getMsg(), ex.getData());
    }

    @ExceptionHandler(Exception.class)
    public R ex(Exception ex) {
        log.error("未处理的请求异常", ex);
        return R.error(BizCodeEnum.SYSTEM_ERROR.getCode(), BizCodeEnum.SYSTEM_ERROR.getMsg(), null);
    }
}
