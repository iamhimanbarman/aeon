package com.aeon.app.ui.screens.finance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aeon.app.BuildConfig
import com.aeon.app.data.ai.AiPreferences
import com.aeon.app.data.ai.BedrockAiRemoteClient
import com.aeon.app.data.ai.DebugLocalAiKeyProvider
import com.aeon.app.data.local.database.entities.FinanceCategoryStorage
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.domain.ai.AiModel
import com.aeon.app.domain.ai.AiModelApi
import com.aeon.app.domain.ai.AiPromptMessage
import com.aeon.app.domain.ai.AiPromptRole
import com.aeon.app.domain.ai.AiRemoteResult
import com.aeon.app.domain.ai.AiRequestOptions
import com.aeon.app.presentation.viewmodel.FinanceTransactionInput
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class FinanceImportSource {
    Manual,
    Camera,
    Image,
    Pdf,
    Csv
}

data class FinanceImportSuggestion(
    val title: String = "",
    val merchant: String = "",
    val amountText: String = "",
    val category: String = FinanceCategoryStorage.General,
    val paymentMethod: String? = null,
    val note: String = "",
    val receiptUri: String? = null,
    val sourceLabel: String? = null,
    val rawText: String = "",
    val aiEnhanced: Boolean = false
)

sealed interface FinanceImportResult {
    data class Single(val suggestion: FinanceImportSuggestion) : FinanceImportResult
    data class Batch(
        val entries: List<FinanceTransactionInput>,
        val sourceLabel: String?,
        val rawText: String
    ) : FinanceImportResult
    data class Failure(val message: String) : FinanceImportResult
}

