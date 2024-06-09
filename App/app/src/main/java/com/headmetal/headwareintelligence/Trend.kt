package com.headmetal.headwareintelligence

import android.graphics.Typeface
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.legend.verticalLegendItem
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector
import com.patrykandpatrick.vico.core.chart.composed.plus
import com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.composed.plus
import com.patrykandpatrick.vico.core.legend.VerticalLegend
import kotlinx.coroutines.launch

data class TrendResponse(
    val monthData: List<Int>?,
    val inclination: Double?,
    val intercept: Double?
)

class TrendViewModel : ViewModel() {
    private val apiService = RetrofitInstance.apiService

    private val _monthData = mutableStateOf<List<Int>?>(null)
    val monthData: State<List<Int>?> = _monthData

    private val _inclination = mutableStateOf<Double?>(null)
    val inclination: State<Double?> = _inclination

    private val _intercept = mutableStateOf<Double?>(null)
    val intercept: State<Double?> = _intercept

    fun getTrendData(start: String, end: String) {
        viewModelScope.launch {
            val response = apiService.getTrendData(start, end)
            _monthData.value = response.monthData
            _inclination.value = response.inclination
            _intercept.value = response.intercept
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Trend(
    navController: NavController,
    trendViewModel: TrendViewModel = remember { TrendViewModel() }
) {
    val monthData by trendViewModel.monthData
    val inclination by trendViewModel.inclination
    val intercept by trendViewModel.intercept

    var expanded by remember { mutableStateOf(false) }
    val options = generateOptions()
    var selectedOption by remember { mutableStateOf("선택") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9F9F9)
    ) {
        Column {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .clickable { navController.navigateUp() }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "월별 사고 건수 및 사고 추세",
                        fontSize = 20.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 5.dp)
                    ) {
                        Text(
                            text = "기간 ",
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 10.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                value = selectedOption,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(200.dp)
                                    .height(50.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                options.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(text = item) },
                                        onClick = {
                                            if (selectedOption != item) {
                                                expanded = false
                                                selectedOption = item
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(selectedOption) {
                if (selectedOption != "선택") {
                    val (startMonth, endMonth) = getMonthsFromOption(selectedOption)
                    trendViewModel.getTrendData(startMonth, endMonth)
                }
            }

            monthData?.let {
                val veryHighDangerLine = 3
                val highDangerLine = 0
                val dangerColor = when {
                    inclination!! > veryHighDangerLine -> Color.Red
                    inclination!! > highDangerLine -> Color(0xFFFF6600)
                    else -> Color.Green
                }
                val dangerText = when {
                    inclination!! > veryHighDangerLine -> "매우 높음"
                    inclination!! > highDangerLine -> "높음"
                    else -> "보통"
                }
                val dangerTextDetail = when {
                    inclination!! > veryHighDangerLine -> "각별한 안전 사고 주의가 필요해요"
                    inclination!! > highDangerLine -> "안전 사고 주의가 필요해요"
                    else -> "안전 관심은 항상 필요해요"
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "$selectedOption 추세 위험도",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        Row {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                tint = dangerColor,
                                modifier = Modifier.padding(start = 10.dp, top = 5.dp)
                            )
                            Text(
                                text = dangerText,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 5.dp, top = 5.dp)
                            )
                        }
                        Text(
                            text = dangerTextDetail,
                            modifier = Modifier.padding(start = 40.dp, bottom = 5.dp)
                        )
                        Column(
                            Modifier
                                .padding(horizontal = 8.dp)
                                .padding(5.dp)
                        ) {
                            ChartPrint(
                                monthData = monthData!!,
                                inclination = inclination!!,
                                intercept = intercept!!,
                                selectedOption = selectedOption,
                                dangerColor = dangerColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartPrint(
    monthData: List<Int>,
    inclination: Double,
    intercept: Double,
    selectedOption: String,
    dangerColor: Color
) {
    val trendData = mutableListOf<Double>()
    for (x in monthData.indices) {
        trendData.add(x * inclination + intercept)
    }
    val monthDataProducer = ChartEntryModelProducer(
        monthData.mapIndexed { index, value ->
            FloatEntry(index.toFloat(), value.toFloat())
        }
    )
    val trendDataProducer = ChartEntryModelProducer(
        trendData.mapIndexed { index, value ->
            FloatEntry(index.toFloat(), value.toFloat())
        }
    )

    Chart(
        chart = columnChart( // 월별 사고 데이터 차트 부분
            columns = listOf(
                lineComponent( // 막대 그래프 속성 지정
                    color = Color(0xFF000080),
                    thickness = 10.dp,
                    shape = Shapes.cutCornerShape(topRightPercent = 20, topLeftPercent = 20)
                )
            ),
            axisValuesOverrider = AxisValuesOverrider.fixed( // y축 범위 지정
                minY = 0f,
                maxY = monthData.maxOrNull()?.toFloat()?.times(1.1f) ?: Float.MIN_VALUE
            )
        ).plus(
            com.patrykandpatrick.vico.compose.chart.line.lineChart( // 추세선 차트 부분
                lines = listOf(
                    LineSpec( // 추세선 속성 지정
                        lineColor = when (dangerColor) { // 선 색상 지정
                            Color.Red -> android.graphics.Color.RED
                            Color(0xFFFF6600) -> android.graphics.Color.rgb(255, 102, 0)
                            else -> android.graphics.Color.GREEN
                        },
                        pointConnector = DefaultPointConnector(cubicStrength = 0f) // 직선 형태 지정
                    )
                ),
                axisValuesOverrider = AxisValuesOverrider.fixed( // y축 범위 지정
                    minY = 0f,
                    maxY = trendData.maxOrNull()?.toFloat()?.times(1.1f) ?: Float.MIN_VALUE
                )
            )
        ),
        chartModelProducer = monthDataProducer.plus(trendDataProducer), // 그래프 데이터 모델 선정
        startAxis = startAxis( // y축 label
            label = textComponent { typeface = Typeface.SERIF },
            valueFormatter = { value, _ -> String.format("%.1f", value) }
        ),
        bottomAxis = bottomAxis( // x축 label
            label = textComponent { typeface = Typeface.SERIF },
            valueFormatter = { value, _ ->
                val startMonth = if (selectedOption.contains("상반기")) 1 else 7
                val month = (startMonth + value.toInt()) % 12
                String.format("%02d", if (month == 0) 12 else month)
            }
        ),
        runInitialAnimation = false, // 그래프 출력 시 애니메이션 동작
        legend = rememberLegend(listOf(Color(0xFF000080), dangerColor)) // 그래프 범례
    )
}

@Composable
fun rememberLegend(colors: List<Color>): VerticalLegend {
    val labelTextList = listOf("월별 사고 건수", "사고 추세")

    return VerticalLegend(
        items = List(labelTextList.size) { index ->
            verticalLegendItem(
                icon = shapeComponent(
                    shape = Shapes.pillShape,
                    color = colors[index],
                ),
                label = textComponent(),
                labelText = labelTextList[index]
            )
        },
        iconSizeDp = 10f,
        iconPaddingDp = 8f
    )
}

fun getMonthsFromOption(option: String): Pair<String, String> {
    val year = option.substringBefore("년").toInt()
    val half = option.substringAfter("년 ").substringBefore("반기")
    val startMonth = if (half == "상") "01" else "07"
    val endMonth = if (half == "상") "06" else "12"
    return Pair("$year-${startMonth.padStart(2, '0')}", "$year-${endMonth.padStart(2, '0')}")
}

fun generateOptions(): List<String> {
    val startDate = Calendar.getInstance()
    startDate.set(2023, Calendar.JANUARY, 1)
    val endDate = Calendar.getInstance()
    val options = mutableListOf<String>()
    val currentDate = startDate.clone() as Calendar

    while (currentDate.before(endDate) || currentDate == endDate) {
        val halfYear = if (currentDate.get(Calendar.MONTH) < Calendar.JUNE) "상반기" else "하반기"
        options.add("${currentDate.get(Calendar.YEAR)}년 $halfYear")
        if (currentDate.get(Calendar.MONTH) < Calendar.JUNE) {
            currentDate.set(currentDate.get(Calendar.YEAR), Calendar.JULY, 1)
        } else {
            currentDate.set(currentDate.get(Calendar.YEAR) + 1, Calendar.JANUARY, 1)
        }
    }

    options[options.lastIndex] += "(현재)"
    return options
}
