package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRole
import com.example.ui.theme.*

@Composable
fun TopHeader(
    currentRole: AppRole,
    onRoleSelected: (AppRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_header_surface"),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Main Branding Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(EmeraldPrimary, ForestGreen)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🍏",
                            fontSize = 22.sp
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AFRUTE ASSANDH",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = EmeraldDark,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SaaS",
                                    color = TextLightGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Multi-Tenant Fruit App • Manjeet Virk (Assandh)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightGreen,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Role Switcher Tabs (3 Distinct Roles)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoleTabButton(
                    title = "Owner",
                    sub = "मालिक",
                    icon = Icons.Default.AdminPanelSettings,
                    isSelected = currentRole == AppRole.OWNER,
                    onClick = { onRoleSelected(AppRole.OWNER) },
                    modifier = Modifier.weight(1f),
                    testTag = "role_tab_owner"
                )
                RoleTabButton(
                    title = "Shopkeeper",
                    sub = "दुकानदार",
                    icon = Icons.Default.Storefront,
                    isSelected = currentRole == AppRole.SHOPKEEPER,
                    onClick = { onRoleSelected(AppRole.SHOPKEEPER) },
                    modifier = Modifier.weight(1f),
                    testTag = "role_tab_shopkeeper"
                )
                RoleTabButton(
                    title = "Customer",
                    sub = "ग्राहक",
                    icon = Icons.Default.LocalMall,
                    isSelected = currentRole == AppRole.CUSTOMER,
                    onClick = { onRoleSelected(AppRole.CUSTOMER) },
                    modifier = Modifier.weight(1f),
                    testTag = "role_tab_customer"
                )
            }
        }
    }
}

@Composable
private fun RoleTabButton(
    title: String,
    sub: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.Black else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isSelected) Color.Black else TextWhite,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) ForestGreen else TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}
