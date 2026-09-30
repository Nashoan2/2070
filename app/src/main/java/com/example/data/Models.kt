package com.example.data

data class StoreConfig(
  val storeNameAr: String = "المملكة للإلكترونيات",
  val storeNameEn: String = "almamlak Electronics",
  val branch: String = "فرع شارع تعز",
  val phone: String = "772707736",
  val addressAr: String = "إب الشارع العام تحت فندق معين",
  val addressEn: String = "YEMEN Ibb",
  val wmAr: String = "المملكة للإلكترونيات",
  val terms: String = "• البضاعة المباعة لا ترد ولا تستبدل بعد خروجها من المحل.\n• استلمت البضاعة الموضحة أعلاه كاملة ، سليمة ، ولعدد ذلك.",
  val logoBase64: String = ""
)

data class TransactionRecord(
  val date: String,
  val type: String, // "فاتورة", "قبض", "صرف", "افتتاح"
  val amount: Double,
  val currency: String = "YER",
  val note: String = "",
  val voucherNum: String? = null,
  val balanceAfter: Double
)

data class Customer(
  val id: Long,
  val accountNumber: String,
  val name: String,
  val phone: String = "",
  val address: String = "",
  val balance: Double = 0.0,
  val transactions: List<TransactionRecord> = emptyList()
)

data class ExchangeRates(
  val yerToUsd: Double = 0.001876,
  val usdToYer: Double = 533.0,
  val yerToSar: Double = 0.007035,
  val sarToYer: Double = 142.1333,
  val usdToSar: Double = 3.75,
  val sarToUsd: Double = 0.2667
)

data class ExtraItem(
  val id: Long,
  val description: String = "",
  val type: String = "بطولة",
  val price: Double = 0.0,
  val qty: Double = 1.0,
  val currency: String = "YER"
)

data class InvoiceData(
  val id: Long,
  val invNum: String,
  val invType: String = "نقداً", // "نقداً" or "أجل"
  val customerAccount: String = "",
  val customerName: String = "",
  val cardId: String = "",
  val price: Double = 0.0,
  val type: String = "months", // "months" or "tournament"
  val qty: Double = 1.0,
  val desc: String = "تجديد باقة تميز",
  val endDate: String = "",
  val currency: String = "YER",
  val extraItems: List<ExtraItem> = emptyList(),
  val grandTotal: Double = 0.0,
  val createdAt: String = ""
)

data class VoucherItem(
  val voucherNum: String,
  val type: String, // "قبض" or "صرف"
  val account: String,
  val customerName: String,
  val date: String,
  val amount: Double,
  val currency: String = "YER",
  val note: String = ""
)

enum class ButtonSize(val label: String, val heightDp: Int, val fontSizeSp: Int) {
  COMPACT("صغير (موفر للمساحة)", 38, 12),
  MEDIUM("قياسي / متوسط", 48, 14),
  LARGE("كبير وبارز", 56, 16)
}

enum class ButtonLayout(val label: String, val columns: Int) {
  SINGLE("عمود واحد كامل", 1),
  DOUBLE("عمودان متجاوران (شبكة)", 2)
}

data class AppActionButton(
  val id: String,
  val label: String,
  val iconEmoji: String,
  val colorHex: String,
  val actionType: String,
  val isVisible: Boolean = true,
  val isQuickShortcut: Boolean = false,
  val customParam: String = "",
  val customDesc: String = ""
)

data class FormCustomField(
  val id: String,
  val title: String,
  val iconEmoji: String,
  val isVisible: Boolean = true,
  val isRequired: Boolean = false
)

