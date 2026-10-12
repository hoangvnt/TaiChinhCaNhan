package com.hoangvnt.taichinh.data

import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.TxType

object DefaultData {
    fun categories(): List<Category> {
        val expense = listOf(
            Triple("Ăn uống", "🍜", 0xFFEF5350),
            Triple("Đi lại", "🚗", 0xFF42A5F5),
            Triple("Mua sắm", "🛒", 0xFFAB47BC),
            Triple("Nhà cửa", "🏠", 0xFF8D6E63),
            Triple("Hóa đơn & tiện ích", "💡", 0xFFFFA726),
            Triple("Sức khỏe", "💊", 0xFF26A69A),
            Triple("Giáo dục", "📚", 0xFF5C6BC0),
            Triple("Giải trí", "🎮", 0xFFEC407A),
            Triple("Gia đình & con cái", "👨‍👩‍👧", 0xFF66BB6A),
            Triple("Quà tặng & từ thiện", "🎁", 0xFFFF7043),
            Triple("Bảo hiểm", "🛡️", 0xFF78909C),
            Triple("Chi phí khác", "📦", 0xFF90A4AE),
        )
        val income = listOf(
            Triple("Lương", "💰", 0xFF43A047),
            Triple("Thưởng", "🎉", 0xFFFDD835),
            Triple("Đầu tư & lãi", "📈", 0xFF1E88E5),
            Triple("Kinh doanh", "🛍️", 0xFF8E24AA),
            Triple("Được tặng", "🎁", 0xFFF4511E),
            Triple("Thu nhập khác", "📦", 0xFF90A4AE),
        )
        return expense.mapIndexed { i, (n, ic, c) -> Category(name = n, icon = ic, color = c, type = TxType.EXPENSE, order = i) } +
            income.mapIndexed { i, (n, ic, c) -> Category(name = n, icon = ic, color = c, type = TxType.INCOME, order = i) }
    }

    val palette = listOf(
        0xFFEF5350, 0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2, 0xFF5C6BC0, 0xFF42A5F5,
        0xFF26C6DA, 0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFFFCA28, 0xFFFFA726,
        0xFFFF7043, 0xFF8D6E63, 0xFF78909C,
    )
}
