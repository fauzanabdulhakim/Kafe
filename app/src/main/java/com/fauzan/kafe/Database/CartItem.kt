package com.fauzan.kafe.Database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "Cart", primaryKeys = ["uid", "foodId", "foodAddon", "foodSize"])
data class CartItem(
    @ColumnInfo(name = "foodId")
    var foodId: String = "",

    @ColumnInfo(name = "foodName")
    var foodName: String? = null,

    @ColumnInfo(name = "foodImage")
    var foodImage: String? = null,

    @ColumnInfo(name = "foodPrice")
    var foodPrice: Double? = 0.0,

    @ColumnInfo(name = "foodQuantity")
    var foodQuantity: Int = 0,

    @ColumnInfo(name = "userPhone")
    var userPhone: String? = null,

    @ColumnInfo(name = "foodExtraPrice")
    var foodExtraPrice: Double? = 0.0,

    @ColumnInfo(name = "foodAddon")
    var foodAddon: String = "",

    @ColumnInfo(name = "foodSize")
    var foodSize: String = "",

    @ColumnInfo(name = "uid")
    var uid: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is CartItem) return false
        return other.foodId == this.foodId &&
                other.foodAddon == this.foodAddon &&
                other.foodSize == this.foodSize
    }

    override fun hashCode(): Int {
        var result = foodId.hashCode()
        result = 31 * result + foodAddon.hashCode()
        result = 31 * result + foodSize.hashCode()
        return result
    }
}
