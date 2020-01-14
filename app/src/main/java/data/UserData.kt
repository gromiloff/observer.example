package data

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

data class UserData(
    private val badges : BadgeData,
    private val account_id : Int,
    private val user_id : Int,
    private val reputation : Int,
    private val creation_date : Long,
    private val link : String,
    private val display_name : String,

    private var profile_image : String? = null,
    private var web : String? = null
) {
    constructor(jsonObject: JsonObject) : this(
        BadgeData(jsonObject.get("badge_counts").asJsonObject),
        jsonObject.get("account_id").asInt,
        jsonObject.get("user_id").asInt,
        jsonObject.get("reputation").asInt,
        jsonObject.get("creation_date").asLong,
        jsonObject.get("link").asString,
        jsonObject.get("display_name").asString
    ) {
        var tmp = jsonObject.get("website_url")
        if(tmp is JsonPrimitive) this.web = tmp.asString

        tmp = jsonObject.get("profile_image")
        if(tmp is JsonPrimitive) this.profile_image = tmp.asString
    }

    fun getUserImage() = this.profile_image
    fun getName() = this.display_name
    fun getUserId() = this.user_id
    fun getReputation() = this.reputation
    fun getBadges() = this.badges
}