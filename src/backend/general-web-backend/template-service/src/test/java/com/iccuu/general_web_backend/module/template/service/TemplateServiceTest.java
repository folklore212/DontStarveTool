package com.iccuu.general_web_backend.module.template.service;

import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.template.entity.Template;
import com.iccuu.general_web_backend.module.template.mapper.TemplateMapper;
import com.iccuu.general_web_backend.module.template.mapper.TemplateWorldGenBindingMapper;
import com.iccuu.general_web_backend.module.template.mapper.WorldGenPresetMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TemplateServiceTest {
    private TemplateMapper templateMapper;
    private TemplateService service;
    private MockedStatic<SecurityUtil> securityMock;

    @BeforeEach
    void setup() {
        securityMock = mockStatic(SecurityUtil.class);
        securityMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
        templateMapper = mock(TemplateMapper.class);
        var presetMapper = mock(WorldGenPresetMapper.class);
        var bindingMapper = mock(TemplateWorldGenBindingMapper.class);
        service = new TemplateService(templateMapper, presetMapper, bindingMapper);
    }

    @AfterEach
    void tearDown() { securityMock.close(); }

    @Test
    void createShouldSetDefaultsAndInsert() {
        doAnswer(inv -> { Template t = inv.getArgument(0); t.setId(100L); return 1; })
            .when(templateMapper).insert(any(Template.class));
        Template input = new Template();
        input.setName("Test Server"); input.setTemplateType("server_template"); input.setAuthorId(1L);
        Template result = service.create(input);
        assertNotNull(result); assertEquals(100L, result.getId()); assertEquals("Test Server", result.getName());
    }

    @Test
    void forkShouldCopyTemplateWithNewAuthor() {
        Template original = new Template();
        original.setId(1L); original.setName("Cool Server"); original.setAuthorId(5L);
        original.setDownloadCount(0); original.setTemplateType("server_template");
        original.setStatus("published"); original.setDeletedAt(0L);
        when(templateMapper.selectById(1L)).thenReturn(original);
        doAnswer(inv -> { Template t = inv.getArgument(0); t.setId(200L); return 1; })
            .when(templateMapper).insert(any(Template.class));
        Template forked = service.fork(1L);
        assertNotNull(forked); assertEquals(200L, forked.getId()); assertEquals(1L, forked.getAuthorId());
    }

    @Test
    void deleteShouldMarkStatusArchived() {
        Template existing = new Template();
        existing.setId(1L); existing.setAuthorId(1L); existing.setDeletedAt(0L); existing.setStatus("published");
        when(templateMapper.selectById(1L)).thenReturn(existing);
        service.delete(1L);
        assertEquals("archived", existing.getStatus());
        verify(templateMapper).updateById(existing);
    }
}
