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
        const val keyPropertyMediumScaleFactor = "animationMediumScaleFactor"
        const val keyPropertySmallScaleFactor = "animationSmallScaleFactor"
        const val keyPropertyInvisibleScaleFactor = "animationInvisibleScaleFactor"
        const val keyPropertyColor = "color"

        const val defaultDotCount = 7
        const val defaultScaleFactor = 0.6F
        const val minScaleFactor = 0.4F
        const val maxScaleFactor = 0.8F
    }

    private val dotSelectedPaint = Paint()
    private val dotDefaultPaint = Paint()
    private val animationDuration: Long = 300

    private var dotCount: Int = 0
    private var dotSize: Float = 0F
    private var dotSpace: Float = 0F
    private var scaleFactor: Float = 0F

    private var totalDotCount: Int = 0
    private var cursorStart: Int = 0
    private var cursorEnd: Int = 0
    private var selectedPosition: Int = 0

    private var animator: ValueAnimator = ValueAnimator()
    private var animationMoveFactor: Float = 0F
    private var animationMediumScaleFactor: Float = 0F
    private var animationSmallScaleFactor: Float = 0F
    private var animationInvisibleScaleFactor: Float = 0F
    private var reverseAnimation: Boolean = false

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FlexiblePageIndicator,
                0,
                0).apply {
            try {
                dotDefaultPaint.color = getColor(R.styleable.FlexiblePageIndicator_dot_color_default, Color.GRAY)
                dotSelectedPaint.color = getColor(R.styleable.FlexiblePageIndicator_dot_color_selected, Color.BLUE)

                dotCount = getInteger(R.styleable.FlexiblePageIndicator_dot_count, defaultDotCount)

                dotSize = getDimension(R.styleable.FlexiblePageIndicator_dot_size, resources.getDimension(R.dimen.dot_size_default))
                dotSpace = getDimension(R.styleable.FlexiblePageIndicator_dot_space, resources.getDimension(R.dimen.dot_space_default))

                if (dotSpace <= dotSize) {
                    scaleFactor = 2 * dotSize
                }

                scaleFactor = getFloat(R.styleable.FlexiblePageIndicator_scale_factor, defaultScaleFactor)

                if (scaleFactor > maxScaleFactor || scaleFactor < minScaleFactor) {
                    scaleFactor = defaultScaleFactor
                }

                animationMediumScaleFactor = scaleFactor
                animationSmallScaleFactor = scaleFactor * scaleFactor
                animationInvisibleScaleFactor = 0F

                setupAnimator()

            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth = dotCount * dotSpace
        val desiredHeight = dotSpace

        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val paddingStart = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            paddingStart
        } else {
            paddingLeft
        }
        val paddingEnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            paddingEnd
        } else {
            paddingRight
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(desiredWidth.toInt(), widthSize)
            else -> desiredWidth.toInt()
        }

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(desiredHeight.toInt(), heightSize)
            else -> desiredHeight.toInt()
        }

        setMeasuredDimension(width + paddingStart + paddingEnd, height + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {

        var width = width

        val paddingStart = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            width -= paddingStart
            paddingStart
        } else {
            width -= paddingLeft
            paddingLeft
        }

//        val paddingEnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            width -= paddingEnd
//            paddingEnd
//        } else {
//            width -= paddingRight
//            paddingRight
//        }

        for (position: Int in 0 until dotCount) {
            if (totalDotCount < dotCount) {
                canvas.drawCircle(
                        (width / (dotCount + 1) * (position + 1)).toFloat() + paddingStart - animationMoveFactor,
                        (height / 2).toFloat(),
                        dotSize / 2,
                        indicatorPaint(position))
            } else {
                canvas.drawCircle(
                        indicatorX(position, paddingStart.toFloat()),
                        (height / 2).toFloat(),
                        indicatorRadius(position),
                        indicatorPaint(position))
            }
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
        move()
    }

    fun setupWithViewPager(viewPager: ViewPager) {
        viewPager.adapter?.let {
            totalDotCount = it.count
            selectedPosition = viewPager.currentItem
            cursorStart = selectedPosition
            cursorEnd = cursorStart + dotCount - 5
            viewPager.addOnPageChangeListener(this)
        }
    }

    private fun setupAnimator() {
        val propertyMoveForwardFactor =
                PropertyValuesHolder.ofFloat(keyPropertyMoveFactor, 0F, dotSpace)
        val propertyMediumScaleFactor =
                PropertyValuesHolder.ofFloat(keyPropertyMediumScaleFactor, scaleFactor, 1F)
        val propertySmallScaleFactor =
                PropertyValuesHolder.ofFloat(keyPropertySmallScaleFactor, scaleFactor * scaleFactor, scaleFactor)
        val propertyInvisibleScaleFactor =
                PropertyValuesHolder.ofFloat(keyPropertyInvisibleScaleFactor, 0F, scaleFactor * scaleFactor)
        val propertyColor =
                PropertyValuesHolder.ofObject(keyPropertyColor, ArgbEvaluator(), dotSelectedPaint.color, dotDefaultPaint.color)

        animator.setValues(propertyMoveForwardFactor,
                propertyMediumScaleFactor,
                propertySmallScaleFactor,
                propertyInvisibleScaleFactor,
                propertyColor)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = animationDuration
        animator.addUpdateListener { animation -> updateView(animation) }
    }

    private fun move() {
        val bias = when {
            selectedPosition > cursorEnd -> selectedPosition - cursorEnd
            selectedPosition < cursorStart -> selectedPosition - cursorStart
            else -> 0
        }

        if (bias == 0) {
            return
        }

        cursorStart += bias
        cursorEnd += bias
    }

    private fun indicatorX(position: Int, paddingStart: Float): Float {
        return paddingStart - animationMoveFactor + when {
            cursorStart == 0 -> (width / (dotCount + 1) * (position + 3)).toFloat()
            cursorStart == 1 -> (width / (dotCount + 1) * (position + 2)).toFloat()
            cursorEnd == totalDotCount - 1 -> (width / (dotCount + 1) * (position - 1)).toFloat()
            cursorEnd == totalDotCount - 2 -> (width / (dotCount + 1) * position).toFloat()
            else -> (width / (dotCount + 1) * (position + 1)).toFloat()
        }
    }

    private fun indicatorRadius(position: Int): Float {
        return if (reverseAnimation) {
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
    }

    private fun indicatorPaint(position: Int): Paint {
        return when (position) {
            selectedPosition -> dotSelectedPaint
            else -> dotDefaultPaint
        }
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
        animation.getAnimatedValue(keyPropertyColor) as Int

        invalidate()
    }
}