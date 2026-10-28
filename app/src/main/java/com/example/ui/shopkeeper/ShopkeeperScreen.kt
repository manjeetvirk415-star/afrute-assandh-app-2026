package com.example.ui.shopkeeper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.AfruteViewModel
import com.example.ui.ShopkeeperTab
import com.example.ui.theme.*

@Composable
fun ShopkeeperScreen(
    viewModel: AfruteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    val fruits by viewModel.fruits.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val activeKey by viewModel.activeShopKey.collectAsState()
    val selectedTab by viewModel.selectedShopkeeperTab.collectAsState()

    val currentShop = shops.find { it.activationKey.equals(activeKey, ignoreCase = true) }
    val showAddFruitDialog by viewModel.showAddFruitDialog.collectAsState()
    val editingFruit by viewModel.editingFruit.collectAsState()

    if (currentShop == null) {
        // Activation / Key Login Screen
        ShopkeeperLoginView(
            shops = shops,
            onLogin = { key -> viewModel.loginShopkeeperWithKey(key) }
        )
    } else {
        // Active Shopkeeper Dashboard
        val shopFruits = fruits.filter { it.shopId == currentShop.id }
        val shopOrders = orders.filter { it.shopId == currentShop.id }

        Scaffold(
            containerColor = OnyxBlack,
            floatingActionButton = {
                if (selectedTab == ShopkeeperTab.CATALOG) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.setShowAddFruitDialog(true) },
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Fruit") },
                        text = { Text("Add Fruit", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("fab_add_fruit")
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shop Header & Rent Banner
                item {
                    ShopkeeperHeaderCard(
                        shop = currentShop,
                        onLogout = { viewModel.logoutShopkeeper() },
                        onCopyKey = { viewModel.copyToClipboard(context, currentShop.activationKey, "Shop Key") }
                    )
                }

                // Order Settings: Payment Mode Toggle
                item {
                    OrderSettingsCard(
                        currentMode = currentShop.paymentMode,
                        onModeChanged = { newMode ->
                            viewModel.updateShopPaymentMode(currentShop.id, newMode)
                        }
                    )
                }

                // Sub-tabs: Fruit Catalog vs Live Orders
                item {
                    ShopkeeperSubTabsRow(
                        selectedTab = selectedTab,
                        fruitCount = shopFruits.size,
                        orderCount = shopOrders.size,
                        onTabSelected = { viewModel.selectShopkeeperTab(it) }
                    )
                }

                // Content for selected subtab
                when (selectedTab) {
                    ShopkeeperTab.CATALOG -> {
                        if (shopFruits.isEmpty()) {
                            item {
                                EmptyCatalogCard(onAddFruitClick = { viewModel.setShowAddFruitDialog(true) })
                            }
                        } else {
                            items(shopFruits, key = { it.id }) { fruit ->
                                ShopFruitCard(
                                    fruit = fruit,
                                    onEdit = { viewModel.setEditingFruit(fruit) },
                                    onDelete = { viewModel.deleteFruit(fruit.id) }
                                )
                            }
                        }
                    }

                    ShopkeeperTab.ORDERS -> {
                        if (shopOrders.isEmpty()) {
                            item {
                                EmptyOrdersCard()
                            }
                        } else {
                            items(shopOrders, key = { it.id }) { order ->
                                ShopkeeperOrderCard(
                                    order = order,
                                    onUpdateStatus = { status ->
                                        viewModel.updateOrderStatus(order.id, status)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Fruit Modal Dialog
    if (showAddFruitDialog && currentShop != null) {
        AddFruitDialog(
            onDismiss = { viewModel.setShowAddFruitDialog(false) },
            onConfirm = { nameEn, nameHi, category, price, unit, stockQty, desc, emoji ->
                viewModel.addFruit(
                    shopId = currentShop.id,
                    nameEn = nameEn,
                    nameHi = nameHi,
                    category = category,
                    price = price,
                    unit = unit,
                    stockQty = stockQty,
                    description = desc,
                    emojiIcon = emoji
                )
            }
        )
    }

    // Edit Fruit Price & Stock Dialog
    if (editingFruit != null) {
        EditFruitDialog(
            fruit = editingFruit!!,
            onDismiss = { viewModel.setEditingFruit(null) },
            onConfirm = { newPrice, newStock, isInStock ->
                viewModel.updateFruit(editingFruit!!.id, newPrice, newStock, isInStock)
            }
        )
    }
}

@Composable
private fun ShopkeeperLoginView(
    shops: List<Shop>,
    onLogin: (String) -> Boolean
) {
    var keyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(ForestGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Storefront, contentDescription = "Shop Login", tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
        }

        Text(
            text = "Shopkeeper Portal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = TextWhite
        )
        Text(
            text = "Enter your Unique Shop Key to activate and manage your Assandh fruit shop",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 18.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("Shop Activation Key (e.g. AFRUTE-KEY-001)") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldRent) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_shopkeeper_key")
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = StatusDueRed, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (keyInput.isBlank()) {
                            errorMessage = "Please enter your activation key"
                        } else {
                            val success = onLogin(keyInput)
                            if (!success) {
                                errorMessage = "Key not recognized. Try one of the test keys below."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_activate_key")
                ) {
                    Text("Activate Shop Portal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Quick Test Keys
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚡ Quick Demo Keys (Click to auto-load):",
                color = TextLightGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )

            shops.forEach { shop ->
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            keyInput = shop.activationKey
                            onLogin(shop.activationKey)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(shop.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Owner: ${shop.ownerName} • ${shop.address}", color = TextMuted, fontSize = 10.sp)
                        }
                        Surface(
                            color = ForestGreen,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = shop.activationKey,
                                color = GoldRent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopkeeperHeaderCard(
    shop: Shop,
    onLogout: () -> Unit,
    onCopyKey: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextWhite
                    )
                    Text(
                        text = "Owner: ${shop.ownerName} • 📞 ${shop.phone}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLightGreen
                    )
                    Text(
                        text = "📍 ${shop.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Switch Shop", tint = TextMuted)
                }
            }

            // Key and Rent Status Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(onClick = onCopyKey)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldRent, modifier = Modifier.size(16.dp))
                    Text(shop.activationKey, color = GoldRent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Surface(
                    color = if (shop.rentStatus == RentStatus.PAID) StatusPaidBg else StatusDueBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (shop.rentStatus == RentStatus.PAID) "Rent Paid (Till ${shop.paidTillDate})" else "Rent DUE (Pay ₹499)",
                        color = if (shop.rentStatus == RentStatus.PAID) StatusPaidGreen else StatusDueRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSettingsCard(
    currentMode: PaymentMode,
    onModeChanged: (PaymentMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Order Payment Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Surface(
                    color = ForestGreen,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Shopkeeper Toggle",
                        color = TextLightGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Choose how customers pay when ordering fruits from your shop:",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )

            // Two-Way Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentModeOption(
                    title = "Online First",
                    sub = "UPI / QR Code",
                    isSelected = currentMode == PaymentMode.ONLINE_FIRST,
                    onClick = { onModeChanged(PaymentMode.ONLINE_FIRST) },
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_payment_online_first"
                )
                PaymentModeOption(
                    title = "Pay After Delivery",
                    sub = "Cash / COD",
                    isSelected = currentMode == PaymentMode.PAY_AFTER_DELIVERY,
                    onClick = { onModeChanged(PaymentMode.PAY_AFTER_DELIVERY) },
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_payment_cod"
                )
            }
        }
    }
}

@Composable
private fun PaymentModeOption(
    title: String,
    sub: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) Color.Black else TextWhite,
                fontSize = 12.sp
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) ForestGreen else TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ShopkeeperSubTabsRow(
    selectedTab: ShopkeeperTab,
    fruitCount: Int,
    orderCount: Int,
    onTabSelected: (ShopkeeperTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ShopkeeperTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val countText = if (tab == ShopkeeperTab.CATALOG) " ($fruitCount)" else " ($orderCount)"

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) EmeraldDark else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp)
                    .testTag("shopkeeper_tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (tab == ShopkeeperTab.CATALOG) Icons.Default.Category else Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = if (isSelected) TextWhite else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${tab.titleEn}$countText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextWhite else TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopFruitCard(
    fruit: Fruit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_fruit_${fruit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fruit Visual Icon / Badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(fruit.emojiIcon, fontSize = 28.sp)
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
                    text = fruit.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₹${fruit.price.toInt()} / ${fruit.unit}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPrimary
                    )
                    Surface(
                        color = if (fruit.isInStock) ForestGreen else StatusDueBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (fruit.isInStock) "${fruit.stockQty} ${fruit.unit} in stock" else "Out of Stock",
                            color = if (fruit.isInStock) TextLightGreen else StatusDueRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Quick Edit & Delete Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = GoldRent, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusDueRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ShopkeeperOrderCard(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (order.status == OrderStatus.DELIVERED) listOf(DarkBorder, ForestGreen)
                else listOf(DarkBorder, GoldRentBg)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_order_${order.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: ID, Customer, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = order.id,
                            fontWeight = FontWeight.Black,
                            color = GoldRent,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${order.placedAt}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Customer: ${order.customerName} (📞 ${order.customerPhone})",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "📍 ${order.customerAddress}",
                        color = TextLightGreen,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${order.totalAmount.toInt()}",
                        fontWeight = FontWeight.Black,
                        color = EmeraldPrimary,
                        fontSize = 18.sp
                    )
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (order.paymentMode == PaymentMode.ONLINE_FIRST) "Online Paid" else "Cash On Delivery",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Items breakdown
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${item.fruitName} (${item.quantity} ${item.unit})", color = TextWhite, fontSize = 11.sp)
                            Text("₹${item.total.toInt()}", color = TextLightGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Order Status Pipeline Buttons: Received -> Packing -> Out for Delivery -> Delivered
            Text(
                text = "Update Order Status (Live Customer Tracking):",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OrderStatus.values().forEach { status ->
                    val isCurrent = order.status == status
                    val isPast = order.status.stepIndex >= status.stepIndex

                    Button(
                        onClick = { onUpdateStatus(status) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isCurrent -> EmeraldPrimary
                                isPast -> ForestGreen
                                else -> DarkSurfaceVariant
                            },
                            contentColor = when {
                                isCurrent -> Color.Black
                                isPast -> TextLightGreen
                                else -> TextMuted
                            }
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("btn_order_${order.id}_${status.name.lowercase()}")
                    ) {
                        Text(
                            text = status.titleEn,
                            fontSize = 9.sp,
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogCard(onAddFruitClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🍉", fontSize = 42.sp)
            Text("No Fruits in Your Shop Catalog", color = TextWhite, fontWeight = FontWeight.Bold)
            Text("Start adding unlimited fruits with custom prices and stock.", color = TextMuted, fontSize = 12.sp)
            Button(
                onClick = onAddFruitClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text("+ Add First Fruit", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyOrdersCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛎️", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Incoming Orders Yet", color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Orders placed by customers will appear here in real time.", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AddFruitDialog(
    onDismiss: () -> Unit,
    onConfirm: (nameEn: String, nameHi: String, category: String, price: Double, unit: String, stockQty: Int, desc: String, emoji: String) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameHi by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("50") }
    var unit by remember { mutableStateOf("kg") }
    var category by remember { mutableStateOf("Fresh Fruits / फल") }
    var description by remember { mutableStateOf("Directly procured from Assandh mandi") }
    var selectedEmoji by remember { mutableStateOf("🍎") }
    var error by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("🍎", "🍏", "🍊", "🍌", "🥭", "🍇", "🍉", "🍍", "🍓", "🫐", "🥝", "🥑", "🍋", "🍒", "🍐")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("+ Add Unlimited Fruit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Select Fruit Icon / Photo:", color = TextMuted, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        emojis.take(7).forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedEmoji == emoji) ForestGreen else DarkSurfaceVariant)
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("English Name (e.g. Kashmiri Apple)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("input_fruit_name_en")
                    )
                }

                item {
                    OutlinedTextField(
                        value = nameHi,
                        onValueChange = { nameHi = it },
                        label = { Text("Hindi Name (e.g. कश्मीरी सेब)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("input_fruit_name_hi")
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Price ₹") },
                            placeholder = { Text("140") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("input_fruit_price")
                        )

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("kg / dozen") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    val price = priceText.toDoubleOrNull()
                    val stock = stockText.toIntOrNull() ?: 50
                    if (nameEn.isBlank() || price == null) {
                        error = "Please enter valid fruit name and price"
                    } else {
                        onConfirm(nameEn, nameHi.ifBlank { nameEn }, category, price, unit, stock, description, selectedEmoji)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("btn_confirm_add_fruit")
            ) {
                Text("Add Fruit", fontWeight = FontWeight.Bold)
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
private fun EditFruitDialog(
    fruit: Fruit,
    onDismiss: () -> Unit,
    onConfirm: (newPrice: Double, newStock: Int, isInStock: Boolean) -> Unit
) {
    var priceText by remember { mutableStateOf(fruit.price.toInt().toString()) }
    var stockText by remember { mutableStateOf(fruit.stockQty.toString()) }
    var inStock by remember { mutableStateOf(fruit.isInStock) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Edit ${fruit.nameEn}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (₹ per ${fruit.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Stock Quantity (${fruit.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("In Stock Toggle", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = inStock,
                        onCheckedChange = { inStock = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary, checkedTrackColor = ForestGreen)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: fruit.price
                    val stock = stockText.toIntOrNull() ?: fruit.stockQty
                    onConfirm(price, stock, inStock)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
