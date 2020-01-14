@file:Suppress("unused")

package ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.mikepenz.fastadapter.ClickListener
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import observer.FastObserver
import observer.impl.ObserverListFragment
import ui.list.adapter.LoadNextPageAdapter
import ui.list.adapter.UnknownNextPage

abstract class ListAbstract<VM : ViewModel, ModelData, ListItem : IItem<*>>: ObserverListFragment<FastAdapter<ListItem>, ArrayList<ModelData>, ListItem, VM>() {
    private val consumeKey = System.nanoTime().toString()

    private var footerAdapter = LoadNextPageAdapter(this.consumeKey)
    private val fastAdapter = ItemAdapter<ListItem>()
    private val headerAdapter = ItemAdapter<IItem<*>>()

    protected open fun loadNext(page: Int) {}

    override fun update(o: FastObserver, arg: Any?) {
        if (arg is UnknownNextPage) loadNext(arg.page) else super.update(o, arg)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        changeFootedState(false)
        this.adapter?.onClickListener = clickListener()
        // тут это не работает
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        this.adapter?.withSavedInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        this.adapter?.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        this.fastAdapter.clear()
        this.footerAdapter.clear()
        super.onDestroyView()
    }

    @MainThread
    protected fun changeFootedState(show: Boolean) {
        if (show) this.footerAdapter.activate() else this.footerAdapter.deactivate()
    }

    @AnyThread
    override fun fillAdapter(data: ArrayList<ModelData>?) {
        // деактивация SwipeRefreshLayout
        this.swipeRefreshLayout?.post { this.swipeRefreshLayout?.isRefreshing = false }

        data?.also {
            createListItemFrom(data)?.mapTo(ArrayList()){ list_item -> list_item }?.also {
                this.list?.post {
                    val oldSize = this.fastAdapter.adapterItemCount
                    this.fastAdapter.add(it)
                    changeFootedState(this.fastAdapter.adapterItemCount > oldSize)
                }
            }
        }
    }

    override fun createAndGetAdapter(): FastAdapter<ListItem> =
        FastAdapter.with(listOf(this.headerAdapter, this.fastAdapter, this.footerAdapter))

    override fun createListItemFrom(item: ArrayList<ModelData>) : Collection<ListItem>? = null

    override fun consumerClass() = this.consumeKey

    @AnyThread
    protected fun setItemsToAdapter(items: List<ListItem>) {
        this.list?.post { this.fastAdapter.setNewList(items) }
    }

    @MainThread
    protected open fun clean() {
        this.fastAdapter.clear()
        this.footerAdapter.clear()
        Log.e(javaClass.simpleName, "clean")
    }

    @MainThread
    protected open fun reopen() {
        clean()
        loadNext(1)
        Log.e(javaClass.simpleName, "reopen")
    }

    protected open fun clickListener(): ClickListener<ListItem>? = null
}
