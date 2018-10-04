package inc.cbrt4.flexiblepageindicator

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator


class FlexiblePageIndicator(context: Context, attrs: AttributeSet) : View(context, attrs), OnPageChangeListener {

//    private var dotColorDefault: Int = 0
//    private var dotColorSelected: Int = 0
    private var dotCount: Int = 5
    private var dotSize: Float = 6F
    private var dotSpace: Float = 12F
    private var scaleFactor: Float = 0.6F

    private val dotSelectedPaint = Paint()
    private val dotDefaultPaint = Paint()
    private val animationDuration: Long = 300

    private var totalDotCount: Int = 0
    private var cursorStart: Int = 0
    private var cursorEnd: Int = 0
    private var selectedPosition: Int = 0

    private lateinit var selectAnimator: ValueAnimator
    private lateinit var deselectAnimator: ValueAnimator
    private lateinit var scaleAnimator: ValueAnimator
    private lateinit var moveAnimator: ValueAnimator

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FlexiblePageIndicator,
                0,
                0).apply {
            try {
                dotDefaultPaint.color = getColor(R.styleable.FlexiblePageIndicator_dot_color_default, Color.GRAY)
                dotSelectedPaint.color = getColor(R.styleable.FlexiblePageIndicator_dot_color_selected, Color.BLUE)

                dotCount = getInteger(R.styleable.FlexiblePageIndicator_dot_count, 5)

                dotSize = getDimension(R.styleable.FlexiblePageIndicator_dot_size, 6F)
                dotSpace = getDimension(R.styleable.FlexiblePageIndicator_dot_space, 12F)
                if (dotSpace <= dotSize) {
                    scaleFactor = 2 * dotSize
                }

                scaleFactor = getFloat(R.styleable.FlexiblePageIndicator_scale_factor, 0.6F)
                if (scaleFactor > 0.8 || scaleFactor < 0.4) {
                    scaleFactor = 0.6F
                }

                setupAnimators()

            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth = dotCount * dotSpace
        val desiredHeight = dotSpace

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

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        for (i: Int in 0 until dotCount) {
            if (dotCount < 5) {
                canvas.drawCircle(
                        (width / (dotCount + 1) * (i + 1)).toFloat(),
                        (height / 2).toFloat(),
                        dotSize / 2,
                        when (i) {
                            selectedPosition -> dotSelectedPaint
                            else -> dotDefaultPaint
                        })
            } else {
                canvas.drawCircle(
                        (width / (dotCount + 1) * (i + 1)).toFloat(),
                        (height / 2).toFloat(),
                        when (i) {
                            0, dotCount - 1 -> dotSize * scaleFactor * scaleFactor / 2
                            1, dotCount - 2 -> dotSize * scaleFactor / 2
                            else -> dotSize / 2
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
        move(position)
    }

    fun setupWithViewPager(viewPager: ViewPager) {
        viewPager.adapter?.let {
            totalDotCount = it.count
            selectedPosition = viewPager.currentItem
            viewPager.addOnPageChangeListener(this)
        }
    }

    private fun setupAnimators() {
        selectAnimator = ValueAnimator()
        selectAnimator.duration = animationDuration
        selectAnimator.interpolator = AccelerateDecelerateInterpolator()
        selectAnimator.addUpdateListener { animation -> "" }


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

        updateView()
    }

    private fun updateView() {
        
    }
}