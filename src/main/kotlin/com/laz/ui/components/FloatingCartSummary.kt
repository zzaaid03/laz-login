package com.laz.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.math.BigDecimal
import kotlin.math.roundToInt

/**
 * Floating Cart Summary Component
 * A movable floating cart icon that shows cart count and total
 * Can be dragged around the screen for better UX
 */
@Composable
fun FloatingCartSummary(
    cartItemCount: Int,
    cartTotal: BigDecimal,
    isVisible: Boolean = true,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Screen dimensions
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Floating cart position state
    var offsetX by remember { mutableFloatStateOf(screenWidth - 200f) } // Start near right edge
    var offsetY by remember { mutableFloatStateOf(screenHeight - 300f) } // Start near bottom
    
    // Animation for visibility
    AnimatedVisibility(
        visible = isVisible && cartItemCount > 0,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .zIndex(999f) // Ensure it's above other content
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        
                        // Update position with bounds checking
                        val newX = (offsetX + dragAmount.x).coerceIn(
                            0f, 
                            screenWidth - 120f // Account for component width
                        )
                        val newY = (offsetY + dragAmount.y).coerceIn(
                            100f, // Keep below status bar
                            screenHeight - 120f // Account for component height
                        )
                        
                        offsetX = newX
                        offsetY = newY
                    }
                }
        ) {
            FloatingCartContent(
                cartItemCount = cartItemCount,
                cartTotal = cartTotal,
                onCartClick = onCartClick
            )
        }
    }
}

@Composable
private fun FloatingCartContent(
    cartItemCount: Int,
    cartTotal: BigDecimal,
    onCartClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .clickable { onCartClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cart Icon with Badge
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = cartItemCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Cart Total
                Text(
                    text = "JOD ${cartTotal}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Expanded Floating Cart Summary with more details
 * Shows when user long-presses the floating cart
 */
@Composable
fun ExpandedFloatingCartSummary(
    cartItemCount: Int,
    cartTotal: BigDecimal,
    isExpanded: Boolean,
    onCartClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .shadow(12.dp)
                .clickable { onCartClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(min = 120.dp, max = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cart Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Cart Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Items:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = cartItemCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "JOD ${cartTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action Button
                Button(
                    onClick = onCartClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View Cart")
                }
            }
        }
    }
}
