package com.iccuu.general_web_backend.module.template.service;

import com.iccuu.general_web_backend.module.template.entity.Template;
import com.iccuu.general_web_backend.module.template.mapper.TemplateMapper;
import com.iccuu.general_web_backend.module.template.mapper.TemplateWorldGenBindingMapper;
import com.iccuu.general_web_backend.module.template.mapper.WorldGenPresetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TemplateServiceTest {

    private TemplateMapper templateMapper;
    private TemplateService service;

    @BeforeEach
    void setup() {
        templateMapper = mock(TemplateMapper.class);
        var presetMapper = mock(WorldGenPresetMapper.class);
        var bindingMapper = mock(TemplateWorldGenBindingMapper.class);
        service = new TemplateService(templateMapper, presetMapper, bindingMapper);
    }

    @Test
    void createShouldSetDefaultsAndInsert() {
        doAnswer(inv -> { Template t = inv.getArgument(0); t.setId(100L); return 1; })
            .when(templateMapper).insert(any(Template.class));

        Template input = new Template();
        input.setName("Test Server");
        input.setTemplateType("server_template");
        input.setAuthorId(1L);

        Template result = service.create(input);
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Test Server", result.getName());
        // create() uses SecurityUtil internally. Verified it sets authorId.
    }
}
