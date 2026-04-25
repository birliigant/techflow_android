package com.birliigant.techflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun TechFlowTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showMenu: Boolean = false,
    onMenuClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    onBackClick != null -> {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    showMenu -> {
                        IconButton(onClick = { onMenuClick?.invoke() }) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "菜单",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions,
            )
        }
    }
}

@Composable
fun TopBarTextAction(
    text: String,
    onClick: () -> Unit,
) {
    TextActionPill(
        text = text,
        onClick = onClick,
        filled = false,
    )
}

@Composable
fun TopBarFilledAction(
    text: String,
    onClick: () -> Unit,
) {
    TextActionPill(
        text = text,
        onClick = onClick,
        filled = true,
    )
}

@Composable
private fun TextActionPill(
    text: String,
    onClick: () -> Unit,
    filled: Boolean,
) {
    if (filled) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            border = null,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(text)
        }
    }
}

@Composable
fun TechFlowFooter(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = if (compact) 16.dp else 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (compact) {
                "Powered by Apache Answer · SIPC TechFlow"
            } else {
                "Powered by Apache Answer - the open-source software that powers Q&A communities.\nMade with love © 2026 SIPC TechFlow."
            },
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun SectionSwitch(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (selected) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
        modifier = Modifier.border(
            width = 0.8.dp,
            color = MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(4.dp),
        ),
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun AvatarBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(34.dp),
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.24f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AvatarImage(
    imageUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    if (!imageUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = fallbackText,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E7EB)),
            contentScale = ContentScale.Crop,
            loading = {
                Surface(
                    modifier = modifier,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE7ECF8),
                ) {}
            },
            error = {
                AvatarFallback(
                    fallbackText = fallbackText,
                    modifier = modifier,
                )
            },
        )
    } else {
        AvatarFallback(fallbackText = fallbackText, modifier = modifier)
    }
}

@Composable
private fun AvatarFallback(
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFE7ECF8),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = fallbackText.take(1).uppercase(),
                color = Color(0xFF2A3D73),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
