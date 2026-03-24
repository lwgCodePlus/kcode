package com.kcode.web.dto.file;

import java.util.List;

/**
 * 文件搜索响应
 *
 * @param files 文件列表
 * @param total 总数
 * @author liwenguang
 * @since 2026/3/17
 */
public record FileSearchResponse(
        List<FileItem> files,
        int total
) {
    /**
     * 文件项
     */
    public record FileItem(
            String path,
            String name,
            String type
    ) {}

    public static FileSearchResponse of(List<FileItem> files) {
        return new FileSearchResponse(files, files.size());
    }
}