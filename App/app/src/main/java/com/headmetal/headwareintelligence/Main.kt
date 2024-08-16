package com.headmetal.headwareintelligence

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class WeatherResponse(
    val temperature: Float,
    val airVelocity: Float,
    val precipitation: Float,
    val humidity: Float
)

// 프리뷰
@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Main(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun WelcomeUserComposablePreview() {
    WelcomeUserComposable(userName = "사용자")
}

@Preview(showBackground = true)
@Composable
fun MainFieldLabelPreview() {
    MainFieldLabel(text = "관리자 기능")
}

@Preview(showBackground = true)
@Composable
fun MainFunctionButtonMenuPreview() {
    MainFunctionButtonMenu(type = "manager", navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun MainContentsHeaderPreview() {
    MainContentsHeader(refreshState = remember { mutableStateOf(false) })
}

@Preview(showBackground = true)
@Composable
fun MainContentsPreview() {
    MainContents(type = "manager", navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun MainContentsBoxPreview() {
    ContentsBox(
        imageVector = Icons.Default.Report,
        iconColor = Color(0xFFFFCC00),
        contentsTexts = arrayOf({ MainContentsBoxText(text = "주의 행동 요령") })
    )
}


@Composable
fun Main(navController: NavController) {
    BackOnPressed()
    IconScreen(
        imageVector = Icons.Default.Menu,
        onClick = { navController.navigate("MenuScreen") },
        content = {
            val sharedAccount: SharedPreferences =
                LocalContext.current.getSharedPreferences("Account", Activity.MODE_PRIVATE)
            val type = sharedAccount.getString("type", "")
            val userName = sharedAccount.getString("name", "")

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                WelcomeUserComposable(userName = userName!!)
                MainFunctionButtonMenu(type = type!!, navController = navController)
                MainContents(type = type, navController = navController)
            }
        }
    )
}

@Composable
fun WelcomeUserComposable(userName: String) {
    Column {
        Text(text = "반갑습니다,", fontSize = 16.sp)
        Row {
            Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "님", fontSize = 24.sp)
        }
    }
}

/**
 * MainFunction
 */
@Composable
fun MainFieldLabel(text: String) {
    BoldTextField(
        text = text,
        fontSize = 18.sp
    )
}

@Composable
fun MainFunctionButtonMenu(type: String, navController: NavController) {
    Column {
        if (type == "manager") {
            MainFieldLabel(text = "관리자 기능")
            RoundedButton(
                buttonText = "사고 추세 확인",
                colors = Color(0xFF99CCFF),
                onClick = { navController.navigate("TrendScreen") }
            )
            RoundedButton(
                buttonText = "사고 발생지 확인",
                colors = Color(0xFFFF6600),
                onClick = { navController.navigate("MapScreen") }
            )
            RoundedButton(
                buttonText = "미처리 사고 발생지 확인",
                colors = Color(0xFFFF8000),
                onClick = { navController.navigate("NullMapScreen") }
            )
            RoundedButton(
                buttonText = "작업장 관리",
                colors = Color(0xFFFF8000),
                onClick = { navController.navigate("WorkListScreen") }
            )
        } else {
            MainFieldLabel(text = "근로자 기능")
            RoundedButton(
                buttonText = "안전모 등록",
                colors = Color(0xFFFFB266),
                onClick = { navController.navigate("HelmetScreen") }
            )
        }
    }
}

/**
 * MainContents
 */
@Composable
fun MainContents(type: String, navController: NavController) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    var temperature by remember { mutableFloatStateOf(0.0f) }
    var airVelocity by remember { mutableFloatStateOf(0.0f) }
    var precipitation by remember { mutableFloatStateOf(0.0f) }
    var humidity by remember { mutableFloatStateOf(0.0f) }

    val refreshState: MutableState<Boolean> = remember { mutableStateOf(false) }

    LaunchedEffect(refreshState.value) {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
            }

            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        if (hasLocationPermission) {
            val location = fusedLocationClient.lastLocation.await()
            location?.let { pos ->
                RetrofitInstance.retryApiService.getWeather(pos.latitude, pos.longitude)
                    .enqueue(object : Callback<WeatherResponse> {
                        override fun onResponse(
                            call: Call<WeatherResponse>,
                            response: Response<WeatherResponse>
                        ) {
                            if (response.isSuccessful) {
                                val weather: WeatherResponse? = response.body()
                                weather?.let {
                                    temperature = it.temperature
                                    airVelocity = it.airVelocity
                                    precipitation = it.precipitation
                                    humidity = it.humidity
                                }

                                if (refreshState.value) {
                                    Toast
                                        .makeText(
                                            context,
                                            "새로고침 되었습니다.",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                    refreshState.value = false
                                }

                                Log.d("HEAD METAL", "날씨 정보 로딩 성공")
                            }
                        }

                        override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                            Log.e("HEAD METAL", "서버 통신 실패: ${t.message}")
                        }
                    })
            }
        } else {
            Log.e("HEAD METAL", "위치 권한이 필요함")
        }
    }

    val (weatherInfo, weatherIcon, weatherColor) = getWeatherInfo(precipitation)

    Column {
        MainContentsHeader(refreshState = refreshState)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ContentsBox(
                imageVector = weatherIcon,
                iconColor = weatherColor,
                contentsTexts = arrayOf(
                    { MainContentsBoxText(text = "기상 정보 : $weatherInfo") },
                    { MainContentsBoxText(text = "1시간 강수량 : ${precipitation}mm") },
                    {
                        MainContentsBoxText(
                            text = "기온 : ${temperature}ºC" + if (temperature > 35) {
                                "(폭염 경보)"
                            } else if (temperature > 33) {
                                "(폭염 주의보)"
                            } else {
                                ""
                            }
                        )
                    },
                    {
                        MainContentsBoxText(
                            text = "풍속 : ${airVelocity}m/s" + if (airVelocity > 21) {
                                "(강풍 경보)"
                            } else if (airVelocity > 14) {
                                "(강풍 주의보)"
                            } else {
                                ""
                            }
                        )
                    },
                    { MainContentsBoxText(text = "습도 : $humidity%") }
                )
            )
            ContentsBox(
                modifier = Modifier.clickable { navController.navigate("CountermeasureScreen") },
                imageVector = Icons.Default.Report,
                iconColor = Color(0xFFFFCC00),
                contentsTexts = arrayOf({ MainContentsBoxText(text = "주의 행동 요령") })
            )
            if (type == "manager") {
                ContentsBox(
                    modifier = Modifier.clickable { navController.navigate("ProcessingScreen") },
                    imageVector = Icons.Default.Inventory,
                    iconColor = Color.Gray,
                    contentsTexts = arrayOf({ MainContentsBoxText(text = "사고 처리 내역") })
                )
            }
        }
    }
}

