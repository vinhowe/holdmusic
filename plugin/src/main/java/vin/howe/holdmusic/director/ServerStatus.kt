package vin.howe.holdmusic.director

enum class ServerStatus(val status: String) {
    OFFLINE("offline"),
    LOADING("loading"),
    ONLINE("online"),
}