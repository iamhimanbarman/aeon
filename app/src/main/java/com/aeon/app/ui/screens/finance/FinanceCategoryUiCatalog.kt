package com.aeon.app.ui.screens.finance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Commute
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.DryCleaning
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsCricket
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Subway
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Tram
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.aeon.app.data.local.database.entities.FinanceCategoryCatalog
import com.aeon.app.data.local.database.entities.FinanceCategoryDefinition
import com.aeon.app.data.local.database.entities.FinanceCategoryEntity
import com.aeon.app.data.local.database.entities.FinanceCategoryFamilyStorage
import com.aeon.app.data.local.database.entities.FinanceCategoryScopeStorage

data class FinanceCategoryOption(
    val key: String,
    val label: String,
    val iconKey: String,
    val familyKey: String,
    val scope: String,
    val isDefault: Boolean
) {
    val icon: ImageVector
        get() = financeIconForKey(iconKey)
}

data class FinanceIconOption(
    val key: String,
    val label: String,
    val familyKey: String,
    val icon: ImageVector
)

fun FinanceCategoryEntity.toFinanceCategoryOption(): FinanceCategoryOption {
    return FinanceCategoryOption(
        key = id,
        label = label,
        iconKey = iconKey,
        familyKey = familyKey,
        scope = scope,
        isDefault = isDefault
    )
}

fun FinanceCategoryDefinition.toFinanceCategoryOption(): FinanceCategoryOption {
    return FinanceCategoryOption(
        key = id,
        label = label,
        iconKey = iconKey,
        familyKey = familyKey,
        scope = scope,
        isDefault = isDefault
    )
}

fun financeCategoryOptions(
    categories: List<FinanceCategoryEntity>
): List<FinanceCategoryOption> {
    if (categories.isEmpty()) {
        return FinanceCategoryCatalog.defaults.map(FinanceCategoryDefinition::toFinanceCategoryOption)
    }

    return categories.map(FinanceCategoryEntity::toFinanceCategoryOption)
}

fun financeFamilyLabel(familyKey: String): String {
    return when (familyKey) {
        FinanceCategoryFamilyStorage.Core -> "Core"
        FinanceCategoryFamilyStorage.Food -> "Food & Drinks"
        FinanceCategoryFamilyStorage.Transport -> "Transport & Travel"
        FinanceCategoryFamilyStorage.Money -> "Money & Bills"
        FinanceCategoryFamilyStorage.Home -> "Home"
        FinanceCategoryFamilyStorage.Health -> "Health"
        FinanceCategoryFamilyStorage.Growth -> "Study & Work"
        FinanceCategoryFamilyStorage.Lifestyle -> "Lifestyle"
        else -> "More"
    }
}

fun financeIconForKey(iconKey: String): ImageVector {
    return financeIconOptionsByKey[iconKey]?.icon ?: Icons.Outlined.Category
}

