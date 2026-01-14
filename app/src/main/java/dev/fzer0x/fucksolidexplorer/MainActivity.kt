package dev.fzer0x.fucksolidexplorer

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fzer0x.fucksolidexplorer.ui.theme.FuckSolidExplorerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class GitHubStats(val downloads: String, val stars: String, val latestStargazer: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuckSolidExplorerTheme {
                val uriHandler = LocalUriHandler.current
                val targetInfo = getTargetAppInfo("pl.solidexplorer2")
                
                var personalStats by remember { mutableStateOf(GitHubStats("...", "...")) }
                var repoStats by remember { mutableStateOf(GitHubStats("...", "...")) }

                LaunchedEffect(Unit) {
                    repeat(3) {
                        launch { fetchGitHubStats("fzer0x/dev.fzer0x.fucksolidexplorer")?.let { personalStats = it } }
                        launch { fetchGitHubStats("Xposed-Modules-Repo/dev.fzer0x.fucksolidexplorer")?.let { repoStats = it } }
                        delay(2500)
                    }
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        MatrixBackground()
                        
                        ModuleStatusScreen(
                            modifier = Modifier.padding(innerPadding),
                            isActive = isModuleActive(),
                            targetVersion = targetInfo
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (repoStats.latestStargazer.isNotEmpty()) {
                                Text(
                                    text = "Last Star LSPosed: ${repoStats.latestStargazer}",
                                    color = Color(0xFF00E676).copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            if (personalStats.latestStargazer.isNotEmpty()) {
                                Text(
                                    text = "Last Star fzer0x: ${personalStats.latestStargazer}",
                                    color = Color(0xFF00E676).copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            
                            if (repoStats.latestStargazer.isNotEmpty() || personalStats.latestStargazer.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            StatsRow(label = "fzer0x:", stats = personalStats)
                            StatsRow(label = "LSPosed:", stats = repoStats)
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GitHubButton(label = "fzer0x Repo", onClick = { uriHandler.openUri("https://github.com/fzer0x/dev.fzer0x.fucksolidexplorer") })
                                GitHubButton(label = "Xposed Repo", onClick = { uriHandler.openUri("https://github.com/Xposed-Modules-Repo/dev.fzer0x.fucksolidexplorer") })
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "★ Please star on GitHub to support! ★",
                                color = Color(0xFF00E676).copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.clickable { uriHandler.openUri("https://github.com/fzer0x/dev.fzer0x.fucksolidexplorer") }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isModuleActive(): Boolean = false

    private fun getTargetAppInfo(packageName: String): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "Not Found"
        } catch (e: Exception) { "Not Found" }
    }

    private suspend fun fetchGitHubStats(repoPath: String): GitHubStats? = withContext(Dispatchers.IO) {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        try {
            fun downloadUrl(urlStr: String): String? {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                return try {
                    conn.apply {
                        setRequestProperty("User-Agent", userAgent)
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    if (conn.responseCode == 200) {
                        conn.inputStream.bufferedReader().use { it.readText() }
                    } else null
                } catch (e: Exception) {
                    null
                } finally {
                    conn.disconnect()
                }
            }

            // Sterne holen
            val repoJsonStr = downloadUrl("https://api.github.com/repos/$repoPath") ?: return@withContext null
            val repoObj = JSONObject(repoJsonStr)
            val starsCount = repoObj.optInt("stargazers_count", 0)
            val stars = starsCount.toString()

            // Letzter Stargazer holen
            var latestStargazer = ""
            if (starsCount > 0) {
                // Letzte Seite abrufen (GitHub API zeigt standardmäßig 30 pro Seite)
                val lastPage = (starsCount + 29) / 30
                val stargazersJson = downloadUrl("https://api.github.com/repos/$repoPath/stargazers?page=$lastPage")
                if (stargazersJson != null) {
                    val stargazersArray = JSONArray(stargazersJson)
                    if (stargazersArray.length() > 0) {
                        latestStargazer = stargazersArray.getJSONObject(stargazersArray.length() - 1).optString("login", "")
                    }
                }
            }

            // Downloads holen
            var downloadsCount = 0
            val releasesJsonStr = downloadUrl("https://api.github.com/repos/$repoPath/releases")
            if (releasesJsonStr != null) {
                val array = JSONArray(releasesJsonStr)
                for (i in 0 until array.length()) {
                    val assets = array.getJSONObject(i).optJSONArray("assets") ?: continue
                    for (j in 0 until assets.length()) {
                        downloadsCount += assets.getJSONObject(j).optInt("download_count", 0)
                    }
                }
            }
            
            GitHubStats(downloadsCount.toString(), stars, latestStargazer)
        } catch (e: Exception) {
            GitHubStats("Err", "Err", "")
        }
    }
}

@Composable
fun GitHubButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676).copy(alpha = 0.15f), contentColor = Color(0xFF00E676)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
    ) {
        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatsRow(label: String, stats: GitHubStats) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Text(text = label, color = Color(0xFF00E676).copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
        Icon(Icons.Default.Star, null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
        Text(text = " ${stats.stars}", color = Color(0xFF00E676), fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
        Text(text = " ${stats.downloads}", color = Color(0xFF00E676), fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun MatrixBackground() {
    val characters = remember { "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ".toCharArray() }
    val columnCount = 50
    val drops = remember { IntArray(columnCount) { Random.nextInt(-100, 0) } }
    val speeds = remember { IntArray(columnCount) { Random.nextInt(1, 4) } }
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { tick++; delay(30) } }

    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
        val t = tick
        val fontSize = size.width / columnCount
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply { textSize = fontSize; isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }
            for (i in 0 until columnCount) {
                val x = i * fontSize + fontSize / 2
                val y = (drops[i] * fontSize)
                paint.color = android.graphics.Color.WHITE
                paint.setShadowLayer(20f, 0f, 0f, android.graphics.Color.WHITE)
                canvas.nativeCanvas.drawText(characters[Random.nextInt(characters.size)].toString(), x, y, paint)
                paint.color = android.graphics.Color.GREEN
                paint.setShadowLayer(15f, 0f, 0f, android.graphics.Color.GREEN)
                for (j in 1..20) {
                    val tailY = y - (j * fontSize)
                    if (tailY > -fontSize && tailY < size.height) {
                        paint.alpha = (255 * (1f - j.toFloat() / 20)).toInt()
                        canvas.nativeCanvas.drawText(characters[Random.nextInt(characters.size)].toString(), x, tailY, paint)
                    }
                }
                if (t % speeds[i] == 0L) drops[i]++
                if (y > size.height + 20 * fontSize) { drops[i] = -Random.nextInt(0, 20); speeds[i] = Random.nextInt(1, 4) }
            }
        }
    }
}

@Composable
fun ModuleStatusScreen(modifier: Modifier = Modifier, isActive: Boolean, targetVersion: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "modern_3d")
    val rotation by infiniteTransition.animateFloat(initialValue = -10f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutCubic), RepeatMode.Reverse), label = "rotation")
    val statusColor = if (isActive) Color(0xFF00E676) else Color(0xFFFF5252)
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(0.85f).aspectRatio(0.8f).graphicsLayer { rotationY = rotation; cameraDistance = 20f * density }.border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(40.dp)),
            shape = RoundedCornerShape(40.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(110.dp).background(Brush.radialGradient(listOf(statusColor.copy(alpha = 0.15f), statusColor.copy(alpha = 0.05f))), CircleShape).border(2.dp, Brush.sweepGradient(listOf(statusColor.copy(alpha = 0.1f), statusColor, statusColor.copy(alpha = 0.1f))), CircleShape), contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.05f) ) {
                        Box(contentAlignment = Alignment.Center) { Text(text = "0x", fontSize = 36.sp, fontWeight = FontWeight.Black, color = statusColor) }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isActive) "ACTIVE" else "INACTIVE", fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = statusColor)
                    Surface(modifier = Modifier.padding(top = 4.dp), color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(text = "Solid Explorer: $targetVersion", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
                val launchInteractionSource = remember { MutableInteractionSource() }
                val isPressed by launchInteractionSource.collectIsPressedAsState()
                val animatedScale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
                Box(
                    modifier = Modifier.fillMaxWidth(0.9f).height(60.dp).graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }.shadow(if (isPressed) 2.dp else 12.dp, RoundedCornerShape(20.dp), spotColor = statusColor).background(Brush.verticalGradient(listOf(statusColor, statusColor.copy(alpha = 0.7f))), RoundedCornerShape(20.dp)).clickable(interactionSource = launchInteractionSource, indication = null) {
                        val intent = context.packageManager.getLaunchIntentForPackage("pl.solidexplorer2")
                        if (intent != null) context.startActivity(intent) else Toast.makeText(context, "Not found", Toast.LENGTH_SHORT).show()
                    }, contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text(text = " LAUNCH APP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}
