package com.iccuu.general_web_backend.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    public <T> IPage<T> toPage() {
        return new Page<>(page, size);
    }
}
