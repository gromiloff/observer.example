package ui.fragment.impl

import android.os.Bundle
import android.view.View
import api.SingleLiveModelResponse
import api.impl.GetUsers
import com.mikepenz.fastadapter.IAdapter
import data.UserData
import observer.event.ShowToast
import ui.fragment.ListAbstract
import ui.list.d.DividerItemDecoration
import ui.list.item.ListItemUser

class ListUsers : ListAbstract<ListUsers.ListUsersModel, UserData, ListItemUser>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.list?.addItemDecoration(DividerItemDecoration(view.context))
    }

    override fun onResume() {
        super.onResume()

        if(this.adapter?.itemCount == 0) loadNext(1)
    }

    override fun clickListener() = { _: View?, _: IAdapter<ListItemUser>, item: ListItemUser, _: Int ->
        ShowToast("Clicked on ${item.model.getUserId()}").send()
        true
    }

    override fun createListItemFrom(item: ArrayList<UserData>) = item.mapTo(ArrayList()) { ListItemUser(it) }

    override fun getModelClass() = ListUsersModel::class.java

    override fun swipeRefreshSupported() = true

    override fun onRefresh() {
        super.onRefresh()
        reopen()
    }

    override fun loadNext(page: Int) {
        getModel().load(page)
    }

    class ListUsersModel : SingleLiveModelResponse<GetUsers, Collection<UserData>>() {
        override fun createRequest() = GetUsers(this)

        fun load(page : Int) {
            get()?.also {
                it.withPage(page)
                reload()
            }
        }
    }
}
