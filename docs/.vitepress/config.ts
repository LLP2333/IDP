import { defineConfig } from "vitepress";

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

export default defineConfig({
  title: "IDP 技术文档",
  description: "IDP 企业后台系统技术文档",
  lang: "zh-CN",
  cleanUrls: true,
  lastUpdated: true,
  themeConfig: {
    nav: [
      { text: "首页", link: "/" },
      { text: "后端 API", link: "http://localhost:8080/swagger-ui.html" },
      { text: "管理后台", link: "http://localhost:3000" }
    ],
    sidebar: [
      {
        text: "业务模块",
        items: [
          { text: "认证与登录", link: "/auth" },
          { text: "用户与角色管理", link: "/user-role" },
          { text: "菜单模块", link: "/menu" },
          { text: "系统配置", link: "/system-config" },
          { text: "字典模块", link: "/dict" },
          { text: "通知公告", link: "/notice" },
          { text: "站内消息", link: "/message" },
          { text: "系统监控", link: "/monitor" }
        ]
      }
    ],
    outline: {
      label: "本页目录",
      level: [2, 3]
    },
    docFooter: {
      prev: "上一篇",
      next: "下一篇"
    },
    lastUpdated: {
      text: "最后更新",
      formatOptions: {
        dateStyle: "medium",
        timeStyle: "short"
      }
    },
    search: {
      provider: "local",
      options: {
        translations: {
          button: {
            buttonText: "搜索文档",
            buttonAriaLabel: "搜索文档"
          },
          modal: {
            noResultsText: "没有找到相关结果",
            resetButtonTitle: "清除查询",
            footer: {
              selectText: "选择",
              navigateText: "切换",
              closeText: "关闭"
            }
          }
        }
      }
    }
  },
  markdown: {
    config(md) {
      const defaultFence =
        md.renderer.rules.fence ??
        ((tokens, idx, options, env, self) =>
          self.renderToken(tokens, idx, options));

      md.renderer.rules.fence = (tokens, idx, options, env, self) => {
        const token = tokens[idx];
        const lang = token.info.trim().split(/\s+/)[0];

        if (lang === "mermaid") {
          return `<div class="mermaid">${escapeHtml(token.content)}</div>`;
        }

        return defaultFence(tokens, idx, options, env, self);
      };
    }
  }
});
