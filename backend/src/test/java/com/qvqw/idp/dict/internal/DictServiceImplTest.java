package com.qvqw.idp.dict.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.dict.Dict;
import com.qvqw.idp.dict.DictItem;
import com.qvqw.idp.dict.model.req.DictItemReq;
import com.qvqw.idp.dict.model.req.DictReq;
import com.qvqw.idp.dict.model.resp.DictItemResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DictServiceImplTest {

    @Mock
    private DictRepository dictRepository;

    @Mock
    private DictItemRepository dictItemRepository;

    @InjectMocks
    private DictServiceImpl dictService;

    @Test
    void createDuplicateCodeShouldThrow() {
        DictReq req = new DictReq();
        req.setName("分类");
        req.setCode("notice_type");
        when(dictRepository.existsByCode("notice_type")).thenReturn(true);

        assertThatThrownBy(() -> dictService.createDict(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("字典编码已存在");
    }

    @Test
    void createSuccessReturnsId() {
        DictReq req = new DictReq();
        req.setName("分类");
        req.setCode("biz_color");
        when(dictRepository.existsByCode("biz_color")).thenReturn(false);
        Dict saved = new Dict();
        saved.setId(7L);
        when(dictRepository.save(any(Dict.class))).thenReturn(saved);

        Long id = dictService.createDict(req);

        assertThat(id).isEqualTo(7L);
        ArgumentCaptor<Dict> captor = ArgumentCaptor.forClass(Dict.class);
        verify(dictRepository).save(captor.capture());
        assertThat(captor.getValue().getIsSystem()).isFalse();
    }

    @Test
    void updateSystemDictCannotChangeCode() {
        Dict dict = new Dict();
        dict.setId(1L);
        dict.setCode("notice_type");
        dict.setIsSystem(true);
        when(dictRepository.findById(1L)).thenReturn(Optional.of(dict));

        DictReq req = new DictReq();
        req.setName("分类");
        req.setCode("notice_type_v2");
        assertThatThrownBy(() -> dictService.updateDict(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置字典不允许修改编码");
    }

    @Test
    void deleteSystemDictShouldThrow() {
        Dict dict = new Dict();
        dict.setId(1L);
        dict.setIsSystem(true);
        dict.setName("分类");
        when(dictRepository.findById(1L)).thenReturn(Optional.of(dict));

        assertThatThrownBy(() -> dictService.deleteDict(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置字典不允许删除");
    }

    @Test
    void deleteShouldClearItems() {
        Dict dict = new Dict();
        dict.setId(2L);
        dict.setIsSystem(false);
        dict.setName("自定义");
        when(dictRepository.findById(2L)).thenReturn(Optional.of(dict));

        dictService.deleteDict(List.of(2L));

        verify(dictItemRepository).deleteByDictId(2L);
        verify(dictRepository).deleteAllById(List.of(2L));
    }

    @Test
    void listItemsByCodeReturnsEnabledOnly() {
        Dict dict = new Dict();
        dict.setId(3L);
        dict.setCode("notice_type");
        when(dictRepository.findByCode("notice_type")).thenReturn(Optional.of(dict));
        DictItem item = new DictItem();
        item.setId(1L);
        item.setDictId(3L);
        item.setValue("1");
        item.setLabel("公告");
        item.setStatus(1);
        when(dictItemRepository.findAllByDictIdAndStatusOrderBySortAsc(3L, 1))
                .thenReturn(List.of(item));

        List<DictItemResp> result = dictService.listItemsByCode("notice_type");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getValue()).isEqualTo("1");
    }

    @Test
    void listItemsByMissingCodeReturnsEmpty() {
        when(dictRepository.findByCode("missing")).thenReturn(Optional.empty());
        assertThat(dictService.listItemsByCode("missing")).isEmpty();
    }

    @Test
    void createItemDuplicateValueShouldThrow() {
        when(dictRepository.findById(1L)).thenReturn(Optional.of(new Dict()));
        when(dictItemRepository.existsByDictIdAndValue(1L, "1")).thenReturn(true);
        DictItemReq req = new DictItemReq();
        req.setLabel("公告");
        req.setValue("1");
        assertThatThrownBy(() -> dictService.createItem(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在相同存储值");
    }

    @Test
    void deleteSystemItemShouldThrow() {
        DictItem item = new DictItem();
        item.setId(1L);
        item.setIsSystem(true);
        item.setLabel("公告");
        when(dictItemRepository.findById(1L)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> dictService.deleteItems(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置字典明细不允许删除");
    }
}
