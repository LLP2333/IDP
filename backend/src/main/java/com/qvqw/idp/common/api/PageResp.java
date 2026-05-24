package com.qvqw.idp.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应结构。
 *
 * @param <T> 列表元素类型
 */
@Schema(description = "分页响应")
public class PageResp<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "总记录数", example = "42")
    private long total;

    @Schema(description = "当前页码（从 1 开始）", example = "1")
    private int page;

    @Schema(description = "每页数量", example = "10")
    private int size;

    public PageResp() {
    }

    public PageResp(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 将 Spring Data 的 {@link Page} 转成统一的 {@link PageResp}。
     *
     * @param source    Spring Data 分页结果（页码从 0 开始）
     * @param converter 元素转换函数（实体 → DTO）
     * @param <S>       源类型
     * @param <T>       目标类型
     * @return 当前页码从 1 开始的分页响应
     */
    public static <S, T> PageResp<T> from(Page<S> source, Function<S, T> converter) {
        List<T> list = source.getContent().stream().map(converter).toList();
        return new PageResp<>(list, source.getTotalElements(), source.getNumber() + 1, source.getSize());
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
