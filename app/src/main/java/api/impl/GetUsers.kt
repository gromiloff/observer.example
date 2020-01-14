package api.impl

import api.ModelResponse
import api.RequestModel
import com.google.gson.JsonElement
import data.UserData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

/*
[link] https://api.stackexchange.com/docs
*/
class GetUsers(modelResponse: ModelResponse?, private var page : Int = 1) : RequestModel(modelResponse) {
    fun withPage(page : Int) : GetUsers {
        this.page = page
        return this
    }

    internal interface Api {
        @GET("2.2/users")
        fun request(@Query("page") page : Int,
                    @Query("order") order : String = "desc",
                    @Query("sort") sort : String = "reputation",
                    @Query("site") site : String = "stackoverflow"): Call<ResponseBody>
    }

    override fun startRequest(retrofit: Retrofit) = retrofit.create(Api::class.java).request(this.page)
    override fun parseResponse(element: JsonElement) = element.asJsonObject.get("items").asJsonArray.mapTo(ArrayList(31)){ UserData(it.asJsonObject) }
}
