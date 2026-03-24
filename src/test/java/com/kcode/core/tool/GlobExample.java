package com.kcode.core.tool;

import com.kcode.core.tool.glob.Globber;

import java.io.IOException;

public class GlobExample {
    public static void main(String[] args) {
        try {
            // 示例1：查找所有Java文件
            String javaFiles = Globber.builder("D:\\application\\opencode-java")
                    .include("**/*.java")
                    .recursive(true)
                    .build()
                    .findAsStrings(Globber.SortOrder.MODIFIED_ASC, 2000);
            System.out.println("Java文件：");
            System.out.println(javaFiles);
            // 示例2：查找图片文件，排除临时文件
            String images = Globber.builder("./downloads")
                    .include("**/*.{jpg,png,gif}", "**/*.jpeg")
                    .exclude("**/temp/**", "**/.*")
                    .recursive(true)
                    .build()
                    .findAsStrings(Globber.SortOrder.MODIFIED_ASC, 2000);
            System.out.println("\n图片文件：");
            System.out.println(images);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}