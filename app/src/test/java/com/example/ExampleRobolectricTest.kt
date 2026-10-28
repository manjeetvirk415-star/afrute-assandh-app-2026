package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AfruteRepository
import com.example.model.OrderStatus
import com.example.model.PaymentMode
import com.example.model.RentStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Afrute Assandh", appName)
    }

    @Test
    fun `repository initializes with five dummy shops and Virk Fresh Fruits`() {
        val repo = AfruteRepository.getInstance()
        val shops = repo.shops.value
        assertTrue(shops.size >= 5)

        val shop1 = shops.find { it.activationKey == "AFRUTE-KEY-001" }
        assertNotNull(shop1)
        assertEquals("Virk Fresh Fruits Assandh", shop1?.name)
        assertEquals("Manjeet Virk", shop1?.ownerName)
        assertEquals(RentStatus.PAID, shop1?.rentStatus)
    }

    @Test
    fun `onboard new shop generates unique key and records rent`() {
        val repo = AfruteRepository.getInstance()
        val initialShopCount = repo.shops.value.size
        val newShop = repo.onboardShop(
            name = "Test Orchard Assandh",
            ownerName = "Harpreet Singh",
            phone = "98123 99999",
            address = "Karnal Road, Assandh"
        )

        assertEquals(initialShopCount + 1, repo.shops.value.size)
        assertTrue(newShop.activationKey.startsWith("AFRUTE-KEY-"))
        assertEquals(RentStatus.PAID, newShop.rentStatus)

        // Rent ledger contains entry
        val ledger = repo.rentLedger.value
        val hasEntry = ledger.any { it.shopId == newShop.id && it.amount == 499 }
        assertTrue(hasEntry)
    }

    @Test
    fun `order status pipeline updates live status`() {
        val repo = AfruteRepository.getInstance()
        val orders = repo.orders.value
        val firstOrder = orders.first()

        repo.updateOrderStatus(firstOrder.id, OrderStatus.DELIVERED)
        val updated = repo.getOrderById(firstOrder.id)
        assertEquals(OrderStatus.DELIVERED, updated?.status)
    }
}
