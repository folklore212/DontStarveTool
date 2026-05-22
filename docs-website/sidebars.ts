import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  // 入门指南侧边栏
  gettingStartedSidebar: [
    {
      type: 'category',
      label: '快速开始',
      link: {
        type: 'generated-index',
        title: '快速开始',
        description: '5 分钟内体验 DST 管理平台',
      },
      items: [
        'getting-started/001-quickstart',
        'getting-started/002-local-setup',
        'getting-started/003-architecture-overview',
      ],
    },
  ],

  // 开发者指南侧边栏
  devGuideSidebar: [
    {
      type: 'category',
      label: '环境搭建',
      link: {
        type: 'doc',
        id: 'dev-guide/setup/001-local-setup',
      },
      items: [
        'dev-guide/setup/001-local-setup',
        'dev-guide/setup/002-docker-setup',
      ],
    },
    {
      type: 'category',
      label: '开发指南',
      items: [
        'dev-guide/guides/003-coding-standards',
        'dev-guide/guides/004-debugging',
        'dev-guide/guides/005-testing',
      ],
    },
    {
      type: 'category',
      label: '部署指南',
      items: [
        'dev-guide/deployment/006-docker-guide',
        'dev-guide/deployment/007-production-config',
      ],
    },
    {
      type: 'category',
      label: '运维指南',
      items: [
        'dev-guide/operations/008-troubleshooting',
      ],
    },
  ],

  // 用户指南侧边栏
  userGuideSidebar: [
    {
      type: 'category',
      label: '功能说明',
      items: [
        'user-guide/features/001-server-mgmt',
        'user-guide/features/002-cluster-mgmt',
        'user-guide/features/003-workshop',
        'user-guide/features/004-dashboard',
        'user-guide/features/005-map-preview',
        'user-guide/features/006-file-manager',
        'user-guide/features/007-collaboration',
      ],
    },
    {
      type: 'category',
      label: '使用教程',
      items: [
        'user-guide/tutorials/001-first-server',
        'user-guide/tutorials/002-deploy-server',
      ],
    },
  ],

  // 参考文档侧边栏
  referenceSidebar: [
    {
      type: 'category',
      label: 'API 文档',
      items: [
        'reference/api/001-rest-api',
        'reference/api/002-json-rpc',
        'reference/api/003-node-commands',
      ],
    },
    {
      type: 'category',
      label: '数据库',
      items: [
        'reference/database/001-schema-reference',
        'reference/database/002-flyway-migrations',
      ],
    },
    {
      type: 'category',
      label: '配置参考',
      items: [
        'reference/configuration/001-application-props',
      ],
    },
  ],

  // 架构文档侧边栏
  architectureSidebar: [
    {
      type: 'category',
      label: '架构概述',
      items: [
        'architecture/overview/001-system-overview',
      ],
    },
    {
      type: 'category',
      label: '架构决策记录 (ADR)',
      items: [
        'architecture/adr/001-gateway-trust-auth',
        'architecture/adr/002-module-deletions',
      ],
    },
    {
      type: 'category',
      label: '详细设计',
      items: [
        'architecture/design/001-node-agent',
        'architecture/design/002-service-calls',
        'architecture/design/003-node-auth-flow',
        'architecture/design/004-workshop-cache-flow',
        'architecture/design/005-deployment-topology',
      ],
    },
  ],

  // 模块文档侧边栏
  modulesSidebar: [
    {
      type: 'category',
      label: '服务器管理',
      items: [
        'modules/server-management/001-server-detail',
      ],
    },
    {
      type: 'category',
      label: '集群管理',
      items: [
        'modules/cluster/002-cluster-mgmt',
      ],
    },
    {
      type: 'category',
      label: 'Node Agent',
      items: [
        'modules/node-agent/003-node-agent',
      ],
    },
    {
      type: 'category',
      label: '创意工坊',
      items: [
        'modules/workshop/004-workshop',
      ],
    },
  ],

  // 前端文档侧边栏
  frontendSidebar: [
    {
      type: 'doc',
      id: 'frontend/001-component-guide',
      label: '组件指南',
    },
  ],
};

export default sidebars;
