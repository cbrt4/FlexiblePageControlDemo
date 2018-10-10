package inc.cbrt4

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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

        const val defaultDotCount = 7
    }

    private val dotSelectedPaint = Paint()
    private val dotUnselectedPaint = Paint()
    private val dotDefaultPaint = Paint()
    private val animationDuration = 300L

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

    private var totalDotCount = 0
    private var bias = 2
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

    private lateinit var animator: ValueAnimator

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FlexiblePageIndicator,
                0,
                0).apply {
            try {
                dotDefaultColor = getColor(R.styleable.FlexiblePageIndicator_dotColorDefault, Color.GRAY)
                dotSelectedColor = getColor(R.styleable.FlexiblePageIndicator_dotColorSelected, Color.BLUE)

                dotSelectedPaint.isAntiAlias = true
                dotUnselectedPaint.isAntiAlias = true
                dotDefaultPaint.isAntiAlias = true

                dotSelectedPaint.color = dotSelectedColor
                dotUnselectedPaint.color = dotDefaultColor
                dotDefaultPaint.color = dotDefaultColor

                dotCount = getInteger(R.styleable.FlexiblePageIndicator_dotCount, defaultDotCount)

                dotSize = getDimension(R.styleable.FlexiblePageIndicator_dotSize, resources.getDimension(R.dimen.dot_size_default))
                dotSpace = getDimension(R.styleable.FlexiblePageIndicator_dotSpace, resources.getDimension(R.dimen.dot_space_default))

                if (dotSpace <= dotSize) {
                    dotSpace = 2 * dotSize
                }

                cursorStartPosition = 2
                cursorEndPosition = dotCount - 3
                cursorStartX = (0.5F + cursorStartPosition) * dotSpace
                cursorEndX = (0.5F + cursorEndPosition) * dotSpace

                animator = ValueAnimator()

                setupAnimations()

            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        fixPadding()

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
        event?.let {
            if (it.actionMasked == MotionEvent.ACTION_DOWN) {
                return true
            }

            if (it.actionMasked == MotionEvent.ACTION_UP) {
                for (position: Int in 0 until dotCount) {
                    if (it.x in xCoordinates[position] - dotSpace / 2..xCoordinates[position] + dotSpace / 2) {
                        viewPager?.currentItem = position - bias
                        return true
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
        reverseAnimation = position < selectedPosition
        animator.currentPlayTime = (animationDuration * positionOffset).toLong()
    }

    override fun onPageSelected(position: Int) {
        selectedPosition = position
        scroll()
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

    private fun fixPadding() {
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
                PropertyValuesHolder.ofObject(keyPropertyColor, ArgbEvaluator(), dotSelectedColor, dotDefaultColor)

        val propertyColorReverse =
                PropertyValuesHolder.ofObject(keyPropertyColorReverse, ArgbEvaluator(), dotDefaultColor, dotSelectedColor)

        animator.setValues(propertyMoveForwardFactor,
                propertyColor,
                propertyColorReverse)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = animationDuration
        animator.addUpdateListener { animation -> updateView(animation) }
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
                    if (position + bias in 0 until dotCount) {
                        xCoordinates[position + bias]
                    } else {
                        //ToDo fix here
                        viewWidth * 2
                    }
                } else {
                    xCoordinates[position]
                }

        val y = (height / 2).toFloat()

        val radius =
                when {
                    x < cursorStartX ->
                        dotSize * (x / cursorStartX) / 2

                    x > cursorEndX ->
                        dotSize * ((viewWidth - x) / (viewWidth - cursorEndX)) / 2

                    else -> dotSize / 2
                }

        dotSelectedPaint.color = animationColor
        dotUnselectedPaint.color = animationColorReverse
        val paint = if (reverseAnimation) {
            when (position) {
                selectedPosition - 1 -> dotSelectedPaint
                selectedPosition -> dotUnselectedPaint
                else -> dotDefaultPaint
            }
        } else {
            when (position) {
                selectedPosition -> dotSelectedPaint
                selectedPosition + 1 -> dotUnselectedPaint
                else -> dotDefaultPaint
            }
        }

        canvas.drawCircle(x, y, radius, paint)
    }

    private fun updateView(animation: ValueAnimator) {
        animationMoveFactor = if (reverseAnimation && selectedPosition == cursorStartPosition - bias) {
            animation.getAnimatedValue(keyPropertyMoveFactor) as Float - dotSpace
        } else if (!reverseAnimation && selectedPosition == cursorEndPosition - bias) {
            animation.getAnimatedValue(keyPropertyMoveFactor) as Float
        } else {
            0F
        }

        animationColor = animation.getAnimatedValue(keyPropertyColor) as Int
        animationColorReverse = animation.getAnimatedValue(keyPropertyColorReverse) as Int

        invalidate()
    }

    private fun scroll() {
        val correction = when {
            selectedPosition > cursorEndPosition - bias ->
                selectedPosition - cursorEndPosition + bias

            selectedPosition < cursorStartPosition - bias ->
                selectedPosition - cursorStartPosition + bias

            else -> 0
        }

        bias -= correction
    }
}