package com.sjdf.util;

import java.io.Serializable;

/**
 * 分页访问参数类
 * @author Mx
 */
public class PagerInfo implements Serializable {

    private static final long serialVersionUID = -6529847479859886397L;

    public static final String ORDER_BY = "orderBy";
    public static final String ORDER_TYPE = "orderType";

    public static final String PREVIOUS_MAX_ID_FIELD_NAME = "previousMaxId";

    /**
     * 每页数量(默认100)
     */
    private int size = 100;
    /**
     * 页码
     */
    private int page = 1;
    /**
     * 总条数
     */
    private long totalSize;
    /**
     * 总页数
     */
    private int totalPage;
    /**
     * 排序字段
     */
    private String orderBy;
    /**
     * 排序方式 ：只有ASC或DESC,默认ASC
     * @see OrderType
     */
    private String orderType;

    /**
     * 是否需要自动统计出总条数
     */
    private Boolean queryTotalRecord = Boolean.TRUE;

    /**
     * 上一次查询的列表中ID的最大值
     */
    private Long previousMaxId;

    public PagerInfo() {

    }

    public PagerInfo(int size, int page) {
        this.size = size;
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
        this.totalPage = (int) Math.ceil(totalSize/(double)this.size);
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Boolean getQueryTotalRecord() {
        return queryTotalRecord;
    }

    public void setQueryTotalRecord(Boolean queryTotalRecord) {
        this.queryTotalRecord = queryTotalRecord;
    }

    /**
     * 是否有上一页 
     */
    public boolean isHasPrev() {
        return page > 1;
    }

    /** 
     * 是否有下一页
     */
    public boolean isHasNext() {
        return page < getTotalPage();
    }


    /**
     * 只有一页
     */
    public boolean isOnlyOne() {
        return totalPage == 1;
    }

    public Long getPreviousMaxId() {
        return previousMaxId;
    }

    public void setPreviousMaxId(Long previousMaxId) {
        this.previousMaxId = previousMaxId;
    }

    @Override
    public String toString() {
        return "PagerInfo [size=" + size + ", page=" + page + ", totalSize="
                + totalSize + ", totalPage=" + totalPage + ", orderBy="
                + orderBy + ", orderType=" + orderType + "]";
    }

}
