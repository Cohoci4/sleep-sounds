package com.sleepsounds.app.data.local.db

import androidx.room.TypeConverter
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.Tier

class Converters {
    @TypeConverter fun tierToString(tier: Tier): String = tier.name
    @TypeConverter fun stringToTier(value: String): Tier =
        runCatching { Tier.valueOf(value) }.getOrDefault(Tier.FREE)

    @TypeConverter fun categoryToString(category: Category): String = category.slug
    @TypeConverter fun stringToCategory(value: String): Category = Category.fromSlug(value)
}
