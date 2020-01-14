package api

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.TimeUnit

class UserAgentInterceptor(private val logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT,
                           private val headeers : Array<Pair<String, String>>? = null) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val sb = StringBuilder()
        val builder = chain.request().newBuilder()
        this.headeers?.forEach { builder.header(it.first, it.second) }
        val request = builder.build()
        val requestBody = request.body
        val hasRequestBody = requestBody != null
        val gson = GsonBuilder().setPrettyPrinting().setLenient().create()
        val jp = JsonParser()

        sb.append("--> START HTTP ")
                .append(request.method)
                .append(' ')
                .append(request.url)
                .append("\n")

        if (hasRequestBody) {
            if (requestBody!!.contentType() != null) {
                sb.append("Content-Type: ").append(requestBody.contentType()).append("\n")
            }
            if (requestBody.contentLength() != -1L) {
                sb.append("Content-Length: ").append(requestBody.contentLength()).append("\n")
            }
        }

        var headers = request.headers
        run {
            var i = 0
            val count = headers.size
            while (i < count) {
                val name = headers.name(i)
                if (!"Content-Type".equals(name, ignoreCase = true) && !"Content-Length".equals(name, ignoreCase = true)) {
                    sb.append(name).append(": ").append(headers.value(i)).append("\n")
                }
                i++
            }
        }

        if (bodyEncoded(request.headers)) {
            sb.append("--> END ").append(request.method).append(" (encoded body omitted)").append("\n")
        } else if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            var charset : Charset? = UTF8
            val contentType = requestBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8)
            }

            if (isPlaintext(buffer) && charset != null) {
                try {
                    sb.append("Body:")
                            .append(gson.toJson(jp.parse(buffer.readString(charset))))
                            .append("\n")
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }

        val startNs = System.nanoTime()
        val response: okhttp3.Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            sb.append("<-- HTTP FAILED: ").append(e).append("\n")
            logger.log(sb.toString())
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body
        val contentLength = responseBody!!.contentLength()

        sb.append("<-- ")
                .append(response.code)
                .append(' ')
                .append(response.message)
                .append(' ')
                .append(response.request.url)
                .append(" (")
                .append(tookMs)
                .append("ms")
                .append(')')
                .append("\n")


        headers = response.headers
        var i = 0
        val count = headers.size
        while (i < count) {
            sb.append(headers.name(i)).append(": ").append(headers.value(i)).append("\n")
            i++
        }

        if (response.body == null) {
            sb.append("<-- END HTTP").append("\n")
        } else if (bodyEncoded(response.headers)) {
            sb.append("<-- END HTTP (encoded body omitted)").append("\n")
        } else {
            val source = responseBody.source()
            source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
            val buffer = source.buffer

            var charset : Charset? = UTF8
            val contentType = responseBody.contentType()
            if (contentType != null) {
                try {
                    charset = contentType.charset(UTF8)
                } catch (e: UnsupportedCharsetException) {
                    sb.append("Couldn't decode the response body; charset is likely malformed.").append("\n").append("<-- END HTTP").append("\n")
                    logger.log(sb.toString())
                    return response
                }

            }

            if (!isPlaintext(buffer)) {
                sb.append("<-- END HTTP (binary ").append(buffer.size).append("-byte body omitted)").append("\n")
                return response
            }

            if (contentLength != 0L && charset != null) {
                var a = buffer.clone().readString(charset)
                try {
                    a = gson.toJson(jp.parse(a))
                } catch (_: Exception) {}
                sb.append("Body:").append(a).append("\n")
            }

            sb.append("<-- END HTTP (").append(buffer.size).append("-byte body)").append("\n")
        }

        logger.log(sb.toString())
        return response
    }

    private fun bodyEncoded(headers: Headers): Boolean {
        val contentEncoding = headers.get("Content-Encoding")
        return contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)
    }

    companion object {
        private val UTF8 = Charset.forName("UTF-8")

        /**
         * Returns true if the body in question probably contains human readable text. Uses a small sample
         * of code points to detect unicode control characters commonly used in binary file signatures.
         */
        private fun isPlaintext(buffer: Buffer): Boolean {
            try {
                val prefix = Buffer()
                val byteCount = if (buffer.size < 64) buffer.size else 64
                buffer.copyTo(prefix, 0, byteCount)
                for (i in 0..15) {
                    if (prefix.exhausted()) {
                        break
                    }
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                return true
            } catch (e: EOFException) {
                return false // Truncated UTF-8 sequence.
            }

        }
    }
}