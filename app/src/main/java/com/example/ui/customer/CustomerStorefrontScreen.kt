package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.AfruteViewModel
import com.example.ui.theme.*

@Composable
fun CustomerStorefrontScreen(
    viewModel: AfruteViewModel,
    modifier: Modifier = Modifier
) {
    val shops by viewModel.shops.collectAsState()
    val fruits by viewModel.fruits.collectAsState()
    val selectedShopId by viewModel.selectedCustomerShopId.collectAsState()
    val searchQuery by viewModel.fruitSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val showCartSheet by viewModel.showCartSheet.collectAsState()
    val showTrackingScreen by viewModel.showTrackingScreen.collectAsState()
    val trackingOrderId by viewModel.trackingOrderId.collectAsState()
    val orders by viewModel.orders.collectAsState()

    val currentShop = shops.find { it.id == selectedShopId } ?: shops.firstOrNull()

    if (showTrackingScreen && trackingOrderId != null) {
        val activeOrder = orders.find { it.id == trackingOrderId }
        if (activeOrder != null) {
            OrderTrackingView(
                order = activeOrder,
                onBack = { viewModel.hideOrderTracking() }
            )
            return
        }
    }

    val shopFruits = fruits.filter { it.shopId == currentShop?.id }
    val totalCartItems = cart.values.sum()
    val totalCartAmount = cart.entries.sumOf { (fruitId, qty) ->
        val fruit = shopFruits.find { it.id == fruitId }
        (fruit?.price ?: 0.0) * qty
    }

    Box(modifier = modifier.fillMaxSize().background(OnyxBlack)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Shop Selector & Delivery Info Banner
            item {
                if (currentShop != null) {
                    ShopSelectorBanner(
                        currentShop = currentShop,
                        allShops = shops,
                        onShopSelected = { viewModel.setCustomerShopId(it.id) }
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setFruitSearchQuery(it) },
                    placeholder = { Text("Search Seb, Santre, Aam... (ताज़ा फल खोजें)", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFruitSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_fruits")
                )
            }

            // Category Chips
            item {
                val categories = listOf("All", "Apples / सेब", "Citrus / संतरे", "Bananas / केले", "Melons / तरबूज", "Seasonal / मौसमी")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurface,
                                labelColor = TextWhite
                            )
                        )
                    }
                }
            }

            // Filtered Fruits
            val filteredFruits = shopFruits.filter { fruit ->
                val matchesSearch = fruit.nameEn.contains(searchQuery, ignoreCase = true) ||
                        fruit.nameHi.contains(searchQuery, ignoreCase = true) ||
                        fruit.category.contains(searchQuery, ignoreCase = true)
                val matchesCat = selectedCategory == "All" || fruit.category.contains(selectedCategory.split("/").first().trim(), ignoreCase = true)
                matchesSearch && matchesCat
            }

            if (filteredFruits.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No fruits found matching your search.", color = TextMuted)
                        }
                    }
                }
            } else {
                items(filteredFruits, key = { it.id }) { fruit ->
                    val quantityInCart = cart[fruit.id] ?: 0
                    CustomerFruitCard(
                        fruit = fruit,
                        quantityInCart = quantityInCart,
                        onAddToCart = { viewModel.addToCart(fruit.id) },
                        onDecrementCart = { viewModel.decrementCart(fruit.id) }
                    )
                }
            }
        }

        // Floating Bottom Cart Bar
        AnimatedVisibility(
            visible = totalCartItems > 0,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                color = EmeraldPrimary,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setShowCartSheet(true) }
                    .testTag("floating_cart_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$totalCartItems", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Column {
                            Text("₹${totalCartAmount.toInt()}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Tap to Checkout • असंध डिलीवरी", color = ForestGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View Cart", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Checkout Modal Sheet
    if (showCartSheet && currentShop != null) {
        CheckoutSheet(
            shop = currentShop,
            cart = cart,
            fruits = fruits,
            onDismiss = { viewModel.setShowCartSheet(false) },
            onIncrement = { viewModel.addToCart(it) },
            onDecrement = { viewModel.decrementCart(it) },
            onPlaceOrder = { name, phone, address, notes ->
                viewModel.placeCustomerOrder(name, phone, address, notes)
            }
        )
    }
}

@Composable
private fun ShopSelectorBanner(
    currentShop: Shop,
    allShops: List<Shop>,
    onShopSelected: (Shop) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ForestGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏪", fontSize = 22.sp)
                    }

                    Column {
                        Text(
                            text = currentShop.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "📍 ${currentShop.address} • 📞 ${currentShop.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightGreen,
                            fontSize = 11.sp
                        )
                    }
                }

                Box {
                    FilledTonalButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkSurfaceVariant, contentColor = TextWhite),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        allShops.forEach { shop ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(shop.name, color = TextWhite, fontWeight = FontWeight.Bold)
                                        Text(shop.address, color = TextMuted, fontSize = 10.sp)
                                    }
                                },
                                onClick = {
                                    onShopSelected(shop)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Delivery and Payment Mode Info Tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Delivery: 15-25 mins in Assandh",
                    color = TextLightGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (currentShop.paymentMode == PaymentMode.ONLINE_FIRST) "💳 Online First" else "💵 Cash / COD Available",
                    color = GoldRent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CustomerFruitCard(
    fruit: Fruit,
    quantityInCart: Int,
    onAddToCart: () -> Unit,
    onDecrementCart: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_fruit_card_${fruit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fruit Graphic Badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(fruit.emojiIcon, fontSize = 30.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fruit.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fruit.nameHi,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLightGreen
                    )
                }

                Text(
                    text = fruit.description.ifBlank { "Fresh farm picked fruit" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₹${fruit.price.toInt()} / ${fruit.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPrimary
                    )

                    if (!fruit.isInStock) {
                        Surface(
                            color = StatusDueBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Sold Out",
                                color = StatusDueRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Cart Actions
            if (!fruit.isInStock) {
                Text("Out of Stock", color = TextMuted, fontSize = 11.sp)
            } else if (quantityInCart == 0) {
                Button(
                    onClick = onAddToCart,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_add_to_cart_${fruit.id}")
                ) {
                    Text("ADD +", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onDecrementCart,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextWhite, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "$quantityInCart",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onAddToCart,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutSheet(
    shop: Shop,
    cart: Map<String, Int>,
    fruits: List<Fruit>,
    onDismiss: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onPlaceOrder: (name: String, phone: String, address: String, notes: String?) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val cartItems = cart.mapNotNull { (fruitId, qty) ->
        val fruit = fruits.find { it.id == fruitId } ?: return@mapNotNull null
        Triple(fruit, qty, fruit.price * qty)
    }

    val totalAmount = cartItems.sumOf { it.third }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                Text("Order Checkout • असंध", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
                Text("Ordering from: ${shop.name}", style = MaterialTheme.typography.bodySmall, color = EmeraldLight)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item list
                item {
                    Text("Your Fresh Fruit Cart:", color = TextMuted, fontSize = 12.sp)
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cartItems.forEach { (fruit, qty, total) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${fruit.emojiIcon} ${fruit.nameEn}", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("₹${fruit.price.toInt()} x $qty ${fruit.unit}", color = TextMuted, fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onDecrement(fruit.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                        }
                                        Text("$qty", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        IconButton(onClick = { onIncrement(fruit.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("₹${total.toInt()}", color = EmeraldPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }

                            Divider(color = DarkBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Bill (बिल):", color = TextWhite, fontWeight = FontWeight.Bold)
                                Text("₹${totalAmount.toInt()}", color = StatusPaidGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Delivery Info Inputs
                item {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Your Name (नाम)") },
                        placeholder = { Text("e.g. Ramesh Kumar") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_name")
                    )
                }

                item {
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Mobile Number (मोबाइल)") },
                        placeholder = { Text("e.g. 98123 44556") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_phone")
                    )
                }

                item {
                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("Delivery Address (असंध का पता)") },
                        placeholder = { Text("Ward No, Street, Landmark, Assandh") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_address")
                    )
                }

                // Payment mode badge (Shopkeeper's active preference)
                item {
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = GoldRent, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Payment Mode: ${shop.paymentMode.titleEn}",
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = shop.paymentMode.description,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                if (error != null) {
                    item {
                        Text(error!!, color = StatusDueRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customerName.isBlank() || customerPhone.isBlank() || customerAddress.isBlank()) {
                        error = "Please enter name, mobile number and Assandh delivery address."
                    } else {
                        onPlaceOrder(customerName, customerPhone, customerAddress, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("btn_confirm_place_order")
            ) {
                Text("Confirm Order (₹${totalAmount.toInt()})", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun OrderTrackingView(
    order: Order,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button & Order ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text(
                text = "Order Tracking • ${order.id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Surface(
                color = ForestGreen,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Live Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, ForestGreen))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (order.status) {
                                OrderStatus.RECEIVED -> "📝"
                                OrderStatus.PACKING -> "📦"
                                OrderStatus.OUT_FOR_DELIVERY -> "🛵"
                                OrderStatus.DELIVERED -> "🎉"
                            },
                            fontSize = 24.sp
                        )
                    }

                    Column {
                        Text(
                            text = if (order.status == OrderStatus.DELIVERED) "My Order is here! 🎉" else "Status: ${order.status.titleEn}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                        Text(
                            text = "${order.status.titleHi} • ${order.status.subtext}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLightGreen
                        )
                    }
                }

                Divider(color = DarkBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("From Shop:", color = TextMuted, fontSize = 11.sp)
                        Text(order.shopName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Delivery Time:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            if (order.status == OrderStatus.DELIVERED) "Delivered!" else "15-20 Mins",
                            color = GoldRent,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 4-Step Animated Tracking Pipeline
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Live Order Progression (Assandh Delivery):",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )

                val steps = OrderStatus.values()
                steps.forEachIndexed { index, step ->
                    val isPastOrCurrent = order.status.stepIndex >= step.stepIndex
                    val isCurrent = order.status == step

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> EmeraldPrimary
                                        isPastOrCurrent -> ForestGreen
                                        else -> DarkSurfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPastOrCurrent) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.Black else TextLightGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("${index + 1}", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${step.titleEn} (${step.titleHi})",
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                color = if (isPastOrCurrent) TextWhite else TextMuted,
                                fontSize = 13.sp
                            )
                            Text(
                                text = step.subtext,
                                color = if (isCurrent) EmeraldLight else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Order Details Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Order Details", fontWeight = FontWeight.Bold, color = TextWhite)
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity} ${item.unit} x ${item.fruitName}", color = TextMuted, fontSize = 12.sp)
                        Text("₹${item.total.toInt()}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Divider(color = DarkBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Paid / Due:", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("₹${order.totalAmount.toInt()}", color = StatusPaidGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text("📍 Delivering to: ${order.customerAddress}", color = TextLightGreen, fontSize = 11.sp)
            }
        }

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextWhite),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Back to Store", fontWeight = FontWeight.Bold)
        }
    }
}
