package org.example.global.common;

import java.util.List;

public record PagingResponse<T>(
        PagingInfo paging,
        List<T> content
) {
}
