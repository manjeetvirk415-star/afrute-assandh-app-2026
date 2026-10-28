package com.example.model

enum class RentStatus(val labelEn: String, val labelHi: String) {
    PAID("Paid", "जमा है"),
    DUE("Due", "बाकी है")
}

enum class PaymentMode(val titleEn: String, val titleHi: String, val description: String) {
    ONLINE_FIRST("Online First (UPI/QR)", "ऑनलाइन पहले (UPI/QR)", "Payment required online at checkout"),
    PAY_AFTER_DELIVERY("Pay After Delivery (COD)", "डिलीवरी के बाद (नकद/COD)", "Pay cash or scan on fruit delivery")
}

enum class OrderStatus(val stepIndex: Int, val titleEn: String, val titleHi: String, val subtext: String) {
    RECEIVED(0, "Order Placed", "ऑर्डर हो गया", "Shopkeeper accepted order"),
    PACKING(1, "Packing", "पैक हो रहा है", "Fresh fruits being selected & weighed"),
    OUT_FOR_DELIVERY(2, "On The Way", "रास्ते में है", "Rider dispatched towards your location"),
    DELIVERED(3, "Reached Home", "घर पहुँच गया", "Delivered fresh at your doorstep!")
}

enum class AppRole(val titleEn: String, val titleHi: String) {
    OWNER("Owner / Admin", "मालिक (मंजीत विर्क)"),
    SHOPKEEPER("Shopkeeper", "दुकानदार पोर्टल"),
    CUSTOMER("Customer", "फल ग्राहक")
}

data class Shop(
    val id: String,
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val activationKey: String,
    val isActive: Boolean = true,
    val daysActive: Int = 1,
    val rentStatus: RentStatus = RentStatus.PAID,
    val rentPlanAmount: Int = 499,
    val paidTillDate: String,
    val appSoldStatus: String = "Active - Licensed",
    val paymentMode: PaymentMode = PaymentMode.PAY_AFTER_DELIVERY,
    val createdAt: Long = System.currentTimeMillis()
)

data class Fruit(
    val id: String,
    val shopId: String,
    val nameEn: String,
    val nameHi: String,
    val category: String,
    val price: Double,
    val unit: String = "kg", // "kg", "dozen", "box"
    val stockQty: Int = 50,
    val isInStock: Boolean = true,
    val description: String = "",
    val emojiIcon: String = "🍎",
    val photoUri: String? = null
)

data class OrderItem(
    val fruitId: String,
    val fruitName: String,
    val unitPrice: Double,
    val quantity: Int,
    val unit: String
) {
    val total: Double get() = unitPrice * quantity
}

data class Order(
    val id: String,
    val shopId: String,
    val shopName: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val paymentMode: PaymentMode,
    val status: OrderStatus = OrderStatus.RECEIVED,
    val placedAt: String,
    val estimatedMinutes: Int = 20,
    val notes: String? = null
)

data class RentRecord(
    val id: String,
    val shopId: String,
    val shopName: String,
    val ownerName: String,
    val amount: Int = 499,
    val date: String,
    val periodCovered: String,
    val status: RentStatus,
    val collectedBy: String = "Manjeet Virk (Assandh)"
)
