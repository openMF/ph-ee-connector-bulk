package org.mifos.connector.bulk.schema;

public class PageableDTO {

    private SortDTO sort;

    private Long pageSize;

    private Long pageNumber;

    private Long offset;

    private boolean unpaged;

    private boolean paged;

    public PageableDTO(){}

    public PageableDTO(SortDTO sort, Long pageSize, Long pageNumber, Long offset, boolean unpaged, boolean paged) {
        this.sort = sort;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.offset = offset;
        this.unpaged = unpaged;
        this.paged = paged;
    }

    public SortDTO getSort() {
        return sort;
    }

    public void setSort(SortDTO sort) {
        this.sort = sort;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Long pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public boolean isUnpaged() {
        return unpaged;
    }

    public void setUnpaged(boolean unpaged) {
        this.unpaged = unpaged;
    }

    public boolean isPaged() {
        return paged;
    }

    public void setPaged(boolean paged) {
        this.paged = paged;
    }
}
