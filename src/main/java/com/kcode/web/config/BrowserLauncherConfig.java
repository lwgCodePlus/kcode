package com.kcode.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;

/**
 * @author liwenguang
 * @since 2026/3/17
 */
@Configuration
public class BrowserLauncherConfig {

    private static final Logger logger = LoggerFactory.getLogger(BrowserLauncherConfig.class);

    @Value("${server.port:8080}")
    private String port;

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String url = "http://localhost:" + port;
        logger.info("\n======================================================\n  KCode 服务已启动成功！\n  web 访问地址: {}\n======================================================\n", url);
        tryOpenWithSystemCommand(url);
    }

    // 调用系统命令打开
    private void tryOpenWithSystemCommand(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                // Windows
                pb = new ProcessBuilder("cmd", "/c", "start", url);
            } else if (os.contains("mac")) {
                // MacOS
                pb = new ProcessBuilder("open", url);
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux
                pb = new ProcessBuilder("xdg-open", url);
            } else {
                return;
            }
            pb.start();
        } catch (IOException ignored) {
        }
    }
}