@file:OptIn(ExperimentalMaterial3Api::class)

package com.hexai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hexai.ui.theme.*

@Composable
fun CyberpunkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = NeonCyan,
    content: @Composable RowScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier.border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = glowAlpha),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = 0.15f),
            contentColor = accentColor,
            disabledContainerColor = CyberGray500,
            disabledContentColor = CyberGray300
        )
    ) {
        content()
    }
}

@Composable
fun CyberpunkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    accentColor: Color = NeonCyan,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val focused = remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = TextMuted) } },
        singleLine = singleLine,
        visualTransformation = if (isPassword) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextMuted,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = CyberGray400,
            disabledBorderColor = CyberGray500,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = TextSecondary,
            disabledLabelColor = TextMuted,
            cursorColor = accentColor,
            focusedContainerColor = DarkCard,
            unfocusedContainerColor = DarkCard,
            disabledContainerColor = DarkCard
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun CyberpunkCard(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = DarkCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun CyberpunkSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    label: String? = null,
    valueLabel: String? = null,
    accentColor: Color = NeonCyan
) {
    Column(modifier = modifier) {
        if (label != null || valueLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                valueLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = CyberGray500,
                activeTickColor = accentColor,
                inactiveTickColor = CyberGray400
            )
        )
    }
}

@Composable
fun CyberpunkSliderWithInput(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    label: String? = null,
    valueLabel: String? = null,
    accentColor: Color = NeonCyan,
    decimalPlaces: Int = 2
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label != null || valueLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                valueLabel?.let {
                    Surface(
                        modifier = Modifier.clickable { showDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = CyberGray500,
                activeTickColor = accentColor,
                inactiveTickColor = CyberGray400
            )
        )
    }

    if (showDialog) {
        NumberInputDialog(
            currentValue = value,
            valueRange = valueRange,
            decimalPlaces = decimalPlaces,
            label = label ?: "Value",
            accentColor = accentColor,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue.coerceIn(valueRange))
                showDialog = false
            }
        )
    }
}

@Composable
fun NumberInputDialog(
    currentValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    decimalPlaces: Int,
    label: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var textValue by remember {
        mutableStateOf(String.format("%.${decimalPlaces}f", currentValue))
    }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = DarkCard
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Range: ${String.format("%.${decimalPlaces}f", valueRange.start)} - ${String.format("%.${decimalPlaces}f", valueRange.endInclusive)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        val parsed = newValue.toFloatOrNull()
                        isError = parsed == null || parsed !in valueRange
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = if (isError) ErrorRed else accentColor,
                        unfocusedBorderColor = if (isError) ErrorRed else CyberGray400,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        cursorColor = accentColor,
                        errorBorderColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a valid number within range",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CyberpunkButton(
                        onClick = onDismiss,
                        accentColor = CyberGray400,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    CyberpunkButton(
                        onClick = {
                            textValue.toFloatOrNull()?.let { onConfirm(it) }
                        },
                        enabled = !isError && textValue.isNotBlank(),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("APPLY")
                    }
                }
            }
        }
    }
}

@Composable
fun IntNumberInputDialog(
    currentValue: Int,
    valueRange: IntRange,
    label: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = DarkCard
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Range: ${valueRange.first} - ${valueRange.last}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        val parsed = newValue.toIntOrNull()
                        isError = parsed == null || parsed !in valueRange
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = if (isError) ErrorRed else accentColor,
                        unfocusedBorderColor = if (isError) ErrorRed else CyberGray400,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        cursorColor = accentColor,
                        errorBorderColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a valid number within range",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CyberpunkButton(
                        onClick = onDismiss,
                        accentColor = CyberGray400,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    CyberpunkButton(
                        onClick = {
                            textValue.toIntOrNull()?.let { onConfirm(it.coerceIn(valueRange)) }
                        },
                        enabled = !isError && textValue.isNotBlank(),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("APPLY")
                    }
                }
            }
        }
    }
}

@Composable
fun CyberpunkDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    accentColor: Color = NeonCyan
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedItem,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = CyberGray400,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedTrailingIconColor = accentColor,
                    unfocusedTrailingIconColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DarkSurfaceVariant)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                color = if (item == selectedItem) accentColor else TextPrimary
                            )
                        },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = TextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0
                0f at 2800
                2f at 2850
                -2f at 2900
                1f at 2950
                0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "glitchX"
    )

    Box(modifier = modifier) {
        // Glitch shadows - grey for monochrome aesthetic
        Text(
            text = text,
            style = style,
            color = HexGrey300.copy(alpha = 0.5f),
            modifier = Modifier.offset(x = (offsetX * -1).dp, y = 1.dp)
        )
        Text(
            text = text,
            style = style,
            color = HexGreenDim.copy(alpha = 0.5f),
            modifier = Modifier.offset(x = offsetX.dp, y = (-1).dp)
        )
        // Main text
        Text(
            text = text,
            style = style,
            color = color
        )
    }
}

@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    thickness: Dp = 1.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "divider")
    val shimmerPosition by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .drawWithContent {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.2f),
                            color,
                            color.copy(alpha = 0.2f)
                        ),
                        start = Offset(size.width * shimmerPosition, 0f),
                        end = Offset(size.width * (shimmerPosition + 0.5f), 0f)
                    )
                )
            }
    )
}

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CONNECTING...",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
            }
        }
    }
}
