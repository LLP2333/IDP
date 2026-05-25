import DefaultTheme from "vitepress/theme";
import { inBrowser } from "vitepress";
import "./custom.css";

const renderMermaid = async () => {
  if (!inBrowser) {
    return;
  }

  const { default: mermaid } = await import("mermaid");

  mermaid.initialize({
    startOnLoad: false,
    securityLevel: "strict",
    theme: document.documentElement.classList.contains("dark")
      ? "dark"
      : "default"
  });

  await mermaid.run({ querySelector: ".mermaid" });
};

export default {
  extends: DefaultTheme,
  enhanceApp({ router }) {
    if (!inBrowser) {
      return;
    }

    window.setTimeout(renderMermaid, 0);
    router.onAfterRouteChanged = () => {
      window.setTimeout(renderMermaid, 0);
    };
  }
};
