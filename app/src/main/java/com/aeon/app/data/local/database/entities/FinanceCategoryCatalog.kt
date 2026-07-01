package com.aeon.app.data.local.database.entities

import java.time.Instant

data class FinanceCategoryDefinition(
    val id: String,
    val label: String,
    val iconKey: String,
    val familyKey: String,
    val scope: String,
    val isDefault: Boolean,
    val sortOrder: Int
)

object FinanceCategoryScopeStorage {
    const val Expense = "expense"
    const val Income = "income"
}

object FinanceCategoryFamilyStorage {
    const val Core = "core"
    const val Food = "food"
    const val Transport = "transport"
    const val Money = "money"
    const val Home = "home"
    const val Health = "health"
    const val Growth = "growth"
    const val Lifestyle = "lifestyle"
}

object FinanceCategoryCatalog {
    val defaults: List<FinanceCategoryDefinition> = listOf(
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.General,
            label = "General",
            iconKey = "category",
            familyKey = FinanceCategoryFamilyStorage.Core,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 0
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Food,
            label = "Food",
            iconKey = "restaurant",
            familyKey = FinanceCategoryFamilyStorage.Food,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 10
        ),
        FinanceCategoryDefinition(
            id = "grocery",
            label = "Grocery",
            iconKey = "shopping_cart",
            familyKey = FinanceCategoryFamilyStorage.Food,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 11
        ),
        FinanceCategoryDefinition(
            id = "dining_out",
            label = "Dining Out",
            iconKey = "dinner_dining",
            familyKey = FinanceCategoryFamilyStorage.Food,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 12
        ),
        FinanceCategoryDefinition(
            id = "coffee",
            label = "Coffee",
            iconKey = "local_cafe",
            familyKey = FinanceCategoryFamilyStorage.Food,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 13
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Travel,
            label = "Travel",
            iconKey = "flight",
            familyKey = FinanceCategoryFamilyStorage.Transport,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 20
        ),
        FinanceCategoryDefinition(
            id = "transport",
            label = "Transport",
            iconKey = "directions_bus",
            familyKey = FinanceCategoryFamilyStorage.Transport,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 21
        ),
        FinanceCategoryDefinition(
            id = "fuel",
            label = "Fuel",
            iconKey = "local_gas_station",
            familyKey = FinanceCategoryFamilyStorage.Transport,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 22
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Shopping,
            label = "Shopping",
            iconKey = "local_mall",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 30
        ),
        FinanceCategoryDefinition(
            id = "clothing",
            label = "Clothing",
            iconKey = "checkroom",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 31
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Bills,
            label = "Bills",
            iconKey = "description",
            familyKey = FinanceCategoryFamilyStorage.Money,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 40
        ),
        FinanceCategoryDefinition(
            id = "rent",
            label = "Rent",
            iconKey = "apartment",
            familyKey = FinanceCategoryFamilyStorage.Home,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 41
        ),
        FinanceCategoryDefinition(
            id = "utilities",
            label = "Utilities",
            iconKey = "bolt",
            familyKey = FinanceCategoryFamilyStorage.Home,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 42
        ),
        FinanceCategoryDefinition(
            id = "internet",
            label = "Internet",
            iconKey = "wifi",
            familyKey = FinanceCategoryFamilyStorage.Home,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 43
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Subscription,
            label = "Subscriptions",
            iconKey = "subscriptions",
            familyKey = FinanceCategoryFamilyStorage.Money,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 44
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Health,
            label = "Health",
            iconKey = "health_and_safety",
            familyKey = FinanceCategoryFamilyStorage.Health,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 50
        ),
        FinanceCategoryDefinition(
            id = "pharmacy",
            label = "Pharmacy",
            iconKey = "medication",
            familyKey = FinanceCategoryFamilyStorage.Health,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 51
        ),
        FinanceCategoryDefinition(
            id = "fitness",
            label = "Fitness",
            iconKey = "fitness_center",
            familyKey = FinanceCategoryFamilyStorage.Health,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 52
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Study,
            label = "Study",
            iconKey = "school",
            familyKey = FinanceCategoryFamilyStorage.Growth,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 60
        ),
        FinanceCategoryDefinition(
            id = "books",
            label = "Books",
            iconKey = "menu_book",
            familyKey = FinanceCategoryFamilyStorage.Growth,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 61
        ),
        FinanceCategoryDefinition(
            id = "work",
            label = "Work",
            iconKey = "work_outline",
            familyKey = FinanceCategoryFamilyStorage.Growth,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 62
        ),
        FinanceCategoryDefinition(
            id = "home",
            label = "Home",
            iconKey = "home",
            familyKey = FinanceCategoryFamilyStorage.Home,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 70
        ),
        FinanceCategoryDefinition(
            id = "entertainment",
            label = "Entertainment",
            iconKey = "movie",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 71
        ),
        FinanceCategoryDefinition(
            id = "pets",
            label = "Pets",
            iconKey = "pets",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 72
        ),
        FinanceCategoryDefinition(
            id = "gift",
            label = "Gift",
            iconKey = "card_giftcard",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 73
        ),
        FinanceCategoryDefinition(
            id = "family",
            label = "Family",
            iconKey = "family_restroom",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 74
        ),
        FinanceCategoryDefinition(
            id = "personal_care",
            label = "Personal Care",
            iconKey = "self_improvement",
            familyKey = FinanceCategoryFamilyStorage.Lifestyle,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 75
        ),
        FinanceCategoryDefinition(
            id = "insurance",
            label = "Insurance",
            iconKey = "shield",
            familyKey = FinanceCategoryFamilyStorage.Money,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 80
        ),
        FinanceCategoryDefinition(
            id = "savings",
            label = "Savings",
            iconKey = "savings",
            familyKey = FinanceCategoryFamilyStorage.Money,
            scope = FinanceCategoryScopeStorage.Expense,
            isDefault = true,
            sortOrder = 81
        ),
        FinanceCategoryDefinition(
            id = FinanceCategoryStorage.Income,
            label = "Income",
            iconKey = "paid",
            familyKey = FinanceCategoryFamilyStorage.Money,
            scope = FinanceCategoryScopeStorage.Income,
            isDefault = true,
            sortOrder = 999
        )
    )

    fun defaultEntities(now: Instant = Instant.now()): List<FinanceCategoryEntity> {
        return defaults.map { definition ->
            FinanceCategoryEntity(
                id = definition.id,
                label = definition.label,
                iconKey = definition.iconKey,
                familyKey = definition.familyKey,
                scope = definition.scope,
                isDefault = definition.isDefault,
                sortOrder = definition.sortOrder,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    val defaultCategoryIds: Set<String> = defaults.map(FinanceCategoryDefinition::id).toSet()
}
