package com.github.leecho.idea.plugin.mybatis.generator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 分页查询参数类
 *
 * @author
 */
public class PageInfo {

    public static final int Default_PageSize = 20;

    // 当前页码
    protected int currentPage = 1;

    // 总页数
    protected int totalPage;

    // 总记录数
    protected int totalCount;

    // 每页条数
    protected int pageSize = Default_PageSize;

    // 开始
    protected int pageBegin = 0;

    // 结束
    protected int pageEnd = 20;

    /**
     * bean起始坐标(不包含)
     */
    private Integer pageBeginId = null;

    public static final String PageQuery_classname = "pageInfo";

    /**
     * 将分布参数传入处理，最终计算出当前页码PageQuery_currPage，开始坐标PageQuery_star，
     * 结束坐标PageQuery_end，总页数PageQuery_Psize
     * <p/>
     * 页数从1开始计数
     *
     * @param totalCount  记录总数
     * @param pageSize    每页显示个数
     * @param currentPage 当前页码
     */
    public void setPageParams(int totalCount, int pageSize, int currentPage) {

        this.totalPage = pageSize == 0 ? 1 : (int) Math.ceil((double) totalCount / (double) pageSize);

        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.currentPage = currentPage;

        float Psize_l = totalCount / (float) (this.pageSize);
        if (currentPage < 2) {
            currentPage = 1;
            pageBegin = 0;
        } else if (currentPage > Psize_l) {
            if (Psize_l == 0) {
                currentPage = 1;
            } else {
                currentPage = (int) Math.ceil(Psize_l);
            }

            pageBegin = (currentPage - 1) * this.pageSize;
        } else {
            pageBegin = (currentPage - 1) * this.pageSize;
        }
        pageSize = (int) Math.ceil(Psize_l);
        this.pageEnd = currentPage * this.pageSize;

        if (this.currentPage <= 0 || this.currentPage > this.totalPage) {
            this.pageSize = 0;
        }
    }

    /**
     * 将分布参数传入处理，最终计算出当前页码PageQuery_currPage，开始坐标PageQuery_star，
     * 结束坐标PageQuery_end，总页数PageQuery_Psize
     *
     * @param totalCount 记录总数
     */
    public void setPageParams(int totalCount) {
        this.setPageParams(totalCount, this.pageSize, this.currentPage);
    }

    @Override
    public String toString() {
        return "PageInfo [currentPage=" + currentPage + ", totalPage=" + totalPage + ", totalCount=" + totalCount
                + ", pageSize=" + pageSize + ", pageBegin=" + pageBegin + ", pageEnd=" + pageEnd + ", pageBeginId="
                + pageBeginId + "]";
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 每页显示个数
     */
    public int getPageSize() {
        return pageSize;
    }

    @JsonIgnore
    public int getPageBegin() {
        return pageBegin;
    }

    @JsonIgnore
    public int getPageEnd() {
        return pageEnd;
    }

    /**
     * bean起始id(不包含)
     */
    @JsonIgnore
    public Integer getPageBeginId() {
        return pageBeginId;
    }

    /**
     * 请求页
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * 每页显示个数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}