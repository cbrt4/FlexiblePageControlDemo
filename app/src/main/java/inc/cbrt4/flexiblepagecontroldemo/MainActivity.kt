package inc.cbrt4.flexiblepagecontroldemo

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager.widget.PagerAdapter
import inc.cbrt4.flexiblepagecontroldemo.databinding.ActivityMainBinding
import inc.cbrt4.flexiblepagecontroldemo.databinding.ItemPageBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private val resIds: Array<Int> =
        if (BuildConfig.IS_COLOR) arrayOf(
            R.color.colorViewPager0,
            R.color.colorViewPager1,
            R.color.colorViewPager2,
            R.color.colorViewPager3,
            R.color.colorViewPager4,
            R.color.colorViewPager5,
            R.color.colorViewPager6,
            R.color.colorViewPager7,
            R.color.colorViewPager8,
            R.color.colorViewPager9,
            R.color.colorViewPager10
        )
        else arrayOf(
            R.drawable.item_pager_1,
            R.drawable.item_pager_2,
            R.drawable.item_pager_3,
            R.drawable.item_pager_4,
            R.drawable.item_pager_5,
            R.drawable.item_pager_6,
            R.drawable.item_pager_7,
            R.drawable.item_pager_8,
            R.drawable.item_pager_9,
            R.drawable.item_pager_10,
            R.drawable.item_pager_11
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        goFullscreen()
        setupViewPager()
        updateWindowInsets()
    }

    private fun goFullscreen() = window.setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    )

    private fun setupViewPager() {
        val mainPagerAdapter = object : PagerAdapter() {

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                ItemPageBinding.inflate(layoutInflater, container, false).root.apply {
                    setImageResource(resIds[position])
                    container.addView(this)
                    return this
                }
            }

            override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
                container.removeView(view as View)
            }

            override fun getCount(): Int {
                return resIds.size
            }

            override fun isViewFromObject(view: View, any: Any): Boolean {
                return view == any
            }

        }

        with(binding) {
            mainPager.adapter = mainPagerAdapter
            pageIndicator.setupWithViewPager(mainPager)
        }
    }

    private fun updateWindowInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemBarsInsets ->
                with(binding) {
                    topOverlay.updateLayoutParams {
                        height = systemBarsInsets.top
                    }
                    pageIndicator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        leftMargin = systemBarsInsets.left
                        bottomMargin = systemBarsInsets.bottom
                        rightMargin = systemBarsInsets.right
                    }
                }
            }
            WindowInsetsCompat.CONSUMED
        }
}
