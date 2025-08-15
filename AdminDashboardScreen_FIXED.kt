// COMPREHENSIVE FIX FOR AdminDashboardScreen.kt
// This file contains the corrected sections to replace the old sales references

// SECTION 1 REPLACEMENT (around line 379):
                        val deliveredOrders = orders.filter { it.status == com.laz.models.OrderStatus.DELIVERED }
                        if (deliveredOrders.isEmpty()) {
                            Text(
                                "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 3 delivered orders (sales)
                            deliveredOrders.take(3).forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Order #${order.id.take(8)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            order.orderDate.take(16).replace("T", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "JOD ${order.totalAmount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (order != deliveredOrders.take(3).last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (deliveredOrders.size > 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onNavigateToSalesOverview() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View All Sales (${deliveredOrders.size})")
                                }
                            }
                        }

// SECTION 2 REPLACEMENT (around line 533) - IDENTICAL TO SECTION 1:
                        val deliveredOrders2 = orders.filter { it.status == com.laz.models.OrderStatus.DELIVERED }
                        if (deliveredOrders2.isEmpty()) {
                            Text(
                                "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 3 delivered orders (sales)
                            deliveredOrders2.take(3).forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Order #${order.id.take(8)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            order.orderDate.take(16).replace("T", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "JOD ${order.totalAmount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (order != deliveredOrders2.take(3).last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (deliveredOrders2.size > 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onNavigateToSalesOverview() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View All Sales (${deliveredOrders2.size})")
                                }
                            }
                        }

// INSTRUCTIONS:
// Replace the two sales sections in AdminDashboardScreen.kt with the above corrected code
// This will complete the sales-to-order architecture replacement
