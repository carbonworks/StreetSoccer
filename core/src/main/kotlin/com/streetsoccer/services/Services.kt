package com.streetsoccer.services

import com.badlogic.gdx.Gdx
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class ProfileData(var experience: Int = 0)

@Serializable
data class SettingsData(var volume: Float = 1f)

class SaveService {
    private val json = Json { ignoreUnknownKeys = true }
    private val profileFile = Gdx.files.local("profile.json")
    private val settingsFile = Gdx.files.local("settings.json")
    
    fun saveProfile(data: ProfileData) {
        profileFile.writeString(json.encodeToString(data), false)
    }

    fun loadProfile(): ProfileData {
        return if (profileFile.exists()) {
            json.decodeFromString<ProfileData>(profileFile.readString())
        } else {
            ProfileData()
        }
    }
}

interface AudioService {
    fun playGlassBreak()
    fun playMetallicClang()
    fun playCarAlarm()
    // etc...
}

class NoopAudioService : AudioService {
    override fun playGlassBreak() {}
    override fun playMetallicClang() {}
    override fun playCarAlarm() {}
}
