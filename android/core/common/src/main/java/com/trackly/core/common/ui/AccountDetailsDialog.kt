package com.trackly.core.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackly.core.common.theme.*
import com.trackly.core.model.User
import com.trackly.core.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsDialog(
    user: User?,
    hasActiveDelivery: Boolean,
    onSaveProfile: (name: String, vehicleNumber: String?) -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember(user) { mutableStateOf(user?.name ?: "") }
    var vehicleNumberText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showActiveDeliveryAlert by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val isDriver = user?.role == UserRole.DRIVER

    if (showActiveDeliveryAlert) {
        AlertDialog(
            onDismissRequest = { showActiveDeliveryAlert = false },
            title = { Text("Account Deletion Blocked ⚠️", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (isDriver)
                        "You cannot delete your account while you have an active delivery in progress. Please complete or finish all active deliveries first."
                    else
                        "You cannot delete your account while you have active order(s) in progress. Please wait until your orders are delivered or cancelled.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showActiveDeliveryAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary)
                ) {
                    Text("OK, Understood", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold, color = StatusCrimsonError) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete your Trackly account? All your profile data will be permanently removed. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCrimsonError)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Permanently Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondaryGrey)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = DeepOceanSecondary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account Details", fontWeight = FontWeight.Bold, color = TextPrimaryCharcoal)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGrey)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Email Field (Read-only)
                Text("EMAIL ADDRESS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextSecondaryGrey)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = user?.email ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondaryGrey) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Full Name Field (Editable)
                Text("FULL NAME", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepOceanSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeholder = { Text("Enter your full name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DeepOceanSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepOceanSecondary,
                        unfocusedBorderColor = DividerLight
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isDriver) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("VEHICLE NUMBER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepOceanSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = vehicleNumberText,
                        onValueChange = { vehicleNumberText = it },
                        placeholder = { Text("e.g. TS 09 AB 1234") },
                        leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DeepOceanSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepOceanSecondary,
                            unfocusedBorderColor = DividerLight
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = TealBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Profile Button
                Button(
                    onClick = {
                        if (nameText.isNotBlank()) {
                            onSaveProfile(nameText.trim(), vehicleNumberText.trim().ifBlank { null })
                            statusMessage = "Profile updated successfully!"
                        }
                    },
                    enabled = nameText.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Account Danger Button
                OutlinedButton(
                    onClick = {
                        if (hasActiveDelivery) {
                            showActiveDeliveryAlert = true
                        } else {
                            showDeleteConfirmDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCrimsonError),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusCrimsonError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusCrimsonError, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account", fontWeight = FontWeight.Bold, color = StatusCrimsonError)
                }
            }
        },
        confirmButton = {},
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(20.dp)
    )
}
