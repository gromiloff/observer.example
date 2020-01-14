package ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.MainThread
import data.BadgeData
import gromiloff.observer.example.R

class Badge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    companion object {
        val goldColorRef = R.color.gold
        val silverColorRef = R.color.silver
        val bronzeColorRef = R.color.bronze
    }

    private val rectGold = RectF()
    private val rectSilver = RectF()
    private val rectBronze = RectF()

    private var data : BadgeData? = null

    private val paint = Paint()

    @MainThread
    fun setValue(data : BadgeData){
        this.data = data
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        this.data?.also {
            var offset = 0.0f
            if(it.hasGold()){

            }
            if(it.hasSilver()){

            }
            if(it.hasBronze()){

            }
        }
    }
}