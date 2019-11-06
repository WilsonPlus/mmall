package com.mmall.controller.backend;

import com.mmall.common.ServerResponse;
import com.mmall.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author YaningLiu
 * @date 2018/9/8/ 9:29
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManagerController {
    private final ICategoryService categoryService;

    @Autowired
    public CategoryManagerController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /*
    权限校验全由拦截器进行拦截校验
     */
    @RequestMapping(value = "/add_category.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> addCategory(@RequestParam(value = "parentId", defaultValue = "0") Integer parentNodeId,
                                              String nodeName) {
        return categoryService.addCategory(parentNodeId, nodeName);
    }

    @RequestMapping("/set_category_name.do")
    @ResponseBody
    public ServerResponse setCategoryName(Integer categoryId, String categoryName) {
        //更新categoryName
        return categoryService.updateCategoryName(categoryId, categoryName);
    }

    @RequestMapping("/get_category.do")
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(@RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
        //查询子节点的category信息,并且不递归,保持平级
        return categoryService.getParallelCategory(categoryId);
    }

    @RequestMapping("/get_deep_category.do")
    @ResponseBody
    public ServerResponse getCategoryAndDeepChildrenCategory(@RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
        return categoryService.getAllCategoryIdById(categoryId);
    }
}
