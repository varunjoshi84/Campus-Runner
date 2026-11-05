package com.example.campusrunner.data

import android.content.Context
import android.content.SharedPreferences

open class TokenStore(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("campus_runner_prefs", Context.MODE_PRIVATE)

    private var memToken: String? = null
    private var memUserId: String? = null
    private var memUserName: String? = null
    private var memUserEmail: String? = null
    private var memUserPhone: String? = null

    open var token: String?
        get() = prefs?.getString("jwt_token", null) ?: memToken
        set(value) {
            prefs?.edit()?.putString("jwt_token", value)?.apply()
            memToken = value
        }

    open var userId: String?
        get() = prefs?.getString("user_id", null) ?: memUserId
        set(value) {
            prefs?.edit()?.putString("user_id", value)?.apply()
            memUserId = value
        }

    open var userName: String?
        get() = prefs?.getString("user_name", null) ?: memUserName
        set(value) {
            prefs?.edit()?.putString("user_name", value)?.apply()
            memUserName = value
        }

    open var userEmail: String?
        get() = prefs?.getString("user_email", null) ?: memUserEmail
        set(value) {
            prefs?.edit()?.putString("user_email", value)?.apply()
            memUserEmail = value
        }

    open var userPhone: String?
        get() = prefs?.getString("user_phone", null) ?: memUserPhone
        set(value) {
            prefs?.edit()?.putString("user_phone", value)?.apply()
            memUserPhone = value
        }

    open fun clear() {
        prefs?.edit()?.clear()?.apply()
        memToken = null
        memUserId = null
        memUserName = null
        memUserEmail = null
        memUserPhone = null
    }
}
