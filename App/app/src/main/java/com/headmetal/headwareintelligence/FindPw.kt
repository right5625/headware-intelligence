package com.headmetal.headwareintelligence

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class ForgotPw(
    val id: String,
    val phoneNo: String,
    val password: String,
    val rePassword: String,
    val type: String
)

@Composable
fun FindPw(navController: NavController = rememberNavController()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9C94C)
    ) { FindPwComposable(navController = navController) }
}

@Composable
fun FindPwComposable(navController: NavController = rememberNavController()) {
    val id: MutableState<String> = remember { mutableStateOf("") }
    val phone: MutableState<String> = remember { mutableStateOf("") }
    val pw: MutableState<String> = remember { mutableStateOf("") }
    val rePw: MutableState<String> = remember { mutableStateOf("") }
    val isEmployee: MutableState<Boolean> = remember { mutableStateOf(true) }
    val isManager: MutableState<Boolean> = remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HelmetImage()
        LoginTextFieldComposable(
            fieldLabel = { LoginFieldLabel(text = "아이디") },
            inputTextField = { InputTextField(inputText = id) }
        )
        LoginTextFieldComposable(
            fieldLabel = { LoginFieldLabel(text = "전화번호") },
            inputTextField = { InputTextField(inputText = phone) }
        )
        LoginTextFieldComposable(
            fieldLabel = { LoginFieldLabel(text = "새 비밀번호") },
            inputTextField = {
                InputTextField(
                    inputText = pw,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        )
        LoginTextFieldComposable(
            fieldLabel = { LoginFieldLabel(text = "비밀번호 확인") },
            inputTextField = {
                InputTextField(
                    inputText = rePw,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        )
        CustomRadioButtonComposable(
            fieldLabel = { LoginFieldLabel(text = "직무") },
            customRadioButtonGroup = { CustomRadioButtonGroup(isEmployee, isManager) }
        )
        LoginFunctionButtonComposable(
            loginFunctionButtons = arrayOf(
                {
                    LoginFunctionButton(buttonText = "비밀번호 변경") {
                        if (pw.value == rePw.value && pw.value.isNotEmpty()) {
                            LoadingState.show()
                            RetrofitInstance.apiService.apiChangePw(
                                ForgotPw(
                                    id.value,
                                    phone.value,
                                    pw.value,
                                    rePw.value,
                                    if (isManager.value) "manager" else "employee"
                                )
                            ).enqueue(object : Callback<ForgotPw> {
                                override fun onResponse(
                                    call: Call<ForgotPw>,
                                    response: Response<ForgotPw>
                                ) {
                                    if (response.isSuccessful) {
                                        showAlertDialog(
                                            context = navController.context,
                                            title = "비밀번호 변경 성공",
                                            message = "로그인 화면으로 이동합니다.",
                                            buttonText = "확인"
                                        ) {
                                            navController.navigate("LoginScreen")
                                        }
                                    } else {
                                        showAlertDialog(
                                            context = navController.context,
                                            title = "비밀번호 변경 실패",
                                            message = "존재하지 않는 계정입니다.",
                                            buttonText = "확인"
                                        )
                                        Log.e("HEAD METAL", "비밀번호 변경 요청 실패: ${response.code()}")
                                    }
                                    LoadingState.hide()
                                }

                                override fun onFailure(call: Call<ForgotPw>, t: Throwable) {
                                    LoadingState.hide()
                                    Log.e("HEAD METAL", "서버 통신 실패: ${t.message}")
                                }
                            })
                        } else {
                            showAlertDialog(
                                context = navController.context,
                                title = "비밀번호 변경 실패",
                                message = "입력한 정보를 다시 확인하세요!",
                                buttonText = "확인"
                            )
                        }
                    }
                }
            )
        )
        AppNameText()
    }
}

// 프리뷰
@Preview(showBackground = true)
@Composable
fun FindPwPreview() {
    FindPw()
}

@Preview(showBackground = true)
@Composable
fun FindPwComposablePreview() {
    FindPwComposable()
}

@Preview(showBackground = true)
@Composable
fun FindPwHelmetImagePreview() {
    HelmetImage()
}

@Preview(showBackground = true)
@Composable
fun FindPwTextFieldComposablePreview() {
    LoginTextFieldComposable(
        fieldLabel = { LoginFieldLabel(text = "아이디") },
        inputTextField = { InputTextField() }
    )
}

@Preview(showBackground = true)
@Composable
fun FindPwFieldLabelPreview() {
    LoginFieldLabel(text = "아이디")
}

@Preview(showBackground = true)
@Composable
fun FindPwInputTextFieldPreview() {
    InputTextField()
}

@Preview(showBackground = true)
@Composable
fun FindPwCustomRadioButtonComposablePreview() {
    CustomRadioButtonComposable(
        fieldLabel = { LoginFieldLabel(text = "직무") },
        customRadioButtonGroup = { CustomRadioButtonGroup() }
    )
}

@Preview(showBackground = true)
@Composable
fun FindPwCustomRadioButtonGroupPreview() {
    CustomRadioButtonGroup()
}

@Preview(showBackground = true)
@Composable
fun FindPwCustomRadioButtonSinglePreview() {
    CustomRadioButtonSingle(buttonText = "일반직")
}

@Preview(showBackground = true)
@Composable
fun FindPwFunctionButtonPreview() {
    LoginFunctionButton(buttonText = "비밀번호 변경")
}

@Preview(showBackground = true)
@Composable
fun FindPwAppNameTextPreview() {
    AppNameText()
}
