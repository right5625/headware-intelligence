package com.headmetal.headwareintelligence

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
fun Loading(navController: NavController) {
    var autoLogin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sharedAccount: SharedPreferences = context.getSharedPreferences("Account", MODE_PRIVATE)
    val sharedAlert: SharedPreferences = context.getSharedPreferences("Alert", MODE_PRIVATE)
    val sharedAccountEdit: SharedPreferences.Editor = sharedAccount.edit()
    val userId = sharedAccount.getString("userid", null)
    val userPassword = sharedAccount.getString("password", null)
    val accessToken = sharedAccount.getString("token", null)
    val type = sharedAccount.getString("type", null).toString()
    val builder = AlertDialog.Builder(navController.context)

    if (userId != null && accessToken != null) {
        autoLogin = true
    }

    // 권한 요청
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH)
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
    } else {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    val permissionsToRequest = mutableListOf<String>()
    permissions.forEach { permission ->
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(permission)
        }
    }

    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            context as Activity, permissionsToRequest.toTypedArray(),
            MainActivity.REQUEST_PERMISSIONS_CODE
        )
        Log.d("HEAD METAL", "권한을 요청하였습니다.")
    } else {
        Log.d("HEAD METAL", "권한이 이미 존재합니다.")
    }

    // 서버 상태 확인
    LaunchedEffect(Unit) {
        RetrofitInstance.apiService.apiGetStatus().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    if (autoLogin) {
                        RetrofitInstance.apiService.apiLogin(
                            alertToken = sharedAlert.getString("alert_token", null).toString(),
                            type = type,
                            id = userId,
                            pw = userPassword
                        ).enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(
                                call: Call<LoginResponse>,
                                response: Response<LoginResponse>
                            ) {
                                if (response.isSuccessful) {
                                    if (navController.currentDestination?.route != "mainScreen") {
                                        Toast.makeText(
                                            navController.context,
                                            response.body()?.name + "님 반갑습니다",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.navigate("mainScreen") {
                                            popUpTo("loadingScreen") { inclusive = true }
                                        }
                                    }
                                } else {
                                    builder.setTitle("자동 로그인 실패")
                                    builder.setMessage("변경된 비밀번호를 확인하세요.")
                                    builder.setPositiveButton("확인") { dialog, _ ->
                                        dialog.dismiss()
                                        navController.navigate("loginScreen")
                                        sharedAccountEdit.clear()
                                        sharedAccountEdit.apply()
                                    }
                                    val dialog = builder.create()
                                    dialog.show()
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                builder.setTitle("로그인 실패")
                                builder.setMessage("서버 상태 및 네트워크 접속 불안정")
                                builder.setPositiveButton("확인") { _, _ ->
                                    (navController.context as Activity).finish()
                                }
                                val dialog = builder.create()
                                dialog.show()
                            }
                        })
                    } else {
                        navController.navigate("loginScreen")
                    }
                } else {
                    builder.setTitle("서버 접속 실패")
                    builder.setMessage("서버 상태 및 네트워크 접속 불안정")
                    builder.setPositiveButton("확인") { _, _ ->
                        (navController.context as Activity).finish()
                    }
                    val dialog = builder.create()
                    dialog.show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                builder.setTitle("서버 접속 실패")
                builder.setMessage("서버 상태 및 네트워크 접속 불안정")
                builder.setPositiveButton("확인") { _, _ ->
                    (navController.context as Activity).finish()
                }
                val dialog = builder.create()
                dialog.show()
            }
        })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9C94C)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.helmet),
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.app_name),
                fontWeight = FontWeight.Bold
            )
            LoadingScreen()
        }
    }
}
