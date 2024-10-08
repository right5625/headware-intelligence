package com.headmetal.headwareintelligence

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// 프리뷰
@Preview(showBackground = true)
@Composable
fun MenuPreview() {
    Menu(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun MenuUserPrivacyButtonPreview() {
    UserCard(type = "manager", userName = "관리자", navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun MenuFunctionsPreview() {
    MenuFunctions(type = "manager", navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun MenuLogoutAlertDialogPreview() {
    LogoutAlertDialog(
        showLogoutDialog = remember { mutableStateOf(false) },
        dismissButton = {},
        navController = rememberNavController()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun notificationSettingOreo(context: Context): Intent {
    return Intent().also { intent ->
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

fun notificationSettingOreoLess(context: Context): Intent {
    return Intent().also { intent ->
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("app_package", context.packageName)
        intent.putExtra("app_uid", context.applicationInfo?.uid)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

@Composable
fun Menu(navController: NavController) {
    val sharedAccount: SharedPreferences =
        LocalContext.current.getSharedPreferences("Account", Activity.MODE_PRIVATE)
    val type: String = sharedAccount.getString("type", "") ?: ""
    val userName: String = sharedAccount.getString("name", "") ?: ""

    IconScreen(
        imageVector = Icons.Default.ArrowBackIosNew,
        onClick = { navController.navigateUp() },
        content = {
            ScreenTitleText(text = "메뉴")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                UserCard(type = type, userName = userName, navController = navController)
                MenuFunctions(type = type, navController = navController)
            }
        }
    )
}

@Composable
fun UserCard(
    type: String,
    userName: String,
    navController: NavController
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = if (type == "manager") "관리자" else "근로자",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = userName,
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                }
            }
        },
        colors = ButtonDefaults.buttonColors(Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
        onClick = { navController.navigate("PrivacyScreen") },
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun MenuFunctions(
    type: String,
    navController: NavController
) {
    val showLogoutDialog: MutableState<Boolean> = remember { mutableStateOf(false) }

    if (showLogoutDialog.value) {
        LogoutAlertDialog(
            showLogoutDialog = showLogoutDialog,
            dismissButton = { showLogoutDialog.value = false },
            navController = navController
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconWithLabelButton(
            leadIcon = Icons.Outlined.Description,
            text = "참여 건설 업체",
            onClick = { navController.navigate("CompanyInfoScreen") }
        )
        if (type == "manager") {
            IconWithLabelButton(
                leadIcon = Icons.Outlined.Notifications,
                text = "알림 설정",
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        notificationSettingOreo(navController.context)
                    } else {
                        notificationSettingOreoLess(navController.context)
                    }
                    try {
                        navController.context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
            )
        }

        IconWithLabelButton(
            leadIcon = Icons.Outlined.Info,
            text = "기타",
            onClick = { navController.navigate("EtcScreen") }
        )
        IconWithLabelButton(
            leadIcon = Icons.AutoMirrored.Outlined.Logout,
            color = Color(0xFFFF6600),
            text = "로그아웃",
            onClick = { showLogoutDialog.value = true }
        )
    }
}

@Composable
fun LogoutAlertDialog(
    showLogoutDialog: MutableState<Boolean>,
    dismissButton: () -> Unit,
    navController: NavController
) {
    val sharedAccount: SharedPreferences =
        LocalContext.current.getSharedPreferences("Account", Activity.MODE_PRIVATE)
    val sharedConfigure: SharedPreferences =
        LocalContext.current.getSharedPreferences("Configure", Activity.MODE_PRIVATE)
    val sharedAlert: SharedPreferences =
        LocalContext.current.getSharedPreferences("Alert", Activity.MODE_PRIVATE)

    YesNoAlertDialog(
        title = "로그아웃",
        textComposable = { Text(text = "로그아웃 하시겠습니까?") },
        confirmButton = {
            logoutConfirm(
                sharedAccount,
                sharedConfigure,
                sharedAlert,
                navController,
                showLogoutDialog
            )
        },
        dismissButton = dismissButton,
        yesButton = "예",
        noButton = "아니오"
    )
}

fun logoutConfirm(
    sharedAccount: SharedPreferences,
    sharedConfigure: SharedPreferences,
    sharedAlert: SharedPreferences,
    navController: NavController,
    showLogoutDialog: MutableState<Boolean>
) {
    val type: String = sharedAccount.getString("type", "") ?: ""
    if (type == "manager") {
        logoutPOST(
            navController = navController,
            sharedAccount = sharedAccount,
            sharedConfigure = sharedConfigure,
            sharedAlert = sharedAlert,
            showLogoutDialog = showLogoutDialog
        )
    } else {
        showLogoutDialog.value = false
        Toast.makeText(
            navController.context,
            "로그아웃을 성공하였습니다.",
            Toast.LENGTH_SHORT
        ).show()
        sharedAccount.edit().clear().apply()
        sharedConfigure.edit().clear().apply()
        navController.navigate("LoginScreen")
    }
}
