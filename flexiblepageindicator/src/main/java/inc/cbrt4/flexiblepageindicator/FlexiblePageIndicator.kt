package inc.cbrt4.flexiblepageindicator

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

class FlexiblePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs), OnPageChangeListener {

    private var dotCount: Int = 0
    private var dotSize: Float = 0F
    private var dotSpace: Float = 0F
    private var scaleFactor: Float = 0F

    private val dotSelectedPaint = Paint()
    private val dotDefaultPaint = Paint()
    private val animationDuration: Long = 300

    private var totalDotCount: Int = 0
    private var cursorStart: Int = 0
    private var cursorEnd: Int = 0
    private var selectedPosition: Int = 0

    private var animator: ValueAnimator = ValueAnimator()

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

        for (i: Int in 0 until dotCount) {
            if (dotCount < 5) {
                canvas.drawCircle(
                        (width / (dotCount + 1) * (i + 1)).toFloat() + paddingStart,
                        (height / 2).toFloat(),
                        dotSize / 2,
                        when (i) {
                            selectedPosition -> dotSelectedPaint
                            else -> dotDefaultPaint
                        })
            } else {
                canvas.drawCircle(
                        when {
                            cursorStart == 0 -> (width / (dotCount + 1) * (i + 3)).toFloat() + paddingStart
                            cursorStart == 1 -> (width / (dotCount + 1) * (i + 2)).toFloat() + paddingStart
                            cursorEnd == totalDotCount - 1 -> (width / (dotCount + 1) * (i - 1)).toFloat() + paddingStart
                            cursorEnd == totalDotCount - 2 -> (width / (dotCount + 1) * i).toFloat() + paddingStart
                            else -> (width / (dotCount + 1) * (i + 1)).toFloat() + paddingStart
                        },
                        (height / 2).toFloat(),
                        when (i) {
                            in cursorStart..cursorEnd -> dotSize / 2
                            cursorStart - 2, cursorEnd + 2 -> dotSize * scaleFactor * scaleFactor / 2
                            cursorStart - 1, cursorEnd + 1 -> dotSize * scaleFactor / 2
                            else -> 0F
                        },
                        when (i) {
                            selectedPosition -> dotSelectedPaint
                            else -> dotDefaultPaint
                        })
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        //
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        selectedPosition = position
        move(position)
        updateView()
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
        val propertyMove = PropertyValuesHolder.ofFloat(keyPropertyMove, 0F, dotSpace)
        val propertyHeight = PropertyValuesHolder.ofFloat(keyPropertyHeight, 0F, 360F)
        val propertyWidth = PropertyValuesHolder.ofFloat(keyPropertyWidth, 0F, 360F)
        val propertyColor = PropertyValuesHolder.ofInt(keyPropertyColor, dotSelectedPaint.color, dotDefaultPaint.color)

        var move: Float
        var height: Float
        var width: Float
        //var color = 0

        animator.setValues(propertyMove, propertyHeight, propertyWidth, propertyColor)
        //animator.setEvaluator(ArgbEvaluator())
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = animationDuration
        animator.addUpdateListener { animation ->
            move = animation.getAnimatedValue(keyPropertyMove) as Float
            height = animation.getAnimatedValue(keyPropertyHeight) as Float
            width = animation.getAnimatedValue(keyPropertyWidth) as Float
            //color = animation.getAnimatedValue(keyPropertyColor) as Int

            print(move)
            print(height)
            print(width)
        }
    }

    private fun move(position: Int) {
        val bias = when {
            position > cursorEnd -> position - cursorEnd
            position < cursorStart -> position - cursorStart
            else -> 0
        }

        if (bias == 0) {
            return
        }

        cursorStart += bias
        cursorEnd += bias
    }

    private fun updateView() {
        animator.start()
        invalidate()
    }

    companion object {
        const val keyPropertyMove = "move"
        const val keyPropertyHeight = "height"
        const val keyPropertyWidth = "width"
        const val keyPropertyColor = "color"

        const val defaultDotCount = 7
        const val defaultScaleFactor = 0.6F
        const val minScaleFactor = 0.4F
        const val maxScaleFactor = 0.8F
    }
}