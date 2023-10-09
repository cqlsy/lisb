package com.cherish.third_login.facebook

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cherish.third_login.base.ILogin
import com.cherish.third_login.base.ThirdUser
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest


/**
 * 暂时不考虑获取更多的信息
 */
class FacebookLogin : ILogin {

    private val TAG = this.javaClass.name
    private val callbackManager by lazy {
        CallbackManager.Factory.create()
    }

    /**
     * 用于获取 facebook 的hash
     */
    fun getHashKey(context: Context): String? {
        try {
            val s = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                s
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash: String = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d(TAG, "Facebook hash: $keyHash")
                return keyHash
            }
        } catch (e: Exception) {
            return "获取异常"
        }
        return null
    }

    override fun login(activity: AppCompatActivity) {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {

                }

                override fun onError(error: FacebookException) {

                }

                override fun onSuccess(result: LoginResult) {

                }
            })
        // 执行登录操作
        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            callbackManager,
            listOf("public_profile", "email")
        )
    }


    /**
     * 获取facebook 的用户信息
     */
    fun getUserInfo(accessToken: AccessToken) {
        val meRequest = GraphRequest.newMeRequest(
            accessToken
        ) { obj, response ->
            if (response!!.error != null) {
                Log.d(TAG, "UserInfo : 获取用户信息失败 ${response.error?.errorMessage}")
            } else {
                if (obj == null) {
                    Log.d(TAG, "UserInfo : 获取用户信息失败 为获取到用户信息")
                    return@newMeRequest
                }
                // 获取到了用户信息，做接下来的处理
                /*
                {"id":"122108618942061915",
                "email":"3317443954@qq.com",
                "name":"龙浩",
                "picture":{"data":{"height":50,"is_silhouette":false,"url":"https:\/\/platform-lookaside.fbsbx.com\/platform\/profilepic\/?asid=122108618942061915&height=50&width=50&ext=1699440079&hash=AeSzCLH9KcIFZAhVggo","width":50}}}
                 */
               val user =  ThirdUser(parseJsonString(obj, "id"),
                    parseJsonString(obj, "name"),
                    parseJsonString(obj, "email"),"",
                    parseJsonString(parseJsonObject(parseJsonObject(obj, "picture"), "data"), "url"))

            }
        }
        val parameters = Bundle()
        parameters.putString("fields", "id,email,name,picture")
        meRequest.parameters = parameters
        meRequest.executeAsync()
    }

    private fun parseJsonString(obj: JSONObject?, key: String): String? {
        if (obj == null) {
            return ""
        }
        try {
            return obj.getString(key)
        } catch (ignore: JSONException) {
        }
        return ""
    }

    private fun parseJsonObject(obj: JSONObject?, key: String): JSONObject? {
        if (obj == null) {
            return null
        }
        try {
            return obj.getJSONObject(key)
        } catch (ignore: JSONException) {
        }
        return null
    }

}