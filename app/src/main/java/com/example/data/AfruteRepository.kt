package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class AfruteRepository private constructor() {

    private val _shops = MutableStateFlow<List<Shop>>(createInitialShops())
    val shops: StateFlow<List<Shop>> = _shops.asStateFlow()

    private val _fruits = MutableStateFlow<List<Fruit>>(createInitialFruits())
    val fruits: StateFlow<List<Fruit>> = _fruits.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(createInitialOrders())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _rentLedger = MutableStateFlow<List<RentRecord>>(createInitialRentLedger())
    val rentLedger: StateFlow<List<RentRecord>> = _rentLedger.asStateFlow()

    private val _activeShopKey = MutableStateFlow<String?>("AFRUTE-KEY-001")
    val activeShopKey: StateFlow<String?> = _activeShopKey.asStateFlow()

    private val _currentRole = MutableStateFlow<AppRole>(AppRole.OWNER)
    val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    private val _lastPlacedOrderId = MutableStateFlow<String?>("ORD-1001")
    val lastPlacedOrderId: StateFlow<String?> = _lastPlacedOrderId.asStateFlow()

    fun setRole(role: AppRole) {
        _currentRole.value = role
    }

    fun setActiveShopKey(key: String?) {
        _activeShopKey.value = key
    }

    fun getShopByKey(key: String): Shop? {
        return _shops.value.find { it.activationKey.equals(key.trim(), ignoreCase = true) }
    }

    fun getShopById(id: String): Shop? {
        return _shops.value.find { it.id == id }
    }

    fun generateUniqueKey(): String {
        val count = _shops.value.size + 1
        return String.format(Locale.US, "AFRUTE-KEY-%03d", count)
    }

    fun onboardShop(name: String, ownerName: String, phone: String, address: String): Shop {
        val nextKey = generateUniqueKey()
        val newShop = Shop(
            id = "shop-${UUID.randomUUID()}",
            name = name.trim(),
            ownerName = ownerName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            activationKey = nextKey,
            isActive = true,
            daysActive = 1,
            rentStatus = RentStatus.PAID,
            rentPlanAmount = 499,
            paidTillDate = getFutureDate(30),
            appSoldStatus = "Active - Licensed",
            paymentMode = PaymentMode.PAY_AFTER_DELIVERY
        )
        _shops.value = listOf(newShop) + _shops.value

        // Record initial onboarding rent payment
        val rentRecord = RentRecord(
            id = "RENT-${UUID.randomUUID().toString().take(6).uppercase()}",
            shopId = newShop.id,
            shopName = newShop.name,
            ownerName = newShop.ownerName,
            amount = 499,
            date = getCurrentDateString(),
            periodCovered = "${getCurrentDateString()} - ${newShop.paidTillDate}",
            status = RentStatus.PAID,
            collectedBy = "Manjeet Virk (Assandh)"
        )
        _rentLedger.value = listOf(rentRecord) + _rentLedger.value

        // Populate starter fruits for this new shop
        val starterFruits = listOf(
            Fruit(
                id = "fruit-${UUID.randomUUID()}",
                shopId = newShop.id,
                nameEn = "Kashmiri Seb",
                nameHi = "कश्मीरी सेब",
                category = "Apples / सेब",
                price = 140.0,
                unit = "kg",
                stockQty = 30,
                isInStock = true,
                description = "Fresh juicy Kashmiri royal apples",
                emojiIcon = "🍎"
            ),
            Fruit(
                id = "fruit-${UUID.randomUUID()}",
                shopId = newShop.id,
                nameEn = "Nagpur Santre",
                nameHi = "नागपुर संतरे",
                category = "Citrus / संतरे",
                price = 70.0,
                unit = "kg",
                stockQty = 40,
                isInStock = true,
                description = "Sweet and sour Nagpur fresh oranges",
                emojiIcon = "🍊"
            ),
            Fruit(
                id = "fruit-${UUID.randomUUID()}",
                shopId = newShop.id,
                nameEn = "Robusta Kela",
                nameHi = "केला",
                category = "Bananas / केले",
                price = 50.0,
                unit = "dozen",
                stockQty = 25,
                isInStock = true,
                description = "Naturally ripened yellow bananas",
                emojiIcon = "🍌"
            )
        )
        _fruits.value = _fruits.value + starterFruits

        return newShop
    }

    fun collectRent(shopId: String) {
        val target = _shops.value.find { it.id == shopId } ?: return
        val newPaidTill = getFutureDate(30)
        _shops.value = _shops.value.map { shop ->
            if (shop.id == shopId) {
                shop.copy(
                    rentStatus = RentStatus.PAID,
                    paidTillDate = newPaidTill,
                    appSoldStatus = "Active - Licensed"
                )
            } else shop
        }

        val record = RentRecord(
            id = "RENT-${UUID.randomUUID().toString().take(6).uppercase()}",
            shopId = target.id,
            shopName = target.name,
            ownerName = target.ownerName,
            amount = 499,
            date = getCurrentDateString(),
            periodCovered = "${getCurrentDateString()} - $newPaidTill",
            status = RentStatus.PAID,
            collectedBy = "Manjeet Virk (Assandh)"
        )
        _rentLedger.value = listOf(record) + _rentLedger.value
    }

    fun sendRentReminder(context: Context, shop: Shop) {
        val message = "Namaste ${shop.ownerName} ji (${shop.name}), your Afrute SaaS monthly platform subscription of ₹499 is due for renewal. Please submit to Manjeet Virk (Assandh) to keep your shop live. Key: ${shop.activationKey}."
        try {
            val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=91${shop.phone.replace(" ", "")}&text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
        } catch (_: Exception) {
            // Fallback to general share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Remind ${shop.ownerName} via"))
        }
    }

    fun copyAndShareAppLink(context: Context) {
        val link = "https://afrute-assandh.app/install"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Afrute App Link", link)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "App Link Copied: $link", Toast.LENGTH_SHORT).show()

        val shareText = "Download Afrute Assandh - Multi-Tenant Fruit App for Assandh!\n$link\nOwner: Manjeet Virk (Assandh) | Rent ₹499/mo"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Afrute App Link"))
        } catch (_: Exception) { }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun updateShopPaymentMode(shopId: String, mode: PaymentMode) {
        _shops.value = _shops.value.map {
            if (it.id == shopId) it.copy(paymentMode = mode) else it
        }
    }

    fun addFruit(
        shopId: String,
        nameEn: String,
        nameHi: String,
        category: String,
        price: Double,
        unit: String,
        stockQty: Int,
        description: String,
        emojiIcon: String,
        photoUri: String? = null
    ) {
        val fruit = Fruit(
            id = "fruit-${UUID.randomUUID()}",
            shopId = shopId,
            nameEn = nameEn.trim(),
            nameHi = nameHi.trim(),
            category = category,
            price = price,
            unit = unit,
            stockQty = stockQty,
            isInStock = stockQty > 0,
            description = description.trim(),
            emojiIcon = emojiIcon,
            photoUri = photoUri
        )
        _fruits.value = listOf(fruit) + _fruits.value
    }

    fun updateFruit(
        fruitId: String,
        price: Double,
        stockQty: Int,
        isInStock: Boolean
    ) {
        _fruits.value = _fruits.value.map {
            if (it.id == fruitId) {
                it.copy(price = price, stockQty = stockQty, isInStock = isInStock)
            } else it
        }
    }

    fun deleteFruit(fruitId: String) {
        _fruits.value = _fruits.value.filter { it.id != fruitId }
    }

    fun placeOrder(
        shopId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        items: List<OrderItem>,
        paymentMode: PaymentMode,
        notes: String? = null
    ): Order {
        val shop = getShopById(shopId)
        val orderNumber = (1000 + _orders.value.size + 1)
        val order = Order(
            id = "ORD-$orderNumber",
            shopId = shopId,
            shopName = shop?.name ?: "Virk Fresh Fruits Assandh",
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            customerAddress = customerAddress.trim(),
            items = items,
            totalAmount = items.sumOf { it.total },
            paymentMode = paymentMode,
            status = OrderStatus.RECEIVED,
            placedAt = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date()),
            estimatedMinutes = 20,
            notes = notes
        )
        _orders.value = listOf(order) + _orders.value
        _lastPlacedOrderId.value = order.id
        return order
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _orders.value = _orders.value.map {
            if (it.id == orderId) it.copy(status = newStatus) else it
        }
    }

    fun getOrderById(orderId: String): Order? {
        return _orders.value.find { it.id == orderId }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())
    }

    private fun getFutureDate(daysAhead: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(cal.time)
    }

    companion object {
        @Volatile
        private var instance: AfruteRepository? = null

        fun getInstance(): AfruteRepository {
            return instance ?: synchronized(this) {
                instance ?: AfruteRepository().also { instance = it }
            }
        }

        private fun createInitialShops(): List<Shop> = listOf(
            Shop(
                id = "shop-1",
                name = "Virk Fresh Fruits Assandh",
                ownerName = "Manjeet Virk",
                phone = "98123 45678",
                address = "Main Sabzi Mandi, Assandh",
                activationKey = "AFRUTE-KEY-001",
                isActive = true,
                daysActive = 29,
                rentStatus = RentStatus.PAID,
                rentPlanAmount = 499,
                paidTillDate = "26 Nov 2026",
                appSoldStatus = "Active - Licensed",
                paymentMode = PaymentMode.PAY_AFTER_DELIVERY
            ),
            Shop(
                id = "shop-2",
                name = "Kisan Fruit Bhandar",
                ownerName = "Ramphal Sharma",
                phone = "98765 43210",
                address = "Safidon Road, Assandh",
                activationKey = "AFRUTE-KEY-002",
                isActive = true,
                daysActive = 21,
                rentStatus = RentStatus.PAID,
                rentPlanAmount = 499,
                paidTillDate = "18 Nov 2026",
                appSoldStatus = "Active - Licensed",
                paymentMode = PaymentMode.ONLINE_FIRST
            ),
            Shop(
                id = "shop-3",
                name = "Haryana Fruit Corner",
                ownerName = "Vikram Singh",
                phone = "98111 22334",
                address = "Jind Chowk, Assandh",
                activationKey = "AFRUTE-KEY-003",
                isActive = true,
                daysActive = 14,
                rentStatus = RentStatus.PAID,
                rentPlanAmount = 499,
                paidTillDate = "10 Nov 2026",
                appSoldStatus = "Active - Licensed",
                paymentMode = PaymentMode.PAY_AFTER_DELIVERY
            ),
            Shop(
                id = "shop-4",
                name = "Apni Sabzi & Fruit Mandi",
                ownerName = "Kuldeep Virk",
                phone = "98450 11223",
                address = "Salwan Chowk, Assandh",
                activationKey = "AFRUTE-KEY-004",
                isActive = false,
                daysActive = 32,
                rentStatus = RentStatus.DUE,
                rentPlanAmount = 499,
                paidTillDate = "25 Oct 2026",
                appSoldStatus = "Payment Due",
                paymentMode = PaymentMode.PAY_AFTER_DELIVERY
            ),
            Shop(
                id = "shop-5",
                name = "Green Orchard Fruits",
                ownerName = "Suresh Kumar",
                phone = "97280 99887",
                address = "Karnal Road, Assandh",
                activationKey = "AFRUTE-KEY-005",
                isActive = true,
                daysActive = 5,
                rentStatus = RentStatus.PAID,
                rentPlanAmount = 499,
                paidTillDate = "01 Nov 2026",
                appSoldStatus = "Active - Licensed",
                paymentMode = PaymentMode.ONLINE_FIRST
            )
        )

        private fun createInitialFruits(): List<Fruit> = listOf(
            Fruit(
                id = "f-1",
                shopId = "shop-1",
                nameEn = "Kashmiri Seb",
                nameHi = "कश्मीरी सेब",
                category = "Apples / सेब",
                price = 140.0,
                unit = "kg",
                stockQty = 45,
                isInStock = true,
                description = "Crispy, sweet, dark red Kashmiri apples straight from Sopore orchards.",
                emojiIcon = "🍎"
            ),
            Fruit(
                id = "f-2",
                shopId = "shop-1",
                nameEn = "Nagpur Santre",
                nameHi = "नागपुर संतरे",
                category = "Citrus / संतरे",
                price = 75.0,
                unit = "kg",
                stockQty = 60,
                isInStock = true,
                description = "Juicy fresh Nagpur oranges rich in Vitamin C.",
                emojiIcon = "🍊"
            ),
            Fruit(
                id = "f-3",
                shopId = "shop-1",
                nameEn = "Robusta Kela",
                nameHi = "केला",
                category = "Bananas / केले",
                price = 50.0,
                unit = "dozen",
                stockQty = 35,
                isInStock = true,
                description = "Freshly ripened sweet Cavendish bananas.",
                emojiIcon = "🍌"
            ),
            Fruit(
                id = "f-4",
                shopId = "shop-1",
                nameEn = "Kandhari Anaar",
                nameHi = "कंधारी अनार",
                category = "Pomegranate / अनार",
                price = 160.0,
                unit = "kg",
                stockQty = 25,
                isInStock = true,
                description = "Sweet ruby pearls with soft seeds, high in antioxidants.",
                emojiIcon = "🫐"
            ),
            Fruit(
                id = "f-5",
                shopId = "shop-1",
                nameEn = "Taiwanese Papita",
                nameHi = "मीठा पपीता",
                category = "Melons / पपीता",
                price = 55.0,
                unit = "kg",
                stockQty = 30,
                isInStock = true,
                description = "Golden orange flesh, supremely sweet and healthy.",
                emojiIcon = "🥭"
            ),
            Fruit(
                id = "f-6",
                shopId = "shop-1",
                nameEn = "Safeda / Chausa Aam",
                nameHi = "चौसा आम",
                category = "Mangoes / आम",
                price = 180.0,
                unit = "kg",
                stockQty = 20,
                isInStock = true,
                description = "Aromatic king of mangoes with honey-sweet nectar.",
                emojiIcon = "🥭"
            ),
            Fruit(
                id = "f-7",
                shopId = "shop-1",
                nameEn = "Shimla Golden Apple",
                nameHi = "गोल्डन सेब",
                category = "Apples / सेब",
                price = 130.0,
                unit = "kg",
                stockQty = 18,
                isInStock = true,
                description = "Himachal golden delicious apple, crisp & light.",
                emojiIcon = "🍏"
            ),
            Fruit(
                id = "f-8",
                shopId = "shop-1",
                nameEn = "Fresh Mosambi",
                nameHi = "ताज़ा मौसमी",
                category = "Citrus / संतरे",
                price = 65.0,
                unit = "kg",
                stockQty = 40,
                isInStock = true,
                description = "Sweet lime juice fruit, best for daily energy.",
                emojiIcon = "🍋"
            ),
            Fruit(
                id = "f-9",
                shopId = "shop-2",
                nameEn = "Kisan Special Seb",
                nameHi = "किसान सेब",
                category = "Apples / सेब",
                price = 135.0,
                unit = "kg",
                stockQty = 50,
                isInStock = true,
                description = "Freshly arrived from Himachal मंडी.",
                emojiIcon = "🍎"
            ),
            Fruit(
                id = "f-10",
                shopId = "shop-2",
                nameEn = "Desi Kela",
                nameHi = "देसी केला",
                category = "Bananas / केले",
                price = 45.0,
                unit = "dozen",
                stockQty = 40,
                isInStock = true,
                description = "Fresh green-yellow bananas.",
                emojiIcon = "🍌"
            )
        )

        private fun createInitialOrders(): List<Order> = listOf(
            Order(
                id = "ORD-1001",
                shopId = "shop-1",
                shopName = "Virk Fresh Fruits Assandh",
                customerName = "Amit Sharma",
                customerPhone = "98122 33445",
                customerAddress = "House 42, Ward 7, Near Gurudwara, Assandh",
                items = listOf(
                    OrderItem("f-1", "Kashmiri Seb", 140.0, 2, "kg"),
                    OrderItem("f-3", "Robusta Kela", 50.0, 1, "dozen")
                ),
                totalAmount = 330.0,
                paymentMode = PaymentMode.PAY_AFTER_DELIVERY,
                status = OrderStatus.OUT_FOR_DELIVERY,
                placedAt = "10:15 AM, Today",
                estimatedMinutes = 15,
                notes = "Please bring fresh red apples only."
            ),
            Order(
                id = "ORD-1002",
                shopId = "shop-1",
                shopName = "Virk Fresh Fruits Assandh",
                customerName = "Pooja Rani",
                customerPhone = "98960 12345",
                customerAddress = "Gali No. 3, Salwan Road, Assandh",
                items = listOf(
                    OrderItem("f-2", "Nagpur Santre", 75.0, 2, "kg"),
                    OrderItem("f-4", "Kandhari Anaar", 160.0, 1, "kg")
                ),
                totalAmount = 310.0,
                paymentMode = PaymentMode.PAY_AFTER_DELIVERY,
                status = OrderStatus.PACKING,
                placedAt = "10:45 AM, Today",
                estimatedMinutes = 25
            )
        )

        private fun createInitialRentLedger(): List<RentRecord> = listOf(
            RentRecord(
                id = "RENT-7821",
                shopId = "shop-1",
                shopName = "Virk Fresh Fruits Assandh",
                ownerName = "Manjeet Virk",
                amount = 499,
                date = "26 Oct 2026",
                periodCovered = "26 Oct 2026 - 26 Nov 2026",
                status = RentStatus.PAID,
                collectedBy = "Manjeet Virk (Assandh)"
            ),
            RentRecord(
                id = "RENT-6532",
                shopId = "shop-2",
                shopName = "Kisan Fruit Bhandar",
                ownerName = "Ramphal Sharma",
                amount = 499,
                date = "18 Oct 2026",
                periodCovered = "18 Oct 2026 - 18 Nov 2026",
                status = RentStatus.PAID,
                collectedBy = "Manjeet Virk (Assandh)"
            ),
            RentRecord(
                id = "RENT-4320",
                shopId = "shop-3",
                shopName = "Haryana Fruit Corner",
                ownerName = "Vikram Singh",
                amount = 499,
                date = "10 Oct 2026",
                periodCovered = "10 Oct 2026 - 10 Nov 2026",
                status = RentStatus.PAID,
                collectedBy = "Manjeet Virk (Assandh)"
            ),
            RentRecord(
                id = "RENT-9011",
                shopId = "shop-5",
                shopName = "Green Orchard Fruits",
                ownerName = "Suresh Kumar",
                amount = 499,
                date = "01 Oct 2026",
                periodCovered = "01 Oct 2026 - 01 Nov 2026",
                status = RentStatus.PAID,
                collectedBy = "Manjeet Virk (Assandh)"
            ),
            RentRecord(
                id = "RENT-DUE-04",
                shopId = "shop-4",
                shopName = "Apni Sabzi & Fruit Mandi",
                ownerName = "Kuldeep Virk",
                amount = 499,
                date = "25 Oct 2026",
                periodCovered = "25 Sep 2026 - 25 Oct 2026",
                status = RentStatus.DUE,
                collectedBy = "Manjeet Virk (Assandh)"
            )
        )
    }
}
