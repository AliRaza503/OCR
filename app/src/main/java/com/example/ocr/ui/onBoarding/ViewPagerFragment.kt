package com.example.ocr.ui.onBoarding


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.ocr.MainActivity
import com.example.ocr.R
import com.example.ocr.ui.onBoarding.Screens.*
import com.example.ocr.databinding.FragmentViewPagerBinding
import com.example.todo.onBoarding.ViewPagerAdapter


class ViewPagerFragment : Fragment() {
    private lateinit var binding: FragmentViewPagerBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentViewPagerBinding.inflate(inflater, container, false)
        val view = binding.root
        val fragmentList = arrayListOf(
            WelcomeFragment(),
            FirstViewFragment(),
            SecondViewFragment(),
            ThirdViewFragment(),
            FourthViewFragment(),
            FifthViewFragment()
        )
        Log.d("Status", "OnCreateView ViewPagerFragment")
        val adapter = ViewPagerAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )
        binding.viewPager.adapter = adapter

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                if (position == 1) {
                    binding.bottomNavView.visibility = View.VISIBLE
                    binding.nextBtn.visibility = View.VISIBLE
                }

            }
        })
        val continueBtn = view.findViewById<Button>(R.id.next_btn)
        val skipAllBtn = view.findViewById<Button>(R.id.skipAllBtn)
        val viewPager = activity?.findViewById<ViewPager2>(R.id.view_pager)
        continueBtn?.setOnClickListener {
            viewPager?.currentItem = 1
        }
        skipAllBtn?.setOnClickListener {
            onBoardingFinished()
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
        binding.nextBtn.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0 -> {
                    skipAllBtn.visibility = View.VISIBLE
                    continueBtn.text = "Next"
                    binding.bottomNavView.visibility = View.VISIBLE
                }
                //Scrolled when current item is 1
                1 -> {
                    binding.indicator1.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator2.setImageResource(R.drawable.ic_indicator_selected)
                    binding.indicator3.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator4.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator5.setImageResource(R.drawable.ic_indicator_default)
                }
                2 -> {
                    binding.indicator1.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator2.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator3.setImageResource(R.drawable.ic_indicator_selected)
                    binding.indicator4.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator5.setImageResource(R.drawable.ic_indicator_default)
                }
                3 -> {
                    binding.indicator1.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator2.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator3.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator4.setImageResource(R.drawable.ic_indicator_selected)
                    binding.indicator5.setImageResource(R.drawable.ic_indicator_default)

                }
                4 -> {
                    binding.indicator1.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator2.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator3.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator4.setImageResource(R.drawable.ic_indicator_default)
                    binding.indicator5.setImageResource(R.drawable.ic_indicator_selected)
                }
                5 -> {
                    onBoardingFinished()
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            binding.viewPager.currentItem = binding.viewPager.currentItem.plus(1)
        }
    }

    private fun onBoardingFinished() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("Finished", true)
        editor.apply()
    }
}
