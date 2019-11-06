package com.mmall.service.impl;

import com.mmall.service.IFileService;
import com.mmall.util.FtpUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * @author YaningLiu
 * @date 2018/9/9/ 19:27
 */
@Service
public class FileServiceImpl implements IFileService {
    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);


    @Override
    public String upload(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            return null;
        }
        String oldName = multipartFile.getOriginalFilename();
        //判断上传的文件名是否含绝对路径，包含则将绝对路径去掉
        oldName = oldName.lastIndexOf("\\") >= 0 ? oldName.substring(oldName.lastIndexOf("\\") + 1) : oldName;
        String filePath = dirGenerateByFileName(oldName);
        //为文件名添加一个前缀，避免文件名重复。
        String filename = UUID.randomUUID().toString() + "_" + oldName;
        logger.info("开始文件上传，上传的文件名：{}，上传路径：{}，保存的文件名{}", oldName, filePath, filename);

        boolean upFlag = false;
        try {
            upFlag = FtpUtil.uploadFile(filePath, filename, multipartFile.getInputStream());
            if(!upFlag){
                return "";
            }
        } catch (IOException e) {
            logger.error("上传文件异常", e);
            return "";
        }

        return filePath + filename;
    }

    private String dirGenerateByFileName(String oldName) {
        if (StringUtils.isBlank(oldName)) {
            return "default/";
        }
        //获取文件名的哈希值，将之转换成16进制。
        int hCode = oldName.hashCode();
        String hex = Integer.toHexString(hCode);

        //将保存的文件路径和截取的字符生成文件目录返回。
        return new String(hex.charAt(0) + "/" + hex.charAt(1) + "/");
    }
}
