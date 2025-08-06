package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laz.models.CartItemWithProduct
import com.laz.models.User
import com.laz.ui.theme.*
import com.laz.viewmodels.CartViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    user: User,
    cartViewModel: CartViewModel,
    onNavigateBack: () -> Unit
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartTotal by cartViewModel.cartTotal.collectAsState()
    val isLoading by cartViewModel.isLoading.collectAsState()
    val errorMessage by cartViewModel.errorMessage.collectAsState()
    val isCheckoutDialogVisible by cartViewModel.isCheckoutDialogVisible.collectAsState()
    
    val scope = rememberCoroutineScope()
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<CartItemWithProduct?>(null) }
    
    // Load cart items when screen is first displayed
    LaunchedEffect(Unit) {
        cartViewModel.loadCartItems(user.id)
    }
    
    // Checkout dialog
    if (isCheckoutDialogVisible) {
        CheckoutDialog(
            cartTotal = cartTotal,
            onDismiss = { cartViewModel.hideCheckoutDialog() },
            onCheckout = {
                scope.launch {
                    // Process checkout
                    cartViewModel.clearCart(user.id)
                    cartViewModel.hideCheckoutDialog()
                }
            }
        )
    }
    
    // Remove confirmation dialog
    if (showRemoveConfirmDialog && itemToRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            title = { Text("Remove Item") },
            text = { Text("Are you sure you want to remove ${itemToRemove?.product?.name} from your cart?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            itemToRemove?.let { cartViewModel.removeFromCart(it.cartItem) }
                            showRemoveConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LazRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRemoveConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping Cart") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LazDarkCard,
                    titleContentColor = LazWhite,
                    navigationIconContentColor = LazRed
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                CartBottomBar(
                    cartTotal = cartTotal,
                    onCheckout = { cartViewModel.showCheckoutDialog() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(LazDarkBackground, LazDarkSurface)
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LazRed)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "An error occurred",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Empty Cart",
                            modifier = Modifier.size(64.dp),
                            tint = LazRed.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your cart is empty",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = LazWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add some products to your cart",
                            fontSize = 16.sp,
                            color = LazWhite.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = LazRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Browse Products",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Products")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(cartItems) { cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            onQuantityChange = { newQuantity ->
                                scope.launch {
                                    cartViewModel.updateCartItemQuantity(cartItem.cartItem, newQuantity)
                                }
                            },
                            onRemove = {
                                itemToRemove = cartItem
                                showRemoveConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItemWithProduct,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LazGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = cartItem.product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LazWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "JOD ${String.format("%.2f", cartItem.product.price)} each",
                    fontSize = 14.sp,
                    color = LazRedGlow
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total: JOD ${String.format("%.2f", cartItem.product.price * cartItem.cartItem.quantity)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = LazWhite
                )
            }
            
            // Quantity controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { 
                            if (cartItem.cartItem.quantity > 1) {
                                onQuantityChange(cartItem.cartItem.quantity - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (cartItem.cartItem.quantity > 1) LazRed else LazRed.copy(alpha = 0.5f)
                        )
                    }
                    
                    Text(
                        text = "${cartItem.cartItem.quantity}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = { 
                            if (cartItem.cartItem.quantity < cartItem.product.quantity) {
                                onQuantityChange(cartItem.cartItem.quantity + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (cartItem.cartItem.quantity < cartItem.product.quantity) LazRed else LazRed.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = LazRed
                    )
                }
            }
        }
    }
}

@Composable
fun CartBottomBar(
    cartTotal: Double,
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = LazDarkCard,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total",
                    fontSize = 14.sp,
                    color = LazWhite.copy(alpha = 0.7f)
                )
                Text(
                    text = "JOD ${String.format("%.2f", cartTotal)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LazWhite
                )
            }
            
            Button(
                onClick = onCheckout,
                colors = ButtonDefaults.buttonColors(containerColor = LazRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Checkout",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checkout")
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    cartTotal: Double,
    onDismiss: () -> Unit,
    onCheckout: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash on Delivery") }
    val paymentMethods = listOf("Cash on Delivery", "Credit Card", "PayPal")
    var expanded by remember { mutableStateOf(false) }
    var orderPlaced by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = LazDarkCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (orderPlaced) {
                    // Order confirmation screen
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Order Placed",
                        modifier = Modifier.size(64.dp),
                        tint = LazRed
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Order Placed Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your order has been placed and will be processed shortly.",
                        fontSize = 14.sp,
                        color = LazWhite.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = LazRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue Shopping")
                    }
                } else {
                    // Checkout form
                    Text(
                        text = "Checkout",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Order summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LazGray.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Order Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = LazWhite
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal",
                                    color = LazWhite.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "JOD ${String.format("%.2f", cartTotal)}",
                                    color = LazWhite
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Shipping",
                                    color = LazWhite.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "JOD 5.00",
                                    color = LazWhite
                                )
                            }
                            
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = LazWhite.copy(alpha = 0.2f)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    fontWeight = FontWeight.Bold,
                                    color = LazWhite
                                )
                                Text(
                                    text = "JOD ${String.format("%.2f", cartTotal + 5.0)}",
                                    fontWeight = FontWeight.Bold,
                                    color = LazRedGlow
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Shipping information
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazWhite.copy(alpha = 0.5f),
                            focusedLabelColor = LazRed,
                            unfocusedLabelColor = LazWhite.copy(alpha = 0.7f),
                            cursorColor = LazRed,
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Shipping Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazWhite.copy(alpha = 0.5f),
                            focusedLabelColor = LazRed,
                            unfocusedLabelColor = LazWhite.copy(alpha = 0.7f),
                            cursorColor = LazRed,
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazWhite.copy(alpha = 0.5f),
                            focusedLabelColor = LazRed,
                            unfocusedLabelColor = LazWhite.copy(alpha = 0.7f),
                            cursorColor = LazRed,
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Payment method dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LazRed,
                                unfocusedBorderColor = LazWhite.copy(alpha = 0.5f),
                                focusedLabelColor = LazRed,
                                unfocusedLabelColor = LazWhite.copy(alpha = 0.7f),
                                cursorColor = LazRed,
                                focusedTextColor = LazWhite,
                                unfocusedTextColor = LazWhite
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(LazGray)
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method, color = LazWhite) },
                                    onClick = {
                                        paymentMethod = method
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(LazRed)
                            )
                        ) {
                            Text("Cancel", color = LazRed)
                        }
                        
                        Button(
                            onClick = {
                                // Validate form
                                if (name.isNotBlank() && address.isNotBlank() && phone.isNotBlank()) {
                                    orderPlaced = true
                                    onCheckout()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LazRed),
                            enabled = name.isNotBlank() && address.isNotBlank() && phone.isNotBlank()
                        ) {
                            Text("Place Order")
                        }
                    }
                }
            }
        }
    }
}
