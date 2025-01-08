package com.example.ftp.ui.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,   // 列数
    private val spacing: Int,     // 间距（像素）
    private val includeEdge: Boolean // 是否包含边缘
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view) // 当前项的位置
        val column = position % spanCount                  // 当前项的列索引

        if (includeEdge) {
            // 设置左边距
            outRect.left = spacing - column * spacing / spanCount
            // 设置右边距
            outRect.right = (column + 1) * spacing / spanCount

            // 设置顶部边距
            if (position < spanCount) {
                outRect.top = spacing
            }
            // 设置底部边距
            outRect.bottom = spacing
        } else {
            // 不包含边缘的间距
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}
