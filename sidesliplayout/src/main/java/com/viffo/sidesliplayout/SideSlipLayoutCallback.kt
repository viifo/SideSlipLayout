package com.viffo.sidesliplayout

interface SideSlipLayoutCallback {

    /**
     * 侧滑菜单已展开
     */
    fun opened(layout: SideSlipLayout)

    /**
     * 侧滑菜单已关闭
     */
    fun closed(layout: SideSlipLayout)

    /**
     * 侧滑菜单正在被拖拽
     */
    fun dragging(layout: SideSlipLayout, left: Int)

}