package ir.jomyekta.jyforward.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class WebhookResult(
    val isSuccess: Boolean,
    val httpStatusCode: Int,
    val resolvedUrl: String,
    val responseBodySummary: String
)

/**
 * Handles dispatching SMS payloads to target Webhook URLs via OkHttp.
 *
 * Supports URL template replacement:
 * - {msg}    -> URL-encoded SMS message body (UTF-8)
 * - {sender} -> URL-encoded sender phone number / short-code
 * - {sim}    -> SIM slot label (SIM1, SIM2, or UNKNOWN)
 * - {time}   -> Unix epoch milliseconds
 *
 * Example URL:
 * https://www.jomyekta.ir/index.php?option=com_osmembership&task=payment_confirm&payment_method=os_cardtocard&cardtocard_action=webhook&token=%24143356%40Aliapalipz&msg={msg}
 */
class WebhookDispatcher {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun dispatchSmsWebhook(
        templateUrl: String,
        httpMethod: String,
        sender: String,
        messageBody: String,
        simSlotIndex: Int,
        subscriptionId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): WebhookResult = withContext(Dispatchers.IO) {
        val encodedMsg = urlEncodeUtf8(messageBody)
        val encodedSender = urlEncodeUtf8(sender)
        val simLabel = when (simSlotIndex) {
            0 -> "SIM1"
            1 -> "SIM2"
            else -> "SIM_SUB_$subscriptionId"
        }

        // Replace {msg}, {sender}, {sim}, {time} placeholders in the destination URL
        val resolvedUrl = templateUrl.trim()
            .replace("{msg}", encodedMsg, ignoreCase = true)
            .replace("{sender}", encodedSender, ignoreCase = true)
            .replace("{sim}", urlEncodeUtf8(simLabel), ignoreCase = true)
            .replace("{time}", timestamp.toString(), ignoreCase = true)

        try {
            val requestBuilder = Request.Builder()
                .url(resolvedUrl)
                .header("User-Agent", "JYForward-Android/1.0 (JomYekta Webhook Client)")
                .header("Accept", "application/json, text/plain, */*")

            val methodUpper = httpMethod.uppercase().trim()
            if (methodUpper == "POST") {
                // Provide both form-encoded body and query parameter replacement compatibility
                val formBody = FormBody.Builder(StandardCharsets.UTF_8)
                    .add("msg", messageBody)
                    .add("sender", sender)
                    .add("sim", simLabel)
                    .add("subscription_id", subscriptionId.toString())
                    .add("timestamp", timestamp.toString())
                    .build()
                requestBuilder.post(formBody)
            } else {
                requestBuilder.get()
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty().trim()
                val summary = if (rawBody.length > 180) {
                    rawBody.take(180) + "..."
                } else if (rawBody.isEmpty()) {
                    response.message.ifEmpty { "HTTP ${response.code}" }
                } else {
                    rawBody
                }

                WebhookResult(
                    isSuccess = response.isSuccessful,
                    httpStatusCode = response.code,
                    resolvedUrl = resolvedUrl,
                    responseBodySummary = summary
                )
            }
        } catch (e: Exception) {
            WebhookResult(
                isSuccess = false,
                httpStatusCode = 0,
                resolvedUrl = resolvedUrl,
                responseBodySummary = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Network Error"}"
            )
        }
    }

    companion object {
        fun urlEncodeUtf8(value: String): String {
            return try {
                URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            } catch (e: Exception) {
                URLEncoder.encode(value, "UTF-8")
            }
        }
    }
}
