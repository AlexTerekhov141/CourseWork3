package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var buttonNext: Button
    private lateinit var buttonSkip: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.view_pager)
        dotsIndicator = findViewById(R.id.dots_indicator)
        buttonNext = findViewById(R.id.button_next)
        buttonSkip = findViewById(R.id.button_skip)


        val pagerAdapter = OnboardingPagerAdapter(supportFragmentManager)
        viewPager.adapter = pagerAdapter

        dotsIndicator.setViewPager(viewPager)

        buttonNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < pagerAdapter.count - 1) {
                viewPager.currentItem = currentItem + 1
            } else {
                startActivity(Intent(this, StartActivity::class.java))
                finish()
            }
        }
        buttonSkip.setOnClickListener{
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                buttonNext.text = if (position == pagerAdapter.count - 1) "Get started" else "Next"
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }
}