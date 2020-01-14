@file:Suppress("unused")

package api

import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import observer.ObserverImpl
import observer.event.LoadingStart
import observer.event.LoadingStop
import observer.event.ShowToast
import observer.event.TryException
import observer.impl.LiveModel
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class RequestModel(open val modelResponse: ModelResponse?) : ObserverImpl, Callback<ResponseBody> {
    init {
        reset()
    }
    
    private var call: Call<ResponseBody>? = null
    private var counts = 0

    abstract fun startRequest(retrofit:Retrofit) : Call<ResponseBody>

    open fun parseResponse(body: ResponseBody?) = false
    open fun parseResponse(element: JsonElement) : Any = Any()
    open fun parseError(element: JsonObject): Pair<Boolean, Any?>? = null

    open fun defaultCountTries() = 0

    override fun send() {
        try {
            val interceptor = UserAgentInterceptor()
            this.call = startRequest(Retrofit.Builder()
                .baseUrl("https://api.stackexchange.com")
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.MILLISECONDS)
                    .build())
                .build())
            Log.e(this::class.java.simpleName, "try call")
            this.call?.enqueue(this)
        } catch (e: Exception) {
            TryException(e).send()
            Handler(Looper.getMainLooper()).post { this.modelResponse?.error(this, e) }
        }
    }

    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        Log.e(this::class.java.simpleName, "onResponse $response")
        reset()

        if (!call.isCanceled) {
            if (!response.isSuccessful) {
                try {
                    val error = JsonParser().parse(response.errorBody()!!.string()).asJsonObject

                    val pairError = parseError(error)
                    if (this.modelResponse != null && pairError?.first == true)
                        Handler(Looper.getMainLooper()).post { this.modelResponse?.error(this, data = pairError.second) }
                    else tryAgain()
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { this.modelResponse?.error(this, e) }
                }
            } else {
                val body = response.body()
                if(parseResponse(body)) {
                    Handler(Looper.getMainLooper()).post { this.modelResponse?.success(this, true) }
                } else {
                    try {
                        val answer = parseResponse(JsonParser().parse(body!!.charStream()))
                        Handler(Looper.getMainLooper()).post { this.modelResponse?.success(this, answer) }
                    } catch (e: Exception) {
                        TryException(e).send()
                        Handler(Looper.getMainLooper()).post { this.modelResponse?.error(this, e) }
                    }
                }
            }
        }
    }

    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
        Log.e(this::class.java.simpleName, "onFailure $t")
        TryException(t).send()
        if (!call.isCanceled) {
            tryAgain()
            Handler(Looper.getMainLooper()).post { this.modelResponse?.error(this, t) }
        }
    }

    fun killMePls() {
        this.call?.also {
            Log.e(this::class.java.simpleName, "killMePls")
            it.cancel()
            this.call = null
        }
    }

    fun isCountEmpty() = this.counts == 0

    private fun reset() {
        this.counts = defaultCountTries()
    }

    private fun tryAgain() {
        if (--counts >= 0) {
            try {
                synchronized(this) { Thread.sleep(10 * DateUtils.SECOND_IN_MILLIS) }
                if (this.call == null) return
                send()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        } else {
            // ошибка
            Handler(Looper.getMainLooper()).post { this.modelResponse?.error(this) }
        }
        Log.e(this::class.java.simpleName, "tryAgain $counts")
    }
}

interface ModelResponse {
    @MainThread
    fun success(it: RequestModel?, response: Any?)

    @MainThread
    fun error(it: RequestModel?, t: Throwable? = null, data: Any? = null)
}

abstract class EmptyModelResponse : ViewModel(), LifecycleEventObserver, ModelResponse {
    private val cache = HashMap<String, RequestModel>()

    override fun error(it: RequestModel?, t: Throwable?, data: Any?) {
        ShowToast(
                when (t) {
                    is SocketTimeoutException -> "Истекло время ожидания ответа сервера. Проверьте подключение к интернету."
                    is UnknownHostException -> "Нет подключения к интернету. Проверьте подключение и перезапустите приложение."
                    null -> "Неизвестная ошибка"
                    else -> "Что-то пошло не так :("
                })
                .send()
    }

    @CallSuper
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Log.i(this::class.java.simpleName, "onStateChanged >> $source >> $event")
        if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
            this.cache.values.forEach { it.killMePls() }
        }
    }

    protected fun addRequestInstance(instance: RequestModel, forceKey: String? = null): String {
        val key =
            forceKey ?: (instance::class.java.canonicalName!! + "_${System.currentTimeMillis()}")
        this.cache[key] = instance
        return key
    }

    protected fun deleteRequestInstance(key: String?) {
        val a = this.cache.remove(key)
        Log.d("TEST", "$a")
    }

    protected fun deleteAllRequestInstance() {
        this.cache.clear()
        Log.d("TEST", "clear cache")
    }

    protected fun addAndSend(instance: RequestModel, forceKey: String? = null): String {
        val key = addRequestInstance(instance, forceKey)
        instance.send()
        return key
    }

    protected fun cacheSize() = this.cache.size

    @Suppress("UNCHECKED_CAST")
    protected fun <T : RequestModel> getRequestInstance(key: String?) = this.cache[key] as? T
}

abstract class SingleModelResponse<REQUEST : RequestModel> : EmptyModelResponse() {
    @Suppress("LeakingThis")
    private val requestKey: String = addRequestInstance(createRequest())

    abstract fun createRequest(): REQUEST

    protected fun get() = getRequestInstance<REQUEST>(this.requestKey)

    @CallSuper
    open fun reload() {
        LoadingStart().send()
        getRequestInstance<REQUEST>(this.requestKey)?.send()
    }
}

abstract class SingleLiveModelResponse<REQUEST : RequestModel, Answer> : SingleModelResponse<REQUEST>(), LiveModel<Answer> {
    private val liveData = MutableLiveData<Answer?>()

    override fun getLiveData() = this.liveData

    @Suppress("UNCHECKED_CAST")
    @MainThread
    override fun success(it: RequestModel?, response: Any?) {
        this.liveData.value = response as Answer
        LoadingStop().send()
    }
}
