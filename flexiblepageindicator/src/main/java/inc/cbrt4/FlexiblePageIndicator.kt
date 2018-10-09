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

    private lateinit var animator: ValueAnimator
    private lateinit var indicatorCalculator: IndicatorCalculator

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

                animator = ValueAnimator()
                indicatorCalculator = IndicatorCalculator()

                setupAnimations()

            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

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

        if (!indicatorCalculator.calculated) {
            indicatorCalculator.calculate()
        }

        for (position: Int in 0 until totalDotCount) {
            canvas.drawCircle(
                    indicatorCalculator.indicator(position).x,
                    indicatorCalculator.indicator(position).y,
                    indicatorCalculator.indicator(position).radius,
                    indicatorCalculator.indicator(position).paint)
        }
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

    private fun scroll() {
        val correction = when {
            selectedPosition > indicatorCalculator.cursorEndPosition - bias ->
                selectedPosition - indicatorCalculator.cursorEndPosition + bias

            selectedPosition < indicatorCalculator.cursorStartPosition - bias ->
                selectedPosition - indicatorCalculator.cursorStartPosition + bias

            else -> 0
        }

        bias -= correction
    }

    private fun updateView(animation: ValueAnimator) {
        animationMoveFactor = if (reverseAnimation && selectedPosition == indicatorCalculator.cursorStartPosition - bias) {
            animation.getAnimatedValue(keyPropertyMoveFactor) as Float - dotSpace
        } else if (!reverseAnimation && selectedPosition == indicatorCalculator.cursorEndPosition - bias) {
            animation.getAnimatedValue(keyPropertyMoveFactor) as Float
        } else {
            0F
        }

        animationColor = animation.getAnimatedValue(keyPropertyColor) as Int
        animationColorReverse = animation.getAnimatedValue(keyPropertyColorReverse) as Int

        invalidate()
    }

    private inner class IndicatorCalculator {

        val cursorStartPosition = 2
        val cursorEndPosition = dotCount - 3
        val cursorStartX = (0.5F + cursorStartPosition) * dotSpace
        val cursorEndX = (0.5F + cursorEndPosition) * dotSpace

        var calculated = false
        private var xCoordinates = floatArrayOf()

        fun calculate() {
            xCoordinates = FloatArray(dotCount)
            for (position: Int in 0 until xCoordinates.size) {
                xCoordinates[position] = viewWidth / dotCount * position + dotSpace / 2
            }
            calculated = true
        }

        fun indicator(position: Int): Indicator {
            val indicator = Indicator()

            indicator.x = viewPaddingStart - animationMoveFactor +
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

            indicator.y = (height / 2).toFloat()

            indicator.radius =
                    when {
                        indicator.x < cursorStartX ->
                            dotSize * (indicator.x / cursorStartX) / 2

                        indicator.x > cursorEndX ->
                            dotSize * ((viewWidth - indicator.x) / (viewWidth - cursorEndX)) / 2

                        else -> dotSize / 2
                    }

            dotSelectedPaint.color = animationColor
            dotUnselectedPaint.color = animationColorReverse
            indicator.paint = if (reverseAnimation) {
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

            return indicator
        }
    }

    private inner class Indicator {
        var x = 0F
        var y = 0F
        var radius = 0F
        var paint = Paint()
    }
}