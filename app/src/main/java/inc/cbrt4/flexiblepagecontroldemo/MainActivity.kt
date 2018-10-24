package inc.cbrt4.flexiblepagecontroldemo

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val colorArray: Array<Int> = arrayOf(
            R.color.colorRed,
            R.color.colorOrange,
            R.color.colorYellow,
            R.color.colorGreen,
            R.color.colorBlue,
            R.color.colorIndigo,
            R.color.colorViolet,
            R.color.colorWhite,
            R.color.colorGrey
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
    }

    private fun setupViews() {
        setupViewPager()
    }

    private fun setupViewPager() {
        val mainPagerAdapter = object : PagerAdapter() {

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val page = LayoutInflater.from(container.context).inflate(R.layout.item_page, container, false)
                page.setBackgroundResource(colorArray[position])
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
