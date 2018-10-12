package inc.cbrt4

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import inc.cbrt4.flexiblepageindicator.R

class FlexiblePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs), OnPageChangeListener {

    companion object {
        const val keyPropertyMoveFactor = "animationMoveFactor"
        const val keyPropertyColor = "color"
        const val keyPropertyColorReverse = "colorReverse"

        const val colorDefault = -0x80000000
        const val colorSelected = -0x333334

        const val defaultDotCount = 7
    }

    private var viewPaddingTop = 0
    private var viewPaddingBottom = 0
    private var viewPaddingStart = 0
    private var viewPaddingEnd = 0

    private var viewWidth = 0F
    private var viewHeight = 0F

    private var dotDefaultColor = 0
    private var dotSelectedColor = 0
    private var dotCount = 0
    private var dotSize = 0F
    private var dotSpace = 0F
    private var animationDuration = 0L
    private var pageNavigationEnabled = false

    private var totalDotCount = 0
    private var bias = 2
    private var previousSelectedPosition = -1
    private var selectedPosition = 0
    private var animationMoveFactor = 0F
    private var animationColor = 0
    private var animationColorReverse = 0
    private var reverseAnimation = false
    private var scrollableIndication = false

    private var cursorStartPosition = 0
    private var cursorEndPosition = 0
    private var cursorStartX = 0F
    private var cursorEndX = 0F

    private var calculated = false
    private var xCoordinates = floatArrayOf()

