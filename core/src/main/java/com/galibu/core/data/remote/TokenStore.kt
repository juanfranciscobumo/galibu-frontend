package com.galibu.core.data.remote

import android.content.Context

interface TokenStorage {
    fun save(accessToken: String, refreshToken: String)
    fun accessToken(): String?
    fun refreshToken(): String?
    fun clear()
}

class SharedPrefsTokenStorage(context: Context) : TokenStorage {
    private val prefs = context.applicationContext
        .getSharedPreferences("galibu_auth", Context.MODE_PRIVATE)

    override fun save(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    override fun accessToken(): String? = prefs.getString("access_token", null)

    override fun refreshToken(): String? = prefs.getString("refresh_token", null)

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

class InMemoryTokenStorage : TokenStorage {
    private var access: String? = null
    private var refresh: String? = null

    override fun save(accessToken: String, refreshToken: String) {
        access = accessToken
        refresh = refreshToken
    }

    override fun accessToken(): String? = access

    override fun refreshToken(): String? = refresh

    override fun clear() {
        access = null
        refresh = null
    }
}

class TokenStore(private val storage: TokenStorage) {
    fun saveTokens(accessToken: String, refreshToken: String) =
        storage.save(accessToken, refreshToken)

    fun accessToken(): String? = storage.accessToken()

    fun refreshToken(): String? = storage.refreshToken()

    fun hasTokens(): Boolean = storage.accessToken() != null

    fun clear() = storage.clear()
}
