package com.kcode.web;

import com.kcode.core.tool.glob.Globber;
import com.kcode.core.tool.glob.Globber.SortOrder;
import com.kcode.core.utils.CommonUtil;
import com.kcode.web.dto.file.FileSearchResponse;
import com.kcode.web.dto.file.FileSearchResponse.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件搜索控制器
 * <p>
 * 提供 Glob 文件搜索接口，用于 @ 提及文件功能
 *
 * @author liwenguang
 * @since 2026/3/17
 */
@RestController
@RequestMapping("/api/files")
public class FileSearchController {

    private static final Logger logger = LoggerFactory.getLogger(FileSearchController.class);

    /**
     * 搜索文件
     * <p>
     * GET /api/files/search?pattern=xxx
     *
     * @param pattern 搜索模式（支持 glob 模式）
     * @return 匹配的文件列表
     */
    @GetMapping("/search")
    public ResponseEntity<FileSearchResponse> searchFiles(
            @RequestParam(required = false, defaultValue = "") String pattern) {
        
        logger.debug("搜索文件: pattern={}", pattern);
        
        String workingDir = CommonUtil.getWorkingDir();
        List<FileItem> files = new ArrayList<>();
        
        try {
            // 如果没有输入模式，返回空列表
            if (pattern == null || pattern.trim().isEmpty()) {
                return ResponseEntity.ok(FileSearchResponse.of(files));
            }
            
            // 构建 glob 模式：支持模糊匹配
            String globPattern = "*" + pattern + "*";
            
            List<Path> matchedPaths = Globber.builder(workingDir)
                    .include(globPattern)
                    .exclude("**/target/**")
                    .exclude("**/node_modules/**")
                    .exclude("**/.git/**")
                    .exclude("**/.idea/**")
                    .exclude("**/*.class")
                    .exclude("**/*.jar")
                    .exclude("**/*.bin")
                    .exclude("**/dist/**")
                    .recursive(true)
                    .build()
                    .find(SortOrder.MODIFIED_DESC, 50);
            
            // 转换为 FileItem 列表
            Path workPath = Paths.get(workingDir);
            files = matchedPaths.stream()
                    .map(path -> {
                        String name = path.getFileName().toString();
                        String type = getFileType(path);
                        String relativePath = workPath.relativize(path).toString();
                        return new FileItem(relativePath, name, type);
                    })
                    .collect(Collectors.toList());
            
            logger.debug("找到 {} 个文件", files.size());
            return ResponseEntity.ok(FileSearchResponse.of(files));
            
        } catch (Exception e) {
            logger.error("搜索文件失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(FileSearchResponse.of(files));
        }
    }
    
    /**
     * 获取文件类型
     */
    private String getFileType(Path path) {
        if (Files.isDirectory(path)) {
            return "directory";
        }
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "file";
    }
}