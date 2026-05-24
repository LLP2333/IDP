package com.qvqw.idp.common.api;

import org.springframework.data.domain.Page;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应结构
 *
 * @param <T> 列表元素类型
 */
public class PageResp<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public PageResp() {
    }

    public PageResp(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

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
