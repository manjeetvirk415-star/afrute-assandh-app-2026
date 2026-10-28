package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AfruteRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class OwnerTab(val titleEn: String, val titleHi: String) {
    SHOPS("Shops", "दुकानें"),
    RENT_LEDGER("Rent Ledger", "किराया बहीखाता"),
    ANALYTICS("Analytics", "व्यापार रिपोर्ट")
}

enum class ShopkeeperTab(val titleEn: String, val titleHi: String) {
    CATALOG("Fruit Catalog", "फलों की सूची"),
    ORDERS("Live Orders", "ग्राहक ऑर्डर")
}

class AfruteViewModel : ViewModel() {

    private val repository = AfruteRepository.getInstance()

    val shops = repository.shops
    val fruits = repository.fruits
    val orders = repository.orders
    val rentLedger = repository.rentLedger
    val activeShopKey = repository.activeShopKey
    val currentRole = repository.currentRole
    val lastPlacedOrderId = repository.lastPlacedOrderId

    // Owner UI state
    private val _selectedOwnerTab = MutableStateFlow(OwnerTab.SHOPS)
    val selectedOwnerTab: StateFlow<OwnerTab> = _selectedOwnerTab.asStateFlow()

    private val _shopSearchQuery = MutableStateFlow("")
    val shopSearchQuery: StateFlow<String> = _shopSearchQuery.asStateFlow()

    private val _shopFilterPaid = MutableStateFlow<RentStatus?>(null)
    val shopFilterPaid: StateFlow<RentStatus?> = _shopFilterPaid.asStateFlow()

    // Shopkeeper UI state
    private val _selectedShopkeeperTab = MutableStateFlow(ShopkeeperTab.CATALOG)
    val selectedShopkeeperTab: StateFlow<ShopkeeperTab> = _selectedShopkeeperTab.asStateFlow()

    private val _keyInputText = MutableStateFlow("AFRUTE-KEY-001")
    val keyInputText: StateFlow<String> = _keyInputText.asStateFlow()

    // Customer UI state
    private val _selectedCustomerShopId = MutableStateFlow("shop-1")
    val selectedCustomerShopId: StateFlow<String> = _selectedCustomerShopId.asStateFlow()

    private val _fruitSearchQuery = MutableStateFlow("")
    val fruitSearchQuery: StateFlow<String> = _fruitSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Cart state: Map<FruitId, Quantity>
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()

    // Dialogs & Modals
    private val _showOnboardDialog = MutableStateFlow(false)
    val showOnboardDialog: StateFlow<Boolean> = _showOnboardDialog.asStateFlow()

    private val _showGenerateKeyDialog = MutableStateFlow(false)
    val showGenerateKeyDialog: StateFlow<Boolean> = _showGenerateKeyDialog.asStateFlow()

    private val _generatedNewKey = MutableStateFlow<String?>(null)
    val generatedNewKey: StateFlow<String?> = _generatedNewKey.asStateFlow()

    private val _showAddFruitDialog = MutableStateFlow(false)
    val showAddFruitDialog: StateFlow<Boolean> = _showAddFruitDialog.asStateFlow()

    private val _showCartSheet = MutableStateFlow(false)
    val showCartSheet: StateFlow<Boolean> = _showCartSheet.asStateFlow()

    private val _editingFruit = MutableStateFlow<Fruit?>(null)
    val editingFruit: StateFlow<Fruit?> = _editingFruit.asStateFlow()

    // Tracking screen order ID
    private val _trackingOrderId = MutableStateFlow<String?>("ORD-1001")
    val trackingOrderId: StateFlow<String?> = _trackingOrderId.asStateFlow()

    private val _showTrackingScreen = MutableStateFlow(false)
    val showTrackingScreen: StateFlow<Boolean> = _showTrackingScreen.asStateFlow()

    fun selectOwnerTab(tab: OwnerTab) {
        _selectedOwnerTab.value = tab
    }

    fun selectShopkeeperTab(tab: ShopkeeperTab) {
        _selectedShopkeeperTab.value = tab
    }

    fun setShopSearchQuery(q: String) {
        _shopSearchQuery.value = q
    }

    fun setShopFilterPaid(status: RentStatus?) {
        _shopFilterPaid.value = status
    }

    fun setKeyInputText(k: String) {
        _keyInputText.value = k
    }