enum class AppThemePreset(
  val id: String,
  val titleAr: String,
  val descAr: String,
  val primaryHex: String,
  val secondaryHex: String,
  val bgHex: String,
  val cardHex: String,
  val textDarkHex: String,
  val accentHex: String,
  val emoji: String
) {
  ROYAL_PURPLE(
    id = "ROYAL_PURPLE",
    titleAr = "المملكة الملكي",
    descAr = "البنفسجي الفاخر مع لمسات ملكية مذهبة",
    primaryHex = "#5E258D",
    secondaryHex = "#8B5CF6",
    bgHex = "#EEF2F5",
    cardHex = "#FFFFFF",
    textDarkHex = "#1E1B4B",
    accentHex = "#EAB308",
    emoji = "👑"
  ),
  MODERN_NAVY(
    id = "MODERN_NAVY",
    titleAr = "الكحلي العصري",
    descAr = "أزرق كحلي عميق مع أزرق سماوي احترافي",
    primaryHex = "#0F172A",
    secondaryHex = "#0284C7",
    bgHex = "#F0F4F8",
    cardHex = "#FFFFFF",
    textDarkHex = "#0F172A",
    accentHex = "#38BDF8",
    emoji = "💎"
  ),
  EMERALD_PRO(
    id = "EMERALD_PRO",
    titleAr = "الزمرد المالي",
    descAr = "أخضر زمردي مع درجات النعناع المنعشة",
    primaryHex = "#065F46",
    secondaryHex = "#10B981",
    bgHex = "#F0FDF4",
    cardHex = "#FFFFFF",
    textDarkHex = "#064E3B",
    accentHex = "#34D399",
    emoji = "🌿"
  ),
  DARK_LUXURY(
    id = "DARK_LUXURY",
    titleAr = "الفخامة الداكنة",
    descAr = "مظهر أسود ليلي فخم مع ذهبي لامع مريح للعين",
    primaryHex = "#18181B",
    secondaryHex = "#3F3F46",
    bgHex = "#09090B",
    cardHex = "#18181B",
    textDarkHex = "#F4F4F5",
    accentHex = "#F59E0B",
    emoji = "🖤"
  ),
  IMPERIAL_RUBY(
    id = "IMPERIAL_RUBY",
    titleAr = "الياقوت الإمبراطوري",
    descAr = "أحمر قرمزي نبيذي دافئ وأنيق",
    primaryHex = "#881337",
    secondaryHex = "#E11D48",
    bgHex = "#FFF1F2",
    cardHex = "#FFFFFF",
    textDarkHex = "#4C0519",
    accentHex = "#FB7185",
    emoji = "🔴"
  ),
  CYBER_TITANIUM(
    id = "CYBER_TITANIUM",
    titleAr = "التيتانيوم التقني",
    descAr = "رمادي معدني صلب مع لمسات فيروزية عصرية",
    primaryHex = "#334155",
    secondaryHex = "#06B6D4",
    bgHex = "#F1F5F9",
    cardHex = "#FFFFFF",
    textDarkHex = "#0F172A",
    accentHex = "#22D3EE",
    emoji = "⚡"
  ),
  SUNSET_GOLD(
    id = "SUNSET_GOLD",
    titleAr = "الغروب الذهبي",
    descAr = "برتقالي عنبري دافئ وذهبي مشرق",
    primaryHex = "#C2410C",
    secondaryHex = "#F97316",
    bgHex = "#FFF7ED",
    cardHex = "#FFFFFF",
    textDarkHex = "#7C2D12",
    accentHex = "#FBBF24",
    emoji = "🌅"
  ),
  SOFT_LAVENDER(
    id = "SOFT_LAVENDER",
    titleAr = "اللافندر الناعم",
    descAr = "درجات الباستيل الهادئة والمريحة جداً للعين",
    primaryHex = "#6B21A8",
    secondaryHex = "#A855F7",
    bgHex = "#FAF5FF",
    cardHex = "#FFFFFF",
    textDarkHex = "#3B0764",
    accentHex = "#F472B6",
    emoji = "🌸"
  ),
  CUSTOM(
    id = "CUSTOM",
    titleAr = "تصميم مخصص مرفوع",
    descAr = "ثيم مخصص تم استيراده أو تركيبه يدوياً لداخل النظام",
    primaryHex = "#5E258D",
    secondaryHex = "#8B5CF6",
    bgHex = "#EEF2F5",
    cardHex = "#FFFFFF",
    textDarkHex = "#111827",
    accentHex = "#EAB308",
    emoji = "🎨"
  )
}

