package com.example.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.model.RentRecord
import com.example.model.RentStatus
import com.example.model.Shop
import com.example.ui.AfruteViewModel
import com.example.ui.OwnerTab
import com.example.ui.theme.*

@Composable
fun OwnerDashboardScreen(
    viewModel: AfruteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    val rentLedger by viewModel.rentLedger.collectAsState()
    val selectedTab by viewModel.selectedOwnerTab.collectAsState()
    val searchQuery by viewModel.shopSearchQuery.collectAsState()
    val rentFilter by viewModel.shopFilterPaid.collectAsState()

    val showOnboardDialog by viewModel.showOnboardDialog.collectAsState()
    val showGenerateKeyDialog by viewModel.showGenerateKeyDialog.collectAsState()
    val generatedKey by viewModel.generatedNewKey.collectAsState()

    // Derived Counters
    val totalShops = shops.size
    val totalRentCollected = rentLedger.filter { it.status == RentStatus.PAID }.sumOf { it.amount }
    val totalActive = shops.count { it.rentStatus == RentStatus.PAID }
    val totalDue = shops.count { it.rentStatus == RentStatus.DUE }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Owner Profile & Quick Action Row
        item {
            OwnerProfileCard(
                onCopyShareLink = { viewModel.copyAndShareAppLink(context) },
                onOnboardClick = { viewModel.setShowOnboardDialog(true) },
                onGenerateKeyClick = { viewModel.openGenerateKeyDialog() }
            )
        }

        // 4 Live Counters
        item {
            CountersGrid(
                totalShops = totalShops,
                totalRentCollected = totalRentCollected,
                totalActive = totalActive,
                totalDue = totalDue
            )
        }

        // 3 SaaS Tabs: Shops | Rent Ledger | Analytics
        item {
            OwnerTabsRow(
                selectedTab = selectedTab,
                shopsCount = totalShops,
                ledgerCount = rentLedger.size,
                onTabSelected = { viewModel.selectOwnerTab(it) }
            )
        }

        // Tab Content
        when (selectedTab) {
            OwnerTab.SHOPS -> {
                item {
                    ShopSearchAndFilterBar(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setShopSearchQuery(it) },
                        selectedFilter = rentFilter,
                        onFilterSelected = { viewModel.setShopFilterPaid(it) }
                    )
                }

                val filteredShops = shops.filter { shop ->
                    val matchesSearch = shop.name.contains(searchQuery, ignoreCase = true) ||
                            shop.ownerName.contains(searchQuery, ignoreCase = true) ||
                            shop.activationKey.contains(searchQuery, ignoreCase = true) ||
                            shop.address.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = rentFilter == null || shop.rentStatus == rentFilter
                    matchesSearch && matchesFilter
                }

                if (filteredShops.isEmpty()) {
                    item {
                        EmptyStateCard("No shops found matching filter")
                    }
                } else {
                    items(filteredShops, key = { it.id }) { shop ->
                        ShopCard(
                            shop = shop,
                            onCollectRent = { viewModel.collectRent(shop.id) },
                            onRemind = { viewModel.sendRentReminder(context, shop) },
                            onCopyKey = { viewModel.copyToClipboard(context, shop.activationKey, "Activation Key") },
                            onManageAsShopkeeper = { viewModel.openShopkeeperForShop(shop) }
                        )
                    }
                }
            }

            OwnerTab.RENT_LEDGER -> {
                item {
                    RentLedgerHeaderCard(totalRentCollected = totalRentCollected)
                }

                items(rentLedger, key = { it.id }) { record ->
                    RentLedgerCard(
                        record = record,
                        onCollectNow = { viewModel.collectRent(record.shopId) }
                    )
                }
            }

            OwnerTab.ANALYTICS -> {
                item {
                    AnalyticsView(
                        totalShops = totalShops,
                        activeShops = totalActive,
                        dueShops = totalDue,
                        totalRevenue = totalRentCollected
                    )
                }
            }
        }
    }

    // Onboard Shop Modal
    if (showOnboardDialog) {
        OnboardShopDialog(
            nextKey = viewModel.shops.value.let { "AFRUTE-KEY-${String.format("%03d", it.size + 1)}" },
            onDismiss = { viewModel.setShowOnboardDialog(false) },
            onConfirm = { name, owner, phone, address ->
                viewModel.onboardShop(name, owner, phone, address)
            }
        )
    }

    // Generate Key Modal
    if (showGenerateKeyDialog && generatedKey != null) {
        GenerateKeyDialog(
            generatedKey = generatedKey!!,
            onDismiss = { viewModel.dismissGenerateKeyDialog() },
            onCopy = { viewModel.copyToClipboard(context, generatedKey!!, "Shopkeeper Key") }
        )
    }
}

