package com.kcode.core.tool.glob;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 支持：通配符、递归搜索、排除模式、相对/绝对路径、按修改时间排序
 */
public class Globber {

    private final Path baseDir;
    private final boolean recursive;
    private final List<PathMatcher> includeMatchers;
    private final List<PathMatcher> excludeMatchers;

    private Globber(Builder builder) {
        this.baseDir = builder.baseDir;
        this.recursive = builder.recursive;
        this.includeMatchers = builder.includePatterns.stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .collect(Collectors.toList());
        this.excludeMatchers = builder.excludePatterns.stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .collect(Collectors.toList());
    }

    /**
     * 执行glob搜索
     *
     * @return 匹配的文件路径列表
     */
    public List<Path> find() throws IOException {
        return find(SortOrder.NONE, -1);
    }

    /**
     * 执行glob搜索
     *
     * @param maxResults 最大结果数量限制，<=0表示无限制
     * @return 匹配的文件路径列表
     */
    public List<Path> find(int maxResults) throws IOException {
        return find(SortOrder.NONE, maxResults);
    }

    /**
     * 执行glob搜索并按指定方式排序
     *
     * @param sortOrder  排序方式
     * @param maxResults 最大结果数量限制，<=0表示无限制
     * @return 排序后的匹配文件路径列表
     */
    public List<Path> find(SortOrder sortOrder, int maxResults) throws IOException {
        List<Path> results = new ArrayList<>();

        if (recursive) {
            // 递归搜索
            Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matches(file)) {
                        results.add(file);

                        // 检查是否达到数量限制
                        if (maxResults > 0 && results.size() >= maxResults) {
                            return FileVisitResult.TERMINATE; // 终止遍历
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 如果目录被排除，跳过整个目录
                    if (isExcluded(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // 忽略无法访问的文件
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            // 非递归搜索
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
                for (Path path : stream) {
                    if (matches(path)) {
                        results.add(path);
                    }
                }
            }
        }

        // 根据排序方式对结果进行排序
        return sortResults(results, sortOrder);
    }

    /**
     * 对结果进行排序
     */
    private List<Path> sortResults(List<Path> paths, SortOrder sortOrder) {
        if (sortOrder == SortOrder.NONE) {
            return paths;
        }

        return paths.stream()
                .sorted(getComparator(sortOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取对应的比较器 - 修正版本
     */
    private Comparator<Path> getComparator(SortOrder sortOrder) {
        return switch (sortOrder) {
            case MODIFIED_ASC -> Comparator.comparing(this::getLastModifiedTime);
            case MODIFIED_DESC -> Comparator.comparing(this::getLastModifiedTime).reversed();
            case NAME_ASC -> Comparator.comparing(p -> p.getFileName().toString());
            case NAME_DESC -> Comparator.comparing((Path p) -> p.getFileName().toString()).reversed();
            case SIZE_ASC -> Comparator.comparingLong(this::getSize);
            case SIZE_DESC -> Comparator.comparingLong(this::getSize).reversed();
            default -> Comparator.comparing(this::getLastModifiedTime);
        };
    }

    /**
     * 安全地获取文件的最后修改时间
     */
    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 安全地获取文件大小
     */
    private long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }


    /**
     * 检查路径是否匹配include且不匹配exclude
     */
    private boolean matches(Path path) {
        Path relativePath = baseDir.relativize(path);

        // 检查是否匹配任一包含模式
        boolean matched = includeMatchers.isEmpty() || includeMatchers.stream()
                .anyMatch(m -> m.matches(path) || m.matches(relativePath) ||
                        m.matches(path.getFileName()));

        if (!matched) {
            return false;
        }

        // 检查是否被排除
        return !isExcluded(path) && !isExcluded(relativePath);
    }

    private boolean isExcluded(Path path) {
        return excludeMatchers.stream()
                .anyMatch(m -> m.matches(path) || m.matches(path.getFileName()));
    }


    /**
     * 获取匹配的文件列表
     */
    public String findAsStrings(SortOrder sortOrder, int maxResults) throws IOException {
        return find(sortOrder, maxResults).stream()
                .map(Path::toString)
                .collect(Collectors.joining("\n"));
    }


    /**
     * 排序方式枚举
     */
    public enum SortOrder {
        NONE,           // 不排序
        MODIFIED_ASC,   // 按修改时间升序（最早->最晚）
        MODIFIED_DESC,  // 按修改时间降序（最晚->最早）
        NAME_ASC,       // 按文件名升序
        NAME_DESC,      // 按文件名降序
        SIZE_ASC,       // 按文件大小升序
        SIZE_DESC       // 按文件大小降序
    }

    /**
     * Builder模式，方便配置
     */
    public static class Builder {
        private Path baseDir;
        private boolean recursive = false;
        private List<String> includePatterns = new ArrayList<>();
        private List<String> excludePatterns = new ArrayList<>();

        public Builder(String baseDir) {
            this.baseDir = Paths.get(baseDir);
        }

        public Builder(Path baseDir) {
            this.baseDir = baseDir;
        }

        /**
         * 设置基础目录
         */
        public Builder baseDir(String baseDir) {
            this.baseDir = Paths.get(baseDir);
            return this;
        }

        /**
         * 添加包含模式（支持多个）
         */
        public Builder include(String... patterns) {
            for (String pattern : patterns) {
                this.includePatterns.add(normalizePattern(pattern));
            }
            return this;
        }

        /**
         * 添加排除模式（支持多个）
         */
        public Builder exclude(String... patterns) {
            for (String pattern : patterns) {
                this.excludePatterns.add(normalizePattern(pattern));
            }
            return this;
        }

        /**
         * 设置递归搜索
         */
        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        /**
         * 规范化模式字符串
         */
        private String normalizePattern(String pattern) {
            // 如果模式以/开头，确保它是相对于根目录的
            if (pattern.startsWith("/")) {
                return pattern;
            }
            // 如果模式包含**，确保递归搜索开启
            if (pattern.contains("**") && !recursive) {
                recursive = true;
            }
            return pattern;
        }

        public Globber build() {
            // 如果没有指定包含模式，默认包含所有
            if (includePatterns.isEmpty()) {
                includePatterns.add("**/*");
            }
            return new Globber(this);
        }
    }

    /**
     * 创建Builder实例的便捷方法
     */
    public static Builder builder(String baseDir) {
        return new Builder(baseDir);
    }

    public static Builder builder(Path baseDir) {
        return new Builder(baseDir);
    }
}