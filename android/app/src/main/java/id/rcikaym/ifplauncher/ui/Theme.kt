package id.rcikaym.ifplauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF14242B)
val Paper = Color(0xFFF4F1EA)
val Card = Color(0xFFFFFCF7)
val Pine = Color(0xFF0E4D45)
val PassGreen = Color(0xFF1B7F4E)
val FailRed = Color(0xFF9E2B25)
val SkipBrown = Color(0xFF7A5310)
val Line = Color(0xFFD9D3C7)

@Composable
fun IfpTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Pine,
        onPrimary = Color.White,
        secondary = Ink,
        onSecondary = Color.White,
        background = Paper,
        onBackground = Ink,
        surface = Card,
        onSurface = Ink,
        error = FailRed,
        onError = Color.White,
        surfaceVariant = Color(0xFFE7E1D6),
        outline = Line,
    )
    val type = androidx.compose.material3.Typography(
        headlineLarge = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
        labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    )
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TwoPane(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < 840.dp) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                left()
                right()
            }
        } else {
            Row(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(
                    Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    left()
                }
                Column(
                    Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    right()
                }
            }
        }
    }
}

@Composable
fun FactList(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { (label, value) ->
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
