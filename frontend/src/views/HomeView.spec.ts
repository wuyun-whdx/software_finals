import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import HomeView from './HomeView.vue'

describe('HomeView', () => {
  it('renders product entry, preview card and primary actions', () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: { RouterLink: { template: '<a><slot /></a>' } }
      }
    })

    expect(wrapper.text()).toContain('性格雷达·生活指南')
    expect(wrapper.text()).toContain('把性格画像变成可执行的生活建议')
    expect(wrapper.text()).toContain('开始测评')
    expect(wrapper.text()).toContain('查看示例报告')
    expect(wrapper.text()).toContain('报告预览')
    expect(wrapper.text()).toContain('探索型生活画像')
    expect(wrapper.text()).toContain('测评')
    expect(wrapper.text()).toContain('报告')
    expect(wrapper.text()).toContain('推荐')
    expect(wrapper.text()).toContain('匹配')
  })
})
