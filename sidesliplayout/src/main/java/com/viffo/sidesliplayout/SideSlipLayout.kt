package com.viffo.sidesliplayout

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs


/**
 * 整体侧滑菜单
 * @author JayeLi
 */
class SideSlipLayout : ViewGroup {

    companion object {
        /** 拖动灵敏度系数 */
        private const val TOUCH_SLOP_SENSITIVITY = 1f
        /** 最小拖动速度 */
        private const val MIN_FLING_VELOCITY = 400
        /** 不拦截 Touch 事件 */
        private var mDisallowInterceptRequested = false

        @JvmStatic
        fun requestDisallowInterceptTouch(disallow: Boolean) {
            mDisallowInterceptRequested = disallow
        }
    }

    /** 最小拖动速度 */
    var minVel = 0f;

    /** 内容视图 */
    private var contentView: View? = null
    private var contentPoint: Point = Point(0, 0)

    /** 侧边栏 */
    private var sideView: View? = null
    private var sidePoint: Point = Point(0, 0)

    /** 拖拽相关工具类 */
    var statusCallback: SideSlipLayoutCallback? = null
    private lateinit var mDragger: ViewDragHelper
    private lateinit var mDraggerCallback: ViewDragCallback


    constructor(context: Context) : super(context) {
        SideSlipLayout(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.descendantFocusability = FOCUS_AFTER_DESCENDANTS
        val density = resources.displayMetrics.density
        this.minVel = MIN_FLING_VELOCITY * density
        initDragger()
    }

    /**
     * 初始化 ViewDragHelper
     */
    private fun initDragger()  {
        this.mDraggerCallback = ViewDragCallback(this)
        this.mDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mDraggerCallback)
        this.mDragger.apply {
            setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT)
            minVelocity = minVel
            mDraggerCallback.mDragger = this
        }
    }

    /**
     * 测量此 ViewGroup 和所有 Child View 的大小
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 当前 ViewGroup 必须要有两个子 View
        val count = childCount
        check(count == 2) {"The number of child view for the SideslipLayout must be two."}

        if (this.contentView == null || this.sideView == null) {
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (isContentView(child)) {
                    this.contentView = child
                } else {
                    this.sideView = child
                }
            }
        }

        // 获取并设置当前 ViewGroup 的可用大小
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)

        // 设置内容 View 的大小
        var lp = this.contentView!!.layoutParams as LayoutParams
        var widthSpec = MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY)
        var heightSpec = MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY)
        this.contentView!!.measure(widthSpec, heightSpec)

        // 设置侧边栏 View 的大小
        lp = this.sideView!!.layoutParams as LayoutParams
        widthSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width)
        heightSpec = getChildMeasureSpec(heightMeasureSpec, lp.topMargin + lp.bottomMargin, lp.height)
        this.sideView!!.measure(widthSpec, heightSpec)
    }

    /**
     * 计算所有子 View 的显示位置
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 设置内容视图的显示位置
        var child = this.contentView!!
        var lp = child.layoutParams as LayoutParams
        child.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + child.measuredWidth, lp.topMargin + child.measuredHeight)
        this.contentPoint.x = child.left
        this.contentPoint.y = child.top

        // 设置侧边栏的显示位置
        // 当前只能将侧边栏设置在内容视图的左侧或者右侧
        child = this.sideView!!
        lp = child.layoutParams as LayoutParams
        check(lp.gravity == Gravity.START || lp.gravity == Gravity.LEFT
                || lp.gravity == Gravity.END || lp.gravity == Gravity.RIGHT) {
            "Currently supports the side slip menu to be placed on the left or right for the content view."
        }

        // 父容器的可用宽度
        val width = r - l
        // Drawer, if it wasn't onMeasure would have thrown an exception.
        val childWidth = child.measuredWidth
        val childHeight = child.measuredHeight
        var childLeft = 0
        // 计算侧边栏的显示位置
        when(lp.gravity) {
            Gravity.START, Gravity.LEFT -> childLeft = -childWidth
            Gravity.END, Gravity.RIGHT -> childLeft = width
        }
        // 设置侧边栏的显示位置
        child.layout(childLeft, lp.topMargin, childLeft + childWidth, lp.topMargin + childHeight)
        this.sidePoint.x = child.left
        this.sidePoint.y = child.top
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when(ev.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                mDisallowInterceptRequested = false
            }
        }
        return if (mDisallowInterceptRequested) {
            super.onInterceptTouchEvent(ev)
        } else {
            mDragger.shouldInterceptTouchEvent(ev)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mDragger.processTouchEvent(ev)
        return true
    }

    /**
     * 判断当前视图是否是内容视图
     */
    private fun isContentView(child: View): Boolean {
        return (child.layoutParams as LayoutParams).gravity == Gravity.NO_GRAVITY
    }

    /**
     * 向当前ViewGroup中添加子View时确保不超过两个子View.
     */
    override fun addView(child: View?) {
        check(childCount <= 1) { "SideslipLayout can host only two direct child at most." }
        super.addView(child)
    }

    /**
     * 向当前ViewGroup中添加子View时确保不超过两个子View.
     */
    override fun addView(child: View?, index: Int) {
        check(childCount <= 1) { "SideslipLayout can host only two direct child at most." }
        super.addView(child, index)
    }

    /**
     * ViewDragHelper 的方法回调.
     */
    private class ViewDragCallback(private val sideSlipLayout: SideSlipLayout) : ViewDragHelper.Callback() {

        /** 手指是否已抬起 */
        var isReleased = true
        /** 侧边栏偏移比率 */
        var offsetRate = 0f
        /** 是否已结束滑动 */
        var isMoveDone = true
        /** 上一次 view 水平移动到的位置 */
        var prevLeft = 0;
        /** 回弹/吸附的目标位置 */
        var point = Point(0, 0)
        /** 当前 [ViewDragCallback] 绑定的 [ViewDragHelper] 对象， 用于手指抬起时设置 View 的 回弹/吸附 效果。 */
        var mDragger: ViewDragHelper? = null

        /**
         * 是否捕获当前手指按下的 View， 只有捕获此 View 才能进行下一步的拖拽动作，否则此次拖拽对当前 View 无效.
         */
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (isReleased && isMoveDone) {
                // 只有当前手指在已抬起状态下才重置释放状态， 以防多个手指以不同的顺序按下造成的视图原始位置错乱.
                isReleased = false
                isMoveDone = false
                // 记录当前View的原始位置
                point.x = child.left
                point.y = child.top
                return true
            }
            // 暂不支持多指交替滑动
            return false
        }

        /**
         * view的水平拖动范围
         */
        override fun getViewHorizontalDragRange(child: View): Int {
            return if (child == sideSlipLayout.sideView || child == sideSlipLayout.contentView) {
                sideSlipLayout.sideView!!.width
            } else 0
        }

        /**
         * 水平拖拽回调，处理 View 的水平拖拽。
         * 由于侧边栏与内容视图是整体滑动，所以需要手动处理手指没有按下的另一个 View 的显示位置.
         */
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            // 获取限制了边界的新的水平位移 left
            val newLeft = getLimitBoundNewLeft(child, left)
            prevLeft = child.left
            // 拖拽 View 的水平移动目标位置
            return newLeft
        }

        /**
         * View 停止拖拽，即手指抬起时调用。此时需处理此 View 的回弹和吸附效果。
         * 因回弹和吸附效果在此回调中只设置目标位置，所以需记录当前 View 所处的显示位置，为另一个View位置的更新做准备。
         */
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            isReleased = true
            // 记录当前位移的 left 位置，便于计算另一个 View 逐帧的水平偏移量
            prevLeft = releasedChild.left

            // 判断并计算 回弹/吸附 效果的水平位移新位置
            point.x = getViewReleaseLeft(releasedChild)
            point.y = releasedChild.top

            // 设置当前 View 的目标位置
            mDragger?.settleCapturedViewAt(point.x, point.y)
            sideSlipLayout.invalidate()
        }

        /**
         * 当前 View 的位置改变回调。当手指抬起时，需逐帧更新另一个 View 的显示位置，实现同被拖拽的 View 同步的回弹/吸附效果。
         */
        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            // 计算侧边栏偏移比率
            val childWidth = sideSlipLayout.sideView!!.width
            offsetRate = abs((childWidth + left).toFloat() / childWidth - 1)

            // 为实现整体侧滑效果， 需设置另一个 View 的水平偏移
            val otherChild = if (sideSlipLayout.contentView == changedView) {
                sideSlipLayout.sideView!!
            } else {
                sideSlipLayout.contentView!!
            }
            ViewCompat.offsetLeftAndRight(otherChild, left - prevLeft)
            prevLeft = left
            // 侧滑状态回调
            sideSlipLayout.statusCallback?.dragging(sideSlipLayout, left)

            // 释放 View 时另一控件的位移
            if (isReleased) {
                // 回弹/吸附效果结束，更新当前View拖拽状态
                if (changedView.left == point.x) {
                    // 判断当前 View 是否回到初始位置，回到初始位置表示关闭
                    val lp = sideSlipLayout.sideView!!.layoutParams as LayoutParams
                    if (sideSlipLayout.sidePoint.x == sideSlipLayout.sideView!!.left) {
                        lp.dragState = LayoutParams.STATUS_CLOSED
                        // 侧滑状态回调
                        sideSlipLayout.statusCallback?.closed(sideSlipLayout)
                    } else {
                        lp.dragState = LayoutParams.STATUS_OPENED
                        // 侧滑状态回调
                        sideSlipLayout.statusCallback?.opened(sideSlipLayout)
                    }
                    // 所有滑动已结束
                    isMoveDone = true
                }
            } else {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
            }
        }

        /**
         * 获取限制了边界的新的水平位移 left
         */
        private fun getLimitBoundNewLeft(child: View, left: Int): Int {
            var newLeft = 0
            var leftBound = 0
            var rightBound = 0
            val sideView = sideSlipLayout.sideView!!
            val sideWidth = sideView.width
            val width = sideSlipLayout.width
            val lp = sideView.layoutParams as LayoutParams
            when(lp.gravity) {
                Gravity.START, Gravity.LEFT -> {
                    if (sideView == child) {
                        leftBound = -sideWidth
                        rightBound = 0
                        newLeft = Math.min(Math.max(left, leftBound), rightBound)
                    } else {
                        leftBound = 0
                        rightBound = sideWidth
                        newLeft = Math.min(Math.max(left, leftBound), rightBound)
                    }
                }
                Gravity.END, Gravity.RIGHT -> {
                    if (sideView == child) {
                        leftBound = width - sideWidth
                        rightBound = width
                        newLeft = Math.min(Math.max(left, leftBound), rightBound)
                    } else {
                        leftBound = -sideWidth
                        rightBound = 0
                        newLeft = Math.min(Math.max(left, leftBound), rightBound)
                    }
                }
            }
            return newLeft
        }

        /**
         * 获取手指释放后的 回弹/吸附 效果需要移动到的目标位置的 left
         */
        private fun getViewReleaseLeft(releasedChild: View): Int {
            var releaseLeft = 0
            val sideView = sideSlipLayout.sideView!!
            val sideWidth = sideView.width
            val width = sideSlipLayout.width
            val lp = sideView.layoutParams as LayoutParams
            val isReleaseSideView = (sideView == releasedChild)
            val isCloseState = (lp.dragState == LayoutParams.STATUS_CLOSED)

            if ((isCloseState && offsetRate >= 0.4)
                || (!isCloseState && offsetRate <= 0.6)) {
                // 移动距离大于侧边栏宽度的一半, 吸附效果
                when(lp.gravity) {
                    Gravity.START, Gravity.LEFT -> {
                        releaseLeft = if (isCloseState) {
                            if (isReleaseSideView) 0 else sideWidth
                        } else {
                            if (isReleaseSideView) -sideWidth else 0
                        }
                    }
                    Gravity.END, Gravity.RIGHT -> {
                        releaseLeft = if (isCloseState) {
                            if (isReleaseSideView) (width - sideWidth) else -sideWidth
                        } else {
                            if (isReleaseSideView) width else 0
                        }
                    }
                }
                // 当前侧边栏已到达目标位置， 无需移动
                if (releasedChild.left == releaseLeft) {
                    isMoveDone = true
                    // 更新侧边栏状态, 侧滑状态回调
                    if (isCloseState) {
                        lp.dragState = LayoutParams.STATUS_OPENED
                        sideSlipLayout.statusCallback?.opened(sideSlipLayout)
                    } else {
                        lp.dragState = LayoutParams.STATUS_CLOSED
                        sideSlipLayout.statusCallback?.closed(sideSlipLayout)
                    }
                }
            } else {
                // 移动距离小于侧边栏宽度的一半, 回弹效果
                // 回弹无需更新 point
                releaseLeft = point.x
                // 当前侧边栏已到达目标位置， 无需移动
                if (releasedChild.left == releaseLeft) {
                    isMoveDone = true
                    // 侧滑状态回调， 回弹无需更新侧边栏状态
                    if (lp.dragState == LayoutParams.STATUS_CLOSED) {
                        sideSlipLayout.statusCallback?.closed(sideSlipLayout)
                    } else {
                        sideSlipLayout.statusCallback?.opened(sideSlipLayout)
                    }
                }
            }

            return releaseLeft
        }

        /**
         * 配置侧边栏初始和目标 left, 便于内容视图配合移动
         */
        fun setSideLeft(orgLeft: Int, desLeft: Int) {
            prevLeft = orgLeft
            point.x = desLeft
        }
    }

    /**
     * 展开侧滑菜单
     */
    fun open() {
        moveSideSlipLayout(sideView!!.left, true)
    }

    /**
     * 关闭侧滑菜单
     */
    fun close() {
        moveSideSlipLayout(sideView!!.left, false)
    }

    /**
     * 展开/关闭 侧边栏
     */
    private fun moveSideSlipLayout(orgLeft: Int, isOpen: Boolean) {
        val sideWidth = sideView!!.width
        val lp = sideView!!.layoutParams as LayoutParams
        val desLeft = when(lp.gravity) {
            Gravity.START, Gravity.LEFT -> if (isOpen) 0 else -sideWidth
            Gravity.END, Gravity.RIGHT -> if (isOpen) (width - sideWidth) else width
            else -> throw IllegalStateException("Currently only supports the left or right side sliding menu.")
        }
        mDraggerCallback.setSideLeft(orgLeft, desLeft)
        mDragger.smoothSlideViewTo(sideView!!, desLeft, 0)
        invalidate()
    }

    /**
     * 侧边栏是否是展开状态
     */
    fun isOpening(): Boolean {
        val lp = sideView!!.layoutParams as LayoutParams
        return (lp.dragState == LayoutParams.STATUS_OPENED)
    }

    /**
     * 兼容Java, 设置回调
     */
    fun setStatusCallBack(listener: SideSlipLayoutCallback) {
        this.statusCallback = listener
    }

    /**
     * 请求不拦截 Touch 事件
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        mDisallowInterceptRequested = disallowIntercept
    }

    /**
     * 回弹/吸附 效果的实现，设置 continueSettling = true 实现逐帧更新UI.
     */
    override fun computeScroll() {
        val dragSettling = mDragger.continueSettling(true)
        if (dragSettling) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }


    /**
     * 每个子 View 拥有的布局参数
     */
    class LayoutParams : MarginLayoutParams {

        companion object {
            const val STATUS_CLOSED = 0x0
            const val STATUS_OPENED = 0x1
            private val SIDE_SLIP_LAYOUT_ATTRS = intArrayOf(android.R.attr.layout_gravity)
        }

        /**
         * 当前侧边栏的显示状态
         */
        var dragState = STATUS_CLOSED
        /**
         * 子 view 除了支持 margin 外还应支持 layout_gravity 参数，以便设置侧边栏方向.
         */
        var gravity = Gravity.NO_GRAVITY

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {

            val a = c.obtainStyledAttributes(attrs, SIDE_SLIP_LAYOUT_ATTRS)
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY)
            a.recycle()
        }

        constructor(width: Int, height: Int): super(width, height) {}

        constructor(width: Int, height: Int, gravity: Int) : this(width, height) {
            this.gravity = gravity
        }

        constructor(source: LayoutParams) : super(source) {
            this.gravity = source.gravity
        }

        constructor(source: ViewGroup.LayoutParams) : super(source) {}

        constructor(source: MarginLayoutParams) : super(source) {}

    }

    /**
     * 将子 View 添加到此容器中时生成默认的 [LayoutParams]
     */
    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    /**
     * 根据子 View 的 [ViewGroup.LayoutParams] 生成 [LayoutParams]
     */
    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return when (p) {
            is LayoutParams -> {
                LayoutParams(p)
            }
            is MarginLayoutParams -> {
                LayoutParams(p)
            }
            else -> {
                LayoutParams(p!!)
            }
        }
    }

    /**
     * 根据子 View 的 [AttributeSet] 生成 [LayoutParams]
     */
    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    /**
     * 检查子 View的布局参数 [LayoutParams] 是否合法.
     */
    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return (p is LayoutParams) && super.checkLayoutParams(p)
    }

}