package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.Category;

import java.util.List;

/**
 * @author YaningLiu
 * @date 2018/9/8/ 9:55
 */
public interface ICategoryService {
    /**
     * 添加分类节点。
     * @param parentNodeId  分类的父节点id
     * @param nodeName  节点的名字
     * @return
     */
    public ServerResponse<String> addCategory(Integer parentNodeId, String nodeName);

    public ServerResponse updateCategoryName(Integer categoryId, String categoryName);

    /**
     * 获取所有平级的分类
     * @param parentId    父节点分类id
     * @return
     */
    public ServerResponse<List<Category>> getParallelCategory(Integer parentId);

    /**
     * 获取当前节点的所有子节点
     * @param categoryId    分类id
     * @return
     */
    ServerResponse<List<Integer>> getAllCategoryIdById(Integer categoryId);
}