val financeIconOptions: List<FinanceIconOption> = listOf(
    FinanceIconOption("category", "General", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Category),
    FinanceIconOption("tune", "Custom", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Tune),
    FinanceIconOption("sell", "Tag", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Sell),
    FinanceIconOption("storefront", "Storefront", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Storefront),
    FinanceIconOption("dashboard", "Dashboard", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Dashboard),
    FinanceIconOption("widgets", "Widgets", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Widgets),
    FinanceIconOption("apps", "Apps", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Apps),
    FinanceIconOption("label", "Label", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Label),
    FinanceIconOption("bookmark_border", "Bookmark", FinanceCategoryFamilyStorage.Core, Icons.Outlined.BookmarkBorder),
    FinanceIconOption("inventory_2", "Inventory", FinanceCategoryFamilyStorage.Core, Icons.Outlined.Inventory2),

    FinanceIconOption("restaurant", "Restaurant", FinanceCategoryFamilyStorage.Food, Icons.Outlined.Restaurant),
    FinanceIconOption("shopping_cart", "Groceries", FinanceCategoryFamilyStorage.Food, Icons.Outlined.ShoppingCart),
    FinanceIconOption("local_cafe", "Cafe", FinanceCategoryFamilyStorage.Food, Icons.Outlined.LocalCafe),
    FinanceIconOption("lunch_dining", "Lunch", FinanceCategoryFamilyStorage.Food, Icons.Outlined.LunchDining),
    FinanceIconOption("dinner_dining", "Dinner", FinanceCategoryFamilyStorage.Food, Icons.Outlined.DinnerDining),
    FinanceIconOption("local_pizza", "Pizza", FinanceCategoryFamilyStorage.Food, Icons.Outlined.LocalPizza),
    FinanceIconOption("fastfood", "Fast Food", FinanceCategoryFamilyStorage.Food, Icons.Outlined.Fastfood),
    FinanceIconOption("bakery_dining", "Bakery", FinanceCategoryFamilyStorage.Food, Icons.Outlined.BakeryDining),
    FinanceIconOption("icecream", "Dessert", FinanceCategoryFamilyStorage.Food, Icons.Outlined.Icecream),
    FinanceIconOption("emoji_food_beverage", "Beverage", FinanceCategoryFamilyStorage.Food, Icons.Outlined.EmojiFoodBeverage),
    FinanceIconOption("ramen_dining", "Noodles", FinanceCategoryFamilyStorage.Food, Icons.Outlined.RamenDining),
    FinanceIconOption("set_meal", "Meal", FinanceCategoryFamilyStorage.Food, Icons.Outlined.SetMeal),

    FinanceIconOption("flight", "Flight", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Flight),
    FinanceIconOption("directions_car", "Car", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.DirectionsCar),
    FinanceIconOption("directions_bus", "Bus", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.DirectionsBus),
    FinanceIconOption("train", "Train", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Train),
    FinanceIconOption("subway", "Metro", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Subway),
    FinanceIconOption("tram", "Tram", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Tram),
    FinanceIconOption("directions_bike", "Bike", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.DirectionsBike),
    FinanceIconOption("local_taxi", "Taxi", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.LocalTaxi),
    FinanceIconOption("directions_boat", "Boat", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.DirectionsBoat),
    FinanceIconOption("two_wheeler", "Scooter", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.TwoWheeler),
    FinanceIconOption("local_gas_station", "Fuel", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.LocalGasStation),
    FinanceIconOption("commute", "Commute", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Commute),

    FinanceIconOption("home", "Home", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Home),
    FinanceIconOption("apartment", "Apartment", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Apartment),
    FinanceIconOption("bed", "Bedroom", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Bed),
    FinanceIconOption("chair", "Furniture", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Chair),
    FinanceIconOption("weekend", "Living Room", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Weekend),
    FinanceIconOption("kitchen", "Kitchen", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Kitchen),
    FinanceIconOption("local_laundry_service", "Laundry", FinanceCategoryFamilyStorage.Home, Icons.Outlined.LocalLaundryService),
    FinanceIconOption("cleaning_services", "Cleaning", FinanceCategoryFamilyStorage.Home, Icons.Outlined.CleaningServices),
    FinanceIconOption("lightbulb", "Electricity", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Lightbulb),
    FinanceIconOption("water_drop", "Water", FinanceCategoryFamilyStorage.Home, Icons.Outlined.WaterDrop),
    FinanceIconOption("wifi", "Internet", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Wifi),
    FinanceIconOption("router", "Router", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Router),
    FinanceIconOption("tv", "TV", FinanceCategoryFamilyStorage.Home, Icons.Outlined.Tv),
    FinanceIconOption("electric_bolt", "Power", FinanceCategoryFamilyStorage.Home, Icons.Outlined.ElectricBolt),

    FinanceIconOption("description", "Bills", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Description),
    FinanceIconOption("receipt_long", "Receipt", FinanceCategoryFamilyStorage.Money, Icons.Outlined.ReceiptLong),
    FinanceIconOption("receipt", "Invoice", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Receipt),
    FinanceIconOption("phone_android", "Phone", FinanceCategoryFamilyStorage.Money, Icons.Outlined.PhoneAndroid),
    FinanceIconOption("smartphone", "Mobile", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Smartphone),
    FinanceIconOption("subscriptions", "Subscriptions", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Subscriptions),
    FinanceIconOption("credit_card", "Card", FinanceCategoryFamilyStorage.Money, Icons.Outlined.CreditCard),
    FinanceIconOption("payments", "Payments", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Payments),
    FinanceIconOption("price_check", "Price Check", FinanceCategoryFamilyStorage.Money, Icons.Outlined.PriceCheck),
    FinanceIconOption("currency_exchange", "Exchange", FinanceCategoryFamilyStorage.Money, Icons.Outlined.CurrencyExchange),
    FinanceIconOption("currency_rupee", "Rupee", FinanceCategoryFamilyStorage.Money, Icons.Outlined.CurrencyRupee),
    FinanceIconOption("mail_outline", "Mail", FinanceCategoryFamilyStorage.Money, Icons.Outlined.MailOutline),

    FinanceIconOption("health_and_safety", "Health", FinanceCategoryFamilyStorage.Health, Icons.Outlined.HealthAndSafety),
    FinanceIconOption("local_hospital", "Hospital", FinanceCategoryFamilyStorage.Health, Icons.Outlined.LocalHospital),
    FinanceIconOption("medication", "Medicine", FinanceCategoryFamilyStorage.Health, Icons.Outlined.Medication),
    FinanceIconOption("vaccines", "Vaccines", FinanceCategoryFamilyStorage.Health, Icons.Outlined.Vaccines),
    FinanceIconOption("fitness_center", "Gym", FinanceCategoryFamilyStorage.Health, Icons.Outlined.FitnessCenter),
    FinanceIconOption("spa", "Spa", FinanceCategoryFamilyStorage.Health, Icons.Outlined.Spa),
    FinanceIconOption("self_improvement", "Wellness", FinanceCategoryFamilyStorage.Health, Icons.Outlined.SelfImprovement),
    FinanceIconOption("psychology", "Mind", FinanceCategoryFamilyStorage.Health, Icons.Outlined.Psychology),
    FinanceIconOption("monitor_heart", "Heart", FinanceCategoryFamilyStorage.Health, Icons.Outlined.MonitorHeart),
    FinanceIconOption("bloodtype", "Blood", FinanceCategoryFamilyStorage.Health, Icons.Outlined.Bloodtype),

    FinanceIconOption("school", "School", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.School),
    FinanceIconOption("menu_book", "Books", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.MenuBook),
    FinanceIconOption("auto_stories", "Stories", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.AutoStories),
    FinanceIconOption("laptop_mac", "Laptop", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.LaptopMac),
    FinanceIconOption("work_outline", "Work", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.WorkOutline),
    FinanceIconOption("business_center", "Office", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.BusinessCenter),
    FinanceIconOption("calculate", "Math", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.Calculate),
    FinanceIconOption("science", "Science", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.Science),
    FinanceIconOption("history_edu", "History", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.HistoryEdu),
    FinanceIconOption("edit_note", "Notes", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.EditNote),
    FinanceIconOption("draw", "Design", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.Draw),
    FinanceIconOption("print", "Print", FinanceCategoryFamilyStorage.Growth, Icons.Outlined.Print),

    FinanceIconOption("local_mall", "Mall", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.LocalMall),
    FinanceIconOption("shopping_bag", "Bag", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.ShoppingBag),
    FinanceIconOption("checkroom", "Clothing", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Checkroom),
    FinanceIconOption("diamond", "Jewelry", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Diamond),
    FinanceIconOption("card_giftcard", "Gift", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.CardGiftcard),
    FinanceIconOption("pets", "Pets", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Pets),
    FinanceIconOption("child_care", "Kids", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.ChildCare),
    FinanceIconOption("family_restroom", "Family", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.FamilyRestroom),
    FinanceIconOption("face", "Personal", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Face),
    FinanceIconOption("dry_cleaning", "Dry Clean", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.DryCleaning),
    FinanceIconOption("content_cut", "Salon", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.ContentCut),
    FinanceIconOption("volunteer_activism", "Donations", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.VolunteerActivism),

    FinanceIconOption("savings", "Savings", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Savings),
    FinanceIconOption("paid", "Income", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Paid),
    FinanceIconOption("wallet", "Wallet", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Wallet),
    FinanceIconOption("account_balance", "Bank", FinanceCategoryFamilyStorage.Money, Icons.Outlined.AccountBalance),
    FinanceIconOption("account_balance_wallet", "Budget", FinanceCategoryFamilyStorage.Money, Icons.Outlined.AccountBalanceWallet),
    FinanceIconOption("trending_up", "Invest", FinanceCategoryFamilyStorage.Money, Icons.Outlined.TrendingUp),
    FinanceIconOption("pie_chart_outline", "Analytics", FinanceCategoryFamilyStorage.Money, Icons.Outlined.PieChartOutline),
    FinanceIconOption("shield", "Insurance", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Shield),
    FinanceIconOption("sell_more", "Sales", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Sell),
    FinanceIconOption("storefront_more", "Retail", FinanceCategoryFamilyStorage.Money, Icons.Outlined.Storefront),

    FinanceIconOption("movie", "Movies", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Movie),
    FinanceIconOption("music_note", "Music", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.MusicNote),
    FinanceIconOption("headphones", "Audio", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Headphones),
    FinanceIconOption("sports_esports", "Gaming", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.SportsEsports),
    FinanceIconOption("celebration", "Events", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Celebration),
    FinanceIconOption("camera_alt", "Camera", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.CameraAlt),
    FinanceIconOption("palette", "Art", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Palette),
    FinanceIconOption("theater_comedy", "Shows", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.TheaterComedy),
    FinanceIconOption("sports_soccer", "Football", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.SportsSoccer),
    FinanceIconOption("sports_cricket", "Cricket", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.SportsCricket),
    FinanceIconOption("sports_basketball", "Basketball", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.SportsBasketball),
    FinanceIconOption("mic", "Mic", FinanceCategoryFamilyStorage.Lifestyle, Icons.Outlined.Mic),

    FinanceIconOption("beach_access", "Beach", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.BeachAccess),
    FinanceIconOption("hiking", "Hiking", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Hiking),
    FinanceIconOption("landscape", "Landscape", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Landscape),
    FinanceIconOption("park", "Park", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Park),
    FinanceIconOption("forest", "Forest", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Forest),
    FinanceIconOption("explore", "Explore", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Explore),
    FinanceIconOption("map", "Map", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Map),
    FinanceIconOption("directions_walk", "Walk", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.DirectionsWalk),
    FinanceIconOption("local_shipping", "Shipping", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.LocalShipping),
    FinanceIconOption("cake", "Celebration", FinanceCategoryFamilyStorage.Transport, Icons.Outlined.Cake)
)

val financeIconOptionsByKey: Map<String, FinanceIconOption> =
    financeIconOptions.associateBy(FinanceIconOption::key)

val financeIconFamilies: List<String> = listOf(
    FinanceCategoryFamilyStorage.Core,
    FinanceCategoryFamilyStorage.Food,
    FinanceCategoryFamilyStorage.Transport,
    FinanceCategoryFamilyStorage.Home,
    FinanceCategoryFamilyStorage.Money,
    FinanceCategoryFamilyStorage.Health,
    FinanceCategoryFamilyStorage.Growth,
    FinanceCategoryFamilyStorage.Lifestyle
)
