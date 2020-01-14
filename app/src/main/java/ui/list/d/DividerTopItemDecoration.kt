package ui.list.d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import gromiloff.observer.example.R

open class DividerTopItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerH: Int = context.resources.getDimensionPixelSize(R.dimen.divider_size)

    init {
        p.color = ContextCompat.getColor(context, R.color.divider)
        p.style = Paint.Style.FILL_AND_STROKE
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        c.drawRect(Rect(
                parent.paddingLeft,
                0,
                parent.width - parent.paddingLeft - parent.paddingRight,
                dividerH),
                p)
    }
}
