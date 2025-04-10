package com.example.safereach.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A loading indicator component for displaying loading states
 * 
 * @param message Optional message to display under the loading indicator
 * @param modifier Additional modifier for customization
 * @param isFullScreen Whether to display the loading indicator at full screen
 */
@Composable
fun LoadingIndicator(
    message: String? = null,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "loading_transition")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "loading_alpha"
    )
    
    val contentModifier = if (isFullScreen) {
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    } else {
        modifier.padding(16.dp)
    }
    
    Box(
        modifier = contentModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(alpha)
                )
            }
        }
    }
}

/**
 * A full screen loading indicator component
 */
@Composable
fun FullScreenLoading(
    message: String? = "Loading..."
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingIndicator(
            message = message,
            isFullScreen = true
        )
    }
} 