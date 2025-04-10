package com.example.safereach.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.safereach.ui.theme.CardBackgroundDark
import com.example.safereach.ui.theme.CardBackgroundLight
import com.example.safereach.ui.theme.CardBorderDark
import com.example.safereach.ui.theme.CardBorderLight
import com.example.safereach.ui.theme.CardShape

/**
 * A reusable card component with standardized styling
 * 
 * @param modifier Additional modifier for customization
 * @param title Optional title to display at the top of the card
 * @param onClick Optional click handler for the card
 * @param backgroundColor Background color of the card, defaults to theme's card background
 * @param borderColor Border color of the card, defaults to theme's card border
 * @param content The content to display inside the card
 */
@Composable
fun CustomCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = if (backgroundColor == CardBackgroundDark) CardBorderDark else CardBorderLight,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

/**
 * A centered card component useful for smaller content like loading states or messages
 */
@Composable
fun CenteredCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CustomCard(
            modifier = Modifier.fillMaxWidth(0.85f),
            title = title,
            backgroundColor = backgroundColor,
            content = content
        )
    }
}

/**
 * A variant of CustomCard specifically styled for alert information
 */
@Composable
fun AlertCard(
    modifier: Modifier = Modifier,
    title: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CustomCard(
        modifier = modifier,
        title = title,
        onClick = onClick,
        backgroundColor = backgroundColor,
        content = content
    )
} 