package com.example.fingerprint.ui.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FeedbackDialog(
    isOpen: Boolean,
    isArabic: Boolean,
    initialName: String,
    initialEmail: String,
    isLoading: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onSubmitFeedback: (name: String, email: String, type: String, rating: Int, message: String) -> Unit
) {
    if (!isOpen) return

    val isDark = isSystemInDarkTheme()

    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var selectedType by remember { mutableStateOf("SUGGESTION") }
    var rating by remember { mutableIntStateOf(5) }
    var message by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val mainScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1DE9B6).copy(alpha = 0.25f) else Color(0xFF00695C).copy(alpha = 0.15f)),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .padding(vertical = 16.dp)
                .testTag("feedback_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(mainScrollState)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "إرسال اقتراح أو ملاحظة" else "Submit Feedback",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("feedback_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feedback Type Chips
                Text(
                    text = if (isArabic) "نوع المشاركة:" else "Feedback Type:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "SUGGESTION" to if (isArabic) "اقتراح 💡" else "Suggestion 💡",
                        "BUG" to if (isArabic) "مشكلة ⚠️" else "Bug ⚠️",
                        "QUESTION" to if (isArabic) "استفسار ❓" else "Question ❓"
                    )
                    types.forEach { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = typeKey },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) (if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C)) else (if (isDark) Color(0xFF004D40) else Color(0xFFB2DFDB)),
                                selectedBorderColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C)
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF7FAF9),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = if (isDark) Color(0xFF00897B) else Color(0xFF00695C),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rating Stars
                Text(
                    text = if (isArabic) "تقييم التطبيق:" else "App Rating:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = "Star $star",
                            tint = if (star <= rating) Color(0xFFFFB300) else (if (isDark) Color(0xFF546E7A) else Color(0xFFB0BEC5)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { rating = star }
                                .padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val customTextFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C),
                    unfocusedBorderColor = if (isDark) Color(0xFF004D40) else Color(0xFFB2DFDB),
                    focusedLabelColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C),
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLeadingIconColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C),
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00695C)
                )

                // Input fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isArabic) "الاسم" else "Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    colors = customTextFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    colors = customTextFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_email_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(if (isArabic) "تفاصيل الملاحظة أو الاقتراح..." else "Your feedback message...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = customTextFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_message_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val isSuccess = statusMessage.contains("successfully", ignoreCase = true) || statusMessage.contains("بنجاح")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess) {
                            if (isDark) Color(0xFF1B3820) else Color(0xFFE8F5E9)
                        } else {
                            if (isDark) Color(0xFF3E1A1A) else Color(0xFFFFEBEE)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSuccess) {
                                if (isDark) Color(0xFF2E7D32) else Color(0xFF81C784)
                            } else {
                                if (isDark) Color(0xFFC62828) else Color(0xFFE57373)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) {
                                if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                            } else {
                                if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
                            },
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || message.isBlank()) {
                            errorText = if (isArabic) "يرجى ملء جميع الحقول المطلوبة" else "Please fill all required fields"
                        } else {
                            errorText = null
                            onSubmitFeedback(name.trim(), email.trim(), selectedType, rating, message.trim())
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF00897B) else Color(0xFF00695C),
                        contentColor = Color.White,
                        disabledContainerColor = if (isDark) Color(0xFF004D40).copy(alpha = 0.5f) else Color(0xFF80CBC4).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_feedback_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "إرسال الاقتراح" else "Send Feedback",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
