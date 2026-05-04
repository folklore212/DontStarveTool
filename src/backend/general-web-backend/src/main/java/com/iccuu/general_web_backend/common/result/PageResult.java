package com.iccuu.general_web_backend.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> extends R<List<T>> {

    private long total;
    private int page;
    private int size;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setCode(0);
        result.setMessage("ok");
        result.setTimestamp(System.currentTimeMillis());
        result.setData(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setList(list);
        return result;
    }
}