@Composable
private fun OwnerProfileCard(
    onCopyShareLink: () -> Unit,
    onOnboardClick: () -> Unit,
    onGenerateKeyClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, ForestGreen))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "Manjeet Virk (Assandh)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Platform Owner & Super Admin • Assandh SaaS",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldLight
                        )
                    }
                }

                Surface(
                    color = GoldRentBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "₹499/mo",
                        color = GoldRent,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Actions: Copy App Link, + Onboard, Generate Key
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopyShareLink,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = TextLightGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_copy_app_link")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onGenerateKeyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_generate_key")
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = "Key", modifier = Modifier.size(16.dp), tint = GoldRent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOnboardClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_onboard_shop")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Onboard", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Onboard", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun CountersGrid(
    totalShops: Int,
    totalRentCollected: Int,
    totalActive: Int,
    totalDue: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CounterCard(
                titleEn = "Total Shops",
                titleHi = "कुल दुकानें",
                value = "$totalShops",
                subtext = "Unlimited Scale",
                accentColor = EmeraldPrimary,
                icon = Icons.Default.Store,
                modifier = Modifier.weight(1f),
                testTag = "counter_total_shops"
            )
            CounterCard(
                titleEn = "Rent Collected",
                titleHi = "कुल किराया जमा",
                value = "₹$totalRentCollected",
                subtext = "₹499 per shop/mo",
                accentColor = GoldRent,
                icon = Icons.Default.CurrencyRupee,
                modifier = Modifier.weight(1f),
                testTag = "counter_rent_collected"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CounterCard(
                titleEn = "Active Shops",
                titleHi = "सक्रिय दुकानें",
                value = "$totalActive",
                subtext = "Access Enabled",
                accentColor = StatusPaidGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                testTag = "counter_active_shops"
            )
            CounterCard(
                titleEn = "Total Due",
                titleHi = "किराया बाकी",
                value = "$totalDue",
                subtext = if (totalDue > 0) "Needs Collection" else "All Cleared",
                accentColor = if (totalDue > 0) StatusDueRed else TextMuted,
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f),
                testTag = "counter_due_shops"
            )
        }
    }
}

@Composable
private fun CounterCard(
    titleEn: String,
    titleHi: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleEn,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = accentColor,
                fontSize = 22.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = titleHi,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLightGreen,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun OwnerTabsRow(
    selectedTab: OwnerTab,
    shopsCount: Int,
    ledgerCount: Int,
    onTabSelected: (OwnerTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OwnerTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val badgeText = when (tab) {
                OwnerTab.SHOPS -> " ($shopsCount)"
                OwnerTab.RENT_LEDGER -> " ($ledgerCount)"
                OwnerTab.ANALYTICS -> ""
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) EmeraldDark else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp)
                    .testTag("owner_subtab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${tab.titleEn}$badgeText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextWhite else TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = tab.titleHi,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) TextLightGreen else TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopSearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: RentStatus?,
    onFilterSelected: (RentStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search shop, owner, mobile or key...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
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
                .testTag("input_search_shops")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All Shops", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurface,
                    labelColor = TextWhite
                )
            )
            FilterChip(
                selected = selectedFilter == RentStatus.PAID,
                onClick = { onFilterSelected(RentStatus.PAID) },
                label = { Text("Paid Only", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusPaidGreen,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurface,
                    labelColor = TextWhite
                )
            )
            FilterChip(
                selected = selectedFilter == RentStatus.DUE,
                onClick = { onFilterSelected(RentStatus.DUE) },
                label = { Text("Due Only", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusDueRed,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurface,
                    labelColor = TextWhite
                )
            )
        }
    }
}

@Composable
private fun ShopCard(
    shop: Shop,
    onCollectRent: () -> Unit,
    onRemind: () -> Unit,
    onCopyKey: () -> Unit,
    onManageAsShopkeeper: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (shop.rentStatus == RentStatus.PAID) listOf(DarkBorder, ForestGreen)
                else listOf(DarkBorder, StatusDueBg)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_card_${shop.activationKey}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Shop Name & Rent Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Owner: ${shop.ownerName} • ${shop.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightGreen
                    )
                    Text(
                        text = "📍 ${shop.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Rent Status Badge
                Surface(
                    color = if (shop.rentStatus == RentStatus.PAID) StatusPaidBg else StatusDueBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (shop.rentStatus == RentStatus.PAID) StatusPaidGreen else StatusDueRed
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (shop.rentStatus == RentStatus.PAID) StatusPaidGreen else StatusDueRed)
                        )
                        Text(
                            text = if (shop.rentStatus == RentStatus.PAID) "PAID" else "DUE",
                            color = if (shop.rentStatus == RentStatus.PAID) StatusPaidGreen else StatusDueRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Shop Details Banner: KEY, Paid Till, Sold Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Key with 1-click copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(onClick = onCopyKey)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = GoldRent, modifier = Modifier.size(16.dp))
                    Text(
                        text = shop.activationKey,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldRent,
                        fontSize = 12.sp
                    )
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(14.dp))
                }

                // Paid Till
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (shop.rentStatus == RentStatus.PAID) "Paid till: ${shop.paidTillDate}" else "Expired: ${shop.paidTillDate}",
                        fontSize = 11.sp,
                        color = if (shop.rentStatus == RentStatus.PAID) TextWhite else StatusDueRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Plan ₹${shop.rentPlanAmount}/mo • ${shop.appSoldStatus}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            // Action Buttons: Collect ₹499, Remind, Manage Shop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collect Button
                Button(
                    onClick = onCollectRent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (shop.rentStatus == RentStatus.DUE) StatusPaidGreen else ForestGreen,
                        contentColor = if (shop.rentStatus == RentStatus.DUE) Color.Black else TextLightGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = "Collect", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (shop.rentStatus == RentStatus.DUE) "Collect ₹499" else "Renew ₹499",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Remind Button
                OutlinedButton(
                    onClick = onRemind,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldRent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldRent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Remind", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Switch to Shopkeeper view for this shop
                FilledTonalButton(
                    onClick = onManageAsShopkeeper,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Shop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun RentLedgerHeaderCard(totalRentCollected: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Afrute SaaS Rent Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Standard Monthly Plan: ₹499 / shop",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldRent
                )
                Text(
                    text = "Managed by Manjeet Virk (Assandh)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹$totalRentCollected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = StatusPaidGreen
                )
                Text(
                    text = "Total Collected",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLightGreen
                )
            }
        }
    }
}

