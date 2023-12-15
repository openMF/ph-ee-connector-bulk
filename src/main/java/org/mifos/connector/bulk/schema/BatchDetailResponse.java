package org.mifos.connector.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

}
