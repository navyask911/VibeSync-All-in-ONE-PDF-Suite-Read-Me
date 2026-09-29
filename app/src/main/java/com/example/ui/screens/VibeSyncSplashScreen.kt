package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralPink
import com.example.ui.theme.PassionRose
import com.example.ui.theme.VibeSyncDarkCanvas
import com.example.ui.theme.VibeSyncEmerald
import com.example.ui.theme.VibeSyncTeal

/**
 * Premium Native Splash & Session Verification Screen.
 * Prevents screen flicker by maintaining active branded splash state while
 * asynchronous Room database & authentication tokens are verified in the background.
 */
@Composable
fun VibeSyncSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1418),
                        VibeSyncDarkCanvas,
                        Color(0xFF080D10)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("vibe_sync_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Center Branded Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated App Emblem
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(108.dp)
                    .scale(pulseScale)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(CoralPink, PassionRose, VibeSyncTeal)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "✨",
                        fontSize = 46.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name with high-contrast vibrant typography
            Text(
                text = "VibeSync",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = 0.5.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Real-Time Chat, Social Connect & Business Hub",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8696A0)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Smooth Subtle Spinner
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = CoralPink,
                strokeWidth = 2.5.dp
            )
        }

        // Bottom Privacy & Security Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = VibeSyncEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero-Leak End-to-End Encrypted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFAEBAC1)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fast & Private • No Ads Tracker",
                fontSize = 10.sp,
                color = Color(0xFF667781)
            )
        }
    }
}