class FinanceImportAssistant(
    private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val keyProvider = DebugLocalAiKeyProvider(context)
    private val aiPreferences = AiPreferences(context)
    private val aiClient by lazy {
        BedrockAiRemoteClient(
            keyProvider = keyProvider,
            region = BuildConfig.BEDROCK_REGION.ifBlank { "us-east-1" }
        )
    }

    suspend fun analyzeCameraPreview(bitmap: Bitmap): FinanceImportResult {
        val rawText = recognizeText(bitmap)
        return buildSingleSuggestion(
            rawText = rawText,
            receiptUri = "camera://preview/${System.currentTimeMillis()}",
            sourceLabel = "Camera receipt"
        )
    }

    suspend fun analyzeImageUri(uri: Uri): FinanceImportResult {
        val bitmap = decodeBitmap(uri)
        val rawText = recognizeText(bitmap)
        return buildSingleSuggestion(
            rawText = rawText,
            receiptUri = uri.toString(),
            sourceLabel = queryDisplayName(uri)
        )
    }

    suspend fun analyzePdfUri(uri: Uri): FinanceImportResult {
        val rawText = renderPdfAndRecognize(uri)
        return buildSingleSuggestion(
            rawText = rawText,
            receiptUri = uri.toString(),
            sourceLabel = queryDisplayName(uri)
        )
    }

    suspend fun analyzeCsvUri(uri: Uri): FinanceImportResult {
        val fileLabel = queryDisplayName(uri)
        val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }.orEmpty()

        if (rawText.isBlank()) {
            return FinanceImportResult.Failure("This CSV file is empty.")
        }

        val entries = parseCsvTransactions(rawText, uri.toString())
        if (entries.isEmpty()) {
            return FinanceImportResult.Failure(
                "Could not detect valid amount and title columns in this CSV."
            )
        }

        return FinanceImportResult.Batch(
            entries = entries,
            sourceLabel = fileLabel,
            rawText = rawText
        )
    }

    private suspend fun buildSingleSuggestion(
        rawText: String,
        receiptUri: String?,
        sourceLabel: String?
    ): FinanceImportResult {
        if (rawText.isBlank()) {
            return FinanceImportResult.Failure(
                "Could not read text from this file. You can still enter it manually."
            )
        }

        val heuristic = heuristicSuggestion(
            rawText = rawText,
            receiptUri = receiptUri,
            sourceLabel = sourceLabel
        )
        val enriched = enrichWithAi(heuristic)
        return FinanceImportResult.Single(enriched)
    }

    private suspend fun decodeBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    private suspend fun renderPdfAndRecognize(uri: Uri): String {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return ""
        descriptor.use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                val pagesToRead = minOf(renderer.pageCount, 2)
                val textParts = mutableListOf<String>()

                repeat(pagesToRead) { index ->
                    renderer.openPage(index).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width.coerceAtLeast(1) * 2,
                            page.height.coerceAtLeast(1) * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        textParts += recognizeText(bitmap)
                    }
                }
                return textParts.joinToString("\n").trim()
            }
        }
    }

    private suspend fun recognizeText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).awaitResult()
        return result.text.orEmpty().trim()
    }

    private fun heuristicSuggestion(
        rawText: String,
        receiptUri: String?,
        sourceLabel: String?
    ): FinanceImportSuggestion {
        val lines = rawText
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
        val merchant = lines.firstOrNull(::looksLikeMerchantLine).orEmpty()
        val amount = extractLargestAmount(rawText)?.toPlainString().orEmpty()
        val paymentMethod = detectPaymentMethod(rawText)
        val title = merchant.ifBlank {
            sourceLabel
                ?.substringBeforeLast('.')
                ?.replace('_', ' ')
                ?.replace('-', ' ')
                .orEmpty()
        }.ifBlank { "Imported expense" }

        return FinanceImportSuggestion(
            title = title,
            merchant = merchant,
            amountText = amount,
            category = detectCategory(rawText),
            paymentMethod = paymentMethod,
            note = "Imported from ${sourceLabel ?: "document"}",
            receiptUri = receiptUri,
            sourceLabel = sourceLabel,
            rawText = rawText,
            aiEnhanced = false
        )
    }

    private suspend fun enrichWithAi(
        fallback: FinanceImportSuggestion
    ): FinanceImportSuggestion {
        if (!aiPreferences.cloudEnabled || !keyProvider.hasKey() || fallback.rawText.length < 40) {
            return fallback
        }

        val response = aiClient.sendMessage(
            model = FINANCE_IMPORT_MODEL,
            messages = listOf(
                AiPromptMessage(
                    role = AiPromptRole.User,
                    content = """
                        Extract one finance transaction from this raw document text.
                        Return strict JSON only with keys:
                        title, merchant, amount, category, payment_method, note.
                        Category must be one of:
                        general, food, grocery, travel, fuel, shopping, bills, subscription, health,
                        study, home, utilities, entertainment, pets, gift, fitness, work, savings, income.
                        If a field is unknown, return an empty string.

                        DOCUMENT:
                        ${fallback.rawText.take(6_000)}
                    """.trimIndent()
                )
            ),
            options = AiRequestOptions(
                systemPrompt = "You extract transaction data from OCR text. Return only valid JSON.",
                maxTokens = 260,
                temperature = 0.1
            )
        )

        if (response !is AiRemoteResult.Success) {
            return fallback
        }

        val json = response.content.extractJsonObjectOrNull() ?: return fallback
        val amount = json.optString("amount").takeIf(String::isNotBlank) ?: fallback.amountText
        val category = json.optString("category").takeIf(String::isNotBlank) ?: fallback.category
        return fallback.copy(
            title = json.optString("title").takeIf(String::isNotBlank) ?: fallback.title,
            merchant = json.optString("merchant").takeIf(String::isNotBlank) ?: fallback.merchant,
            amountText = amount,
            category = category,
            paymentMethod = json.optString("payment_method").takeIf(String::isNotBlank)
                ?: fallback.paymentMethod,
            note = json.optString("note").takeIf(String::isNotBlank) ?: fallback.note,
            aiEnhanced = true
        )
    }

    private fun parseCsvTransactions(
        csvText: String,
        receiptUri: String
    ): List<FinanceTransactionInput> {
        val rows = parseCsv(csvText)
        if (rows.size < 2) return emptyList()

        val headers = rows.first().map { normalizeHeader(it) }
        return rows.drop(1)
            .mapNotNull { values ->
                if (values.isEmpty()) return@mapNotNull null
                val row = headers.mapIndexed { index, header ->
                    header to values.getOrElse(index) { "" }.trim()
                }.toMap()
                val title = row["title"]
                    .orIfBlank(row["description"])
                    .orIfBlank(row["merchant"])
                    .orIfBlank(row["payee"])
                    ?: return@mapNotNull null

                val expenseAmount = row["amount"]
                    .orIfBlank(row["debit"])
                    .orIfBlank(row["expense"])
                val incomeAmount = row["credit"]
                    .orIfBlank(row["income"])
                val amount = parseAmountValue(expenseAmount ?: incomeAmount) ?: return@mapNotNull null
                val transactionType = if (!incomeAmount.isNullOrBlank() && incomeAmount != "0") {
                    FinanceTransactionTypeStorage.Income
                } else {
                    FinanceTransactionTypeStorage.Expense
                }
                val date = parseDateCandidate(
                    row["date"]
                        .orIfBlank(row["transaction_date"])
                        .orIfBlank(row["posted_at"])
                )
                FinanceTransactionInput(
                    title = title,
                    amount = amount.abs(),
                    transactionType = transactionType,
                    category = row["category"]?.takeIf(String::isNotBlank) ?: detectCategory(
                        buildString {
                            append(title)
                            append(' ')
                            append(row["merchant"].orEmpty())
                            append(' ')
                            append(row["note"].orEmpty())
                        }
                    ),
                    merchant = row["merchant"].orIfBlank(row["payee"]),
                    paymentMethod = row["payment_method"].orIfBlank(row["mode"]),
                    note = row["note"],
                    receiptUri = receiptUri,
                    occurredAt = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant() ?: Instant.now()
                )
            }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }

    private fun parseCsv(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false

        text.forEachIndexed { index, char ->
            when {
                char == '"' && inQuotes && text.getOrNull(index + 1) == '"' -> {
                    cell.append('"')
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    row += cell.toString()
                    cell.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && text.getOrNull(index + 1) == '\n') return@forEachIndexed
                    row += cell.toString()
                    cell.clear()
                    if (row.any { it.isNotBlank() }) {
                        result += row.toList()
                    }
                    row.clear()
                }
                else -> cell.append(char)
            }
        }

        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            if (row.any { it.isNotBlank() }) {
                result += row.toList()
            }
        }

        return result
    }

    private fun normalizeHeader(value: String): String {
        return value.trim()
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
    }

    private fun detectCategory(text: String): String {
        val normalized = text.lowercase()
        return when {
            normalized.contains("swiggy") || normalized.contains("zomato") ||
                normalized.contains("restaurant") || normalized.contains("cafe") -> FinanceCategoryStorage.Food
            normalized.contains("grocery") || normalized.contains("mart") || normalized.contains("supermarket") -> "grocery"
            normalized.contains("uber") || normalized.contains("ola") || normalized.contains("taxi") ||
                normalized.contains("flight") || normalized.contains("hotel") -> FinanceCategoryStorage.Travel
            normalized.contains("fuel") || normalized.contains("petrol") || normalized.contains("diesel") -> "fuel"
            normalized.contains("shopping") || normalized.contains("mall") || normalized.contains("amazon") ||
                normalized.contains("flipkart") -> FinanceCategoryStorage.Shopping
            normalized.contains("electricity") || normalized.contains("water bill") ||
                normalized.contains("rent") || normalized.contains("internet") -> FinanceCategoryStorage.Bills
            normalized.contains("subscription") || normalized.contains("netflix") ||
                normalized.contains("spotify") || normalized.contains("youtube") -> FinanceCategoryStorage.Subscription
            normalized.contains("hospital") || normalized.contains("pharmacy") ||
                normalized.contains("clinic") -> FinanceCategoryStorage.Health
            normalized.contains("course") || normalized.contains("book") ||
                normalized.contains("tuition") -> FinanceCategoryStorage.Study
            normalized.contains("salary") || normalized.contains("credited") ||
                normalized.contains("income") -> "income"
            normalized.contains("gym") || normalized.contains("fitness") -> "fitness"
            normalized.contains("gift") -> "gift"
            normalized.contains("pet") || normalized.contains("vet") -> "pets"
            else -> FinanceCategoryStorage.General
        }
    }

    private fun detectPaymentMethod(text: String): String? {
        val normalized = text.lowercase()
        return when {
            normalized.contains("upi") -> "UPI"
            normalized.contains("card") || normalized.contains("visa") || normalized.contains("mastercard") -> "Card"
            normalized.contains("cash") -> "Cash"
            normalized.contains("wallet") -> "Wallet"
            normalized.contains("bank") || normalized.contains("neft") || normalized.contains("imps") -> "Bank"
            else -> null
        }
    }

    private fun looksLikeMerchantLine(line: String): Boolean {
        if (line.length < 3 || line.length > 48) return false
        val normalized = line.lowercase()
        if (normalized.any(Char::isDigit)) return false
        return listOf("invoice", "receipt", "total", "tax", "date", "bill").none(normalized::contains)
    }

    private fun extractLargestAmount(text: String): BigDecimal? {
        val matches = MONEY_PATTERN.findAll(text)
            .mapNotNull { parseAmountValue(it.value) }
            .filter { it > BigDecimal.ZERO }
            .toList()
        return matches.maxOrNull()
    }

    private fun parseAmountValue(raw: String?): BigDecimal? {
        val cleaned = raw.orEmpty()
            .replace("[^0-9.,-]".toRegex(), "")
            .replace(",", "")
            .trim()
        return cleaned.toBigDecimalOrNull()
    }

    private fun parseDateCandidate(raw: String?): LocalDate? {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isBlank()) return null

        val datePatterns = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("dd/MM/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("dd-MM-uuuu"),
            DateTimeFormatter.ofPattern("d MMM uuuu"),
            DateTimeFormatter.ofPattern("dd MMM uuuu")
        )

        datePatterns.forEach { formatter ->
            try {
                return LocalDate.parse(candidate, formatter)
            } catch (_: DateTimeParseException) {
                // Try next format.
            }
        }

        try {
            return LocalDateTime.parse(
                candidate,
                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            ).toLocalDate()
        } catch (_: DateTimeParseException) {
            // Fall through.
        }

        return null
    }

    private fun String.extractJsonObjectOrNull(): JSONObject? {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return runCatching { JSONObject(substring(start, end + 1)) }.getOrNull()
    }

    private fun String?.orIfBlank(fallback: String?): String? {
        return if (this.isNullOrBlank()) fallback else this
    }

    private companion object {
        val MONEY_PATTERN = Regex("""(?:₹|rs\.?|inr|\$)?\s*-?\d[\d,]*(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)

        val FINANCE_IMPORT_MODEL = AiModel(
            key = "finance_import_assistant",
            displayName = "Claude Sonnet 4.6",
            provider = "Anthropic",
            modelId = "anthropic.claude-sonnet-4-6",
            role = "Finance import extraction",
            api = AiModelApi.BedrockConverse
        )
    }
}

private suspend fun <T> Task<T>.awaitResult(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}
