package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.*
import com.laz.viewmodels.*
import com.laz.ui.theme.*
import com.laz.ui.components.OrderReceiptDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Payment Screen
 * Handles checkout process: Cart → Payment → Order
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    user: User,
    cartTotal: BigDecimal,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (Order) -> Unit,
    cartViewModel: SecureFirebaseCartViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    productViewModel: SecureFirebaseProductViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    ordersViewModel: FirebaseOrdersViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    var paymentMethod by remember { mutableStateOf("Credit Card") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var shippingAddress by remember { mutableStateOf(user.address ?: "") }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdOrder by remember { mutableStateOf<Order?>(null) }

    val operationSuccess by ordersViewModel.operationSuccess.collectAsState()
    val errorMessage by ordersViewModel.errorMessage.collectAsState()
    
    // Load cart items and products separately, then combine them
    val cartItems by cartViewModel.cartItems.collectAsState()
    val isLoadingCart by cartViewModel.isLoading.collectAsState()
    var cartItemsWithProducts by remember { mutableStateOf<List<CartItemWithProduct>>(emptyList()) }
    
    // Load cart items when screen opens
    LaunchedEffect(Unit) {
        cartViewModel.loadCartItems()
    }
    
    // Combine cart items with real product information from Firebase
    LaunchedEffect(cartItems) {
        cartItemsWithProducts = cartItems.mapNotNull { cartItem ->
            try {
                // Load real product data from Firebase
                val productResult = productViewModel.getProductById(cartItem.productId)
                if (productResult.isSuccess) {
                    val product = productResult.getOrNull()
                    if (product != null) {
                        CartItemWithProduct(
                            cartItem = cartItem,
                            product = product
                        )
                    } else {
                        // Fallback for missing products
                        CartItemWithProduct(
                            cartItem = cartItem,
                            product = Product(
                                id = cartItem.productId,
                                name = "Product Not Found",
                                quantity = 0,
                                cost = BigDecimal.ZERO,
                                price = BigDecimal("25.00"), // Default fallback price
                                shelfLocation = "Unknown"
                            )
                        )
                    }
                } else {
                    // Fallback for failed product loading
                    CartItemWithProduct(
                        cartItem = cartItem,
                        product = Product(
                            id = cartItem.productId,
                            name = "Loading Error",
                            quantity = 0,
                            cost = BigDecimal.ZERO,
                            price = BigDecimal("25.00"), // Default fallback price
                            shelfLocation = "Unknown"
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback for exceptions
                CartItemWithProduct(
                    cartItem = cartItem,
                    product = Product(
                        id = cartItem.productId,
                        name = "Error Loading Product",
                        quantity = 0,
                        cost = BigDecimal.ZERO,
                        price = BigDecimal("25.00"), // Default fallback price
                        shelfLocation = "Unknown"
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Order Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    cartItemsWithProducts.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.product.name} x${item.cartItem.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "JOD %.2f".format(item.product.price.multiply(BigDecimal(item.cartItem.quantity))),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "JOD %.2f".format(cartTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Payment Method Selection
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = paymentMethod == "Credit Card",
                            onClick = { paymentMethod = "Credit Card" }
                        )
                        Text("Credit Card")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = paymentMethod == "Cash on Delivery",
                            onClick = { paymentMethod = "Cash on Delivery" }
                        )
                        Text("Cash on Delivery")
                    }
                }
            }

            // Credit Card Details (if selected)
            if (paymentMethod == "Credit Card") {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Card Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = cardholderName,
                            onValueChange = { cardholderName = it },
                            label = { Text("Cardholder Name") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { if (it.length <= 19) cardNumber = it },
                            label = { Text("Card Number") },
                            placeholder = { Text("1234 5678 9012 3456") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { if (it.length <= 5) expiryDate = it },
                                label = { Text("MM/YY") },
                                placeholder = { Text("12/25") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { if (it.length <= 3) cvv = it },
                                label = { Text("CVV") },
                                placeholder = { Text("123") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Shipping Address
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Shipping Address",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = shippingAddress,
                        onValueChange = { shippingAddress = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )
                }
            }

            // Place Order Button
            Button(
                onClick = {
                    if (isValidPayment(paymentMethod, cardNumber, expiryDate, cvv, cardholderName, shippingAddress)) {
                        processPayment(
                            user = user,
                            cartItems = cartItemsWithProducts,
                            cartTotal = cartTotal,
                            paymentMethod = paymentMethod,
                            shippingAddress = shippingAddress,
                            ordersViewModel = ordersViewModel,
                            onProcessingStart = { isProcessing = true },
                            onSuccess = { order ->
                                createdOrder = order
                                showSuccessDialog = true
                                isProcessing = false
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Place Order - JOD %.2f".format(cartTotal))
                }
            }

            // Error Message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Receipt Dialog - Show receipt instead of generic success message
    if (showSuccessDialog && createdOrder != null) {
        OrderReceiptDialog(
            order = createdOrder!!,
            onDismiss = {
                showSuccessDialog = false
                // Clear cart and navigate
                cartViewModel.clearCart()
                onPaymentSuccess(createdOrder!!)
            }
        )
    }

    // Clear messages after showing them
    LaunchedEffect(errorMessage, operationSuccess) {
        if (errorMessage != null || operationSuccess != null) {
            delay(3000)
            ordersViewModel.clearMessages()
        }
    }
}

/**
 * Validate payment information
 */
private fun isValidPayment(
    paymentMethod: String,
    cardNumber: String,
    expiryDate: String,
    cvv: String,
    cardholderName: String,
    shippingAddress: String
): Boolean {
    if (shippingAddress.isBlank()) return false
    
    if (paymentMethod == "Credit Card") {
        return cardNumber.isNotBlank() && 
               expiryDate.isNotBlank() && 
               cvv.isNotBlank() && 
               cardholderName.isNotBlank()
    }
    
    return true // Cash on delivery only needs address
}

/**
 * Process payment and create order
 */
private fun processPayment(
    user: User,
    cartItems: List<CartItemWithProduct>,
    cartTotal: BigDecimal,
    paymentMethod: String,
    shippingAddress: String,
    ordersViewModel: FirebaseOrdersViewModel,
    onProcessingStart: () -> Unit,
    onSuccess: (Order) -> Unit
) {
    onProcessingStart()
    
    // Create order items
    val orderItems = cartItems.map { item ->
        OrderItem(
            productId = item.product.id,
            productName = item.product.name,
            quantity = item.cartItem.quantity,
            unitPrice = item.product.price,
            totalPrice = item.product.price.multiply(BigDecimal(item.cartItem.quantity))
        )
    }
    
    // Create order
    val order = Order(
        customerId = user.id,
        customerUsername = user.username,
        items = orderItems,
        totalAmount = cartTotal,
        status = OrderStatus.PENDING, // All orders start as PENDING until admin approval
        paymentMethod = paymentMethod,
        shippingAddress = shippingAddress,
        estimatedDelivery = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
        notes = "Order placed via LAZ Store mobile app"
    )
    
    // Submit order
    ordersViewModel.createOrder(order)
    
    // Wait for the order to be created and get the real ID from Firebase
    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
        delay(2000) // 2 second processing simulation
        
        // Collect the lastCreatedOrder to get the real ID
        var attempts = 0
        val maxAttempts = 10 // Wait up to 5 seconds (500ms * 10)
        
        while (attempts < maxAttempts) {
            val createdOrder = ordersViewModel.lastCreatedOrder.value
            if (createdOrder != null && createdOrder.customerId == user.id && 
                createdOrder.totalAmount == cartTotal) {
                // Found the matching order with real ID
                onSuccess(createdOrder)
                return@launch
            }
            delay(500) // Wait 500ms before checking again
            attempts++
        }
        
        // Fallback: if we couldn't get the real order, use a timestamp-based ID
        val fallbackOrder = order.copy(id = System.currentTimeMillis() / 1000)
        onSuccess(fallbackOrder)
    }
}
