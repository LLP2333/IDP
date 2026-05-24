package com.qvqw.idp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 主入口。
 *
 * <p>启动后将由 Spring Modulith 自动扫描 {@code com.qvqw.idp} 下的各模块。
 * 同时开启 {@link EnableScheduling} 让 notice 模块的 {@code NoticeScheduler}
 * 可以按 fixedDelay 触发定时发布。</p>
 */
@SpringBootApplication
@EnableScheduling
public class IdpApplication {

    /**
     * JVM 入口；不要在此处放业务逻辑，把启动后的初始化工作放到 {@code CommandLineRunner} 中。
     *
     * @param args 命令行参数（透传给 Spring）
     */
    public static void main(String[] args) {
        SpringApplication.run(IdpApplication.class, args);
    }

}
