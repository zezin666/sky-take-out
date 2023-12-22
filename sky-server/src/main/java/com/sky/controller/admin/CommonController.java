package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    @ResponseBody
    @ApiOperation("文件上传接口")
    public Result<String> upload(MultipartFile file){
        try {
            log.info("文件上传：{}",file);
            String originFileName = file.getOriginalFilename();
            String extension = originFileName.substring(originFileName.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;
            String path = aliOssUtil.upload(file.getBytes(),objectName);
            return Result.success(path);
        } catch (IOException e) {
            log.info("上传失败");
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
