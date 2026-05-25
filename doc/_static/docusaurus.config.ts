import { themes as prismThemes } from 'prism-react-renderer';
import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import type * as OpenApiPlugin from 'docusaurus-preset-openapi';

const config: Config = {
  title: 'DST 管理平台文档',
  tagline: 'Don't Starve Together 服务器管理平台',
  favicon: 'img/favicon.ico',

  // 设置生产环境的 URL
  url: 'https://your-org.github.io',
  baseUrl: '/DontStarveTool/',

  // GitHub Pages 配置
  organizationName: 'your-org',  // 替换为你的 GitHub 组织名
  projectName: 'DontStarveTool',  // 替换为你的项目名
  trailingSlash: false,
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // i18n 配置（目前仅中文，预留多语言扩展）
  i18n: {
    defaultLocale: 'zh-CN',
    locales: ['zh-CN'],
    localeConfigs: {
      'zh-CN': {
        label: '简体中文',
        htmlLang: 'zh-CN',
      },
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          // 编辑链接配置
          editUrl: 'https://github.com/your-org/DontStarveTool/tree/master/doc/website/',
          routeBasePath: '/',  // 文档作为首页
        },
        blog: false,  // 暂时禁用博客
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    // 替换为你的项目 Logo
    navbar: {
      title: 'DST 管理平台',
      logo: {
        alt: 'DST 管理平台 Logo',
        src: 'img/logo.svg',
        srcDark: 'img/logo-dark.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'gettingStartedSidebar',
          position: 'left',
          label: '入门',
        },
        {
          type: 'docSidebar',
          sidebarId: 'devGuideSidebar',
          position: 'left',
          label: '开发指南',
        },
        {
          type: 'docSidebar',
          sidebarId: 'userGuideSidebar',
          position: 'left',
          label: '用户指南',
        },
        {
          type: 'docSidebar',
          sidebarId: 'referenceSidebar',
          position: 'left',
          label: '参考文档',
        },
        {
          type: 'docSidebar',
          sidebarId: 'architectureSidebar',
          position: 'left',
          label: '架构',
        },
        {
          href: 'https://github.com/your-org/DontStarveTool',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: '文档',
          items: [
            {
              label: '入门',
              to: '/docs/category/getting-started',
            },
            {
              label: '开发指南',
              to: '/docs/category/dev-guide',
            },
            {
              label: '用户指南',
              to: '/docs/category/user-guide',
            },
          ],
        },
        {
          title: '社区',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/your-org/DontStarveTool',
            },
          ],
        },
        {
          title: '更多',
          items: [
            {
              label: 'API Reference',
              href: '/api',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} DST 管理平台. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'bash', 'sql', 'json', 'yaml', 'typescript', 'react'],
    },
    // Mermaid 图表配置
    mermaid: {
      theme: {
        light: 'default',
        dark: 'dark',
      },
    },
    // 全文搜索配置（使用 Algolia DocSearch）
    algolia: {
      appId: 'YOUR_ALGOLIA_APP_ID',  // 需要申请
      apiKey: 'YOUR_ALGOLIA_API_KEY',
      indexName: 'dst-management-platform',
      contextualSearch: true,
    },
  } satisfies Preset.ThemeConfig,

  // 插件配置
  plugins: [
    // Mermaid 支持
    async function mermaidPlugin() {
      return {
        name: 'docusaurus-mermaid',
        configureWebpack() {
          return {
            resolve: {
              alias: {
                'mermaid': require.resolve('mermaid'),
              },
            },
          };
        },
      };
    },
  ],

  // TypeScript 配置
  typescript: {
    ignoreBuildErrors: true,
  },
};

export default config;
