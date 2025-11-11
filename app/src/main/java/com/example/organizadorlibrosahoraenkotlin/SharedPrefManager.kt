package com.example.organizadorlibrosahoraenkotlin

import android.content.Context
import android.content.SharedPreferences

data class User(
    val email: String,
    val username: String,
    val country: String,
    val description: String = "",
    val profileImageUri: String? = null
)

object SharedPrefManager {
    private const val PREFS_NAME = "MyBooksPrefs"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_USERNAME = "user_username"
    private const val KEY_COUNTRY = "user_country"
    private const val KEY_DESCRIPTION = "user_description"
    private const val KEY_PROFILE_IMAGE_URI = "user_profile_image_uri"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(context: Context, user: User) {
        prefs(context).edit().apply {
            putString(KEY_EMAIL, user.email)
            putString(KEY_USERNAME, user.username)
            putString(KEY_COUNTRY, user.country)
            putString(KEY_DESCRIPTION, user.description)
            putString(KEY_PROFILE_IMAGE_URI, user.profileImageUri)
            apply()
        }
    }

    fun getUser(context: Context): User? {
        val p = prefs(context)
        val email = p.getString(KEY_EMAIL, null) ?: return null
        val username = p.getString(KEY_USERNAME, "") ?: ""
        val country = p.getString(KEY_COUNTRY, "") ?: ""
        val description = p.getString(KEY_DESCRIPTION, "") ?: ""
        val imageUri = p.getString(KEY_PROFILE_IMAGE_URI, null)
        return User(email, username, country, description, imageUri)
    }

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
