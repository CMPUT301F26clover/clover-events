package com.example.camp_serve.ui.owner.lotteries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.ui.theme.CampPurple
import com.example.camp_serve.ui.theme.CampSurfaceVariant

/*
 * LotteryUi.kt
 * Small building blocks shared by the owner lottery screens
 */

internal val LotteryHeaderTint = Color(0xFFE9DDFF)
internal val LotteryRowTint = Color(0xFFF7F0FC)
internal val PillGrey = Color(0xFFB9B4BF)

/** Light purple header with a back arrow and a purple title. */
@Composable
internal fun LotteryHeader(title: String, onBack: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(LotteryHeaderTint, Color.Transparent)))
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CampPurple
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = title,
            color = CampPurple,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
    }
}

/** Small purple label above a group of fields, e.g. "Registration Period". */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = CampPurple,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
internal fun FieldError(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Rounded purple button, or grey when [secondary]. */
@Composable
internal fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) PillGrey else CampPurple,
            contentColor = Color.White
        )
    ) {
        Text(text, fontSize = 14.sp)
    }
}

@Composable
internal fun lotteryFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    unfocusedBorderColor = CampSurfaceVariant,
    focusedBorderColor = CampPurple
)

/** A rounded single-line text field used for numbers in the form. */
@Composable
internal fun LotteryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(8.dp),
        colors = lotteryFieldColors(),
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * A read-only field that opens a date picker when tapped.
 *
 * @param day the chosen day as a date-picker value (UTC midnight), or null
 * @param onDayChange called with the newly picked day
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    day: Long?,
    placeholder: String,
    isError: Boolean,
    onDayChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var picking by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = day?.let { LotteryDates.formatDay(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(8.dp),
            colors = lotteryFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        // The text field would swallow taps, so a transparent layer on top opens the picker.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClickLabel = "Choose $placeholder date") { picking = true }
        )
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onDayChange)
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** A start and end [DateField] stacked like the storyboard, with one error message. */
@Composable
internal fun DateRangeFields(
    label: String,
    startDay: Long?,
    endDay: Long?,
    error: String?,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit
) {
    SectionLabel(label)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DateField(startDay, "Start", error != null, onStartChange)
        DateField(endDay, "End", error != null, onEndChange)
    }
    FieldError(error)
}

/** A light purple block of "Label: value" lines, as at the top of the lottery page. */
@Composable
internal fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(value, fontSize = 14.sp)
    }
}
