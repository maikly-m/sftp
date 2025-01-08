//package com.example.ftp.ui.sftp
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.viewpager2.adapter.FragmentStateAdapter
//import androidx.viewpager2.widget.ViewPager2
//import com.example.ftp.R
//import com.example.ftp.databinding.FragmentClientBrowserBinding
//import com.example.ftp.ui.local.LocalFileFragment
//import com.example.ftp.ui.file.LocalLastFragment
//
//class ClientBrowserFragmentTest : Fragment() {
//
//    private var _binding: FragmentClientBrowserBinding? = null
//
//    // This property is only valid between onCreateView and
//    // onDestroyView.
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val viewModel =
//            ViewModelProvider(this).get(ClientBrowserViewModel::class.java)
//
//        _binding = FragmentClientBrowserBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        initView()
//        return root
//    }
//
//    private fun initView() {
//        // 设置适配器
//        val adapter = FragmentAdapter(this)
//        binding.viewPager.adapter = adapter
//        binding.viewPager.offscreenPageLimit = 1
//
//        // BottomNavigationView 联动 ViewPager2
//        binding.bottomNavigationView.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.navigation_local_last -> binding.viewPager.currentItem = 0
//                R.id.navigation_local_file -> binding.viewPager.currentItem = 1
//            }
//            true
//        }
//
//        // ViewPager2 联动 BottomNavigationView
//        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                when (position) {
//                    0 -> binding.bottomNavigationView.selectedItemId = R.id.navigation_local_last
//                    1 -> binding.bottomNavigationView.selectedItemId = R.id.navigation_local_file
//                }
//            }
//        })
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    inner class FragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
//
//        private val fragmentList = listOf(
//            LocalFileFragment(),
//            LocalLastFragment()
//        )
//
//        override fun getItemCount(): Int = fragmentList.size
//
//        override fun createFragment(position: Int): Fragment = fragmentList[position]
//    }
//}