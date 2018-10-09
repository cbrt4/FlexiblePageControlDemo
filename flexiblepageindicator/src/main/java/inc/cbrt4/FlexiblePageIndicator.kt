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
import java.util.*

class FlexiblePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs), OnPageChangeListener {

    companion object {
        const val tag = "FlexiblePageIndicator"

        const val keyPropertyMoveFactor = "animationMoveFactor"
        const val keyPropertyMediumScaleFactor = "animationMediumScaleFactor"
        const val keyPropertySmallScaleFactor = "animationSmallScaleFactor"
        const val keyPropertyInvisibleScaleFactor = "animationInvisibleScaleFactor"
        const val keyPropertyColor = "color"
        const val keyPropertyColorReverse = "colorReverse"

        const val defaultDotCount = 7
        const val defaultScaleFactor = 0.6F
        const val minScaleFactor = 0.4F
        const val maxScaleFactor = 0.8F
    }

    private val dotSelectedPaint = Paint()
    private val dotUnselectedPaint = Paint()
    private val dotDefaultPaint = Paint()
    private val animationDuration = 300L
    private val indicatorCalculator = IndicatorCalculator()

    private var viewPaddingTop = 0
    private var viewPaddingBottom = 0
    private var viewPaddingStart = 0
    private var viewPaddingEnd = 0

    private var viewWidth = 0
    private var viewHeight = 0

    private var dotDefaultColor = 0
    private var dotSelectedColor = 0
    private var dotCount = 0
    private var dotSize = 0F
    private var dotSpace = 0F
    private var scaleFactor = 0F

    private var totalDotCount = 0
    private var cursorStart = 0
    private var cursorEnd = 0
    private var bias = 2
    private var selectedPosition = 0

    private var animator = ValueAnimator()
    private var animationMoveFactor = 0F
    private var animationMediumScaleFactor = 0F
    private var animationSmallScaleFactor = 0F
    private var animationInvisibleScaleFactor = 0F
    private var animationColor = 0
    private var animationColorReverse = 0
    private var reverseAnimation = false
    private var scrollableIndication = false

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

                scaleFactor = getFloat(R.styleable.FlexiblePageIndicator_scaleFactor, defaultScaleFactor)

                if (scaleFactor > maxScaleFactor || scaleFactor < minScaleFactor) {
                    scaleFactor = defaultScaleFactor
                }

                animationMediumScaleFactor = scaleFactor
                animationSmallScaleFactor = scaleFactor * scaleFactor
                animationInvisibleScaleFactor = 0F

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

        viewWidth = width - viewPaddingStart - viewPaddingEnd
        viewHeight = height - viewPaddingTop - viewPaddingBottom

        if (!indicatorCalculator.calculated) {
            indicatorCalculator.calculate()
        }

        for (position: Int in 0 until dotCount) {
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
        bias += selectedPosition - position
        animator.currentPlayTime = (animationDuration * positionOffset).toLong()
    }

    override fun onPageSelected(position: Int) {
        selectedPosition = position
    }

    fun setupWithViewPager(viewPager: ViewPager) {
        viewPager.adapter?.let {
            totalDotCount = it.count
            selectedPosition = viewPager.currentItem

            scrollableIndication = totalDotCount > dotCount

            if (scrollableIndication) {
                cursorStart = selectedPosition
                cursorEnd = cursorStart + dotCount - 5
            } else {
                dotCount = totalDotCount
                cursorStart = 0
                cursorEnd = totalDotCount - 1
            }

            viewPager.addOnPageChangeListener(this)
        }
    }

    private fun setupAnimations() {
        val propertyMoveForwardFactor =
                PropertyValuesHolder.ofFloat(keyPropertyMoveFactor, 0F, dotSpace)
        val propertyMediumScaleFactor =
                PropertyValuesHolder.ofFloat(keyPropertyMediumScaleFactor, scaleFactor, 1F)
        val propertySmallScaleFactor =
                PropertyValuesHolder.ofFloat(keyPropertySmallScaleFactor, scaleFactor * scaleFactor, scaleFactor)
        val propertyInvisibleScaleFactor =
                PropertyValuesHolder.ofFloat(keyPropertyInvisibleScaleFactor, 0F, scaleFactor * scaleFactor)
        val propertyColor =
                PropertyValuesHolder.ofObject(keyPropertyColor, ArgbEvaluator(), dotSelectedColor, dotDefaultColor)
        val propertyColorReverse =
                PropertyValuesHolder.ofObject(keyPropertyColorReverse, ArgbEvaluator(), dotDefaultColor, dotSelectedColor)

        animator.setValues(propertyMoveForwardFactor,
                propertyMediumScaleFactor,
                propertySmallScaleFactor,
                propertyInvisibleScaleFactor,
                propertyColor,
                propertyColorReverse)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = animationDuration
        animator.addUpdateListener { animation -> updateView(animation) }
    }

