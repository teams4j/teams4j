import { defineConfig } from 'vitepress'

// Library code comments link into this site; grep for the old URL before renaming a page.
export default defineConfig({
  title: 'teams4j',
  description: 'Adaptive Cards and Microsoft Teams for the JVM',
  lang: 'en-US',
  base: '/teams4j/',
  cleanUrls: true,
  ignoreDeadLinks: false,

  head: [['link', { rel: 'icon', type: 'image/svg+xml', href: '/teams4j/favicon.svg' }]],

  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/getting-started', activeMatch: '/guide/' },
      { text: 'Cookbook', link: '/cookbook/deploy-notification', activeMatch: '/cookbook/' },
      { text: 'Reference', link: '/reference/limits', activeMatch: '/reference/' },
    ],

    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Getting started', link: '/guide/getting-started' },
          { text: 'Building cards', link: '/guide/cards' },
          { text: 'Validation', link: '/guide/validation' },
          { text: 'The webhook client', link: '/guide/webhook' },
          { text: 'Spring Boot', link: '/guide/spring-boot' },
          { text: 'JSON binding', link: '/guide/json-binding' },
          { text: 'Compatibility', link: '/guide/compatibility' },
        ],
      },
      {
        text: 'Cookbook',
        items: [
          { text: 'Deploy notification in five minutes', link: '/cookbook/deploy-notification' },
          { text: 'Developing against a local stub', link: '/cookbook/local-stub' },
          { text: 'Cards authored in the Designer', link: '/cookbook/designer-cards' },
        ],
      },
      {
        text: 'Reference',
        items: [
          { text: 'Teams limits', link: '/reference/limits' },
          { text: 'Validation rules', link: '/reference/validation-rules' },
          { text: 'Measurements', link: '/reference/measurements' },
          { text: 'API documentation', link: '/reference/javadoc' },
        ],
      },
    ],

    socialLinks: [{ icon: 'github', link: 'https://github.com/teams4j/teams4j' }],

    editLink: {
      pattern: 'https://github.com/teams4j/teams4j/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },

    search: { provider: 'local' },

    outline: { level: [2, 3] },

    footer: {
      message:
        'Apache-2.0. An unofficial community project, not affiliated with or endorsed by Microsoft.',
      copyright: '© 2026 teams4j contributors',
    },
  },
})
