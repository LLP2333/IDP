package com.qvqw.idp.file.internal;

/**
 * 文件类型聚合统计中间结果。
 *
 * @param type   类型整数
 * @param size   该类型总字节数（{@code null} 表示无数据）
 * @param number 该类型文件数（{@code null} 表示无数据）
 */
public record FileTypeStat(Integer type, Long size, Long number) {
}
