package inc.cbrt4.flexiblepagecontroldemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

	val colorArray: Array<Int> = arrayOf(
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

//			R.drawable.item_pager_1,
//			R.drawable.item_pager_2,
//			R.drawable.item_pager_3,
//			R.drawable.item_pager_4,
//			R.drawable.item_pager_5,
//			R.drawable.item_pager_6,
//			R.drawable.item_pager_7,
//			R.drawable.item_pager_8,
//			R.drawable.item_pager_9,
//			R.drawable.item_pager_10,
//			R.drawable.item_pager_11
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
		val mainPagerAdapter = object : androidx.viewpager.widget.PagerAdapter() {

			override fun instantiateItem(container: ViewGroup, position: Int): Any {
				val page = LayoutInflater.from(container.context)
						.inflate(R.layout.item_page, container, false) as ImageView
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
