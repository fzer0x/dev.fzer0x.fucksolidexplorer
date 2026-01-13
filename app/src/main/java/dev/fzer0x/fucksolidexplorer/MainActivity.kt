package dev.fzer0x.fucksolidexplorer

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fzer0x.fucksolidexplorer.ui.theme.FuckSolidExplorerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuckSolidExplorerTheme {
                val uriHandler = LocalUriHandler.current
                val targetInfo = getTargetAppInfo("pl.solidexplorer2")
                var downloadCount by remember { mutableStateOf<Int?>(null) }

                LaunchedEffect(Unit) {
                    downloadCount = fetchDownloadCount()
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        ModuleStatusScreen(
                            modifier = Modifier.padding(innerPadding),
                            isActive = isModuleActive(),
                            targetVersion = targetInfo,
                            downloadCount = downloadCount
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = { uriHandler.openUri("https://github.com/fzer0x/dev.fzer0x.fucksolidexplorer") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub fzer0x",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (downloadCount != null) {
                                Text(
                                    text = "$downloadCount Downloads",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Developed by fzer0x",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 3.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isModuleActive(): Boolean {
        return false
    }

    private fun getTargetAppInfo(packageName: String): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "Not Found"
        }
    }

    private suspend fun fetchDownloadCount(): Int? = withContext(Dispatchers.IO) {
        try {
            val response = URL("https://api.github.com/repos/Xposed-Modules-Repo/dev.fzer0x.fucksolidexplorer/releases").readText()
            val releases = JSONArray(response)
            var totalDownloads = 0
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val assets = release.getJSONArray("assets")
                for (j in 0 until assets.length()) {
                    totalDownloads += assets.getJSONObject(j).getInt("download_count")
                }
            }
            totalDownloads
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun ModuleStatusScreen(modifier: Modifier = Modifier, isActive: Boolean, targetVersion: String, downloadCount: Int?) {
    val infiniteTransition = rememberInfiniteTransition(label = "modern_3d")
    var clickCount by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val statusColor = if (isActive) Color(0xFF00E676) else Color(0xFFFF5252)

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Fuck Solid Explorer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Version: 1.0", style = MaterialTheme.typography.bodyLarge)
                    Text("Developer: fzer0x", style = MaterialTheme.typography.bodyLarge)
                    if (downloadCount != null) {
                        Text("Total Downloads: $downloadCount", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { uriHandler.openUri("https://github.com/fzer0x/dev.fzer0x.fucksolidexplorer") },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Github", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    alpha = 0.15f
                }
                .background(statusColor, CircleShape)
                .blur(80.dp)
        )

        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f)
                .aspectRatio(0.75f)
                .graphicsLayer {
                    rotationY = rotation
                    rotationX = -rotation / 2f
                    cameraDistance = 20f * density
                }
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(40.dp)
                ),
            shape = RoundedCornerShape(40.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(elevation = 20.dp, shape = CircleShape, spotColor = statusColor)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.15f),
                                    statusColor.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.1f),
                                    statusColor,
                                    statusColor.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                clickCount++
                                if (clickCount >= 10) {
                                    showAboutDialog = true
                                    clickCount = 0
                                }
                            },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "0x",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = statusColor,
                                modifier = Modifier.graphicsLayer {
                                    shadowElevation = 10f
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isActive) "ACTIVE" else "INACTIVE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = statusColor
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Solid Explorer: $targetVersion",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Launch Button with Scale Effect
                val launchInteractionSource = remember { MutableInteractionSource() }
                val isPressed by launchInteractionSource.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "button_scale"
                )

                Button(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage("pl.solidexplorer2")
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Solid Explorer not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    interactionSource = launchInteractionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .shadow(
                            elevation = if (isPressed) 4.dp else 12.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = statusColor
                        )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Launch App",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isActive) "Modul is running." else "Module not running.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
