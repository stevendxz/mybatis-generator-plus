package com.github.leecho.idea.plugin.mybatis.generator.base;

import com.github.leecho.idea.plugin.mybatis.generator.model.PageInfo;

/**
 * BaseExample 基类
 *
 * @author Marvis
 * @ClassName BaseExample
 * @Description 增加分页参数
 * @date Jul 31, 2017 11:26:53 AM
 */
public abstract class BaseExample {

    protected PageInfo pageInfo;
    protected String groupByClause;

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public String getGroupByClause() {
        return groupByClause;
    }

    public void setGroupByClause(String groupByClause) {
        this.groupByClause = groupByClause;
    }

}