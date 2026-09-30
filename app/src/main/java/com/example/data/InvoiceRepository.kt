package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.room.RoomBackupManager
import com.example.util.ArabicNumberHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(
  val success: Boolean,
  val invoiceCount: Int = 0,
  val customerCount: Int = 0,
  val errorMessage: String = ""
)

class InvoiceRepository(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("mamlaka_prefs", Context.MODE_PRIVATE)
  val roomBackupManager = RoomBackupManager(context)
  private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  var storeConfig: StoreConfig = loadStoreConfig()
    private set

  var customers: MutableList<Customer> = loadCustomers()
    private set

  var exchangeRates: ExchangeRates = loadExchangeRates()
    private set

  var savedInvoices: MutableList<InvoiceData> = loadSavedInvoices()
    private set

  var nextReceiptVoucherNum: Int = prefs.getInt("nextReceiptVoucherNum", 1)
    private set

  var nextPaymentVoucherNum: Int = prefs.getInt("nextPaymentVoucherNum", 1)
    private set

  var uiCustomizationConfig: UiCustomizationConfig = loadUiCustomizationConfig()
    private set

  var reportCustomizationConfig: ReportCustomizationConfig = loadReportCustomizationConfig()
    private set

  init {
    repoScope.launch {
      try {
        val roomInvoices = roomBackupManager.loadInvoicesFromRoom()
        val roomCustomers = roomBackupManager.loadCustomersFromRoom()
        var needUpdate = false
        if (savedInvoices.isEmpty() && roomInvoices.isNotEmpty()) {
          savedInvoices = roomInvoices.toMutableList()
          needUpdate = true
        }
        if (customers.isEmpty() && roomCustomers.isNotEmpty()) {
          customers = roomCustomers.toMutableList()
          needUpdate = true
        }
        if (needUpdate) {
          saveInvoices(savedInvoices)
          saveCustomers(customers)
        }
        // Always guarantee latest state is saved in Room database
        roomBackupManager.syncAllToRoom(savedInvoices, customers)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun loadStoreConfig(): StoreConfig {
    val raw = prefs.getString("storeConfigData", null) ?: return StoreConfig()
    return try {
      val obj = JSONObject(raw)
      StoreConfig(
        storeNameAr = obj.optString("storeNameAr", "المملكة للإلكترونيات"),
        storeNameEn = obj.optString("storeNameEn", "almamlak Electronics"),
        branch = obj.optString("branch", "فرع شارع تعز"),
        phone = obj.optString("phone", "772707736"),
        addressAr = obj.optString("addressAr", "إب الشارع العام تحت فندق معين"),
        addressEn = obj.optString("addressEn", "YEMEN Ibb"),
        wmAr = obj.optString("wmAr", "المملكة للإلكترونيات"),
        terms = obj.optString("terms", "• البضاعة المباعة لا ترد ولا تستبدل بعد خروجها من المحل.\n• استلمت البضاعة الموضحة أعلاه كاملة ، سليمة ، ولعدد ذلك."),
        logoBase64 = obj.optString("logoBase64", "")
      )
    } catch (_: Exception) {
      StoreConfig()
    }
  }

  fun saveStoreConfig(config: StoreConfig) {
    storeConfig = config
    val obj = JSONObject().apply {
      put("storeNameAr", config.storeNameAr)
      put("storeNameEn", config.storeNameEn)
      put("branch", config.branch)
      put("phone", config.phone)
      put("addressAr", config.addressAr)
      put("addressEn", config.addressEn)
      put("wmAr", config.wmAr)
      put("terms", config.terms)
      put("logoBase64", config.logoBase64)
    }
    prefs.edit().putString("storeConfigData", obj.toString()).apply()
  }

  private fun loadUiCustomizationConfig(): UiCustomizationConfig {
    val raw = prefs.getString("uiCustomizationConfig", null) ?: return UiCustomizationConfig()
    return try {
      val obj = JSONObject(raw)
      val uiConfigVersion = obj.optInt("uiConfigVersion", 0)
      if (uiConfigVersion < 3) {
        val def = UiCustomizationConfig()
        saveUiCustomizationConfig(def)
        return def
      }

      val sizeName = obj.optString("buttonSize", ButtonSize.MEDIUM.name)
      val layoutName = obj.optString("buttonLayout", ButtonLayout.SINGLE.name)
      val showShortcuts = obj.optBoolean("showQuickShortcutsBar", true)

      val btnArray = obj.optJSONArray("buttons")
      val loadedButtons = mutableListOf<AppActionButton>()
      if (btnArray != null) {
        for (i in 0 until btnArray.length()) {
          val b = btnArray.getJSONObject(i)
          val btnId = b.optString("id")
          var btnLabel = b.optString("label")
          var btnEmoji = b.optString("iconEmoji")
          var btnColor = b.optString("colorHex")
          var btnAction = b.optString("actionType")

          if (btnId == "PERCENTAGE_CALCULATOR") {
            if (btnLabel.isBlank()) btnLabel = "حاسبة النسبة المئوية"
            if (btnEmoji.isBlank()) btnEmoji = "٪"
            if (btnColor.isBlank()) btnColor = "#1B4D3E"
            if (btnAction.isBlank()) btnAction = "PERCENTAGE_CALCULATOR"
          } else if (btnId == "FULL_CALCULATOR") {
            if (btnLabel.isBlank()) btnLabel = "اله حاسبه النسبة "
            if (btnEmoji.isBlank()) btnEmoji = "🧮"
            if (btnColor.isBlank()) btnColor = "#1E293B"
            if (btnAction.isBlank()) btnAction = "FULL_CALCULATOR"
          }

          loadedButtons.add(
            AppActionButton(
              id = btnId,
              label = btnLabel,
              iconEmoji = btnEmoji,
              colorHex = btnColor,
              actionType = btnAction,
              isVisible = b.optBoolean("isVisible", true),
              isQuickShortcut = b.optBoolean("isQuickShortcut", false),
              customParam = b.optString("customParam", ""),
              customDesc = b.optString("customDesc", "")
            )
          )
        }
      }

      val finalButtons = if (loadedButtons.isEmpty()) {
        UiCustomizationConfig.defaultButtons()
      } else {
        val existingIds = loadedButtons.map { it.id }.toSet()
        val missingDefaults = UiCustomizationConfig.defaultButtons().filter { it.id !in existingIds }
        // Ensure both PERCENTAGE_CALCULATOR and FULL_CALCULATOR have isVisible=true and isQuickShortcut=true if newly added
        val adjustedMissing = missingDefaults.map { def ->
          if (def.id == "PERCENTAGE_CALCULATOR" || def.id == "FULL_CALCULATOR") {
            def.copy(isVisible = true, isQuickShortcut = true)
          } else def
        }
        loadedButtons + adjustedMissing
      }

      val formFieldsArray = obj.optJSONArray("formFields")
      val loadedFields = mutableListOf<FormCustomField>()
      if (formFieldsArray != null) {
        for (i in 0 until formFieldsArray.length()) {
          val f = formFieldsArray.getJSONObject(i)
          loadedFields.add(
            FormCustomField(
              id = f.optString("id"),
              title = f.optString("title"),
              iconEmoji = f.optString("iconEmoji"),
              isVisible = f.optBoolean("isVisible", true),
              isRequired = f.optBoolean("isRequired", false)
            )
          )
        }
      }

      val finalFields = if (loadedFields.isEmpty()) {
        UiCustomizationConfig.defaultFormFields()
      } else {
        val existingFieldIds = loadedFields.map { it.id }.toSet()
        val missingFields = UiCustomizationConfig.defaultFormFields().filter { it.id !in existingFieldIds }
        // Ensure default titles and icons are maintained if empty
        val updatedLoaded = loadedFields.map { loaded ->
          val def = UiCustomizationConfig.defaultFormFields().find { it.id == loaded.id }
          if (def != null) {
            loaded.copy(
              title = if (loaded.title.isBlank()) def.title else loaded.title,
              iconEmoji = if (loaded.iconEmoji.isBlank()) def.iconEmoji else loaded.iconEmoji,
              isRequired = def.isRequired
            )
          } else loaded
        }
        updatedLoaded + missingFields
      }

      val shadedColorHex = obj.optString("shadedFieldColorHex", "#FFF0F3")
      val shadedAlpha = obj.optDouble("shadedFieldAlpha", 1.0).toFloat().coerceIn(0.05f, 1.0f)

      UiCustomizationConfig(
        buttonSize = try { ButtonSize.valueOf(sizeName) } catch (_: Exception) { ButtonSize.MEDIUM },
        buttonLayout = try { ButtonLayout.valueOf(layoutName) } catch (_: Exception) { ButtonLayout.SINGLE },
        showQuickShortcutsBar = showShortcuts,
        buttons = finalButtons,
        formFields = finalFields,
        shadedFieldColorHex = shadedColorHex,
        shadedFieldAlpha = shadedAlpha
      )
    } catch (_: Exception) {
      UiCustomizationConfig()
    }
  }

  fun saveUiCustomizationConfig(config: UiCustomizationConfig) {
    uiCustomizationConfig = config
    val obj = JSONObject().apply {
      put("uiConfigVersion", 3)
      put("buttonSize", config.buttonSize.name)
      put("buttonLayout", config.buttonLayout.name)
      put("showQuickShortcutsBar", config.showQuickShortcutsBar)
      put("shadedFieldColorHex", config.shadedFieldColorHex)
      put("shadedFieldAlpha", config.shadedFieldAlpha.toDouble())
      val arr = JSONArray()
      config.buttons.forEach { b ->
        arr.put(JSONObject().apply {
          put("id", b.id)
          put("label", b.label)
          put("iconEmoji", b.iconEmoji)
          put("colorHex", b.colorHex)
          put("actionType", b.actionType)
          put("isVisible", b.isVisible)
          put("isQuickShortcut", b.isQuickShortcut)
          put("customParam", b.customParam)
          put("customDesc", b.customDesc)
        })
      }
      put("buttons", arr)

      val fArr = JSONArray()
      config.formFields.forEach { f ->
        fArr.put(JSONObject().apply {
          put("id", f.id)
          put("title", f.title)
          put("iconEmoji", f.iconEmoji)
          put("isVisible", f.isVisible)
          put("isRequired", f.isRequired)
        })
      }
      put("formFields", fArr)
    }
    prefs.edit().putString("uiCustomizationConfig", obj.toString()).apply()
  }

  fun resetUiCustomizationConfig(): UiCustomizationConfig {
    val defaults = UiCustomizationConfig()
    saveUiCustomizationConfig(defaults)
    return defaults
  }

  private fun loadReportCustomizationConfig(): ReportCustomizationConfig {
    val raw = prefs.getString("reportCustomizationConfig", null) ?: return ReportCustomizationConfig()
    return try {
      val obj = JSONObject(raw)
      ReportCustomizationConfig(
        fontScalePercent = obj.optInt("fontScalePercent", 100),
        fontFamily = obj.optString("fontFamily", "Cairo"),
        primaryTextColorHex = obj.optString("primaryTextColorHex", "#000000"),
        headerColorHex = obj.optString("headerColorHex", "#5E258D"),
        tableBorderColorHex = obj.optString("tableBorderColorHex", "#0070BA"),
        tableHeaderBgHex = obj.optString("tableHeaderBgHex", "#EBF5FB"),
        watermarkColorHex = obj.optString("watermarkColorHex", "#5E258D"),
        customerNameColorHex = obj.optString("customerNameColorHex", "#0070BA"),
        customerAccountColorHex = obj.optString("customerAccountColorHex", "#C62828"),
        currencySymbolColorHex = obj.optString("currencySymbolColorHex", "#111111"),
        invoiceAmountColorHex = obj.optString("invoiceAmountColorHex", "#111111"),
        unitPriceColorHex = obj.optString("unitPriceColorHex", "#111111"),
        totalValueColorHex = obj.optString("totalValueColorHex", "#111111"),
        quantityColorHex = obj.optString("quantityColorHex", "#111111"),
        itemNumberColorHex = obj.optString("itemNumberColorHex", "#111111"),
        grandTotalAmountColorHex = obj.optString("grandTotalAmountColorHex", "#000000"),
        showLogo = obj.optBoolean("showLogo", true),
        showStoreInfo = obj.optBoolean("showStoreInfo", true),
        showDateTime = obj.optBoolean("showDateTime", true),
        showCustomerAccountNumber = obj.optBoolean("showCustomerAccountNumber", true),
        showBranch = obj.optBoolean("showBranch", true),
        showAmountInWords = obj.optBoolean("showAmountInWords", true),
        showTermsAndNotes = obj.optBoolean("showTermsAndNotes", true),
        showSignatures = obj.optBoolean("showSignatures", true),
        showWatermark = obj.optBoolean("showWatermark", true),
        showCardSubscriptionBox = obj.optBoolean("showCardSubscriptionBox", true),
        customHeaderTitle = obj.optString("customHeaderTitle", ""),
        customFooterText = obj.optString("customFooterText", ""),
        taxOrCrNumber = obj.optString("taxOrCrNumber", ""),
        showStampSeal = obj.optBoolean("showStampSeal", false),
        customNoticeBadge = obj.optString("customNoticeBadge", ""),
        accountantSignatureName = obj.optString("accountantSignatureName", ""),
        managerSignatureName = obj.optString("managerSignatureName", "")
      )
    } catch (_: Exception) {
      ReportCustomizationConfig()
    }
  }

  fun saveReportCustomizationConfig(config: ReportCustomizationConfig) {
    reportCustomizationConfig = config
    val obj = JSONObject().apply {
      put("fontScalePercent", config.fontScalePercent)
      put("fontFamily", config.fontFamily)
      put("primaryTextColorHex", config.primaryTextColorHex)
      put("headerColorHex", config.headerColorHex)
      put("tableBorderColorHex", config.tableBorderColorHex)
      put("tableHeaderBgHex", config.tableHeaderBgHex)
      put("watermarkColorHex", config.watermarkColorHex)
      put("customerNameColorHex", config.customerNameColorHex)
      put("customerAccountColorHex", config.customerAccountColorHex)
      put("currencySymbolColorHex", config.currencySymbolColorHex)
      put("invoiceAmountColorHex", config.invoiceAmountColorHex)
      put("unitPriceColorHex", config.unitPriceColorHex)
      put("totalValueColorHex", config.totalValueColorHex)
      put("quantityColorHex", config.quantityColorHex)
      put("itemNumberColorHex", config.itemNumberColorHex)
      put("grandTotalAmountColorHex", config.grandTotalAmountColorHex)
      put("showLogo", config.showLogo)
      put("showStoreInfo", config.showStoreInfo)
      put("showDateTime", config.showDateTime)
      put("showCustomerAccountNumber", config.showCustomerAccountNumber)
      put("showBranch", config.showBranch)
      put("showAmountInWords", config.showAmountInWords)
      put("showTermsAndNotes", config.showTermsAndNotes)
      put("showSignatures", config.showSignatures)
      put("showWatermark", config.showWatermark)
      put("showCardSubscriptionBox", config.showCardSubscriptionBox)
      put("customHeaderTitle", config.customHeaderTitle)
      put("customFooterText", config.customFooterText)
      put("taxOrCrNumber", config.taxOrCrNumber)
      put("showStampSeal", config.showStampSeal)
      put("customNoticeBadge", config.customNoticeBadge)
      put("accountantSignatureName", config.accountantSignatureName)
      put("managerSignatureName", config.managerSignatureName)
    }
    prefs.edit().putString("reportCustomizationConfig", obj.toString()).apply()
  }

  fun resetReportCustomizationConfig(): ReportCustomizationConfig {
    val defaults = ReportCustomizationConfig()
    saveReportCustomizationConfig(defaults)
    return defaults
  }

  private fun loadCustomers(): MutableList<Customer> {
    val raw = prefs.getString("customersData", null)
    if (raw == null) {
      val defaults = mutableListOf(
        Customer(
          id = 1,
          accountNumber = "100",
          name = "أحمد علي",
          phone = "777111222",
          address = "صنعاء",
          balance = 0.0,
          transactions = emptyList()
        ),
        Customer(
          id = 2,
          accountNumber = "101",
          name = "محمد حسن",
          phone = "777333444",
          address = "عدن",
          balance = 0.0,
          transactions = emptyList()
        )
      )
      saveCustomers(defaults)
      return defaults
    }
    return try {
      val array = JSONArray(raw)
      val list = mutableListOf<Customer>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val txList = mutableListOf<TransactionRecord>()
        val txArray = obj.optJSONArray("transactions")
        if (txArray != null) {
          for (j in 0 until txArray.length()) {
            val tObj = txArray.getJSONObject(j)
            txList.add(
              TransactionRecord(
                date = tObj.optString("date"),
                type = tObj.optString("type"),
                amount = tObj.optDouble("amount"),
                currency = tObj.optString("currency", "$"),
                note = tObj.optString("note"),
                voucherNum = if (tObj.has("voucherNum") && !tObj.isNull("voucherNum")) tObj.optString("voucherNum") else null,
                balanceAfter = tObj.optDouble("balanceAfter")
              )
            )
          }
        }
        list.add(
          Customer(
            id = obj.optLong("id", System.currentTimeMillis()),
            accountNumber = obj.optString("accountNumber", (100 + i).toString()),
            name = obj.optString("name"),
            phone = obj.optString("phone"),
            address = obj.optString("address"),
            balance = obj.optDouble("balance", 0.0),
            transactions = txList
          )
        )
      }
      // Migrate initial untouched template customers from 1001/1002 to 100/101 if applicable
      if (list.size == 2 && list[0].accountNumber == "1001" && list[1].accountNumber == "1002" && list[0].transactions.isEmpty() && list[1].transactions.isEmpty()) {
        val migrated = list.mapIndexed { idx, c -> c.copy(accountNumber = (100 + idx).toString()) }.toMutableList()
        saveCustomers(migrated)
        return migrated
      }
      list
    } catch (_: Exception) {
      mutableListOf()
    }
  }

  fun saveCustomers(list: List<Customer> = customers) {
    customers = list.toMutableList()
    val array = JSONArray()
    for (c in customers) {
      val cObj = JSONObject().apply {
        put("id", c.id)
        put("accountNumber", c.accountNumber)
        put("name", c.name)
        put("phone", c.phone)
        put("address", c.address)
        put("balance", c.balance)
        val txArray = JSONArray()
        for (t in c.transactions) {
          txArray.put(JSONObject().apply {
            put("date", t.date)
            put("type", t.type)
            put("amount", t.amount)
            put("currency", t.currency)
            put("note", t.note)
            put("voucherNum", t.voucherNum)
            put("balanceAfter", t.balanceAfter)
          })
        }
        put("transactions", txArray)
      }
      array.put(cObj)
    }
    prefs.edit().putString("customersData", array.toString()).apply()
    repoScope.launch {
      try {
        roomBackupManager.syncAllToRoom(savedInvoices, customers)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun getNextAccountNumber(): String {
    if (customers.isEmpty()) return "100"
    var maxAcc = 99
    for (c in customers) {
      val num = ArabicNumberHelper.toEngDigits(c.accountNumber).toIntOrNull()
      if (num != null && num > maxAcc) {
        maxAcc = num
      }
    }
    return if (maxAcc < 100) "100" else (maxAcc + 1).toString()
  }

  fun findCustomerByAccount(account: String): Customer? {
    val clean = ArabicNumberHelper.toEngDigits(account).trim()
    return customers.find { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == clean }
  }

  fun recalculateCustomerBalance(customer: Customer): Customer {
    var currentBalance = 0.0
    val updatedTransactions = customer.transactions.map { t ->
      when (t.type) {
        "قبض" -> currentBalance -= t.amount
        "صرف", "فاتورة", "افتتاح" -> {
          if (t.type == "افتتاح") {
            currentBalance += t.amount
          } else {
            currentBalance += t.amount
          }
        }
        else -> currentBalance += t.amount
      }
      t.copy(balanceAfter = currentBalance)
    }
    return customer.copy(balance = currentBalance, transactions = updatedTransactions)
  }

  fun updateCustomerBalance(
    account: String,
    amount: Double,
    type: String, // "قبض", "صرف", "فاتورة"
    note: String,
    voucherNum: String? = null,
    currency: String = "$",
    customDate: String? = null
  ): Customer? {
    val customerIndex = customers.indexOfFirst {
      ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == ArabicNumberHelper.toEngDigits(account).trim()
    }
    if (customerIndex == -1) return null

    val customer = customers[customerIndex]
    val dateToUse = if (!customDate.isNullOrBlank()) customDate else ArabicNumberHelper.formatDateTime()

    val fullNote = note.trim()

    val updatedTx = customer.transactions.toMutableList().apply {
      add(
        TransactionRecord(
          date = dateToUse,
          type = type,
          amount = amount,
          currency = currency,
          note = fullNote,
          voucherNum = voucherNum,
          balanceAfter = 0.0
        )
      )
    }

    val updatedCustomer = recalculateCustomerBalance(customer.copy(transactions = updatedTx))
    customers[customerIndex] = updatedCustomer
    saveCustomers()
    return updatedCustomer
  }

  fun getAllVouchers(type: String): List<VoucherItem> {
    val list = mutableListOf<VoucherItem>()
    for (c in customers) {
      for (t in c.transactions) {
        if (t.type == type && t.voucherNum != null) {
          list.add(
            VoucherItem(
              voucherNum = t.voucherNum,
              type = t.type,
              account = c.accountNumber,
              customerName = c.name,
              date = t.date,
              amount = t.amount,
              currency = t.currency,
              note = t.note
            )
          )
        }
      }
    }
    return list.sortedByDescending { it.voucherNum.toIntOrNull() ?: 0 }
  }

  fun findVoucher(voucherNum: String, type: String, account: String? = null): Pair<Customer, TransactionRecord>? {
    val cleanVNum = ArabicNumberHelper.toEngDigits(voucherNum).trim()
    val cleanAcc = account?.let { ArabicNumberHelper.toEngDigits(it).trim() }

    if (!cleanAcc.isNullOrEmpty()) {
      val customer = customers.find { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == cleanAcc }
      if (customer != null) {
        val tx = customer.transactions.find { t ->
          t.type == type && (
            (t.voucherNum != null && ArabicNumberHelper.toEngDigits(t.voucherNum).trim() == cleanVNum) ||
            t.note.contains("($cleanVNum)") ||
            t.note.contains("رقم $cleanVNum")
          )
        }
        if (tx != null) return Pair(customer, tx)
      }
    }

    for (c in customers) {
      for (t in c.transactions) {
        if (t.type == type && (
          (t.voucherNum != null && ArabicNumberHelper.toEngDigits(t.voucherNum).trim() == cleanVNum) ||
          t.note.contains("($cleanVNum)") ||
          t.note.contains("رقم $cleanVNum")
        )) {
          return Pair(c, t)
        }
      }
    }
    return null
  }

  fun voucherExists(
    voucherNum: String,
    type: String,
    excludeVoucherNum: String? = null,
    excludeAccount: String? = null
  ): Boolean {
    val cleanNum = ArabicNumberHelper.toEngDigits(voucherNum).trim()
    if (cleanNum.isEmpty()) return false
    val cleanExcludeNum = excludeVoucherNum?.let { ArabicNumberHelper.toEngDigits(it).trim() }
    val cleanExcludeAcc = excludeAccount?.let { ArabicNumberHelper.toEngDigits(it).trim() }

    for (c in customers) {
      val isExcludedAcc = cleanExcludeAcc != null && ArabicNumberHelper.toEngDigits(c.accountNumber).trim() == cleanExcludeAcc
      for (t in c.transactions) {
        if (t.type != type) continue
        val vNum = t.voucherNum?.let { ArabicNumberHelper.toEngDigits(it).trim() }
        val note = ArabicNumberHelper.toEngDigits(t.note)

        val matchesNum = (vNum != null && vNum == cleanNum) ||
            note.contains("($cleanNum)") ||
            note.contains("رقم $cleanNum")

        if (matchesNum) {
          if (isExcludedAcc && cleanExcludeNum != null) {
            val isOldSelf = (vNum != null && vNum == cleanExcludeNum) ||
                note.contains("($cleanExcludeNum)") ||
                note.contains("رقم $cleanExcludeNum")
            if (isOldSelf) {
              continue
            }
          }
          return true
        }
      }
    }
    return false
  }

  fun getUniqueNextReceiptVoucherNumber(): String {
    var num = nextReceiptVoucherNum
    while (voucherExists(num.toString(), "قبض")) {
      num++
    }
    nextReceiptVoucherNum = num + 1
    prefs.edit().putInt("nextReceiptVoucherNum", nextReceiptVoucherNum).apply()
    return num.toString()
  }

  fun getUniqueNextPaymentVoucherNumber(): String {
    var num = nextPaymentVoucherNum
    while (voucherExists(num.toString(), "صرف")) {
      num++
    }
    nextPaymentVoucherNum = num + 1
    prefs.edit().putInt("nextPaymentVoucherNum", nextPaymentVoucherNum).apply()
    return num.toString()
  }

  fun editVoucher(
    oldVoucherNum: String,
    type: String,
    targetAccount: String,
    newAmount: Double,
    newCurrency: String,
    newNote: String,
    newDate: String,
    newVoucherNum: String = oldVoucherNum,
    origAccount: String? = null
  ): Boolean {
    val cleanOldVNum = ArabicNumberHelper.toEngDigits(oldVoucherNum).trim()
    val match = findVoucher(cleanOldVNum, type, origAccount) ?: return false
    val (origCustomer, origTx) = match
    val cleanTargetAcc = ArabicNumberHelper.toEngDigits(targetAccount).trim()
    val cleanOrigAcc = ArabicNumberHelper.toEngDigits(origCustomer.accountNumber).trim()
    val finalVoucherNum = ArabicNumberHelper.toEngDigits(newVoucherNum).trim().ifEmpty { cleanOldVNum }

    val formattedNote = newNote.trim()

    val updatedRecord = origTx.copy(
      voucherNum = finalVoucherNum,
      amount = newAmount,
      currency = newCurrency,
      note = formattedNote,
      date = if (newDate.isNotBlank()) newDate else origTx.date
    )

    // Advance sequence if higher voucher number entered
    finalVoucherNum.toIntOrNull()?.let { num ->
      if (type == "قبض" && num >= nextReceiptVoucherNum) {
        nextReceiptVoucherNum = num + 1
        prefs.edit().putInt("nextReceiptVoucherNum", nextReceiptVoucherNum).apply()
      } else if (type == "صرف" && num >= nextPaymentVoucherNum) {
        nextPaymentVoucherNum = num + 1
        prefs.edit().putInt("nextPaymentVoucherNum", nextPaymentVoucherNum).apply()
      }
    }

    fun isMatchingVoucherTx(t: TransactionRecord): Boolean {
      if (t.type != type) return false
      val vNum = t.voucherNum?.let { ArabicNumberHelper.toEngDigits(it).trim() }
      if (!vNum.isNullOrEmpty() && (vNum == cleanOldVNum || vNum == finalVoucherNum)) return true
      val engNote = ArabicNumberHelper.toEngDigits(t.note)
      if (engNote.contains("($cleanOldVNum)") || engNote.contains("($finalVoucherNum)")) return true
      if (engNote.contains("رقم $cleanOldVNum") || engNote.contains("رقم $finalVoucherNum")) return true
      return false
    }

    if (cleanTargetAcc == cleanOrigAcc) {
      // Same customer: update in place and remove any duplicate records
      val custIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == cleanOrigAcc }
      if (custIdx == -1) return false
      val currentCust = customers[custIdx]
      var replaced = false
      val newTxList = currentCust.transactions.mapNotNull { t ->
        if (isMatchingVoucherTx(t)) {
          if (!replaced) {
            replaced = true
            updatedRecord
          } else {
            null // Remove duplicate
          }
        } else {
          t
        }
      }
      customers[custIdx] = recalculateCustomerBalance(currentCust.copy(transactions = newTxList))
    } else {
      // Different customer: transfer transaction
      val targetIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == cleanTargetAcc }
      if (targetIdx == -1) return false
      val origIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == cleanOrigAcc }
      if (origIdx == -1) return false

      // Remove from original
      val currentOrigCust = customers[origIdx]
      val origNewTx = currentOrigCust.transactions.filterNot { isMatchingVoucherTx(it) }
      customers[origIdx] = recalculateCustomerBalance(currentOrigCust.copy(transactions = origNewTx))

      // Add to target, removing any duplicates
      val currentTargetCust = customers[targetIdx]
      val targetCleanedTx = currentTargetCust.transactions.filterNot { isMatchingVoucherTx(it) }
      val targetNewTx = targetCleanedTx + updatedRecord
      customers[targetIdx] = recalculateCustomerBalance(currentTargetCust.copy(transactions = targetNewTx))
    }

    saveCustomers()
    return true
  }

  fun deleteVoucher(voucherNum: String, type: String, account: String? = null): Boolean {
    val cleanVNum = ArabicNumberHelper.toEngDigits(voucherNum).trim()
    val match = findVoucher(cleanVNum, type, account) ?: return false
    val (origCustomer, _) = match
    val cleanOrigAcc = ArabicNumberHelper.toEngDigits(origCustomer.accountNumber).trim()
    val custIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == cleanOrigAcc }
    if (custIdx == -1) return false

    fun isMatchingVoucherTx(t: TransactionRecord): Boolean {
      if (t.type != type) return false
      val vNum = t.voucherNum?.let { ArabicNumberHelper.toEngDigits(it).trim() }
      if (!vNum.isNullOrEmpty() && vNum == cleanVNum) return true
      val engNote = ArabicNumberHelper.toEngDigits(t.note)
      if (engNote.contains("($cleanVNum)") || engNote.contains("رقم $cleanVNum")) return true
      return false
    }

    val updatedTx = origCustomer.transactions.filterNot { isMatchingVoucherTx(it) }
    customers[custIdx] = recalculateCustomerBalance(origCustomer.copy(transactions = updatedTx))
    saveCustomers()
    return true
  }

  fun updateSavedInvoice(updatedInvoice: InvoiceData, oldInvoice: InvoiceData? = null): Boolean {
    val targetId = updatedInvoice.id
    val cleanNewInvNum = ArabicNumberHelper.toEngDigits(updatedInvoice.invNum).trim()

    // Locate existing invoice by ID or invoice number
    val idx = savedInvoices.indexOfFirst {
      it.id == targetId || ArabicNumberHelper.toEngDigits(it.invNum).trim() == cleanNewInvNum
    }
    val existingOldInv = oldInvoice ?: (if (idx != -1) savedInvoices[idx] else null)
    val cleanOldInvNum = existingOldInv?.let { ArabicNumberHelper.toEngDigits(it.invNum).trim() } ?: cleanNewInvNum
    val oldCustAcc = existingOldInv?.let { ArabicNumberHelper.toEngDigits(it.customerAccount).trim() } ?: ""
    val newCustAcc = ArabicNumberHelper.toEngDigits(updatedInvoice.customerAccount).trim()

    // 1. Update savedInvoices list without duplicates
    val updatedInvoices = savedInvoices.filterNot {
      it.id == targetId || ArabicNumberHelper.toEngDigits(it.invNum).trim() == cleanNewInvNum
    }.toMutableList()

    val insertIndex = if (idx in 0..updatedInvoices.size) idx else 0
    updatedInvoices.add(insertIndex, updatedInvoice)
    savedInvoices.clear()
    savedInvoices.addAll(updatedInvoices)
    saveInvoices()

    // 2. Synchronize transactions in customer accounts
    val newInvNote = if (updatedInvoice.desc.isNotBlank()) {
      "فاتورة رقم (${updatedInvoice.invNum}) - ${updatedInvoice.desc}"
    } else {
      "فاتورة رقم (${updatedInvoice.invNum})"
    }

    val updatedTxRecord = TransactionRecord(
      date = if (updatedInvoice.createdAt.isNotBlank()) updatedInvoice.createdAt else ArabicNumberHelper.formatDateTime(),
      type = "فاتورة",
      amount = updatedInvoice.grandTotal,
      currency = updatedInvoice.currency,
      note = newInvNote,
      voucherNum = updatedInvoice.invNum,
      balanceAfter = 0.0
    )

    fun isMatchingInvoiceTx(t: TransactionRecord): Boolean {
      if (t.type != "فاتورة") return false
      val vNum = t.voucherNum?.let { ArabicNumberHelper.toEngDigits(it).trim() }
      if (!vNum.isNullOrEmpty() && (vNum == cleanOldInvNum || vNum == cleanNewInvNum)) return true
      val engNote = ArabicNumberHelper.toEngDigits(t.note)
      if (engNote.contains("($cleanOldInvNum)") || engNote.contains("($cleanNewInvNum)")) return true
      if (engNote.contains("رقم $cleanOldInvNum") || engNote.contains("رقم $cleanNewInvNum")) return true
      if (existingOldInv != null && t.date == existingOldInv.createdAt && Math.abs(t.amount - existingOldInv.grandTotal) < 0.001) return true
      return false
    }

    // A) If customer account changed, remove old invoice transactions from previous customer
    if (oldCustAcc.isNotEmpty() && oldCustAcc != newCustAcc) {
      val oldCustIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == oldCustAcc }
      if (oldCustIdx != -1) {
        val oldCust = customers[oldCustIdx]
        val filteredTx = oldCust.transactions.filterNot { isMatchingInvoiceTx(it) }
        customers[oldCustIdx] = recalculateCustomerBalance(oldCust.copy(transactions = filteredTx))
      }
    }

    // B) If invoice is "أجل", update transaction in target customer
    if (updatedInvoice.invType == "أجل") {
      val targetCustIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == newCustAcc }
      if (targetCustIdx != -1) {
        val targetCust = customers[targetCustIdx]
        val existingTxIndex = targetCust.transactions.indexOfFirst { isMatchingInvoiceTx(it) }
        val newTxList: List<TransactionRecord>
        if (existingTxIndex != -1) {
          // Replace in place and eliminate any duplicates
          var replaced = false
          newTxList = targetCust.transactions.mapNotNull { t ->
            if (isMatchingInvoiceTx(t)) {
              if (!replaced) {
                replaced = true
                updatedTxRecord
              } else {
                null // remove duplicate
              }
            } else {
              t
            }
          }
        } else {
          newTxList = targetCust.transactions + updatedTxRecord
        }
        customers[targetCustIdx] = recalculateCustomerBalance(targetCust.copy(transactions = newTxList))
      }
    } else {
      // If invoice changed to "نقداً", ensure it is removed from customer transactions
      val targetCustIdx = customers.indexOfFirst { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == newCustAcc }
      if (targetCustIdx != -1) {
        val targetCust = customers[targetCustIdx]
        val filteredTx = targetCust.transactions.filterNot { isMatchingInvoiceTx(it) }
        if (filteredTx.size != targetCust.transactions.size) {
          customers[targetCustIdx] = recalculateCustomerBalance(targetCust.copy(transactions = filteredTx))
        }
      }
    }

    saveCustomers()
    return true
  }

  fun deleteCustomer(account: String): Boolean {
    val clean = ArabicNumberHelper.toEngDigits(account).trim()
    val removed = customers.removeAll { ArabicNumberHelper.toEngDigits(it.accountNumber).trim() == clean }
    if (removed) {
      saveCustomers()
    }
    return removed
  }

  fun incrementReceiptVoucher(): Int {
    val current = nextReceiptVoucherNum
    nextReceiptVoucherNum++
    prefs.edit().putInt("nextReceiptVoucherNum", nextReceiptVoucherNum).apply()
    return current
  }

  fun updateNextReceiptVoucherNum(num: Int) {
    if (num > nextReceiptVoucherNum) {
      nextReceiptVoucherNum = num
      prefs.edit().putInt("nextReceiptVoucherNum", nextReceiptVoucherNum).apply()
    }
  }

  fun incrementPaymentVoucher(): Int {
    val current = nextPaymentVoucherNum
    nextPaymentVoucherNum++
    prefs.edit().putInt("nextPaymentVoucherNum", nextPaymentVoucherNum).apply()
    return current
  }

  fun updateNextPaymentVoucherNum(num: Int) {
    if (num > nextPaymentVoucherNum) {
      nextPaymentVoucherNum = num
      prefs.edit().putInt("nextPaymentVoucherNum", nextPaymentVoucherNum).apply()
    }
  }

  private fun loadExchangeRates(): ExchangeRates {
    val raw = prefs.getString("exchangeRatesData", null) ?: return ExchangeRates()
    return try {
      val obj = JSONObject(raw)
      ExchangeRates(
        yerToUsd = obj.optDouble("yerToUsd", 0.001),
        usdToYer = obj.optDouble("usdToYer", 1000.0),
        yerToSar = obj.optDouble("yerToSar", 0.0027),
        sarToYer = obj.optDouble("sarToYer", 370.0),
        usdToSar = obj.optDouble("usdToSar", 3.75),
        sarToUsd = obj.optDouble("sarToUsd", 0.2667)
      )
    } catch (_: Exception) {
      ExchangeRates()
    }
  }

  fun saveExchangeRates(rates: ExchangeRates) {
    exchangeRates = rates
    val obj = JSONObject().apply {
      put("yerToUsd", rates.yerToUsd)
      put("usdToYer", rates.usdToYer)
      put("yerToSar", rates.yerToSar)
      put("sarToYer", rates.sarToYer)
      put("usdToSar", rates.usdToSar)
      put("sarToUsd", rates.sarToUsd)
    }
    prefs.edit().putString("exchangeRatesData", obj.toString()).apply()
  }

  fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
    val from = if (fromCurrency == "USD") "$" else fromCurrency
    val to = if (toCurrency == "USD") "$" else toCurrency
    if (from == to) return amount

    val rate = when ("$from->$to") {
      "$->YER" -> exchangeRates.usdToYer
      "YER->$" -> exchangeRates.yerToUsd
      "$->SAR" -> exchangeRates.usdToSar
      "SAR->$" -> exchangeRates.sarToUsd
      "YER->SAR" -> exchangeRates.yerToSar
      "SAR->YER" -> exchangeRates.sarToYer
      else -> 1.0
    }
    return if (rate > 0) amount * rate else amount
  }

  private fun loadSavedInvoices(): MutableList<InvoiceData> {
    val raw = prefs.getString("savedInvoicesData", null) ?: return mutableListOf()
    return try {
      val array = JSONArray(raw)
      val list = mutableListOf<InvoiceData>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val extraArray = obj.optJSONArray("extraItems")
        val extras = mutableListOf<ExtraItem>()
        if (extraArray != null) {
          for (j in 0 until extraArray.length()) {
            val eObj = extraArray.getJSONObject(j)
            extras.add(
              ExtraItem(
                id = eObj.optLong("id", j.toLong()),
                description = eObj.optString("description"),
                type = eObj.optString("type", "بطولة"),
                price = eObj.optDouble("price", 0.0),
                qty = eObj.optDouble("qty", 1.0),
                currency = eObj.optString("currency", "$")
              )
            )
          }
        }
        list.add(
          InvoiceData(
            id = obj.optLong("id", System.currentTimeMillis()),
            invNum = obj.optString("invNum", "1"),
            invType = obj.optString("invType", "نقداً"),
            customerAccount = obj.optString("customerAccount"),
            customerName = obj.optString("customerName"),
            cardId = obj.optString("cardId"),
            price = obj.optDouble("price", 0.0),
            type = obj.optString("type", "months"),
            qty = obj.optDouble("qty", 1.0),
            desc = obj.optString("desc").trim().ifEmpty { "تجديد باقه تميز" },
            endDate = obj.optString("endDate"),
            currency = obj.optString("currency", "$"),
            extraItems = extras,
            grandTotal = obj.optDouble("grandTotal", 0.0),
            createdAt = obj.optString("createdAt")
          )
        )
      }
      list
    } catch (_: Exception) {
      mutableListOf()
    }
  }

  fun saveInvoices(list: List<InvoiceData> = savedInvoices) {
    savedInvoices = list.toMutableList()
    val array = JSONArray()
    for (inv in savedInvoices) {
      val obj = JSONObject().apply {
        put("id", inv.id)
        put("invNum", inv.invNum)
        put("invType", inv.invType)
        put("customerAccount", inv.customerAccount)
        put("customerName", inv.customerName)
        put("cardId", inv.cardId)
        put("price", inv.price)
        put("type", inv.type)
        put("qty", inv.qty)
        put("desc", inv.desc)
        put("endDate", inv.endDate)
        put("currency", inv.currency)
        put("grandTotal", inv.grandTotal)
        put("createdAt", inv.createdAt)
        val exArray = JSONArray()
        for (e in inv.extraItems) {
          exArray.put(JSONObject().apply {
            put("id", e.id)
            put("description", e.description)
            put("type", e.type)
            put("price", e.price)
            put("qty", e.qty)
            put("currency", e.currency)
          })
        }
        put("extraItems", exArray)
      }
      array.put(obj)
    }
    prefs.edit().putString("savedInvoicesData", array.toString()).apply()
    repoScope.launch {
      try {
        roomBackupManager.syncAllToRoom(savedInvoices, customers)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun getNextInvoiceNumber(): String {
    var maxNum = 0
    for (inv in savedInvoices) {
      val num = ArabicNumberHelper.toEngDigits(inv.invNum).toIntOrNull() ?: 0
      if (num > maxNum) maxNum = num
    }
    return (maxNum + 1).toString()
  }

  fun exportAllBackupJson(): String {
    return roomBackupManager.buildBackupJson(
      invoices = savedInvoices,
      customers = customers,
      storeConfig = storeConfig,
      exchangeRates = exchangeRates,
      nextReceiptVoucherNum = nextReceiptVoucherNum,
      nextPaymentVoucherNum = nextPaymentVoucherNum
    )
  }

  private fun cleanJson(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("\uFEFF")) {
      s = s.substring(1).trim()
    }
    if (s.startsWith("```")) {
      val firstNewline = s.indexOf('\n')
      s = if (firstNewline != -1) {
        s.substring(firstNewline + 1)
      } else {
        s.removePrefix("```json").removePrefix("```")
      }
      if (s.endsWith("```")) {
        s = s.substring(0, s.length - 3)
      }
      s = s.trim()
    }
    return s
  }

  private fun parseSafeDouble(obj: JSONObject, key: String, default: Double = 0.0): Double {
    if (!obj.has(key) || obj.isNull(key)) return default
    val raw = obj.opt(key) ?: return default
    if (raw is Number) return raw.toDouble()
    val str = ArabicNumberHelper.toEngDigits(raw.toString().trim())
      .replace(",", "")
      .replace("$", "")
      .replace("ر.ي", "")
      .replace("ر.س", "")
      .trim()
    return str.toDoubleOrNull() ?: default
  }

  private fun parseSafeLong(obj: JSONObject, key: String, default: Long = 0L): Long {
    if (!obj.has(key) || obj.isNull(key)) return default
    val raw = obj.opt(key) ?: return default
    if (raw is Number) return raw.toLong()
    val str = ArabicNumberHelper.toEngDigits(raw.toString().trim())
      .replace(",", "")
      .trim()
    return str.toLongOrNull() ?: default
  }

  private fun parseInvoicesArray(invArray: JSONArray): List<InvoiceData> {
    val invList = mutableListOf<InvoiceData>()
    for (i in 0 until invArray.length()) {
      val o = invArray.optJSONObject(i) ?: continue
      val exList = mutableListOf<ExtraItem>()
      val exArray = o.optJSONArray("extraItems")
      if (exArray != null) {
        for (j in 0 until exArray.length()) {
          val eo = exArray.optJSONObject(j) ?: continue
          exList.add(
            ExtraItem(
              id = parseSafeLong(eo, "id", System.currentTimeMillis() + j),
              description = eo.optString("description", eo.optString("desc", "")),
              type = eo.optString("type", "بطولة"),
              price = parseSafeDouble(eo, "price", 0.0),
              qty = parseSafeDouble(eo, "qty", 1.0),
              currency = eo.optString("currency", "$")
            )
          )
        }
      }
      val invId = parseSafeLong(o, "id", System.currentTimeMillis() + i)
      val invNum = ArabicNumberHelper.toEngDigits(o.optString("invNum", (i + 1).toString()))
      val invType = o.optString("invType", "نقداً")
      val customerAccount = o.optString("customerAccount", "")
      val customerName = o.optString("customerName", "")
      val cardId = o.optString("cardId", "")
      val price = parseSafeDouble(o, "price", 0.0)
      val type = o.optString("type", "months")
      val qty = parseSafeDouble(o, "qty", 1.0)
      val desc = o.optString("desc").trim().ifEmpty { "تجديد باقه تميز" }
      val endDate = o.optString("endDate", "")
      val currency = o.optString("currency", "$")
      val grandTotal = parseSafeDouble(o, "grandTotal", price * qty)
      val createdAt = o.optString("createdAt", "")

      invList.add(
        InvoiceData(
          id = invId,
          invNum = invNum,
          invType = invType,
          customerAccount = customerAccount,
          customerName = customerName,
          cardId = cardId,
          price = price,
          type = type,
          qty = qty,
          desc = desc,
          endDate = endDate,
          currency = currency,
          extraItems = exList,
          grandTotal = grandTotal,
          createdAt = createdAt
        )
      )
    }
    return invList
  }

  private fun parseCustomersArray(cArray: JSONArray): List<Customer> {
    val cList = mutableListOf<Customer>()
    for (i in 0 until cArray.length()) {
      val co = cArray.optJSONObject(i) ?: continue
      val txList = mutableListOf<TransactionRecord>()
      val txArray = co.optJSONArray("transactions")
      if (txArray != null) {
        for (j in 0 until txArray.length()) {
          val to = txArray.optJSONObject(j) ?: continue
          val rawVoucher = to.opt("voucherNum")?.toString()?.trim()
          val voucherNum = if (rawVoucher != null && rawVoucher != "null" && rawVoucher.isNotBlank()) rawVoucher else null
          txList.add(
            TransactionRecord(
              date = to.optString("date", ""),
              type = to.optString("type", ""),
              amount = parseSafeDouble(to, "amount", 0.0),
              currency = to.optString("currency", "$"),
              note = to.optString("note", ""),
              voucherNum = voucherNum,
              balanceAfter = parseSafeDouble(to, "balanceAfter", 0.0)
            )
          )
        }
      }
      val cId = parseSafeLong(co, "id", System.currentTimeMillis() + i)
      val accNum = ArabicNumberHelper.toEngDigits(co.optString("accountNumber", (100 + i).toString()))
      val name = co.optString("name", "")
      val phone = co.optString("phone", "")
      val address = co.optString("address", "")
      val balance = parseSafeDouble(co, "balance", 0.0)

      cList.add(
        Customer(
          id = cId,
          accountNumber = accNum,
          name = name,
          phone = phone,
          address = address,
          balance = balance,
          transactions = txList
        )
      )
    }
    return cList
  }

  fun importBackupJson(jsonStr: String): ImportResult {
    return try {
      val clean = cleanJson(jsonStr)
      if (clean.isBlank()) {
        return ImportResult(false, errorMessage = "ملف أو نص النسخة الاحتياطية فارغ.")
      }

      // Case 1: Direct JSON Array
      if (clean.startsWith("[")) {
        val arr = JSONArray(clean)
        if (arr.length() == 0) {
          return ImportResult(false, errorMessage = "مصفوفة البيانات فارغة.")
        }
        val first = arr.optJSONObject(0)
        if (first != null && (first.has("invNum") || first.has("cardId") || first.has("customerName") || first.has("grandTotal") || first.has("extraItems"))) {
          val parsedInvoices = parseInvoicesArray(arr)
          saveInvoices(parsedInvoices)
          return ImportResult(true, invoiceCount = parsedInvoices.size, customerCount = customers.size)
        } else if (first != null && (first.has("accountNumber") || first.has("transactions") || first.has("balance"))) {
          val parsedCustomers = parseCustomersArray(arr)
          saveCustomers(parsedCustomers)
          return ImportResult(true, invoiceCount = savedInvoices.size, customerCount = parsedCustomers.size)
        } else {
          return ImportResult(false, errorMessage = "تنسيق مصفوفة البيانات غير معروف.")
        }
      }

      // Case 2: JSON Object
      var root = JSONObject(clean)
      if (root.has("backupDataJson")) {
        val inner = cleanJson(root.getString("backupDataJson"))
        if (inner.startsWith("{")) {
          root = JSONObject(inner)
        }
      }
      if (root.has("data") && root.optJSONObject("data") != null) {
        root = root.getJSONObject("data")
      }

      var restoredInvoices = false
      var restoredCustomers = false
      var restoredStore = false
      var restoredRates = false

      // 1. Invoices
      val invArray = root.optJSONArray("savedInvoices")
        ?: root.optJSONArray("invoices")
        ?: root.optJSONArray("invoiceList")
        ?: (if (root.has("savedInvoicesData")) {
          try { JSONArray(root.getString("savedInvoicesData")) } catch (_: Exception) { null }
        } else null)

      if (invArray != null) {
        val parsedInvoices = parseInvoicesArray(invArray)
        savedInvoices = parsedInvoices.toMutableList()
        val array = JSONArray()
        for (inv in savedInvoices) {
          val obj = JSONObject().apply {
            put("id", inv.id)
            put("invNum", inv.invNum)
            put("invType", inv.invType)
            put("customerAccount", inv.customerAccount)
            put("customerName", inv.customerName)
            put("cardId", inv.cardId)
            put("price", inv.price)
            put("type", inv.type)
            put("qty", inv.qty)
            put("desc", inv.desc)
            put("endDate", inv.endDate)
            put("currency", inv.currency)
            put("grandTotal", inv.grandTotal)
            put("createdAt", inv.createdAt)
            val extraArray = JSONArray()
            for (ex in inv.extraItems) {
              extraArray.put(JSONObject().apply {
                put("id", ex.id)
                put("description", ex.description)
                put("type", ex.type)
                put("price", ex.price)
                put("qty", ex.qty)
                put("currency", ex.currency)
              })
            }
            put("extraItems", extraArray)
          }
          array.put(obj)
        }
        prefs.edit().putString("savedInvoicesData", array.toString()).apply()
        restoredInvoices = true
      }

      // 2. Customers
      val cArray = root.optJSONArray("customers")
        ?: root.optJSONArray("customerList")
        ?: root.optJSONArray("clients")
        ?: (if (root.has("customersData")) {
          try { JSONArray(root.getString("customersData")) } catch (_: Exception) { null }
        } else null)

      if (cArray != null) {
        val parsedCustomers = parseCustomersArray(cArray)
        customers = parsedCustomers.toMutableList()
        val array = JSONArray()
        for (c in customers) {
          val cObj = JSONObject().apply {
            put("id", c.id)
            put("accountNumber", c.accountNumber)
            put("name", c.name)
            put("phone", c.phone)
            put("address", c.address)
            put("balance", c.balance)
            val txArray = JSONArray()
            for (t in c.transactions) {
              txArray.put(JSONObject().apply {
                put("date", t.date)
                put("type", t.type)
                put("amount", t.amount)
                put("currency", t.currency)
                put("note", t.note)
                put("voucherNum", t.voucherNum)
                put("balanceAfter", t.balanceAfter)
              })
            }
            put("transactions", txArray)
          }
          array.put(cObj)
        }
        prefs.edit().putString("customersData", array.toString()).apply()
        restoredCustomers = true
      }

      // 3. Store Config
      val sc = root.optJSONObject("storeConfig")
        ?: root.optJSONObject("store")
        ?: root.optJSONObject("config")
      if (sc != null) {
        saveStoreConfig(
          StoreConfig(
            storeNameAr = sc.optString("storeNameAr", storeConfig.storeNameAr),
            storeNameEn = sc.optString("storeNameEn", storeConfig.storeNameEn),
            branch = sc.optString("branch", storeConfig.branch),
            phone = sc.optString("phone", storeConfig.phone),
            addressAr = sc.optString("addressAr", storeConfig.addressAr),
            addressEn = sc.optString("addressEn", storeConfig.addressEn),
            wmAr = sc.optString("wmAr", storeConfig.wmAr),
            terms = sc.optString("terms", storeConfig.terms),
            logoBase64 = sc.optString("logoBase64", storeConfig.logoBase64)
          )
        )
        restoredStore = true
      }

      // 4. Exchange Rates
      val r = root.optJSONObject("exchangeRates")
        ?: root.optJSONObject("rates")
      if (r != null) {
        saveExchangeRates(
          ExchangeRates(
            yerToUsd = parseSafeDouble(r, "yerToUsd", exchangeRates.yerToUsd),
            usdToYer = parseSafeDouble(r, "usdToYer", exchangeRates.usdToYer),
            yerToSar = parseSafeDouble(r, "yerToSar", exchangeRates.yerToSar),
            sarToYer = parseSafeDouble(r, "sarToYer", exchangeRates.sarToYer),
            usdToSar = parseSafeDouble(r, "usdToSar", exchangeRates.usdToSar),
            sarToUsd = parseSafeDouble(r, "sarToUsd", exchangeRates.sarToUsd)
          )
        )
        restoredRates = true
      }

      // 5. Counters
      if (root.has("nextReceiptVoucherNum")) {
        nextReceiptVoucherNum = root.optInt("nextReceiptVoucherNum", nextReceiptVoucherNum)
        prefs.edit().putInt("nextReceiptVoucherNum", nextReceiptVoucherNum).apply()
      }
      if (root.has("nextPaymentVoucherNum")) {
        nextPaymentVoucherNum = root.optInt("nextPaymentVoucherNum", nextPaymentVoucherNum)
        prefs.edit().putInt("nextPaymentVoucherNum", nextPaymentVoucherNum).apply()
      }

      if (!restoredInvoices && !restoredCustomers && !restoredStore && !restoredRates) {
        return ImportResult(false, errorMessage = "لم يتم العثور على بيانات صالحة (فواتير، عملاء، أو إعدادات) في هذا الملف.")
      }

      // Synchronize atomically to Room
      repoScope.launch {
        try {
          roomBackupManager.syncAllToRoom(savedInvoices, customers)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      ImportResult(
        success = true,
        invoiceCount = savedInvoices.size,
        customerCount = customers.size
      )
    } catch (e: Exception) {
      e.printStackTrace()
      ImportResult(false, errorMessage = "خطأ في قراءة ملف النسخة: ${e.localizedMessage ?: e.message}")
    }
  }
}
