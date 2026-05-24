package com.qvqw.idp.dict.internal;

import com.qvqw.idp.dict.Dict;
import com.qvqw.idp.dict.DictItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典内置数据初始化。
 *
 * <p>启动时灌入以下系统字典（幂等）：</p>
 * <ul>
 *   <li>{@code notice_type}：通知公告分类（公告 / 通知 / 活动 / 培训）；</li>
 *   <li>{@code notice_scope_enum}：枚举字典，承担前端 Tag 渲染（所有人 / 指定用户）；</li>
 *   <li>{@code notice_method_enum}：通知方式（系统消息 / 登录弹窗）；</li>
 *   <li>{@code notice_status_enum}：公告状态（草稿 / 待发布 / 已发布）。</li>
 * </ul>
 *
 * <p>注意：前三个 “枚举字典” 的 value 必须与 notice 模块 {@code NoticeScope / NoticeMethod / NoticeStatus}
 * 枚举的整数值保持一致，否则前端无法按 value 渲染出对应的 label / color。</p>
 */
@Component
@Order(12)
public class DictSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DictSeeder.class);

    private final DictRepository dictRepository;
    private final DictItemRepository dictItemRepository;

    public DictSeeder(DictRepository dictRepository, DictItemRepository dictItemRepository) {
        this.dictRepository = dictRepository;
        this.dictItemRepository = dictItemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Dict noticeType = ensureDict("notice_type", "公告分类", "通知公告分类字典");
        ensureItems(noticeType, List.of(
                new SeedItem("公告", "1", "primary", 1),
                new SeedItem("通知", "2", "success", 2),
                new SeedItem("活动", "3", "warning", 3),
                new SeedItem("培训", "4", "info", 4)
        ));

        Dict scopeEnum = ensureDict("notice_scope_enum", "公告通知范围", "通知范围枚举");
        ensureItems(scopeEnum, List.of(
                new SeedItem("所有人", "1", "primary", 1),
                new SeedItem("指定用户", "2", "warning", 2)
        ));

        Dict methodEnum = ensureDict("notice_method_enum", "公告通知方式", "通知方式枚举");
        ensureItems(methodEnum, List.of(
                new SeedItem("系统消息", "1", "primary", 1),
                new SeedItem("登录弹窗", "2", "success", 2)
        ));

        Dict statusEnum = ensureDict("notice_status_enum", "公告状态", "公告状态枚举");
        ensureItems(statusEnum, List.of(
                new SeedItem("草稿", "1", "warning", 1),
                new SeedItem("待发布", "2", "primary", 2),
                new SeedItem("已发布", "3", "success", 3)
        ));
    }

    private Dict ensureDict(String code, String name, String description) {
        return dictRepository.findByCode(code).orElseGet(() -> {
            Dict dict = new Dict();
            dict.setCode(code);
            dict.setName(name);
            dict.setDescription(description);
            dict.setIsSystem(true);
            log.info("[初始化] 创建系统内置字典: code={}, name={}", code, name);
            return dictRepository.save(dict);
        });
    }

    private void ensureItems(Dict dict, List<SeedItem> seeds) {
        for (SeedItem s : seeds) {
            if (dictItemRepository.existsByDictIdAndValue(dict.getId(), s.value())) {
                continue;
            }
            DictItem item = new DictItem();
            item.setDictId(dict.getId());
            item.setLabel(s.label());
            item.setValue(s.value());
            item.setColor(s.color());
            item.setSort(s.sort());
            item.setStatus(1);
            item.setIsSystem(true);
            dictItemRepository.save(item);
        }
    }

    /** 内置字典明细的种子记录。 */
    private record SeedItem(String label, String value, String color, int sort) {
    }
}
