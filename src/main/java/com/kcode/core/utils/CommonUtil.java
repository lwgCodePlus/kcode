package com.kcode.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author liwenguang
 * @since 2026/3/9
 */
public class CommonUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);

    /**
     * 获取当前java 工作目录
     */
    public static String getWorkingDir() {
        return System.getProperty("user.dir");
    }

    /**
     * 验证并解析路径
     *
     * @param path    用户提供的路径（可能为null或相对路径）
     * @param baseDir 基础目录
     * @return 解析后的绝对路径字符串
     */
    public static String validatePath(String path, Path baseDir) {
        if (path == null || path.trim().isEmpty()) {
            // 如果path为空，使用baseDir
            return baseDir != null ? baseDir.toString() : getWorkingDir();
        }

        Path resolved = Paths.get(path);
        if (!resolved.isAbsolute()) {
            // 相对路径，相对于baseDir解析
            resolved = baseDir != null ? baseDir.resolve(path) : Paths.get(getWorkingDir()).resolve(path);
        }

        return resolved.toAbsolutePath().normalize().toString();
    }

    /**
     * 执行command 命令
     *
     * @param command 命令
     * @return 是否执行成功
     */
    public static boolean checkCommand(String command) {
        Process process = null;
        try {
            // Windows 下需要通过 cmd /c 执行命令
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.redirectErrorStream(true);
            process = pb.start();

            boolean completed = process.waitFor(2, TimeUnit.SECONDS);

            if (!completed) {
                // 超时，销毁进程
                process.destroyForcibly();
                return false;
            }

            // 进程完成，检查退出码
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 安装 npm 包
     *
     * @param pkg npm 包名
     */
    public static void installNpmPackage(String pkg) {
        Process process = null;
        try {
            process = new ProcessBuilder("cmd", "/c", "npm", "install", "-g", pkg,
                    "--registry=https://registry.npmmirror.com")
                    .inheritIO()
                    .start();

            // npm install 最长等待 5 分钟
            boolean completed = process.waitFor(5, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                log.error("Timeout installing{}", pkg);
            }

            if (process.exitValue() != 0) {
                log.error("Failed to install {} exit code: {}", pkg, process.exitValue());
            }
        } catch (Exception e) {
            log.error("Failed to install {} ", pkg);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
