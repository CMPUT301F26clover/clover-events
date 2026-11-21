package com.example.camp_serve.ui.owner.lotteries

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.theme.CampPurple
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

/*
 * LotteryFormScreen.kt
 * "Create Lottery" (US 02.04.01) and "Update Lottery" (US 02.04.06) screens.
 * Both use the same form with the registration period (US 02.04.03), booking period
 * (US 02.04.04), number of winners (US 02.04.05) and an optional waitlist capacity.
 * Leaving either screen without submitting saves nothing.
 */

/**
 * Lets an owner create a lottery for one or more of their campsites.
 *
 * @param preselectedCampsiteId campsite to tick by default (when opened from a campsite page)
 * @param onCreated called with the new lottery's id after it is saved
 * @param onCancel called when the owner leaves without submitting
 */
@Composable
fun CreateLotteryScreen(
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    ownerId: String,
    preselectedCampsiteId: String?,
    onCreated: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var campsites by remember { mutableStateOf<List<Campsite>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf(LotteryForm()) }
    var errors by remember { mutableStateOf(LotteryFormErrors()) }
    var triedSubmit by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ownerId) {
        try {
            val loaded = campsiteRepository.getCampsitesForOwner(ownerId)
            campsites = loaded
            if (preselectedCampsiteId != null && loaded.any { it.id == preselectedCampsiteId }) {
                form = form.copy(campsiteIds = setOf(preselectedCampsiteId))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LotteryHeader(title = "Create Lottery", onBack = onCancel)
        val sites = campsites
        when {
            loadFailed -> CenteredMessage("Could not load your campsites. Check your connection and try again.")
            sites == null -> CenteredLoading()
            sites.isEmpty() -> CenteredMessage("Register a campsite before creating a lottery.")
            else -> LotteryFormContent(
                campsites = sites,
                form = form,
                errors = errors,
                onFormChange = {
                    form = it
                    if (triedSubmit) errors = validateLotteryForm(it, LotteryDates.today(), isNew = true)
                },
                message = message,
                submitting = submitting,
                showCancel = false,
                onCancel = onCancel,
                submitLabel = "Submit",
                onSubmit = {
                    triedSubmit = true
                    errors = validateLotteryForm(form, LotteryDates.today(), isNew = true)
                    if (errors.isValid && !submitting) {
                        submitting = true
                        message = null
                        scope.launch {
                            try {
                                val lottery = form.toLottery(UUID.randomUUID().toString(), ownerId, sites)
                                lotteryRepository.createLottery(lottery)
                                Toast.makeText(context, "Lottery created", Toast.LENGTH_SHORT).show()
                                onCreated(lottery.id)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                message = lotteryErrorMessage(e, "create")
                            } finally {
                                submitting = false
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Lets an owner change a lottery's settings. The form starts filled with the current
 * settings. Once winners are drawn (registration has closed) the lottery can't be edited.
 *
 * @param onSaved called after the changes are saved
 * @param onCancel called when the owner leaves without saving
 */
@Composable
fun EditLotteryScreen(
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    ownerId: String,
    lotteryId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var campsites by remember { mutableStateOf<List<Campsite>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf(LotteryForm()) }
    var errors by remember { mutableStateOf(LotteryFormErrors()) }
    var triedSubmit by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lotteryId, ownerId) {
        try {
            val saved = lotteryRepository.getLottery(lotteryId)?.takeIf { it.ownerId == ownerId }
            lottery = saved
            if (saved != null) {
                form = LotteryForm.from(saved)
                // Keep campsites already on the lottery even if they were since removed.
                val owned = campsiteRepository.getCampsitesForOwner(ownerId)
                campsites = owned + saved.campsites.filter { site -> owned.none { it.id == site.id } }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
        loading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        LotteryHeader(title = "Update Lottery", onBack = onCancel)
        val current = lottery
        when {
            loading -> CenteredLoading()
            loadFailed -> CenteredMessage("Could not load the lottery. Check your connection and try again.")
            current == null -> CenteredMessage("Lottery not found.")
            !current.isEditable() -> CenteredMessage(
                "Registration for this lottery has closed and winners are drawn, so it can no longer be edited."
            )
            else -> {
                val entrants = current.getWaitingList(ownerId).size
                LotteryFormContent(
                    campsites = campsites,
                    form = form,
                    errors = errors,
                    onFormChange = {
                        form = it
                        if (triedSubmit) {
                            errors = validateLotteryForm(it, LotteryDates.today(), isNew = false, entrants)
                        }
                    },
                    message = message,
                    submitting = submitting,
                    showCancel = true,
                    onCancel = onCancel,
                    submitLabel = "Save Changes",
                    onSubmit = {
                        triedSubmit = true
                        errors = validateLotteryForm(form, LotteryDates.today(), isNew = false, entrants)
                        if (errors.isValid && !submitting) {
                            submitting = true
                            message = null
                            scope.launch {
                                try {
                                    val updated = form.toLottery(current.id, ownerId, campsites)
                                    lotteryRepository.updateLottery(updated, ownerId)
                                    Toast.makeText(context, "Changes saved", Toast.LENGTH_SHORT).show()
                                    onSaved()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    message = lotteryErrorMessage(e, "update")
                                } finally {
                                    submitting = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

/** The form itself, shared by the create and update screens. */
@Composable
private fun LotteryFormContent(
    campsites: List<Campsite>,
    form: LotteryForm,
    errors: LotteryFormErrors,
    onFormChange: (LotteryForm) -> Unit,
    message: String?,
    submitting: Boolean,
    showCancel: Boolean,
    onCancel: () -> Unit,
    submitLabel: String,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        SectionLabel("Select Campsites", Modifier.padding(top = 0.dp))
        campsites.forEach { site ->
            val checked = site.id in form.campsiteIds
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        val ids = if (isChecked) form.campsiteIds + site.id else form.campsiteIds - site.id
                        onFormChange(form.copy(campsiteIds = ids))
                    },
                    colors = CheckboxDefaults.colors(checkedColor = CampPurple)
                )
                Text(site.name.ifBlank { "Unnamed campsite" }, fontSize = 14.sp)
            }
        }
        FieldError(errors.campsites)

        DateRangeFields(
            label = "Registration Period",
            startDay = form.registrationStartDay,
            endDay = form.registrationEndDay,
            error = errors.registration,
            onStartChange = { onFormChange(form.copy(registrationStartDay = it)) },
            onEndChange = { onFormChange(form.copy(registrationEndDay = it)) }
        )

        DateRangeFields(
            label = "Booking Period",
            startDay = form.bookingStartDay,
            endDay = form.bookingEndDay,
            error = errors.booking,
            onStartChange = { onFormChange(form.copy(bookingStartDay = it)) },
            onEndChange = { onFormChange(form.copy(bookingEndDay = it)) }
        )

        SectionLabel("Number of Winners")
        LotteryTextField(
            value = form.winners,
            onValueChange = { onFormChange(form.copy(winners = it)) },
            placeholder = "Value",
            isError = errors.winners != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        FieldError(errors.winners)

        SectionLabel("Waitlist Capacity (Optional)")
        LotteryTextField(
            value = form.waitlistCapacity,
            onValueChange = { onFormChange(form.copy(waitlistCapacity = it)) },
            placeholder = "No limit",
            isError = errors.waitlistCapacity != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        FieldError(errors.waitlistCapacity)

        Spacer(Modifier.height(12.dp))
        Text(
            "Winners are drawn automatically when registration closes.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (showCancel) Arrangement.Center else Arrangement.End
        ) {
            if (showCancel) {
                PillButton("Cancel", onClick = onCancel, secondary = true, enabled = !submitting)
                Spacer(Modifier.width(12.dp))
            }
            PillButton(
                text = if (submitting) "Saving…" else submitLabel,
                onClick = onSubmit,
                enabled = !submitting
            )
        }
    }
}

@Composable
internal fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CampPurple)
    }
}

@Composable
internal fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}
