package com.facultytimetable.pro.presentation.root

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facultytimetable.pro.presentation.theme.HeaderGradientEnd
import com.facultytimetable.pro.presentation.theme.HeaderGradientStart
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    verifyPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (pin.length < 4 || checking) {
            error = "Enter your 4–8 digit PIN."
            return
        }
        scope.launch {
            checking = true
            val ok = verifyPin(pin)
            checking = false
            if (ok) {
                onUnlocked()
            } else {
                error = "Incorrect PIN. Try again."
                pin = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeaderGradientStart, HeaderGradientEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = CircleShape,
                modifier = Modifier.size(82.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Faculty TimeTable Pro",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Enter your PIN to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(if (pin.length >= 6) 8 else 4) { index ->
                    val filled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) Color.White else Color.White.copy(alpha = 0.28f)
                            )
                    )
                }
            }

            if (pin.length > 4) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${pin.length} digits entered",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (error != null) {
                Text(
                    error!!,
                    color = Color(0xFFFECACA),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back")
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "" -> Spacer(modifier = Modifier.size(72.dp))
                            "back" -> IconButton(
                                onClick = {
                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    error = null
                                },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = "Delete digit",
                                    tint = Color.White
                                )
                            }
                            else -> Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .then(
                                            Modifier.clickableKey {
                                                if (pin.length < 8) {
                                                    pin += key
                                                    error = null
                                                }
                                            }
                                        )
                                ) {
                                    Text(
                                        key,
                                        fontSize = 26.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = ::submit,
                enabled = !checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = HeaderGradientStart
                )
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (checking) "Checking…" else "Unlock",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun Modifier.clickableKey(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
