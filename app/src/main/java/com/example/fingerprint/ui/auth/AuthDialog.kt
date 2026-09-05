package com.example.fingerprint.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.fingerprint.auth.AuthState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    uiState: AuthUiState,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onToggleSignUp: (Boolean) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onSubmitEmailAuth: () -> Unit,
    onGoogleSignIn: (String) -> Unit = {},
    onError: (String) -> Unit = {},
    onSendPasswordReset: (String) -> Unit = {},
    onSignOut: () -> Unit
) {
    if (!uiState.showAuthDialog) return

    val isAuthenticated = uiState.authState is AuthState.Authenticated
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }
    var isResetting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (isAuthenticated) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = isAuthenticated,
            dismissOnClickOutside = isAuthenticated
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("auth_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button (only visible when authenticated)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAuthenticated) {
                            if (isArabic) "الملف الشخصي والحساب" else "User Account"
                        } else {
                            if (isArabic) "تسجيل الدخول / حساب جديد" else "Sign In / Register"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isAuthenticated) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("auth_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState.authState) {
                    is AuthState.Authenticated -> {
                        // User Profile View
                        AuthenticatedProfileView(
                            state = state,
                            isArabic = isArabic,
                            onSignOut = onSignOut
                        )
                    }
                    else -> {
                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        val googleWebClientId = "138529547927-16f7pm7onslaaptu3naq7aq6dqav5ua3.apps.googleusercontent.com"
                        val credentialManager = remember { CredentialManager.create(context) }

                        // Unauthenticated Login / Register View
                        UnauthenticatedLoginView(
                            uiState = uiState,
                            isArabic = isArabic,
                            onToggleSignUp = onToggleSignUp,
                            onEmailChanged = onEmailChanged,
                            onPasswordChanged = onPasswordChanged,
                            onNameChanged = onNameChanged,
                            onSubmitEmailAuth = onSubmitEmailAuth,
                            onGoogleSignInClick = {
                                coroutineScope.launch {
                                    try {
                                        // Strategy 1: Explicit Sign-In with Google prompt (always shows account selector UI)
                                        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(googleWebClientId)
                                            .build()

                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId(googleWebClientId)
                                            .setAutoSelectEnabled(false)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(signInWithGoogleOption)
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        val result = credentialManager.getCredential(
                                            request = request,
                                            context = context
                                        )

                                        val credential = result.credential
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        onGoogleSignIn(idToken)
                                    } catch (e: GetCredentialCancellationException) {
                                        // User dismissed/cancelled account picker intentionally
                                    } catch (e: NoCredentialException) {
                                        val msg = if (isArabic)
                                            "لم يتم العثور على حساب جوجل مسجل على هذا الجهاز، يرجى تسجيل حساب في الجهاز أولاً أو استخدام البريد وكلمة المرور."
                                        else
                                            "No Google account found on this device. Please add a Google account to your device or sign in with email and password."
                                        onError(msg)
                                    } catch (e: Exception) {
                                        if (!e.message.orEmpty().contains("cancelled", ignoreCase = true) &&
                                            !e.message.orEmpty().contains("canceled", ignoreCase = true)) {
                                            val msg = if (isArabic) "تعذر تسجيل الدخول عبر جوجل: ${e.localizedMessage ?: "حدث خطأ"}"
                                                      else "Google sign-in error: ${e.localizedMessage ?: "Unknown error"}"
                                            onError(msg)
                                        }
                                    }
                                }
                            },
                            onOpenForgotPassword = {
                                resetEmailInput = uiState.emailInput
                                resetStatusMessage = null
                                showForgotPasswordDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) "استعادة كلمة المرور" else "Forgot Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) {
                            "أدخل بريدك الإلكتروني المسجل وسنرسل لك رابطاً لإعادة تعيين كلمة المرور الخاصة بك."
                        } else {
                            "Enter your registered email address to receive password reset instructions."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val currentResetMsg = resetStatusMessage
                    if (currentResetMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentResetMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showForgotPasswordDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isArabic) "إلغاء" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (resetEmailInput.isNotBlank()) {
                                    isResetting = true
                                    onSendPasswordReset(resetEmailInput.trim())
                                    resetStatusMessage = if (isArabic) {
                                        "تم إرسال تعليمات الاستعادة إلى بريدك الإلكتروني بنجاح (سيتم ربطه بالعملية الفعلية قريباً)."
                                    } else {
                                        "Reset instructions sent to your email (will be linked soon)."
                                    }
                                    isResetting = false
                                }
                            },
                            enabled = !isResetting && resetEmailInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_reset_password_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isArabic) "إرسال" else "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedProfileView(
    state: AuthState.Authenticated,
    isArabic: Boolean,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Avatar Circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = state.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = state.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subscription Tag
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "اشتراك مجاني تجريبي (60 يوماً)" else "Free Trial Plan (60 Days)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_out_button")
        ) {
            Text(if (isArabic) "تسجيل الخروج" else "Sign Out")
        }
    }
}

@Composable
private fun UnauthenticatedLoginView(
    uiState: AuthUiState,
    isArabic: Boolean,
    onToggleSignUp: (Boolean) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onSubmitEmailAuth: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onOpenForgotPassword: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isArabic) {
                "سجل دخولك أو أنشئ حساباً بالبريد الإلكتروني لمزامنة بياناتك سحابياً"
            } else {
                "Sign in or register with email to backup and sync your fingerprint schedules"
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Login vs Register
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!uiState.isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onToggleSignUp(false) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "دخول" else "Sign In",
                    fontWeight = FontWeight.Bold,
                    color = if (!uiState.isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onToggleSignUp(true) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "حساب جديد" else "Register",
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isSignUpMode) {
            OutlinedTextField(
                value = uiState.nameInput,
                onValueChange = onNameChanged,
                label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_name_input"),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = uiState.emailInput,
            onValueChange = onEmailChanged,
            label = { Text(if (isArabic) "البريد الإلكتروني" else "Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.passwordInput,
            onValueChange = onPasswordChanged,
            label = { Text(if (isArabic) "كلمة المرور" else "Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input"),
            shape = RoundedCornerShape(12.dp)
        )

        if (!uiState.isSignUpMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenForgotPassword,
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text(
                        text = if (isArabic) "هل نسيت كلمة المرور؟" else "Forgot password?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmitEmailAuth,
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00796B),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_email_auth_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (uiState.isSignUpMode) {
                        if (isArabic) "إنشاء حساب" else "Create Account"
                    } else {
                        if (isArabic) "تسجيل الدخول" else "Sign In"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Divider with "OR / أو"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = if (isArabic) "أو" else "OR",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Continue with Google Native Button
        OutlinedButton(
            onClick = onGoogleSignInClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isArabic) "متابعة باستخدام Google" else "Continue with Google",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
