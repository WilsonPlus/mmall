package com.mmall.controller.backend;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.service.IFileService;
import com.mmall.service.IProductService;
import com.mmall.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * @author YaningLiu
 * @date 2018/9/9/ 9:27
 */
@Controller
@RequestMapping("/manage/product")
public class ProductManagerController {
    private final IProductService productService;
    private final IFileService fileService;

    @Autowired
    public ProductManagerController(IProductService productService, IFileService fileService) {
        this.productService = productService;
        this.fileService = fileService;
    }

    @RequestMapping(value = "/save.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> productSave(Product product) {
        return productService.saveOrUpDateProduct(product);
    }

    @RequestMapping(value = "/set_status.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> setProductStatus(Integer productId, Integer productStatus) {
        return productService.setProductStatus(productId, productStatus);
    }

    @RequestMapping("/get_detail.do")
    @ResponseBody
    public ServerResponse getDetail(Integer productId) {
        return productService.managerGetDetail(productId);
    }

    @RequestMapping("/get_list.do")
    @ResponseBody
    public ServerResponse getList(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return productService.getProducts(pageNum, pageSize);
    }

    @RequestMapping("/search.do")
    @ResponseBody
    public ServerResponse productSearch(String productName, Integer productId, @RequestParam(value = "pageNum", defaultValue = "1") int pageNum, @RequestParam(value = "pageSize", defaultValue = "30") int pageSize) {
        // xml中配置的过滤器只对post方式提交的有效，get提交的中文数据需要重新编码。
        try {
            if (StringUtils.isNotEmpty(productName)) {
                productName = new String(productName.getBytes("iso8859-1"), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return productService.searchProduct(productName, productId, pageNum, pageSize);
    }


    /*
    @RequestMapping(value = "/upload.do", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE )
    告诉浏览器返回的是一个json字符串
    */
    @RequestMapping(value = "/upload.do", method = RequestMethod.POST)
    @ResponseBody
    public String upload(@RequestParam(value = "upload_file", required = false) MultipartFile multipartFile) {
        String targetFileName = fileService.upload(multipartFile);
        Map<String, Object> resultMap = Maps.newHashMap();

        if (StringUtils.isBlank(targetFileName)) {
            return JSON.toJSONString(ServerResponse.createBySuccessMsg("上传失败"));
        }
        String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFileName;

        resultMap.put("uri", targetFileName);
        resultMap.put("url", url);
        return JSON.toJSONString(ServerResponse.createBySuccess(resultMap));
    }


    @RequestMapping(value = "/richtext_img_upload.do", method = RequestMethod.POST)
    @ResponseBody
    public String richtextImgUpload(@RequestParam(value = "upload_file", required = false) MultipartFile multipartFile, HttpServletResponse response) {
        Map<String, Object> resultMap = Maps.newHashMap();
        //富文本中对于返回值有要求,按照simditor的要求进行返回
        /*{
            "success": true/false,
                "msg": "error message", # optional
            "file_path": "[real file path]"
        }*/
        String targetFileName = fileService.upload(multipartFile);
        if (StringUtils.isBlank(targetFileName)) {
            resultMap.put("success", false);
            resultMap.put("msg", "上传失败");
            resultMap.put("file_path", multipartFile.getOriginalFilename());
            return JSON.toJSONString(resultMap);
        }
        String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFileName;
        resultMap.put("success", true);
        resultMap.put("msg", "上传成功");
        resultMap.put("file_path", url);
        response.addHeader("Access-Control-Allow-Headers", "X-File-Name");
        return JSON.toJSONString(resultMap);
    }
}