    fun setFruitSearchQuery(q: String) {
        _fruitSearchQuery.value = q
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setCustomerShopId(shopId: String) {
        if (_selectedCustomerShopId.value != shopId) {
            _selectedCustomerShopId.value = shopId
            _cart.value = emptyMap() // Reset cart when switching store
        }
    }

    fun switchRole(role: AppRole) {
        repository.setRole(role)
    }

    fun openShopkeeperForShop(shop: Shop) {
        repository.setActiveShopKey(shop.activationKey)
        _keyInputText.value = shop.activationKey
        repository.setRole(AppRole.SHOPKEEPER)
    }

    fun loginShopkeeperWithKey(key: String): Boolean {
        val shop = repository.getShopByKey(key)
        if (shop != null) {
            repository.setActiveShopKey(shop.activationKey)
            return true
        }
        return false
    }

    fun logoutShopkeeper() {
        repository.setActiveShopKey(null)
    }

    fun setShowOnboardDialog(show: Boolean) {
        _showOnboardDialog.value = show
    }

    fun openGenerateKeyDialog() {
        _generatedNewKey.value = repository.generateUniqueKey()
        _showGenerateKeyDialog.value = true
    }

    fun dismissGenerateKeyDialog() {
        _showGenerateKeyDialog.value = false
        _generatedNewKey.value = null
    }

    fun setShowAddFruitDialog(show: Boolean) {
        _showAddFruitDialog.value = show
    }

    fun setEditingFruit(fruit: Fruit?) {
        _editingFruit.value = fruit
    }

    fun setShowCartSheet(show: Boolean) {
        _showCartSheet.value = show
    }

    fun showOrderTracking(orderId: String) {
        _trackingOrderId.value = orderId
        _showTrackingScreen.value = true
    }

    fun hideOrderTracking() {
        _showTrackingScreen.value = false
    }

    // Business actions
    fun onboardShop(name: String, ownerName: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.onboardShop(name, ownerName, phone, address)
            _showOnboardDialog.value = false
        }
    }

    fun collectRent(shopId: String) {
        viewModelScope.launch {
            repository.collectRent(shopId)
        }
    }

    fun sendRentReminder(context: Context, shop: Shop) {
        repository.sendRentReminder(context, shop)
    }

    fun copyAndShareAppLink(context: Context) {
        repository.copyAndShareAppLink(context)
    }

    fun copyToClipboard(context: Context, text: String, label: String) {
        repository.copyToClipboard(context, text, label)
    }

    fun updateShopPaymentMode(shopId: String, mode: PaymentMode) {
        repository.updateShopPaymentMode(shopId, mode)
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
        emojiIcon: String
    ) {
        viewModelScope.launch {
            repository.addFruit(
                shopId = shopId,
                nameEn = nameEn,
                nameHi = nameHi,
                category = category,
                price = price,
                unit = unit,
                stockQty = stockQty,
                description = description,
                emojiIcon = emojiIcon
            )
            _showAddFruitDialog.value = false
        }
    }

    fun updateFruit(fruitId: String, price: Double, stockQty: Int, isInStock: Boolean) {
        viewModelScope.launch {
            repository.updateFruit(fruitId, price, stockQty, isInStock)
            _editingFruit.value = null
        }
    }

    fun deleteFruit(fruitId: String) {
        viewModelScope.launch {
            repository.deleteFruit(fruitId)
        }
    }

    // Cart actions
    fun addToCart(fruitId: String) {
        val current = _cart.value.toMutableMap()
        current[fruitId] = (current[fruitId] ?: 0) + 1
        _cart.value = current
    }

    fun decrementCart(fruitId: String) {
        val current = _cart.value.toMutableMap()
        val count = current[fruitId] ?: 0
        if (count <= 1) {
            current.remove(fruitId)
        } else {
            current[fruitId] = count - 1
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun placeCustomerOrder(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        notes: String?
    ): Order? {
        val shopId = _selectedCustomerShopId.value
        val shop = repository.getShopById(shopId) ?: return null
        val fruitList = repository.fruits.value

        val items = _cart.value.mapNotNull { (fruitId, qty) ->
            val fruit = fruitList.find { it.id == fruitId } ?: return@mapNotNull null
            OrderItem(
                fruitId = fruit.id,
                fruitName = "${fruit.nameEn} (${fruit.nameHi})",
                unitPrice = fruit.price,
                quantity = qty,
                unit = fruit.unit
            )
        }

        if (items.isEmpty()) return null

        val order = repository.placeOrder(
            shopId = shopId,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            items = items,
            paymentMode = shop.paymentMode,
            notes = notes
        )
        clearCart()
        _showCartSheet.value = false
        showOrderTracking(order.id)
        return order
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }
}
