package inc.cbrt4.flexiblepagecontroldemo

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val colorArray: Array<Int> = arrayOf(
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
        setContentView(R.layout.activity_main)

        goFullscreen()

        setupViews()
    }

    private fun goFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun setupViews() {
        setupViewPager()
    }

    private fun setupViewPager() {
        val mainPagerAdapter = object : PagerAdapter() {

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val page: ImageView = LayoutInflater.from(container.context).inflate(R.layout.item_page, container, false) as ImageView
                page.setImageResource(colorArray[position])
                container.addView(page)
                return page
            }

            override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
                container.removeView(view as View)
            }

            override fun getCount(): Int {
                return colorArray.size
            }

            override fun isViewFromObject(view: View, any: Any): Boolean {
                return view == any
            }

        }

        mainPager.adapter = mainPagerAdapter

        pageIndicator.setupWithViewPager(mainPager)
    }
}
