package com.anamuslim.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anamuslim.app.R

// نعتمد خط النظام الافتراضي (يدعم العربية بشكل ممتاز على كل أجهزة أندرويد
// الحديثة عبر Noto Sans Arabic المدمج) لضمان التوافق الكامل بلا أي اعتماديات
// إضافية؛ خط القرآن المتخصص (أميري قرآن) يُستخدم حصرياً داخل شاشة القرآن.
val AppFontFamily = FontFamily.Default

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

/** خط أميري قرآن الأصلي (SIL OFL 1.1) — مصمَّم خصيصاً لكتابة القرآن بالرسم العثماني. */
val QuranFontFamily = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

/** طراز خاص لعرض نص القرآن بخط أميري قرآن. */
val QuranTextStyle = TextStyle(
    fontFamily = QuranFontFamily,
    fontSize = 24.sp,
    lineHeight = 46.sp,
    fontWeight = FontWeight.Normal
)
