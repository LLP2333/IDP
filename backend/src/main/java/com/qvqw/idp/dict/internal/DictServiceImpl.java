package com.qvqw.idp.dict.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.dict.Dict;
import com.qvqw.idp.dict.DictItem;
import com.qvqw.idp.dict.DictService;
import com.qvqw.idp.dict.model.req.DictItemReq;
import com.qvqw.idp.dict.model.req.DictReq;
import com.qvqw.idp.dict.model.resp.DictItemResp;
import com.qvqw.idp.dict.model.resp.DictResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典服务实现：维护 {@code idp_sys_dict} 与 {@code idp_sys_dict_item} 两张表。
 *
 * <p>该模块不缓存到 Redis：字典数据量小、读多写少，前端已通过 React Query 做 5min staleTime
 * 缓存，足以应对日常负载。后续如需更高性能再加 Caffeine 本地缓存即可。</p>
 */
@Service
public class DictServiceImpl implements DictService {

    private final DictRepository dictRepository;
    private final DictItemRepository dictItemRepository;

    public DictServiceImpl(DictRepository dictRepository, DictItemRepository dictItemRepository) {
        this.dictRepository = dictRepository;
        this.dictItemRepository = dictItemRepository;
    }

    @Override
    public List<DictResp> listDict() {
        return dictRepository.findAllByOrderByIdAsc().stream().map(DictServiceImpl::toResp).toList();
    }

    @Override
    public DictResp getDict(Long id) {
        return toResp(loadDict(id));
    }

    @Override
    @Transactional
    public Long createDict(DictReq req) {
        if (dictRepository.existsByCode(req.getCode())) {
            throw new BusinessException("字典编码已存在");
        }
        Dict dict = new Dict();
        dict.setName(req.getName());
        dict.setCode(req.getCode());
        dict.setDescription(req.getDescription());
        dict.setIsSystem(false);
        return dictRepository.save(dict).getId();
    }

    @Override
    @Transactional
    public void updateDict(Long id, DictReq req) {
        Dict dict = loadDict(id);
        if (Boolean.TRUE.equals(dict.getIsSystem()) && !dict.getCode().equals(req.getCode())) {
            throw new BusinessException("系统内置字典不允许修改编码");
        }
        if (!dict.getCode().equals(req.getCode()) && dictRepository.existsByCode(req.getCode())) {
            throw new BusinessException("字典编码已存在");
        }
        dict.setName(req.getName());
        dict.setCode(req.getCode());
        dict.setDescription(req.getDescription());
        dictRepository.save(dict);
    }

    @Override
    @Transactional
    public void deleteDict(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Dict dict = loadDict(id);
            if (Boolean.TRUE.equals(dict.getIsSystem())) {
                throw new BusinessException("系统内置字典不允许删除: " + dict.getName());
            }
        }
        for (Long id : ids) {
            dictItemRepository.deleteByDictId(id);
        }
        dictRepository.deleteAllById(ids);
    }

    @Override
    public List<DictItemResp> listItems(Long dictId) {
        // 校验存在
        loadDict(dictId);
        return dictItemRepository.findAllByDictIdOrderBySortAsc(dictId)
                .stream().map(DictServiceImpl::toItemResp).toList();
    }

    @Override
    public List<DictItemResp> listItemsByCode(String code) {
        return dictRepository.findByCode(code)
                .map(d -> dictItemRepository.findAllByDictIdAndStatusOrderBySortAsc(d.getId(), 1)
                        .stream().map(DictServiceImpl::toItemResp).toList())
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public Long createItem(Long dictId, DictItemReq req) {
        loadDict(dictId);
        if (dictItemRepository.existsByDictIdAndValue(dictId, req.getValue())) {
            throw new BusinessException("该字典下已存在相同存储值");
        }
        DictItem item = new DictItem();
        item.setDictId(dictId);
        item.setLabel(req.getLabel());
        item.setValue(req.getValue());
        item.setColor(req.getColor());
        item.setSort(req.getSort() == null ? 999 : req.getSort());
        item.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        item.setIsSystem(false);
        return dictItemRepository.save(item).getId();
    }

    @Override
    @Transactional
    public void updateItem(Long itemId, DictItemReq req) {
        DictItem item = dictItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("字典明细不存在"));
        if (Boolean.TRUE.equals(item.getIsSystem()) && !item.getValue().equals(req.getValue())) {
            throw new BusinessException("系统内置字典明细不允许修改存储值");
        }
        if (!item.getValue().equals(req.getValue())
                && dictItemRepository.existsByDictIdAndValue(item.getDictId(), req.getValue())) {
            throw new BusinessException("该字典下已存在相同存储值");
        }
        item.setLabel(req.getLabel());
        item.setValue(req.getValue());
        item.setColor(req.getColor());
        if (req.getSort() != null) {
            item.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            item.setStatus(req.getStatus());
        }
        dictItemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        for (Long id : itemIds) {
            DictItem item = dictItemRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("字典明细不存在: " + id));
            if (Boolean.TRUE.equals(item.getIsSystem())) {
                throw new BusinessException("系统内置字典明细不允许删除: " + item.getLabel());
            }
        }
        dictItemRepository.deleteAllById(itemIds);
    }

    private Dict loadDict(Long id) {
        return dictRepository.findById(id)
                .orElseThrow(() -> new BusinessException("字典不存在"));
    }

    private static DictResp toResp(Dict dict) {
        DictResp resp = new DictResp();
        resp.setId(dict.getId());
        resp.setName(dict.getName());
        resp.setCode(dict.getCode());
        resp.setDescription(dict.getDescription());
        resp.setIsSystem(dict.getIsSystem());
        resp.setCreatedAt(dict.getCreatedAt());
        resp.setUpdatedAt(dict.getUpdatedAt());
        return resp;
    }

    private static DictItemResp toItemResp(DictItem item) {
        DictItemResp resp = new DictItemResp();
        resp.setId(item.getId());
        resp.setDictId(item.getDictId());
        resp.setLabel(item.getLabel());
        resp.setValue(item.getValue());
        resp.setColor(item.getColor());
        resp.setSort(item.getSort());
        resp.setStatus(item.getStatus());
        resp.setIsSystem(item.getIsSystem());
        return resp;
    }
}
