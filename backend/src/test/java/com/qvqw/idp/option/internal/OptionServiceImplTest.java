package com.qvqw.idp.option.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.option.OptionCategory;
import com.qvqw.idp.option.SystemOption;
import com.qvqw.idp.option.model.req.OptionReq;
import com.qvqw.idp.option.model.req.OptionValueResetReq;
import com.qvqw.idp.option.model.resp.OptionResp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OptionServiceImplTest {

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private OptionServiceImpl optionService;

    private SystemOption titleOption;

    @BeforeEach
    void setUp() {
        titleOption = new SystemOption();
        titleOption.setId(1L);
        titleOption.setCategory(OptionCategory.SITE);
        titleOption.setCode("SITE_TITLE");
        titleOption.setDefaultValue("Default Title");
        titleOption.setName("系统名称");
    }

    @AfterEach
    void tearDown() {
        // 防止用例之间残留 UserContext 互相污染
        UserContextHolder.clear();
    }

    /** 构造一个普通用户上下文（非 admin），赋予指定权限码。 */
    private static void useUserWithPermissions(String... codes) {
        UserContextHolder.set(new UserContext(
                10L, "ops", null, Set.of("ops"), Set.of(codes)));
    }

    /** 构造 admin 用户上下文（权限码集合可空，admin 角色直通）。 */
    private static void useAdmin() {
        UserContextHolder.set(new UserContext(
                1L, "admin", null, Set.of("admin"), Set.of()));
    }

    @Test
    void listByCategoryReturnsMappedResp() {
        when(optionRepository.findByCategoryOrderByIdAsc(OptionCategory.SITE))
                .thenReturn(List.of(titleOption));
        List<OptionResp> resp = optionService.list(buildQuery(OptionCategory.SITE, null));
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getValue()).isEqualTo("Default Title");
    }

    @Test
    void updateWithMismatchedIdCodeShouldFail() {
        when(optionRepository.findAllById(List.of(1L))).thenReturn(List.of(titleOption));
        OptionReq req = new OptionReq();
        req.setId(1L);
        req.setCode("SITE_OTHER");
        req.setValue("v");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不匹配");
    }

    @Test
    void updateMissingOptionShouldFail() {
        when(optionRepository.findAllById(List.of(9L))).thenReturn(List.of());
        OptionReq req = new OptionReq();
        req.setId(9L);
        req.setCode("SITE_TITLE");
        req.setValue("v");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参数不存在");
    }

    @Test
    void updatePasswordPolicyOutOfRangeShouldFail() {
        SystemOption opt = new SystemOption();
        opt.setId(2L);
        opt.setCategory(OptionCategory.PASSWORD);
        opt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findAllById(List.of(2L))).thenReturn(List.of(opt));
        OptionReq req = new OptionReq();
        req.setId(2L);
        req.setCode("PASSWORD_MIN_LENGTH");
        req.setValue("5");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("取值范围");
    }

    @Test
    void updatePasswordPolicyValid() {
        SystemOption opt = new SystemOption();
        opt.setId(2L);
        opt.setCategory(OptionCategory.PASSWORD);
        opt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findAllById(List.of(2L))).thenReturn(List.of(opt));
        OptionReq req = new OptionReq();
        req.setId(2L);
        req.setCode("PASSWORD_MIN_LENGTH");
        req.setValue("12");
        optionService.update(List.of(req));
        verify(optionRepository).saveAll(any(Iterable.class));
        assertThat(opt.getValue()).isEqualTo("12");
    }

    @Test
    void resetWithoutAnyCriteriaShouldFail() {
        OptionValueResetReq req = new OptionValueResetReq();
        assertThatThrownBy(() -> optionService.resetValue(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请指定");
    }

    @Test
    void resetByCategory() {
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCategory(OptionCategory.SITE);
        optionService.resetValue(req);
        verify(optionRepository).resetByCategory(OptionCategory.SITE);
        verify(optionRepository, never()).resetByCodes(anyList());
    }

    @Test
    void resetByCodes() {
        when(optionRepository.findByCodeIn(List.of("SITE_TITLE")))
                .thenReturn(List.of(titleOption));
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCodes(List.of("SITE_TITLE"));
        optionService.resetValue(req);
        verify(optionRepository).resetByCodes(List.of("SITE_TITLE"));
    }

    @Test
    void getValueShouldReturnEffectiveValue() {
        titleOption.setValue("Real Title");
        when(optionRepository.findByCode("SITE_TITLE")).thenReturn(Optional.of(titleOption));
        String v = optionService.getValue("SITE_TITLE", s -> s);
        assertThat(v).isEqualTo("Real Title");
    }

    @Test
    void getIntOrDefaultFallback() {
        when(optionRepository.findByCode("X")).thenReturn(Optional.empty());
        assertThat(optionService.getIntOrDefault("X", 7)).isEqualTo(7);
    }

    @Test
    void validateImageShouldRejectPlainText() {
        assertThatThrownBy(() -> optionService.validateImage("not-image"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validateImageShouldRejectTooLarge() {
        // ~2MB
        String big = "A".repeat(2 * 1024 * 1024);
        assertThatThrownBy(() -> optionService.validateImage("data:image/png;base64," + big))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1MB");
    }

    @Test
    void validateImageOk() {
        assertThat(optionService.validateImage("data:image/png;base64,abcd")).startsWith("data:image/png");
    }

    // ====== fine-grained category 鉴权 ======

    @Test
    void listByCategoryShouldThrow403WhenMissingReadPerm() {
        useUserWithPermissions("system:loginConfig:get");
        assertThatThrownBy(() -> optionService.list(buildQuery(OptionCategory.SITE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查询")
                .hasMessageContaining("网站配置");
        verify(optionRepository, never()).findByCategoryOrderByIdAsc(any());
    }

    @Test
    void listAllShouldFilterOutCategoriesUserCannotRead() {
        SystemOption loginOpt = new SystemOption();
        loginOpt.setId(2L);
        loginOpt.setCategory(OptionCategory.LOGIN);
        loginOpt.setCode("LOGIN_CAPTCHA_ENABLED");
        loginOpt.setDefaultValue("0");
        when(optionRepository.findAll()).thenReturn(List.of(titleOption, loginOpt));

        useUserWithPermissions("system:loginConfig:get");
        List<OptionResp> resp = optionService.list(buildQuery(null, null));

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getCode()).isEqualTo("LOGIN_CAPTCHA_ENABLED");
    }

    @Test
    void listByCategoryAdminBypass() {
        when(optionRepository.findByCategoryOrderByIdAsc(OptionCategory.SITE))
                .thenReturn(List.of(titleOption));
        useAdmin();
        assertThat(optionService.list(buildQuery(OptionCategory.SITE, null))).hasSize(1);
    }

    @Test
    void updateBlockedWhenCategoryWritePermMissing() {
        when(optionRepository.findAllById(List.of(1L))).thenReturn(List.of(titleOption));
        useUserWithPermissions("system:loginConfig:update");
        OptionReq req = new OptionReq();
        req.setId(1L);
        req.setCode("SITE_TITLE");
        req.setValue("Hacked");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("修改")
                .hasMessageContaining("网站配置");
        verify(optionRepository, never()).saveAll(any(Iterable.class));
    }

    @Test
    void updateAllowedWhenCategoryWritePermPresent() {
        when(optionRepository.findAllById(List.of(1L))).thenReturn(List.of(titleOption));
        useUserWithPermissions("system:siteConfig:update");
        OptionReq req = new OptionReq();
        req.setId(1L);
        req.setCode("SITE_TITLE");
        req.setValue("New Title");
        optionService.update(List.of(req));
        verify(optionRepository).saveAll(any(Iterable.class));
        assertThat(titleOption.getValue()).isEqualTo("New Title");
    }

    @Test
    void updateAdminBypassAcrossCategories() {
        SystemOption pwdOpt = new SystemOption();
        pwdOpt.setId(2L);
        pwdOpt.setCategory(OptionCategory.PASSWORD);
        pwdOpt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(titleOption, pwdOpt));
        useAdmin();

        OptionReq r1 = new OptionReq();
        r1.setId(1L);
        r1.setCode("SITE_TITLE");
        r1.setValue("X");
        OptionReq r2 = new OptionReq();
        r2.setId(2L);
        r2.setCode("PASSWORD_MIN_LENGTH");
        r2.setValue("12");

        optionService.update(List.of(r1, r2));
        verify(optionRepository).saveAll(any(Iterable.class));
    }

    @Test
    void resetByCategoryBlockedWhenMissingWritePerm() {
        useUserWithPermissions("system:loginConfig:update");
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCategory(OptionCategory.SITE);
        assertThatThrownBy(() -> optionService.resetValue(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重置")
                .hasMessageContaining("网站配置");
        verify(optionRepository, never()).resetByCategory(any());
    }

    @Test
    void resetByCodesBlockedWhenAnyCategoryUnauthorized() {
        SystemOption pwdOpt = new SystemOption();
        pwdOpt.setId(2L);
        pwdOpt.setCategory(OptionCategory.PASSWORD);
        pwdOpt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findByCodeIn(List.of("SITE_TITLE", "PASSWORD_MIN_LENGTH")))
                .thenReturn(List.of(titleOption, pwdOpt));

        useUserWithPermissions("system:siteConfig:update");
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCodes(List.of("SITE_TITLE", "PASSWORD_MIN_LENGTH"));
        assertThatThrownBy(() -> optionService.resetValue(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("安全配置");
        verify(optionRepository, never()).resetByCodes(anyList());
    }

    @Test
    void resetByCodesMissingCodeShouldFail() {
        when(optionRepository.findByCodeIn(List.of("NOT_EXIST")))
                .thenReturn(List.of());
        useAdmin();
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCodes(List.of("NOT_EXIST"));
        assertThatThrownBy(() -> optionService.resetValue(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("NOT_EXIST");
    }

    @Test
    void resetByCodesAdminBypassesCrossCategory() {
        SystemOption pwdOpt = new SystemOption();
        pwdOpt.setId(2L);
        pwdOpt.setCategory(OptionCategory.PASSWORD);
        pwdOpt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findByCodeIn(List.of("SITE_TITLE", "PASSWORD_MIN_LENGTH")))
                .thenReturn(List.of(titleOption, pwdOpt));

        useAdmin();
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCodes(List.of("SITE_TITLE", "PASSWORD_MIN_LENGTH"));
        optionService.resetValue(req);
        verify(optionRepository).resetByCodes(List.of("SITE_TITLE", "PASSWORD_MIN_LENGTH"));
    }

    private static com.qvqw.idp.option.model.query.OptionQuery buildQuery(OptionCategory category,
                                                                         List<String> codes) {
        com.qvqw.idp.option.model.query.OptionQuery q = new com.qvqw.idp.option.model.query.OptionQuery();
        q.setCategory(category);
        q.setCodes(codes);
        return q;
    }
}
