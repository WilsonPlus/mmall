package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author YaningLiu
 * @date 2018/9/9/ 19:26
 */
public interface IFileService {
    String upload(MultipartFile multipartFile);
}
