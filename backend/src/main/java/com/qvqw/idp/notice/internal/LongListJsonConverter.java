package com.qvqw.idp.notice.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * 将 {@code List<Long>} 序列化为 JSON 字符串存入 TEXT 列。
 *
 * <p>用 Jackson 序列化是为了同时兼容 PostgreSQL 与 H2（测试 profile）；
 * PostgreSQL 上想升级为原生 jsonb，可改为 {@code @JdbcTypeCode(SqlTypes.JSON)}，
 * 但当前更看重跨库一致性，先用 TEXT。</p>
 */
@Converter
public class LongListJsonConverter implements AttributeConverter<List<Long>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Long>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<Long> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("noticeUsers 序列化失败", e);
        }
    }

    @Override
    public List<Long> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("noticeUsers 反序列化失败: " + dbData, e);
        }
    }
}
