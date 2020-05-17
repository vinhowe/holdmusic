package vin.howe.holdmusic

import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class DirectorManager {
    private val callEndpointUrl = URI.create(MANAGER_ENDPOINT_CALL)
    private val gson = Gson()
    private val client = HttpClient.newHttpClient()

    companion object {
        private const val MANAGER_URL = "http://127.0.0.1:5000"
        private const val MANAGER_ENDPOINT_CALL = "$MANAGER_URL/call"
    }

    fun call(): CompletableFuture<DirectorResponse> {
        val request: HttpRequest = HttpRequest.newBuilder()
                .uri(callEndpointUrl)
                .GET()
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { it.body() }
                .thenApply { gson.fromJson(it, DirectorResponse::class.java) }
    }
}