@Composable
private fun RentLedgerCard(
    record: RentRecord,
    onCollectNow: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.shopName,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (record.status == RentStatus.PAID) StatusPaidBg else StatusDueBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (record.status == RentStatus.PAID) "PAID" else "DUE",
                            color = if (record.status == RentStatus.PAID) StatusPaidGreen else StatusDueRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Owner: ${record.ownerName} • Period: ${record.periodCovered}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "Txn ID: ${record.id} • ${record.date}",
                    color = TextLightGreen,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${record.amount}",
                    fontWeight = FontWeight.Black,
                    color = if (record.status == RentStatus.PAID) StatusPaidGreen else StatusDueRed,
                    fontSize = 16.sp
                )
                if (record.status == RentStatus.DUE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onCollectNow,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Collect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsView(
    totalShops: Int,
    activeShops: Int,
    dueShops: Int,
    totalRevenue: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SaaS Revenue & Performance (Assandh)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current MRR", color = TextMuted, fontSize = 12.sp)
                        Text("₹${activeShops * 499}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = EmeraldPrimary)
                    }
                    Column {
                        Text("Collection Rate", color = TextMuted, fontSize = 12.sp)
                        val rate = if (totalShops > 0) (activeShops * 100 / totalShops) else 0
                        Text("$rate%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = GoldRent)
                    }
                    Column {
                        Text("Annual Run Rate", color = TextMuted, fontSize = 12.sp)
                        Text("₹${activeShops * 499 * 12}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextLightGreen)
                    }
                }

                Divider(color = DarkBorder)

                Text(
                    text = "Assandh Market Penetration:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )

                // Visual progress bar of active vs due
                val activeFraction = if (totalShops > 0) activeShops.toFloat() / totalShops else 0f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(StatusDueRed)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(activeFraction)
                                .fillMaxHeight()
                                .background(EmeraldPrimary)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$activeShops Active (Green)", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$dueShops Due (Red)", color = StatusDueRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Onboarding Advice Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "💡 Growth Tip for Manjeet Virk (Assandh):",
                    fontWeight = FontWeight.Bold,
                    color = GoldRent,
                    fontSize = 13.sp
                )
                Text(
                    text = "Share the direct install link (https://afrute-assandh.app/install) with fruit vendors in Assandh Sabzi Mandi, Salwan Chowk, Safidon Road, and Jind Chowk. Each vendor onboarded brings ₹499/month recurring rent.",
                    color = TextWhite,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
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
            Text(message, color = TextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun OnboardShopDialog(
    nextKey: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, owner: String, phone: String, address: String) -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Assandh Mandi") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                Text(
                    text = "+ Onboard New Shop",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Rent Plan: ₹499/mo • Auto-Assigned Key: $nextKey",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldLight
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name (दुकान का नाम)") },
                    placeholder = { Text("e.g. Haryana Super Fruits") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_onboard_shop_name")
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name (मालिक का नाम)") },
                    placeholder = { Text("e.g. Jasbir Singh") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_onboard_owner_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (मोबाइल)") },
                    placeholder = { Text("e.g. 98123 00000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_onboard_phone")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop Location / Address (पता)") },
                    placeholder = { Text("e.g. Main Sabzi Mandi, Assandh") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_onboard_address")
                )

                if (error != null) {
                    Text(error!!, color = StatusDueRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shopName.isBlank() || ownerName.isBlank() || phone.isBlank()) {
                        error = "Please fill in shop name, owner, and mobile number."
                    } else {
                        onConfirm(shopName, ownerName, phone, address)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("btn_confirm_onboard")
            ) {
                Text("Activate Shop (₹499/mo)", fontWeight = FontWeight.Bold)
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
private fun GenerateKeyDialog(
    generatedKey: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "🔑 New Shopkeeper Key",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Share this activation key with a fruit vendor in Assandh. They can enter this key to open their fruit shop portal.",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldRent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ACTIVATION KEY", color = TextMuted, fontSize = 10.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = generatedKey,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = GoldRent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Rent: ₹499/month", color = TextLightGreen, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldRent, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy & Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TextMuted)
            }
        }
    )
}
