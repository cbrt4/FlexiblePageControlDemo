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
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import inc.cbrt4.flexiblepageindicator.R
import kotlin.math.sqrt

class FlexiblePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs), OnPageChangeListener {

    private val keyPropertyMoveFactor = "animationMoveFactor"
    private val keyPropertyColor = "color"
    private val keyPropertyColorReverse = "colorReverse"

    private val colorDefault = -2130706433
    private val colorSelected = -1

    private val defaultDotCount = 7

    private val paint = Paint()

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

    private var totalDotCount = 0
    private var bias = 2
    private var currentSelection = 0
    private var newSelection = 0
    private var pagerCurrentItem = 0
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

    private var xCoordinates = floatArrayOf()

    private var viewPager: ViewPager? = null
    private var animator: ValueAnimator? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.FlexiblePageIndicator, 0, 0).apply {
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

                animationDuration = 1000L

                paint.isAntiAlias = true

                animator = ValueAnimator()

                setupAnimations()

            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        setupPadding()

        viewWidth = dotCount * dotSpace
        viewHeight = dotSpace

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(viewWidth.toInt(), widthSize)
            else -> viewWidth.toInt()
        }

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(viewHeight.toInt(), heightSize)
            else -> viewHeight.toInt()
        }

        fixPadding((width - viewWidth.toInt()) / 2,
                (height - viewHeight.toInt()) / 2)

        setupCursor()

        measureCoordinates()

        setMeasuredDimension(viewWidth.toInt() + viewPaddingStart + viewPaddingEnd,
                viewHeight.toInt() + viewPaddingTop + viewPaddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        for (position: Int in 0 until totalDotCount) {
            drawIndicator(canvas, position)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        //
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        pageScrolled(position, positionOffset)
    }

    override fun onPageSelected(position: Int) {
        pagerCurrentItem = position
    }

    fun setupWithViewPager(viewPager: ViewPager) {

        this.viewPager = viewPager

        viewPager.adapter?.let {
            totalDotCount = it.count
            currentSelection = viewPager.currentItem

            scrollableIndication = totalDotCount > dotCount

            if (!scrollableIndication) {
                dotCount = totalDotCount
            }

            viewPager.addOnPageChangeListener(this)
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

    private fun setupPadding() {

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

        viewPaddingTop = paddingTop

        viewPaddingBottom = paddingBottom
    }

    private fun fixPadding(horizontalFix: Int, verticalFix: Int) {

        if (horizontalFix != 0) {
            viewPaddingStart = horizontalFix
            viewPaddingEnd = horizontalFix
        }

        if (verticalFix != 0) {
            viewPaddingTop = verticalFix
            viewPaddingBottom = verticalFix
        }
    }

    private fun setupCursor() {
        cursorStartPosition = 2
        cursorEndPosition = dotCount - 3
        cursorStartX = viewPaddingStart + (0.5F + cursorStartPosition) * dotSpace
        cursorEndX = viewPaddingStart + (0.5F + cursorEndPosition) * dotSpace
    }

    private fun measureCoordinates() {
        xCoordinates = FloatArray(dotCount)
        for (position: Int in 0 until xCoordinates.size) {
            xCoordinates[position] = viewPaddingStart + dotSpace * position + dotSpace / 2
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

    private fun drawIndicator(canvas: Canvas, position: Int) {

        val x = if (scrollableIndication) {
            when {
                position + bias in 0 until dotCount -> xCoordinates[position + bias]
                position + bias == -1 -> xCoordinates[0] - dotSpace
                position + bias == dotCount -> xCoordinates[dotCount - 1] + dotSpace
                else -> -1F
            } - animationMoveFactor
        } else {
            xCoordinates[position]
        }

        val y = (height / 2).toFloat()

        val radius = if (scrollableIndication) {
            when {
                x < -animationMoveFactor -> 0F

                x < cursorStartX ->
                    dotSize * sqrt((x - viewPaddingStart) / (cursorStartX - viewPaddingStart)) / 2

                x > cursorEndX ->
                    dotSize * sqrt((viewPaddingStart + viewWidth - x) / (viewPaddingStart + viewWidth - cursorEndX)) / 2

                else -> dotSize / 2
            }
        } else {
            dotSize / 2
        }

        paint.color = when (position) {
            currentSelection -> animationColorReverse
            newSelection -> animationColor
            else -> dotDefaultColor
        }

        canvas.drawCircle(x, y, radius, paint)
    }

    private fun pageScrolled(position: Int, positionOffset: Float) {

        reverseAnimation = position < currentSelection && positionOffset != 0F

        newSelection = when {
            reverseAnimation -> currentSelection - 1
            else -> currentSelection + 1
        }

        if (positionOffset == 0F ||
                !reverseAnimation && position != currentSelection ||
                reverseAnimation && position != currentSelection - 1) {
            pageSelected(pagerCurrentItem)
        }

        if (scrollableIndication) {
            canScroll = !reverseAnimation && newSelection + bias > cursorEndPosition ||
                    reverseAnimation && newSelection + bias < cursorStartPosition
        }

        animator?.currentPlayTime = (animationDuration * when {
            reverseAnimation -> 1 - positionOffset
            else -> positionOffset
        }).toLong()
    }

    private fun pageSelected(position: Int) {
        currentSelection = position
        fixBias()
    }

    private fun fixBias() {
        val fix = when {
            currentSelection > cursorEndPosition - bias ->
                currentSelection - cursorEndPosition + bias

            currentSelection < cursorStartPosition - bias ->
                currentSelection - cursorStartPosition + bias

            else -> 0
        }

        bias -= fix
    }
}