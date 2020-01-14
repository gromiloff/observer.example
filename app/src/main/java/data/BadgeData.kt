@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package data

import com.google.gson.JsonObject

data class BadgeData(
    private val bronze : Int,
    private val silver : Int,
    private val gold : Int
) {
    constructor(jsonObject: JsonObject) : this(
        jsonObject.get("bronze").asInt,
        jsonObject.get("silver").asInt,
        jsonObject.get("gold").asInt
    )

    fun hasGold() = valueGold() > 0
    fun hasSilver() = valueSilver() > 0
    fun hasBronze() = valueBronze() > 0

    fun valueGold() = this.gold
    fun valueSilver() = this.silver
    fun valueBronze() = this.bronze
}
