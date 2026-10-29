package me.weishu.kernelsu.license

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val accent = Color(0xFF1ED760)
private val danger = Color(0xFFEF4444)
private val muted = Color(0xFF64748B)
private val bg = Color(0xFF121212)
private val cardBg = Color(0xFF282828)

@Composable
fun LicenseGate(onValid: () -> Unit, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val stored = remember { LicenseManager.loadLicense(ctx) }
    var validated by remember {
        mutableStateOf(stored?.let { LicenseManager.validate(ctx, it) } as? LicenseManager.ValidationResult.Valid)
    }

    if (validated != null) { content(); return }
    LicenseScreen(context = ctx, onValidated = { validated = it; onValid() })
}

@Composable
private fun LicenseScreen(context: Context, onValidated: (LicenseManager.ValidationResult.Valid) -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val deviceId = remember { LicenseManager.getDeviceId(context) }

    Box(modifier = Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spotify", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("License Required", color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(32.dp))

            Card(colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("YOUR DEVICE ID", color = muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(deviceId, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(10.dp))
                    Text("Send this ID to @STEAMXCHEATS to get your license key.", color = muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it.trim(); error = null },
                label = { Text("Paste key (ARTHUR-DR-XXXXX)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(Modifier.height(8.dp))

            if (error != null) {
                Text(error ?: "", color = danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val result = LicenseManager.validate(context, input)
                    if (result is LicenseManager.ValidationResult.Valid) {
                        LicenseManager.saveLicense(context, input)
                        onValidated(result)
                    } else if (result is LicenseManager.ValidationResult.Invalid) {
                        error = result.reason
                    }
                },
                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(10.dp), enabled = input.isNotBlank(),
            ) { Text("ACTIVATE", fontWeight = FontWeight.SemiBold, color = Color.Black) }

            Spacer(Modifier.height(24.dp))
            Text("Telegram  ·  t.me/STEAMXCHEATS", color = muted, fontSize = 11.sp)
        }
    }
}
