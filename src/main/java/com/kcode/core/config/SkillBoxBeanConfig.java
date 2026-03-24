package com.kcode.core.config;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

/**
 * SkillBox 配置类
 * <p>
 * 加载文件系统中的技能到 SkillBox
 *
 * @author liwenguang
 * @since 2026/3/9
 */
@Configuration
public class SkillBoxBeanConfig {

    private static final Logger logger = LoggerFactory.getLogger(SkillBoxBeanConfig.class);

    private final Toolkit toolkit;

    public SkillBoxBeanConfig(Toolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Bean
    public SkillBox skillBox() {
        SkillBox skillBox = new SkillBox(toolkit);
        loadSkills(skillBox);
        return skillBox;
    }

    /**
     * 从文件系统加载技能
     *
     * @param skillBox 技能容器
     */
    private void loadSkills(SkillBox skillBox) {
        try (FileSystemSkillRepository repo = new FileSystemSkillRepository(
                Path.of(System.getProperty("user.home"), ".kcode", "skills"),
                true,
                "fileSystem_")) {
            List<AgentSkill> skills = repo.getAllSkills();
            for (AgentSkill skill : skills) {
                skillBox.registerSkill(skill);
                logger.info("Loaded skill: {}", skill.getName());
            }
            logger.info("Total {} skills loaded", skills.size());
        } catch (Exception e) {
            logger.debug("No skills loaded: {}", e.getMessage());
        }
    }
}