    private var viewPager: ViewPager? = null
    private var animator: ValueAnimator? = null

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FlexiblePageIndicator,
                0,
                0).apply {
            try {
                dotDefaultColor = getColor(R.styleable.FlexiblePageIndicator_dotColorDefault,
                        colorDefault)

                dotSelectedColor = getColor(R.styleable.FlexiblePageIndicator_dotColorSelected,
                        colorSelected)

                dotCount = getInteger(R.styleable.FlexiblePageIndicator_dotCount, defaultDotCount)

                dotSize = getDimension(R.styleable.FlexiblePageIndicator_dotSize,
                        resources.getDimension(R.dimen.dot_size_default))

                dotSpace = getDimension(R.styleable.FlexiblePageIndicator_dotSpace,
                        resources.getDimension(R.dimen.dot_space_default))

                if (dotSpace <= dotSize) {
                    dotSpace = 2 * dotSize
                }

                animationDuration = getInteger(R.styleable.FlexiblePageIndicator_animationDuration, 300).toLong()

                pageNavigationEnabled = getBoolean(R.styleable.FlexiblePageIndicator_pageNavigationEnabled, true)

                animator = ValueAnimator()

                setupCursor()
                setupAnimations()

            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        setupPadding()

        val contentWidth = dotCount * dotSpace
        val contentHeight = dotSpace

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(contentWidth.toInt(), widthSize)
            else -> contentWidth.toInt()
        }

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(contentHeight.toInt(), heightSize)
            else -> contentHeight.toInt()
        }

        setMeasuredDimension(width + viewPaddingStart + viewPaddingEnd, height + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {

        viewWidth = (width - viewPaddingStart - viewPaddingEnd).toFloat()
        viewHeight = (height - viewPaddingTop - viewPaddingBottom).toFloat()

        if (!calculated) {
            calculate()
        }

        for (position: Int in 0 until totalDotCount) {
            drawIndicator(canvas, position)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (pageNavigationEnabled) {
            event?.let {
                if (it.actionMasked == MotionEvent.ACTION_DOWN) {
                    return true
                }

                if (it.actionMasked == MotionEvent.ACTION_UP) {
                    for (position: Int in 0 until dotCount) {
                        if (it.x in xCoordinates[position] - (dotSize + dotSpace) / 4..xCoordinates[position] + (dotSize + dotSpace) / 4) {
                            setCurrentItem(position - bias)
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    override fun onPageScrollStateChanged(state: Int) {
        //
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        pageScrolled(position, positionOffset)
    }

    override fun onPageSelected(position: Int) {
        pageSelected(position)
    }

    fun setupWithViewPager(viewPager: ViewPager) {

        this.viewPager = viewPager

        viewPager.adapter?.let {
            totalDotCount = it.count
            selectedPosition = viewPager.currentItem

            scrollableIndication = totalDotCount > dotCount

            if (!scrollableIndication) {
                dotCount = totalDotCount
            }

            viewPager.addOnPageChangeListener(this)
        }
    }

    private fun setupCursor() {
        cursorStartPosition = 2
        cursorEndPosition = dotCount - 3
        cursorStartX = (0.5F + cursorStartPosition) * dotSpace
        cursorEndX = (0.5F + cursorEndPosition) * dotSpace
    }

    private fun setupPadding() {
        viewPaddingTop = paddingTop
        viewPaddingBottom = paddingBottom
        viewPaddingStart = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            paddingStart
        } else {
            paddingLeft
        }
        viewPaddingEnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            paddingEnd
        } else {
            paddingRight
        }
    }

    private fun setupAnimations() {
        val propertyMoveForwardFactor =
                PropertyValuesHolder.ofFloat(keyPropertyMoveFactor, 0F, dotSpace)

        val propertyColor =
                PropertyValuesHolder.ofObject(keyPropertyColor, ArgbEvaluator(), dotDefaultColor, dotSelectedColor)

        val propertyColorReverse =
                PropertyValuesHolder.ofObject(keyPropertyColorReverse, ArgbEvaluator(), dotSelectedColor, dotDefaultColor)

        animator?.let {
            it.setValues(propertyMoveForwardFactor,
                    propertyColor,
                    propertyColorReverse)
            it.interpolator = AccelerateDecelerateInterpolator()
            it.duration = animationDuration
            it.addUpdateListener { animation -> updateValues(animation) }
        }
    }

    private fun calculate() {
        xCoordinates = FloatArray(dotCount)
        for (position: Int in 0 until xCoordinates.size) {
            xCoordinates[position] = viewWidth / dotCount * position + dotSpace / 2
        }
        calculated = true
    }

    private fun drawIndicator(canvas: Canvas, position: Int) {

        val x = viewPaddingStart - animationMoveFactor +
                if (scrollableIndication) {
                    when {
                        position + bias in 0 until dotCount -> xCoordinates[position + bias]
                        position + bias == -1 -> xCoordinates[0] - dotSpace
                        position + bias == dotCount -> xCoordinates[dotCount - 1] + dotSpace
                        else -> -1F
                    }
                } else {
                    xCoordinates[position]
                }

        val y = (height / 2).toFloat()

        val radius =
                when {
                    x < viewPaddingStart - animationMoveFactor -> 0F

                    x < cursorStartX ->
                        dotSize * (x / cursorStartX) / 2

                    x > cursorEndX ->
                        dotSize * ((viewWidth - x) / (viewWidth - cursorEndX)) / 2

                    else -> dotSize / 2
                }

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = when {
            reverseAnimation && position == selectedPosition - 1 -> animationColor
            !reverseAnimation && position == selectedPosition + 1 -> animationColor
            position == selectedPosition -> animationColorReverse
            else -> dotDefaultColor
        }

        canvas.drawCircle(x, y, radius, paint)
    }

    private fun updateValues(animation: ValueAnimator) {
        animationMoveFactor =
                when {
                    reverseAnimation -> -(animation.getAnimatedValue(keyPropertyMoveFactor) as Float)
                    else -> animation.getAnimatedValue(keyPropertyMoveFactor) as Float
                }

        animationColor = animation.getAnimatedValue(keyPropertyColor) as Int
        animationColorReverse = animation.getAnimatedValue(keyPropertyColorReverse) as Int

        invalidate()
    }

    private fun pageScrolled(position: Int, positionOffset: Float) {
        reverseAnimation = position < selectedPosition
        animator?.currentPlayTime =
                if (reverseAnimation) {
                    (animationDuration * (1 - positionOffset)).toLong()
                } else {
                    (animationDuration * positionOffset).toLong()
                }
    }

    private fun pageSelected(position: Int) {
        previousSelectedPosition = selectedPosition
        selectedPosition = position
        fixBias()
    }

    private fun fixBias() {
        val fix =
                when {
                    selectedPosition > cursorEndPosition - bias ->
                        selectedPosition - cursorEndPosition + bias

                    selectedPosition < cursorStartPosition - bias ->
                        selectedPosition - cursorStartPosition + bias

                    else -> 0
                }

        bias -= fix
    }

    private fun setCurrentItem(position: Int) {
        viewPager?.currentItem = position - bias
    }
}