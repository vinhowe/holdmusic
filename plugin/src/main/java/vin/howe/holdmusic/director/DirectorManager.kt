package vin.howe.holdmusic.director

import com.google.gson.*
import com.velocitypowered.api.proxy.Player
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

class DirectorManager {
    private val pokeEndpointUri = URI.create(MANAGER_ENDPOINT_POKE)
    private val usageEndpointUri = URI.create(MANAGER_ENDPOINT_USAGE)
    private val gson = Gson()
    private val client = HttpClient.newHttpClient()

    companion object {
        private const val MANAGER_URL = "http://127.0.0.1:5000"
        private const val MANAGER_ENDPOINT_POKE = "$MANAGER_URL/poke"
        private const val MANAGER_ENDPOINT_ELIGIBLE = "$MANAGER_URL/eligible"
        private const val MANAGER_ENDPOINT_USAGE = "$MANAGER_URL/usage"
        private const val TIMEOUT_SECONDS = 10L
    }

    fun poke(): CompletableFuture<PokeResponse> {
        val request: HttpRequest = HttpRequest.newBuilder()
                .uri(pokeEndpointUri)
                .GET()
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { it.body() }
                .thenApply { gson.fromJson(it, PokeResponse::class.java) }
    }

    fun checkEligible(uuid: String): CompletableFuture<CheckEligibleResponse> {
        val eligibleEndpointUri = URI.create("$MANAGER_ENDPOINT_ELIGIBLE/$uuid")
        val request: HttpRequest = HttpRequest.newBuilder()
                .uri(eligibleEndpointUri)
                .GET()
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { it.body() }
                .thenApply { gson.fromJson(it, CheckEligibleResponse::class.java) }
    }

    fun reportUsage(uuids: Collection<UUID>): CompletableFuture<StatusResponse> {
        val stringUuids = uuids.map { it.toString() }
        val uuidsJson = JsonArray()

        stringUuids.forEach { uuidsJson.add(it) }

        val json = JsonObject()
        json.add("uuids", uuidsJson)

        val jsonString = gson.toJson(json)

        val request: HttpRequest = HttpRequest.newBuilder()
                .uri(usageEndpointUri)
                .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { it.body() }
                .thenApply { gson.fromJson(it, StatusResponse::class.java) }
    }
}
