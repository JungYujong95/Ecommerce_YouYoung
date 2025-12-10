package org.example.global.common;

public record PagingInfo(
        int currentPage,
        int pageSize,
        long totalElements
) {
}
