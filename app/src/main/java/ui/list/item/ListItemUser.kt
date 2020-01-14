package ui.list.item

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.ModelAbstractItem
import data.UserData
import gromiloff.observer.example.R
import ui.view.Badge
import utils.transformation.RoundedCornersTransformation

class ListItemUser(
    model : UserData,
    override val layoutRes: Int = R.layout.list_item_user,
    override val type: Int = R.id.list_item_user
) : ModelAbstractItem<UserData, ViewHolderUser>(model) {
    override fun getViewHolder(v: View) = ViewHolderUser(v)
}

class ViewHolderUser(view: View) : FastAdapter.ViewHolder<ListItemUser>(view) {
    private val icon = view.findViewById<ImageView>(android.R.id.icon)
    private val name = view.findViewById<TextView>(android.R.id.text1)
    private val reputation = view.findViewById<TextView>(android.R.id.text2)
    private val badge = view.findViewById<Badge>(R.id.badges)

    @SuppressLint("SetTextI18n")
    override fun bindView(item: ListItemUser, payloads: MutableList<Any>) {
        Glide.with(this.icon)
            .load(item.model.getUserImage())
            .apply(RequestOptions().transform(RoundedCornersTransformation(itemView.context.resources.getDimensionPixelSize(R.dimen.corner), RoundedCornersTransformation.CornerType.ALL)))
          // .error(this.placeHolder)
            .into(this.icon)

        this.name.text = item.model.getName()
        this.reputation.text = "Reputation: ${item.model.getReputation()}"
        this.badge.setValue(item.model.getBadges())
    }

    override fun unbindView(item: ListItemUser) {
        Glide.with(this.icon).clear(this.icon)
    }
}