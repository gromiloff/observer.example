@file:Suppress("unused")

package ui.list.adapter

import android.view.View
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import gromiloff.observer.example.R
import observer.ProtectObserverImpl

class LoadNextPageAdapter(private val key: String) : ItemAdapter<LoadingListItem>() {

    val currentPage: Int
        @MainThread
        get() = if (adapterItemCount == 0) 0 else getAdapterItem(0).currentPage
    
    @MainThread
    fun deactivate() {
        clear()
    }

    @MainThread
    fun activate() {
        if (adapterItemCount == 0) {
            add(LoadingListItem(this.key))
        }
    }

}

/*todo: добавить решение для ситцации когда пользователь показал лоадер, потом метнулся в верх,
* а потом снова показал лоадер пока еще не получил данных.
* надо ориентироваться на количество айтемов в адаптере
* */
class LoadingListItem (
    private val key: String,
    var currentPage: Int = 1,
    override val layoutRes: Int = R.layout.list_item_loading,
    override val type: Int = R.id.footer_loading
) : AbstractItem<LoadingViewHolder>() {
    override fun getViewHolder(v: View) = LoadingViewHolder(v)
    override fun bindView(holder: LoadingViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        UnknownNextPage(this.key, ++this.currentPage).send()
    }
}

class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

class UnknownNextPage(key: String, val page: Int) : ProtectObserverImpl(key)