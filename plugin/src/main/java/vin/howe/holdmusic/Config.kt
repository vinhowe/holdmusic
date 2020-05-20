package vin.howe.holdmusic

import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.slf4j.Logger
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class Config {
    var queue = "holding"

    companion object {
        /**
         * Load a config file or create one if it doesn't exist
         *
         * @param path path of config file
         */
        fun loadConfig(path: String?, logger: Logger): Config {
            val gson = Gson()
            try {
                val fileReader = FileReader(path)
                return gson.fromJson(fileReader, Config::class.java)
            } catch (e: JsonParseException) {
                e.printStackTrace()
            } catch (e: IOException) {
                logger.info("HoldMusic config not found, generating")
                generateConfig(path)
            }
            return Config()
        }

        /**
         * Generate a new config file with defaults
         *
         * @param path path of config file
         */
        private fun generateConfig(path: String?) {
            val config = File(path)
            try {
                config.createNewFile()
                val fileWriter = FileWriter(config)
                fileWriter.write("""
                {
                "queue": "holding",
                }
                """.trimIndent())
                fileWriter.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }
}