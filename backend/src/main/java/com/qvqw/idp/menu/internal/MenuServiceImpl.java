package com.qvqw.idp.menu.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.menu.Menu;
import com.qvqw.idp.menu.MenuService;
import com.qvqw.idp.menu.model.query.MenuQuery;
import com.qvqw.idp.menu.model.req.MenuReq;
import com.qvqw.idp.menu.model.resp.MenuResp;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现。
 *
 * <p>删除菜单只校验 {@code is_system} 与是否有子节点；“是否仍被角色引用” 由 role 模块的
 * {@code RoleMenuRepository#countByMenuId} 兜底。任何写操作均会调用
 * {@link AuthCacheEvictor#evictAll()} 让 JWT 过滤器的角色 / 权限缓存失效。</p>
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final int TYPE_DIR = 1;
    private static final int TYPE_MENU = 2;
    private static final int TYPE_BUTTON = 3;
    private static final String DEFAULT_DIR_COMPONENT = "Layout";

    private final MenuRepository menuRepository;
    private final AuthCacheEvictor authCacheEvictor;

    public MenuServiceImpl(MenuRepository menuRepository,
                           AuthCacheEvictor authCacheEvictor) {
        this.menuRepository = menuRepository;
        this.authCacheEvictor = authCacheEvictor;
    }

    @Override
    public List<MenuResp> list(MenuQuery query) {
        Specification<Menu> spec = buildSpec(query);
        return menuRepository.findAll(spec).stream()
                .sorted(menuComparator())
                .map(this::toResp)
                .toList();
    }

    @Override
    public List<MenuResp> tree(MenuQuery query) {
        List<MenuResp> flat = list(query);
        return assemble(flat, /*includeButton*/ true);
    }

    @Override
    public MenuResp get(Long id) {
        Menu m = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException("菜单不存在"));
        return toResp(m);
    }

    /**
     * 新增菜单。
     *
     * @throws BusinessException 同级标题重复 / 权限码重复 / 父节点不存在 / 字段缺失
     */
    @Override
    @Transactional
    public Long create(MenuReq req) {
        normalize(req);
        validate(req, null);
        Menu m = new Menu();
        applyToEntity(m, req);
        m.setIsSystem(false);
        Long id = menuRepository.save(m).getId();
        authCacheEvictor.evictAll();
        return id;
    }

    /**
     * 修改菜单：系统内置菜单不允许改 permission、type、parentId 等关键字段。
     */
    @Override
    @Transactional
    public void update(Long id, MenuReq req) {
        Menu m = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException("菜单不存在"));
        normalize(req);
        if (Boolean.TRUE.equals(m.getIsSystem())) {
            if (!java.util.Objects.equals(safeNormalize(m.getPermission()), safeNormalize(req.getPermission()))) {
                throw new BusinessException("系统内置菜单不允许修改权限标识");
            }
            if (!java.util.Objects.equals(m.getType(), req.getType())) {
                throw new BusinessException("系统内置菜单不允许修改类型");
            }
            if (!java.util.Objects.equals(m.getParentId(), normalizeParent(req.getParentId()))) {
                throw new BusinessException("系统内置菜单不允许修改父节点");
            }
        }
        validate(req, id);
        applyToEntity(m, req);
        menuRepository.save(m);
        authCacheEvictor.evictAll();
    }

    /**
     * 批量删除菜单。
     *
     * <p>校验：系统内置不允许删除；存在子节点不允许删除。删除完调用
     * {@link AuthCacheEvictor#evictAll()} 让缓存失效；
     * role 模块仍需在自己 listener 里清理 {@code role_menu} 关联（不通过外键级联）。</p>
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Menu m = menuRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("菜单不存在: " + id));
            if (Boolean.TRUE.equals(m.getIsSystem())) {
                throw new BusinessException("系统内置菜单不允许删除: " + m.getTitle());
            }
            if (menuRepository.countByParentId(id) > 0) {
                throw new BusinessException("存在子菜单，请先删除子菜单: " + m.getTitle());
            }
        }
        menuRepository.deleteAllById(ids);
        authCacheEvictor.evictAll();
    }

    @Override
    public Set<String> listCodesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return menuRepository.findByIdIn(ids).stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .filter(m -> m.getType() != null && m.getType() == TYPE_BUTTON)
                .map(Menu::getPermission)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public List<Long> listSystemMenuIds() {
        return menuRepository.findByIsSystemTrueOrderBySortAscIdAsc().stream()
                .map(Menu::getId)
                .toList();
    }

    @Override
    public List<MenuResp> treeByIds(Collection<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<MenuResp> flat = menuRepository.findByIdIn(menuIds).stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .filter(m -> m.getType() != null && m.getType() != TYPE_BUTTON)
                .sorted(menuComparator())
                .map(this::toResp)
                .toList();
        return assemble(flat, /*includeButton*/ false);
    }

    @Override
    public List<MenuResp> treeAllEnabledRoutes() {
        List<MenuResp> flat = menuRepository.findAllByOrderBySortAscIdAsc().stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .filter(m -> m.getType() != null && m.getType() != TYPE_BUTTON)
                .map(this::toResp)
                .toList();
        return assemble(flat, /*includeButton*/ false);
    }

    private static Specification<Menu> buildSpec(MenuQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>(5);
            if (query != null) {
                if (StringUtils.hasText(query.getTitle())) {
                    predicates.add(cb.like(cb.lower(root.get("title")),
                            "%" + query.getTitle().trim().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(query.getPath())) {
                    predicates.add(cb.like(cb.lower(root.get("path")),
                            "%" + query.getPath().trim().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(query.getPermission())) {
                    predicates.add(cb.like(cb.lower(root.get("permission")),
                            "%" + query.getPermission().trim().toLowerCase() + "%"));
                }
                if (query.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), query.getStatus()));
                }
                if (query.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), query.getType()));
                }
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 把平铺的 MenuResp 按 parentId 组装为树形结构。
     *
     * @param flat         平铺列表（已排序）
     * @param includeButton 是否保留 type=3 的按钮叶子节点
     */
    private List<MenuResp> assemble(List<MenuResp> flat, boolean includeButton) {
        Set<Long> ids = flat.stream().map(MenuResp::getId).collect(Collectors.toSet());
        Map<Long, List<MenuResp>> byParent = flat.stream()
                .filter(m -> includeButton || m.getType() == null || m.getType() != TYPE_BUTTON)
                .collect(Collectors.groupingBy(m -> ids.contains(m.getParentId()) ? m.getParentId() : 0L));
        List<MenuResp> roots = byParent.getOrDefault(0L, new ArrayList<>());
        for (MenuResp root : roots) {
            attachChildren(root, byParent);
        }
        return roots;
    }

    private void attachChildren(MenuResp node, Map<Long, List<MenuResp>> byParent) {
        List<MenuResp> children = byParent.getOrDefault(node.getId(), new ArrayList<>());
        node.setChildren(children);
        for (MenuResp child : children) {
            attachChildren(child, byParent);
        }
    }

    /**
     * 字段规范化：去前后空白、目录类型补默认 component、按钮类型清空路由字段。
     */
    private void normalize(MenuReq req) {
        req.setTitle(req.getTitle() == null ? null : req.getTitle().trim());
        req.setPath(safeNormalize(req.getPath()));
        req.setName(safeNormalize(req.getName()));
        req.setComponent(safeNormalize(req.getComponent()));
        req.setRedirect(safeNormalize(req.getRedirect()));
        req.setIcon(safeNormalize(req.getIcon()));
        req.setPermission(safeNormalize(req.getPermission()));
        if (req.getIsExternal() == null) {
            req.setIsExternal(false);
        }
        if (req.getIsCache() == null) {
            req.setIsCache(false);
        }
        if (req.getIsHidden() == null) {
            req.setIsHidden(false);
        }
        if (req.getSort() == null) {
            req.setSort(999);
        }
        if (req.getStatus() == null) {
            req.setStatus(1);
        }
        Integer type = req.getType();
        if (type != null && type == TYPE_DIR && !StringUtils.hasText(req.getComponent())) {
            req.setComponent(DEFAULT_DIR_COMPONENT);
        }
        if (type != null && type == TYPE_BUTTON) {
            req.setPath(null);
            req.setName(null);
            req.setComponent(null);
            req.setRedirect(null);
            req.setIcon(null);
            req.setIsExternal(false);
            req.setIsCache(false);
            req.setIsHidden(false);
        }
    }

    private static String safeNormalize(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Long normalizeParent(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    /**
     * 业务校验：类型必填字段、同级标题不重复、权限码唯一、父节点存在且类型合法。
     */
    private void validate(MenuReq req, Long currentId) {
        Integer type = req.getType();
        if (type == null || (type != TYPE_DIR && type != TYPE_MENU && type != TYPE_BUTTON)) {
            throw new BusinessException("类型无效");
        }
        Long parentId = normalizeParent(req.getParentId());
        if (parentId > 0) {
            Menu parent = menuRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException("父菜单不存在"));
            if (parent.getType() != null && parent.getType() == TYPE_BUTTON) {
                throw new BusinessException("不能在按钮节点下创建子节点");
            }
        }
        if (type == TYPE_BUTTON) {
            if (!StringUtils.hasText(req.getPermission())) {
                throw new BusinessException("按钮类型必须填写权限标识");
            }
            menuRepository.findByPermission(req.getPermission()).ifPresent(existing -> {
                if (currentId == null || !existing.getId().equals(currentId)) {
                    throw new BusinessException("权限标识已存在: " + req.getPermission());
                }
            });
        } else {
            if (!StringUtils.hasText(req.getPath())) {
                throw new BusinessException("路由地址不能为空");
            }
            if (Boolean.TRUE.equals(req.getIsExternal()) && !isHttpUrl(req.getPath())) {
                throw new BusinessException("外链路由必须以 http:// 或 https:// 开头");
            }
            if (Boolean.FALSE.equals(req.getIsExternal()) && isHttpUrl(req.getPath())) {
                throw new BusinessException("非外链路由不能以 http:// 或 https:// 开头");
            }
            if (type == TYPE_MENU && !Boolean.TRUE.equals(req.getIsExternal())
                    && !StringUtils.hasText(req.getComponent())) {
                throw new BusinessException("菜单类型必须填写组件路径");
            }
            if (StringUtils.hasText(req.getPermission())) {
                menuRepository.findByPermission(req.getPermission()).ifPresent(existing -> {
                    if (currentId == null || !existing.getId().equals(currentId)) {
                        throw new BusinessException("权限标识已存在: " + req.getPermission());
                    }
                });
            }
        }
        // 同级标题唯一（编辑时排除自己）
        boolean titleConflict = menuRepository.findAll().stream()
                .filter(m -> currentId == null || !m.getId().equals(currentId))
                .anyMatch(m -> java.util.Objects.equals(m.getParentId(), parentId)
                        && java.util.Objects.equals(m.getTitle(), req.getTitle()));
        if (titleConflict) {
            throw new BusinessException("同级菜单下标题已存在: " + req.getTitle());
        }
    }

    private static boolean isHttpUrl(String s) {
        if (!StringUtils.hasText(s)) {
            return false;
        }
        String lower = s.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private void applyToEntity(Menu m, MenuReq req) {
        m.setTitle(req.getTitle());
        m.setParentId(normalizeParent(req.getParentId()));
        m.setType(req.getType());
        m.setPath(req.getPath());
        m.setName(req.getName());
        m.setComponent(req.getComponent());
        m.setRedirect(req.getRedirect());
        m.setIcon(req.getIcon());
        m.setIsExternal(Boolean.TRUE.equals(req.getIsExternal()));
        m.setIsCache(Boolean.TRUE.equals(req.getIsCache()));
        m.setIsHidden(Boolean.TRUE.equals(req.getIsHidden()));
        m.setPermission(req.getPermission());
        m.setSort(req.getSort());
        m.setStatus(req.getStatus());
        m.setDescription(req.getDescription());
    }

    private static Comparator<Menu> menuComparator() {
        return Comparator.comparingInt((Menu m) -> m.getSort() == null ? Integer.MAX_VALUE : m.getSort())
                .thenComparingLong(Menu::getId);
    }

    private MenuResp toResp(Menu m) {
        MenuResp resp = new MenuResp();
        resp.setId(m.getId());
        resp.setTitle(m.getTitle());
        resp.setParentId(m.getParentId());
        resp.setType(m.getType());
        resp.setPath(m.getPath());
        resp.setName(m.getName());
        resp.setComponent(m.getComponent());
        resp.setRedirect(m.getRedirect());
        resp.setIcon(m.getIcon());
        resp.setIsExternal(m.getIsExternal());
        resp.setIsCache(m.getIsCache());
        resp.setIsHidden(m.getIsHidden());
        resp.setPermission(m.getPermission());
        resp.setSort(m.getSort());
        resp.setStatus(m.getStatus());
        resp.setIsSystem(m.getIsSystem());
        resp.setDescription(m.getDescription());
        resp.setCreatedAt(m.getCreatedAt());
        resp.setUpdatedAt(m.getUpdatedAt());
        return resp;
    }
}
