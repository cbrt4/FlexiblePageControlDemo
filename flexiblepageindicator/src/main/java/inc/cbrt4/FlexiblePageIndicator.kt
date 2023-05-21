package inc.cbrt4

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import inc.cbrt4.flexiblepageindicator.R
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class FlexiblePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs),
    OnPageChangeListener {

    private val keyPropertyMoveFactor = "animationMoveFactor"
    private val keyPropertyColor = "color"
    private val keyPropertyColorReverse = "colorReverse"

    @ColorInt
    private val colorSelected = 0xFFFFFFFF.toInt()

    @ColorInt
    private val colorDefault = 0x80FFFFFF.toInt()

    private val minDotCount = 5
    private val defaultDotCount = 7
    private val animationDuration = 1000L

    private val dotDefaultColor: Int
    private val dotSelectedColor: Int
    private val dotSize: Float
    private val dotSpace: Float
    private val pageNavigationEnabled: Boolean

    private val paint: Paint
    private val animator: ValueAnimator

    private val coordinates by lazy { initCoordinates() }
    private val touchRanges by lazy { initTouchRanges() }

    private var viewPaddingTop = 0
    private var viewPaddingBottom = 0
    private var viewPaddingStart = 0
    private var viewPaddingEnd = 0

    private var viewWidth = 0F
    private var viewHeight = 0F

    private var dotCount = 0
    private var totalDotCount = 0
    private var bias = 2
    private var currentSelection = 0
    private var newSelection = 0
    private var animationMoveFactor = 0F
    private var animationColor = 0
    private var animationColorReverse = 0
    private var reverseAnimation = false
    private var scrollableIndication = false
    private var canScroll = false

    private var cursorStartPosition = 0
    private var cursorEndPosition = 0
    private var cursorStartX = 0F
    private var cursorEndX = 0F
    private var touchStart = 0F

    private var viewPager: ViewPager? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FlexiblePageIndicator,
            0,
            0
        ).apply {
            try {
                dotCount = getInteger(
                    R.styleable.FlexiblePageIndicator_dotCount,
                    defaultDotCount
                )

                dotDefaultColor = getColor(
                    R.styleable.FlexiblePageIndicator_dotColorDefault,
                    colorDefault
                )

                dotSelectedColor = getColor(
                    R.styleable.FlexiblePageIndicator_dotColorSelected,
                    colorSelected
                )

                dotSize = getDimension(
                    R.styleable.FlexiblePageIndicator_dotSize,
                    resources.getDimension(R.dimen.dot_size_default)
                )

                val givenDotSpace = getDimension(
                    R.styleable.FlexiblePageIndicator_dotSpace,
                    resources.getDimension(R.dimen.dot_space_default)
                )

                dotSpace = if (givenDotSpace > dotSize) {
                    givenDotSpace
                } else {
                    dotSize * 1.5F
                }

                pageNavigationEnabled = getBoolean(
                    R.styleable.FlexiblePageIndicator_pageNavigationEnabled,
                    true
                )

                paint = Paint(Paint.ANTI_ALIAS_FLAG)

                animator = valueAnimator()

            } finally {
                recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setupPadding()

        viewWidth = dotCount * dotSpace
        viewHeight = dotSpace

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(viewWidth.toInt(), widthSize)
            else -> viewWidth.toInt()
        }

        val height: Int = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(viewHeight.toInt(), heightSize)
            else -> viewHeight.toInt()
        }

        fixPadding(
            (width - viewWidth.toInt()) / 2,
            (height - viewHeight.toInt()) / 2
        )

        setupCursor()

        setMeasuredDimension(
            viewWidth.toInt() + viewPaddingStart + viewPaddingEnd,
            viewHeight.toInt() + viewPaddingTop + viewPaddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        for (position: Int in 0 until totalDotCount) {
            drawIndicator(canvas, position)
        }
    }

    fun setupWithViewPager(viewPager: ViewPager) {
        this.viewPager = viewPager

        viewPager.adapter?.let {
            totalDotCount = it.count
            currentSelection = viewPager.currentItem

            scrollableIndication = totalDotCount > dotCount

            if (scrollableIndication && dotCount < minDotCount) {
                dotCount = minDotCount
            }

            if (!scrollableIndication) {
                dotCount = totalDotCount
                bias = 0
            }

            viewPager.addOnPageChangeListener(this)
        }
    }

    private fun initCoordinates() = FloatArray(dotCount) { position ->
        viewPaddingStart + dotSpace * position + dotSpace / 2
    }

    private fun initTouchRanges() = Array(dotCount) { position ->
        (coordinates[position] - dotSpace / 2)..
                (coordinates[position] + dotSpace / 2)
    }

    private fun valueAnimator(): ValueAnimator {
        val propertyMoveForwardFactor = PropertyValuesHolder.ofFloat(
            keyPropertyMoveFactor,
            0F,
            dotSpace
        )

        val propertyColor = PropertyValuesHolder.ofObject(
            keyPropertyColor,
            ArgbEvaluator(),
            dotDefaultColor,
            dotSelectedColor
        )

        val propertyColorReverse = PropertyValuesHolder.ofObject(
            keyPropertyColorReverse,
            ArgbEvaluator(),
            dotSelectedColor,
            dotDefaultColor
        )

        return ValueAnimator().apply {
            setValues(
                propertyMoveForwardFactor,
                propertyColor,
                propertyColorReverse
            )
            interpolator = AccelerateDecelerateInterpolator()
            duration = animationDuration
            addUpdateListener { animation -> updateValues(animation) }
        }
    }

    private fun setupPadding() {
        viewPaddingStart = paddingStart
        viewPaddingEnd = paddingEnd
        viewPaddingTop = paddingTop
        viewPaddingBottom = paddingBottom
    }

    private fun setupCursor() {
        cursorStartPosition = 2
        cursorEndPosition = dotCount - 3
        cursorStartX = viewPaddingStart + (0.5F + cursorStartPosition) * dotSpace
        cursorEndX = viewPaddingStart + (0.5F + cursorEndPosition) * dotSpace
    }

    private fun fixPadding(horizontalPadding: Int, verticalPadding: Int) {
        if (horizontalPadding > 0) {
            viewPaddingStart = horizontalPadding
            viewPaddingEnd = horizontalPadding
        }

        if (verticalPadding > 0) {
            viewPaddingTop = verticalPadding
            viewPaddingBottom = verticalPadding
        }
    }

    private fun updateValues(animation: ValueAnimator) {
        animationMoveFactor = if (canScroll) {
            when {
                reverseAnimation -> -(animation.getAnimatedValue(keyPropertyMoveFactor) as Float)
                else -> animation.getAnimatedValue(keyPropertyMoveFactor) as Float
            }
        } else {
            0F
        }

        animationColor = animation.getAnimatedValue(keyPropertyColor) as Int
        animationColorReverse = animation.getAnimatedValue(keyPropertyColorReverse) as Int

        invalidate()
    }

    private fun fixBias() {
        if (!scrollableIndication) {
            return
        }

        bias -= when {
            currentSelection > cursorEndPosition - bias ->
                currentSelection - cursorEndPosition + bias

            currentSelection < cursorStartPosition - bias ->
                currentSelection - cursorStartPosition + bias

            else -> 0
        }
    }

    private fun drawIndicator(canvas: Canvas, position: Int) {
        val x = if (scrollableIndication) {
            when (position + bias) {
                in 0 until dotCount -> coordinates[position + bias]
                -1 -> coordinates[0] - dotSpace
                dotCount -> coordinates[dotCount - 1] + dotSpace
                else -> -dotSpace
            } - animationMoveFactor
        } else {
            coordinates[position]
        }

        val y = (height / 2).toFloat()

        val radius = if (scrollableIndication) {
            when {
                x < cursorStartX ->
                    dotSize * sqrt(x / cursorStartX)

                x > cursorEndX ->
                    dotSize * sqrt((width - x) / (width - cursorEndX))

                else -> dotSize
            }
        } else {
            dotSize
        } / 2

        paint.color = when (position) {
            currentSelection -> animationColorReverse
            newSelection -> animationColor
            else -> dotDefaultColor
        }

        canvas.drawCircle(x, y, radius, paint)
    }

    private fun pageScrolled(position: Int, positionOffset: Float) {
        reverseAnimation = currentSelection > position && positionOffset != 0F

        newSelection = when {
            reverseAnimation -> currentSelection - 1
            else -> currentSelection + 1
        }

        if (!reverseAnimation && currentSelection != position ||
            reverseAnimation && currentSelection != position + 1) {
            pageSelected((position + positionOffset).roundToInt())
        }

        if (scrollableIndication) {
            canScroll = !reverseAnimation && currentSelection + bias >= cursorEndPosition ||
                    reverseAnimation && currentSelection + bias <= cursorStartPosition
        }

        animator.currentPlayTime = (animationDuration * when {
            reverseAnimation -> 1 - positionOffset
            else -> positionOffset
        }).toLong()
    }

    private fun pageSelected(position: Int) {
        if (position == currentSelection) {
            return
        }

        currentSelection = position
        fixBias()
    }

    private fun onDotTouch(event: MotionEvent) {
        event.run {
            when (actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStart = x
                }

                MotionEvent.ACTION_UP -> {
                    for ((index, range) in touchRanges.withIndex()) {
                        if (x in range && touchStart in range) {
                            setCurrentItem(index - bias)
                        }
                    }
                }
            }
        }
    }

    private fun setCurrentItem(position: Int) {
        if (currentSelection == position || position < 0 || position >= totalDotCount) {
            return
        }

        viewPager?.setCurrentItem(position, true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (pageNavigationEnabled) {
            onDotTouch(event)
        }

        return pageNavigationEnabled
    }

    override fun onPageScrollStateChanged(state: Int) {  }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        pageScrolled(position, positionOffset)
    }

    override fun onPageSelected(position: Int) {  }
}
