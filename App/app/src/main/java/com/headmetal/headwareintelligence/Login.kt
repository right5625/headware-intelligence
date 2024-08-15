package com.headmetal.headwareintelligence

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import retrofit2.Call
import retrofit2.Callback

data class LoginResponse(
    val id: String,
    val name: String,
    val phoneNo: String,
    val email: String,
    val accessToken: String,
    val tokenType: String
)

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    Login()
}

@Preview(showBackground = true)
@Composable
fun LoginTextFieldComposablePreview() {
    LabelAndInputComposable(
        labelText = "ID",
        inputText = remember { mutableStateOf("") }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginFunctionButtonComposablePreview() {
    LoginFunctionButtonComposable(
        { LoginFunctionButton(buttonText = "로그인") },
        { LoginFunctionButton(buttonText = "회원가입") },
        { LoginFunctionButton(buttonText = "계정 찾기") }
    )
}

@Composable
fun Login(navController: NavController = rememberNavController()) {
    val id: MutableState<String> = remember { mutableStateOf("") }
    val pw: MutableState<String> = remember { mutableStateOf("") }
    val isEmployee: MutableState<Boolean> = remember { mutableStateOf(true) }
    val isManager: MutableState<Boolean> = remember { mutableStateOf(false) }

    BackOnPressed()
    LoginScreen {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HelmetImage()
            LabelAndInputComposable(labelText = "ID", inputText = id)
            LabelAndInputComposable(
                labelText = "PW",
                inputText = pw,
                visualTransformation = PasswordVisualTransformation()
            )
            LabelAndRadioButtonComposable(
                labelText = "Part",
                firstButtonSwitch = isEmployee,
                secondButtonSwitch = isManager
            )
            LoginFunctionButtonComposable(
                loginFunctionButtons = arrayOf(
                    {
                        LoginFunctionButton(
                            modifier = Modifier.weight(1f),
                            buttonText = "로그인"
                        ) {
                            loginButton(
                                inputId = id.value,
                                inputPw = pw,
                                isManager = isManager.value,
                                navController = navController
                            )
                        }
                    },
                    {
                        LoginFunctionButton(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            buttonText = "회원가입"
                        ) { navController.navigate("SignUpScreen") }
                    },
                    {
                        LoginFunctionButton(
                            modifier = Modifier.weight(1f),
                            buttonText = "계정 찾기"
                        ) { navController.navigate("FindIdScreen") }
                    }
                )
            )
            Text(
                text = stringResource(id = R.string.app_name),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun loginButton(
    inputId: String,
    inputPw: MutableState<String>,
    isManager: Boolean,
    navController: NavController
) {
    val sharedAlert: SharedPreferences =
        navController.context.getSharedPreferences(
            "Alert",
            Activity.MODE_PRIVATE
        )
    val sharedAccount: SharedPreferences =
        navController.context.getSharedPreferences(
            "Account",
            Activity.MODE_PRIVATE
        )
    val sharedAccountEdit: SharedPreferences.Editor = sharedAccount.edit()

    LoadingState.show()
    RetrofitInstance.apiService.apiLogin(
        alertToken = sharedAlert.getString("alert_token", null).toString(),
        type = if (isManager) "manager" else "employee",
        id = inputId,
        pw = inputPw.value
    ).enqueue(object : Callback<LoginResponse> {
        override fun onResponse(
            call: Call<LoginResponse>,
            response: retrofit2.Response<LoginResponse>
        ) {
            if (response.isSuccessful) {
                sharedAccountEdit.putString("userid", response.body()?.id)
                sharedAccountEdit.putString("password", inputPw.value)
                sharedAccountEdit.putString("name", response.body()?.name)
                sharedAccountEdit.putString("phone", response.body()?.phoneNo)
                sharedAccountEdit.putString("email", response.body()?.email)
                sharedAccountEdit.putString(
                    "token",
                    response.body()?.accessToken
                )
                sharedAccountEdit.putString(
                    "token_type",
                    response.body()?.tokenType
                )
                sharedAccountEdit.putString(
                    "type",
                    if (isManager) "manager" else "employee"
                )
                sharedAccountEdit.apply()
                navController.navigate("MainScreen")
                Toast.makeText(
                    navController.context,
                    response.body()?.name + "님 반갑습니다",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showAlertDialog(
                    context = navController.context,
                    title = "로그인 실패",
                    message = "아이디 및 비밀번호를 확인하세요.",
                    buttonText = "확인"
                ) { inputPw.value = "" }
            }
            LoadingState.hide()
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            showAlertDialog(
                context = navController.context,
                title = "로그인 실패",
                message = "서버 상태 및 네트워크 접속 불안정",
                buttonText = "확인"
            ) { (navController.context as Activity).finish() }
            LoadingState.hide()
            Log.e("HEAD METAL", "서버 통신 실패: ${t.message}")
        }
    })
}
