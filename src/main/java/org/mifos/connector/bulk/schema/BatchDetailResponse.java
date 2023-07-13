package org.mifos.connector.bulk.schema;

import java.util.List;

public class BatchDetailResponse {

    private List<Transfer> content;

    private PageableDTO pageable;

    private Long totalPages;

    private Long totalElements;

    private boolean last;

    private boolean first;

    private SortDTO sort;

    private Long numberOfElements;

    private Long size;

    private Long number;

    private boolean empty;

    public BatchDetailResponse(){}

    public BatchDetailResponse(List<Transfer> content, PageableDTO pageable,
                               Long totalPages, Long totalElements, boolean last, boolean first,
                               SortDTO sort, Long numberOfElements, Long size, Long number, boolean empty) {
        this.content = content;
        this.pageable = pageable;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.last = last;
        this.first = first;
        this.sort = sort;
        this.numberOfElements = numberOfElements;
        this.size = size;
        this.number = number;
        this.empty = empty;
    }

    public List<Transfer> getContent() {
        return content;
    }

    public void setContent(List<Transfer> content) {
        this.content = content;
    }

    public PageableDTO getPageable() {
        return pageable;
    }

    public void setPageable(PageableDTO pageable) {
        this.pageable = pageable;
    }

    public Long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Long totalPages) {
        this.totalPages = totalPages;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public SortDTO getSort() {
        return sort;
    }

    public void setSort(SortDTO sort) {
        this.sort = sort;
    }

    public Long getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(Long numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