enum class HomeScreenStyle(
  val id: String,
  val titleAr: String,
  val descAr: String,
  val emoji: String
) {
  MODERN_CARDS(
    id = "MODERN_CARDS",
    titleAr = "البطاقات الحديثة",
    descAr = "بطاقات كلاسيكية منظمة مع إبراز الشعار في الترويسة",
    emoji = "📱"
  ),
  EXECUTIVE_HERO(
    id = "EXECUTIVE_HERO",
    titleAr = "الترويسة التنفيذية الفاخرة",
    descAr = "شريط علوي متدرج عريض يعرض شعار الشركة والاسم بشكل ملكي",
    emoji = "🏛️"
  ),
  GLASSMORPHISM(
    id = "GLASSMORPHISM",
    titleAr = "النمط الزجاجي الشفاف",
    descAr = "تأثير زجاجي بلوري أنيق مع حواف عاكسة ومظهر عصري",
    emoji = "🪟"
  ),
  COMPACT_MINIMAL(
    id = "COMPACT_MINIMAL",
    titleAr = "المدمج العملي السريع",
    descAr = "كثافة عالية وتركيز عملي يتيح الوصول السريع لجميع الوظائف",
    emoji = "⚡"
  )
}

data class UiCustomizationConfig(
  val buttonSize: ButtonSize = ButtonSize.MEDIUM,
  val buttonLayout: ButtonLayout = ButtonLayout.SINGLE,
  val showQuickShortcutsBar: Boolean = true,
  val buttons: List<AppActionButton> = defaultButtons(),
  val formFields: List<FormCustomField> = defaultFormFields(),
  val shadedFieldColorHex: String = "#FFF0F3",
  val shadedFieldAlpha: Float = 1.0f,
  val themePresetId: String = AppThemePreset.ROYAL_PURPLE.id,
  val homeScreenStyleId: String = HomeScreenStyle.MODERN_CARDS.id,
  val customPrimaryColorHex: String = "#5E258D",
  val customSecondaryColorHex: String = "#8B5CF6",
  val customBgColorHex: String = "#ECEFF1",
  val customCardColorHex: String = "#FFFFFF",
  val customThemeJson: String = "",
  val customBackgroundImageBase64: String = "",
  val bgImageAlpha: Float = 0.85f
) {
  fun currentTheme(): AppThemePreset {
    return AppThemePreset.entries.find { it.id == themePresetId } ?: AppThemePreset.ROYAL_PURPLE
  }

  fun currentHomeStyle(): HomeScreenStyle {
    return HomeScreenStyle.entries.find { it.id == homeScreenStyleId } ?: HomeScreenStyle.MODERN_CARDS
  }
  companion object {
    fun defaultFormFields(): List<FormCustomField> = listOf(
      FormCustomField(
        id = "INV_HEADER",
        title = "نوع الفاتورة ورقم الفاتورة والعملة",
        iconEmoji = "🔢",
        isVisible = true,
        isRequired = true
      ),
      FormCustomField(
        id = "CUSTOMER_NAME",
        title = "اسم العميل / الحساب",
        iconEmoji = "👤",
        isVisible = true,
        isRequired = false
      ),
      FormCustomField(
        id = "CARD_AND_PRICE",
        title = "رقم الاشتراك / الكارت وسعر الاشتراك",
        iconEmoji = "💳",
        isVisible = true,
        isRequired = false
      ),
      FormCustomField(
        id = "QTY_AND_TYPE",
        title = "العدد (الكمية) ونوع التجديد / المدة",
        iconEmoji = "📦",
        isVisible = true,
        isRequired = false
      ),
      FormCustomField(
        id = "DESCRIPTION",
        title = "الوصف الرئيسي",
        iconEmoji = "📝",
        isVisible = true,
        isRequired = false
      ),
      FormCustomField(
        id = "END_DATE",
        title = "تاريخ الانتهاء",
        iconEmoji = "📅",
        isVisible = true,
        isRequired = false
      ),
      FormCustomField(
        id = "EXTRA_ITEMS",
        title = "الأصناف الإضافية وأزرار الحفظ والإغلاق",
        iconEmoji = "🧾",
        isVisible = true,
        isRequired = true
      )
    )

    fun defaultButtons(): List<AppActionButton> = listOf(
      // 1. إضافة فاتورة جديدة
      AppActionButton(
        id = "NEW_INVOICE",
        label = "إضافة فاتورة جديدة",
        iconEmoji = "➕",
        colorHex = "#3B1E08",
        actionType = "NEW_INVOICE",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 2. إدارة الفواتير المحفوظة
      AppActionButton(
        id = "SAVED_INVOICES",
        label = "إدارة الفواتير المحفوظة",
        iconEmoji = "📁",
        colorHex = "#361A07",
        actionType = "SAVED_INVOICES",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 3. فتح التقرير
      AppActionButton(
        id = "OPEN_REPORT",
        label = "فتح التقرير",
        iconEmoji = "📄",
        colorHex = "#283B4F",
        actionType = "OPEN_REPORT",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 4. إدارة العملاء
      AppActionButton(
        id = "CUSTOMERS",
        label = "إدارة العملاء",
        iconEmoji = "👥",
        colorHex = "#4F3BE0",
        actionType = "CUSTOMERS",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 5. سند قبض
      AppActionButton(
        id = "RECEIPT_VOUCHER",
        label = "سند قبض",
        iconEmoji = "💰",
        colorHex = "#02592F",
        actionType = "RECEIPT_VOUCHER",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 6. سند صرف
      AppActionButton(
        id = "PAYMENT_VOUCHER",
        label = "سند صرف",
        iconEmoji = "💸",
        colorHex = "#DC2626",
        actionType = "PAYMENT_VOUCHER",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 7. جميع العملاء (يظهر في القائمة الرئيسية وأيضاً كأول اختصار سريع)
      AppActionButton(
        id = "ALL_CUSTOMERS",
        label = "جميع العملاء",
        iconEmoji = "📋",
        colorHex = "#1D4ED8",
        actionType = "ALL_CUSTOMERS",
        isVisible = true,
        isQuickShortcut = true
      ),
      // 8. محول العملات (اختصار سريع)
      AppActionButton(
        id = "CURRENCY_CONVERTER",
        label = "محول العملات",
        iconEmoji = "💱",
        colorHex = "#0D7A56",
        actionType = "CURRENCY_CONVERTER",
        isVisible = false,
        isQuickShortcut = true
      ),
      // 9. الآلة الحاسبة (اختصار سريع)
      AppActionButton(
        id = "FULL_CALCULATOR",
        label = "الآلة الحاسبة",
        iconEmoji = "🧮",
        colorHex = "#DC2626",
        actionType = "FULL_CALCULATOR",
        isVisible = false,
        isQuickShortcut = true
      ),
      // 10. النسبة المئوية (اختصار سريع)
      AppActionButton(
        id = "PERCENTAGE_CALCULATOR",
        label = "النسبة المئوية",
        iconEmoji = "٪",
        colorHex = "#4F46E5",
        actionType = "PERCENTAGE_CALCULATOR",
        isVisible = false,
        isQuickShortcut = true
      ),
      // 11. إعدادات المتجر
      AppActionButton(
        id = "SETTINGS",
        label = "إعدادات المتجر",
        iconEmoji = "⚙️",
        colorHex = "#182230",
        actionType = "SETTINGS",
        isVisible = true,
        isQuickShortcut = false
      ),
      // 12. خروج من التطبيق
      AppActionButton(
        id = "EXIT_APP",
        label = "خروج من التطبيق",
        iconEmoji = "🚪",
        colorHex = "#9E1212",
        actionType = "EXIT_APP",
        isVisible = true,
        isQuickShortcut = false
      ),
      // بقية الأزرار المتاحة للاستخدام ويمكن تفعيلها من تخصيص الواجهة
      AppActionButton(
        id = "CUSTOMER_DISPLAY",
        label = "وضع العرض للعميل",
        iconEmoji = "📱",
        colorHex = "#2E7D32",
        actionType = "CUSTOMER_DISPLAY",
        isVisible = false,
        isQuickShortcut = false
      ),
      AppActionButton(
        id = "PRINT_INVOICE",
        label = "طباعة الفاتورة",
        iconEmoji = "🖨️",
        colorHex = "#17A2B8",
        actionType = "PRINT_INVOICE",
        isVisible = false,
        isQuickShortcut = false
      ),
      AppActionButton(
        id = "EXPORT_PDF",
        label = "تصدير الفاتورة PDF",
        iconEmoji = "📄",
        colorHex = "#C62828",
        actionType = "EXPORT_PDF",
        isVisible = false,
        isQuickShortcut = false
      ),
      AppActionButton(
        id = "ROOM_BACKUP",
        label = "النسخ الاحتياطي المحلي",
        iconEmoji = "💾",
        colorHex = "#4682B4",
        actionType = "ROOM_BACKUP",
        isVisible = false,
        isQuickShortcut = false
      ),
      AppActionButton(
        id = "REPORT_CONTROL",
        label = "التحكم في التقارير",
        iconEmoji = "📊",
        colorHex = "#8E44AD",
        actionType = "REPORT_CONTROL",
        isVisible = false,
        isQuickShortcut = false
      )
    )
  }
}

data class ReportCustomizationConfig(
  // 1. حجم الخط (Scale percentage: 80% to 150%)
  val fontScalePercent: Int = 100,

  // 2. نوع الخط (Cairo, Tajawal, Almarai, Amiri, Tahoma, Sans-serif)
  val fontFamily: String = "Cairo",

  // 3. ألوان الخط والترويسة والحدود
  val primaryTextColorHex: String = "#000000",
  val headerColorHex: String = "#5E258D",
  val tableBorderColorHex: String = "#0070BA",
  val tableHeaderBgHex: String = "#EBF5FB",
  val watermarkColorHex: String = "#5E258D",

  // 3.1 ألوان عناصر وبيانات الفاتورة والتقرير المحددة
  val customerNameColorHex: String = "#0070BA", // لون اسم العميل
  val customerAccountColorHex: String = "#C62828", // لون رقم حساب العميل
  val currencySymbolColorHex: String = "#111111", // لون رمز العملة
  val invoiceAmountColorHex: String = "#111111", // لون المبلغ في الفاتورة
  val unitPriceColorHex: String = "#111111", // لون سعر الوحدة
  val totalValueColorHex: String = "#111111", // لون القيمة الإجمالية
  val quantityColorHex: String = "#111111", // لون العدد
  val itemNumberColorHex: String = "#111111", // لون رقم الصنف
  val grandTotalAmountColorHex: String = "#000000", // لون المبلغ الإجمالي

  // 4. إخفاء وإظهار عناصر التقرير
  val showLogo: Boolean = true,
  val showStoreInfo: Boolean = true,
  val showDateTime: Boolean = true,
  val showCustomerAccountNumber: Boolean = true,
  val showBranch: Boolean = true,
  val showAmountInWords: Boolean = true,
  val showTermsAndNotes: Boolean = true,
  val showSignatures: Boolean = true,
  val showWatermark: Boolean = true,
  val showCardSubscriptionBox: Boolean = true,

  // 5. إضافات على التقرير
  val customHeaderTitle: String = "", // نص إضافي أعلى التقرير (مثل: بسم الله الرحمن الرحيم)
  val customFooterText: String = "", // نص إضافي أسفل التقرير
  val taxOrCrNumber: String = "", // الرقم الضريبي أو السجل التجاري
  val showStampSeal: Boolean = false, // ختم معتمد إلكتروني
  val customNoticeBadge: String = "", // شريط ملاحظة أو تنبيه
  val accountantSignatureName: String = "", // اسم المحاسب
  val managerSignatureName: String = "" // اسم المدير
) {
  val fontScale: Float
    get() = (fontScalePercent.coerceIn(70, 180)) / 100.0f
}