@Composable
fun MainContentsHeader(
    refreshState: MutableState<Boolean>
) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshClickable by remember { mutableStateOf(true) }

    Row {
        BoldTextField(text = "정보", fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier.clickable(enabled = isRefreshClickable) {
                refreshState.value = true
                isRefreshClickable = false
                coroutineScope.launch {
                    delay(3000)
                    isRefreshClickable = true
                }
            },
            imageVector = Icons.Default.Update,
            contentDescription = null
        )
    }
}

/**
 * ContentBox
 */
@Composable
fun ContentsBox(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    iconColor: Color = Color.Black,
    vararg contentsTexts: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(color = Color.White)
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = MaterialTheme.shapes.medium
            )
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = modifier
                    .padding(start = 10.dp, top = 25.dp, bottom = 25.dp)
                    .size(40.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = iconColor
            )
            Column {
                contentsTexts.forEach { contentsText ->
                    contentsText()
                }
            }
        }
    }
}

@Composable
fun MainContentsBoxText(
    text: String
) {
    Text(
        text = text,
        fontSize = 16.sp,
        modifier = Modifier
            .padding(start = 10.dp)
            .padding(vertical = 8.dp)
    )
}

fun getWeatherInfo(precipitation: Float): Triple<String, ImageVector, Color> {
    return when {
        precipitation > 30 -> Triple("호우 경보", Icons.Default.Water, Color(0xFF00BFFF))
        precipitation > 20 -> Triple("호우 주의보", Icons.Default.Water, Color(0xFF00BFFF))
        precipitation > 0 -> Triple("비", Icons.Default.WaterDrop, Color(0xFF00BFFF))
        else -> Triple("맑음", Icons.Default.WbSunny, Color(0xFFFF7F00))
    }
}