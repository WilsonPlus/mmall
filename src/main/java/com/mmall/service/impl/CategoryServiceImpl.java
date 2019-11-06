package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author YaningLiu
 * @date 2018/9/8/ 9:57
 */
@Service
public class CategoryServiceImpl implements ICategoryService {
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public ServerResponse<String> addCategory(Integer parentNodeId, String nodeName) {
        if (parentNodeId == null || StringUtils.isBlank(nodeName)) {
            return ServerResponse.createBySuccessMsg("参数错误！");
        }

        Category target = new Category();
        target.setParentId(parentNodeId);
        target.setName(nodeName);
        target.setStatus(true);

        int rowCount = categoryMapper.insert(target);
        if (rowCount < 1) {
            return ServerResponse.createByErrorMsg("添加失败！");
        }
        return ServerResponse.createByErrorMsg("添加成功！");
    }

    @Override
    public ServerResponse<String> updateCategoryName(Integer categoryId, String categoryName) {
        if(StringUtils.isEmpty(categoryName)|| categoryId == null){
            ResponseCode code = ResponseCode.ILLEGAL_ARGUMENT;
            return ServerResponse.createByErrorCodeMessage(code.getCode(),code.getDesc());
        }
        Category target = new Category();
        target.setId(categoryId);

        target.setName(categoryName);
        int rowCount = categoryMapper.updateByPrimaryKeySelective(target);
        if (rowCount < 1) {
            return ServerResponse.createByErrorMsg("更新失败！");
        }
        return ServerResponse.createBySuccessMsg("更新成功！");
    }

    @Override
    public ServerResponse getParallelCategory(Integer parentId) {
        List<Category> categories = categoryMapper.selectByParentId(parentId);
        if (categories.isEmpty()) {
            return ServerResponse.createByErrorMsg("未找到与当前平级的分类。");
        }
        return ServerResponse.createBySuccess(categories);
    }

    @Override
    public ServerResponse<List<Integer>> getAllCategoryIdById(Integer categoryId) {

        Set<Category> childCategorys = findChildCategory(categoryId);
        List<Integer> categoryIdList = Lists.newArrayList();

        if (childCategorys != null){
            for (Category item : childCategorys) {
                categoryIdList.add(item.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }

    // 可以对比原来的写法。
    private Set<Category> findChildCategory(Integer categoryId) {
        // 用来存储category以及其子category
        Set<Category> categorySet = Sets.newHashSet();

        // 所有的子分类
        List<Category> categoryList = categoryMapper.selectByParentId(categoryId);

        // 若这个categoryList查出来是个空的，说明当前的分类节点是没有子分类的
        if (categoryList != null && categoryList.isEmpty()) {
            // 若这个分类没有子分类，将它加入到set集合中
            categorySet.add(categoryMapper.selectByPrimaryKey(categoryId));
        } else {
            // 若它是存在子分类节点的，进行集合遍历
            for (Category listItem : categoryList) {
                // 先将当前节点存进集合中。
                categorySet.add(listItem);
                // 递归调用，获取当前分类的子分类，并且将返回的分类节点集合存储到集合中。
                categorySet.addAll(findChildCategory(listItem.getId()));
            }
        }
        // 返回集合
        return categorySet;
    }
}