    private fun updateView(animation: ValueAnimator) {
        animationMoveFactor = if (reverseAnimation && selectedPosition == cursorStart) {
            animation.getAnimatedValue(keyPropertyMoveFactor) as Float - dotSpace
        } else if (!reverseAnimation && selectedPosition == cursorEnd) {
            animation.getAnimatedValue(keyPropertyMoveFactor) as Float
        } else {
            0F
        }

        animationMediumScaleFactor = animation.getAnimatedValue(keyPropertyMediumScaleFactor) as Float
        animationSmallScaleFactor = animation.getAnimatedValue(keyPropertySmallScaleFactor) as Float
        animationInvisibleScaleFactor = animation.getAnimatedValue(keyPropertyInvisibleScaleFactor) as Float

        animationColor = animation.getAnimatedValue(keyPropertyColor) as Int
        animationColorReverse = animation.getAnimatedValue(keyPropertyColorReverse) as Int

        invalidate()
    }

    private inner class IndicatorCalculator {

        private var xCoordinates = floatArrayOf()
        var cursorStartX = 0F
        var cursorEndX = 0F
        var calculated = false

        fun calculate() {
            xCoordinates = FloatArray(dotCount)
            for (position: Int in 0 until xCoordinates.size) {
                xCoordinates[position] = (viewWidth / (dotCount + 1) * (position + 1)).toFloat()
            }
            calculated = true

            cursorStartX = xCoordinates[2]
            cursorEndX = xCoordinates[dotCount - 3]
            println("${Arrays.toString(xCoordinates)}\nStart = $cursorStartX\nEnd = $cursorEndX")
        }

        fun indicator(position: Int): Indicator {
            val x = viewPaddingStart - animationMoveFactor +
                    if (scrollableIndication) {
                        xCoordinates[position]

//                        cursorStart == 0 -> (viewWidth / (dotCount + 1) * (position + 3)).toFloat()
//                        cursorStart == 1 -> (viewWidth / (dotCount + 1) * (position + 2)).toFloat()
//                        cursorEnd == totalDotCount - 1 -> (viewWidth / (dotCount + 1) * (position - 1)).toFloat()
//                        cursorEnd == totalDotCount - 2 -> (viewWidth / (dotCount + 1) * position).toFloat()
//                        else -> (viewWidth / (dotCount + 1) * (position + 1)).toFloat()

                    } else {
                        xCoordinates[position]
                    }

            val y = (height / 2).toFloat()

            val radius = if (reverseAnimation) {
                when (position) {

                    cursorStart - 3 -> dotSize * (scaleFactor * scaleFactor - animationInvisibleScaleFactor) / 2
                    cursorStart - 2 -> dotSize * (scaleFactor * scaleFactor + scaleFactor - animationSmallScaleFactor) / 2
                    cursorStart - 1 -> dotSize * (scaleFactor + scaleFactor - animationSmallScaleFactor) / 2

                    in cursorStart until cursorEnd -> dotSize / 2
                    cursorEnd -> dotSize * animationMediumScaleFactor / 2

                    cursorEnd + 1 -> dotSize * animationSmallScaleFactor / 2
                    cursorEnd + 2 -> dotSize * animationInvisibleScaleFactor / 2

                    else -> 0F
                }
            } else {
                when (position) {

                    cursorStart - 2 -> dotSize * (scaleFactor * scaleFactor - animationInvisibleScaleFactor) / 2
                    cursorStart - 1 -> dotSize * (scaleFactor * scaleFactor + scaleFactor - animationSmallScaleFactor) / 2

                    cursorStart -> dotSize * (scaleFactor + 1F - animationMediumScaleFactor) / 2
                    in cursorStart + 1..cursorEnd -> dotSize / 2

                    cursorEnd + 1 -> dotSize * animationMediumScaleFactor / 2
                    cursorEnd + 2 -> dotSize * animationSmallScaleFactor / 2
                    cursorEnd + 3 -> dotSize * animationInvisibleScaleFactor / 2

                    else -> 0F
                }
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

            return Indicator(x, y, radius, paint)
        }
    }

    private inner class Indicator(val x: Float, val y: Float, val radius: Float, val paint: Paint)
}