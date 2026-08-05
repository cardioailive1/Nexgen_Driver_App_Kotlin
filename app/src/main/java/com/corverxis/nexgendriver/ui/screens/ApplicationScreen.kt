package com.corverxis.nexgendriver.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.data.DocType
import com.corverxis.nexgendriver.data.DriverApplication
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel

@Composable
fun ApplicationScreen(viewModel: DriverViewModel) {
    val application by viewModel.application.collectAsState()

    Column(
        Modifier.fillMaxSize().background(NexgenBackground).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("NEXGEN.", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Spacer(Modifier.height(12.dp))

        when (application.status) {
            "draft" -> ApplicationForm(viewModel)
            "rejected" -> StatusCard(
                "Application not approved",
                application.reviewNotes ?: "Your application did not meet our requirements to drive with Nexgen.",
                NexgenStop
            )
            else -> SubmittedStatusBody(viewModel, application)
        }
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String, tint: androidx.compose.ui.graphics.Color = NexgenText) {
    Column(
        Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(16.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = NexgenTextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SubmittedStatusBody(viewModel: DriverViewModel, application: DriverApplication) {
    val context = LocalContext.current
    val checkrDone = application.checkrStatus != null && application.checkrStatus != "pending"

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatusCard(
            "Application ${application.status.replace('_', ' ')}",
            if (application.checkrInvitationUrl != null && !checkrDone)
                "One more step — finish your background check with Checkr to continue."
            else "Our team is reviewing your documents. This usually takes 1\u20132 business days."
        )

        if (application.checkrInvitationUrl != null && !checkrDone) {
            Button(
                onClick = {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(application.checkrInvitationUrl)))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue to background check", fontWeight = FontWeight.SemiBold) }
        }

        TextButton(onClick = { viewModel.refreshApplication() }) {
            Text("Check for updates", color = NexgenTextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ApplicationForm(viewModel: DriverViewModel) {
    val application by viewModel.application.collectAsState()
    val error by viewModel.applicationError.collectAsState()
    var isSubmitting by remember { mutableStateOf(false) }

    val fields = remember(application.id) {
        mutableStateMapOf(
            "legalFirstName" to (application.legalFirstName ?: ""), "legalLastName" to (application.legalLastName ?: ""),
            "dateOfBirth" to (application.dateOfBirth ?: ""), "phone" to (application.phone ?: ""),
            "email" to (application.email ?: ""), "addressLine1" to (application.addressLine1 ?: ""),
            "addressLine2" to (application.addressLine2 ?: ""), "city" to (application.city ?: ""),
            "state" to (application.state ?: ""), "zip" to (application.zip ?: ""),
            "licenseNumber" to (application.licenseNumber ?: ""), "licenseState" to (application.licenseState ?: ""),
            "licenseExpiration" to (application.licenseExpiration ?: ""),
            "insuranceProvider" to (application.insuranceProvider ?: ""), "insurancePolicyNum" to (application.insurancePolicyNum ?: ""),
            "insuranceExpiration" to (application.insuranceExpiration ?: ""),
            "vehicleMake" to (application.vehicleMake ?: ""), "vehicleModel" to (application.vehicleModel ?: ""),
            "vehicleYear" to (application.vehicleYear ?: ""), "vehicleColor" to (application.vehicleColor ?: ""),
            "licensePlate" to (application.licensePlate ?: ""), "vin" to (application.vin ?: "")
        )
    }

    Column {
        SectionTitle("Personal information")
        FormRow { FormField("Legal first name", fields, "legalFirstName", Modifier.weight(1f)); FormField("Legal last name", fields, "legalLastName", Modifier.weight(1f)) }
        FormRow { FormField("Date of birth (YYYY-MM-DD)", fields, "dateOfBirth", Modifier.weight(1f)); FormField("Phone", fields, "phone", Modifier.weight(1f)) }
        FormRow { FormField("Email", fields, "email", Modifier.weight(1f)) }
        FormRow { FormField("Address line 1", fields, "addressLine1", Modifier.weight(1f)) }
        FormRow { FormField("Address line 2 (optional)", fields, "addressLine2", Modifier.weight(1f)) }
        FormRow {
            FormField("City", fields, "city", Modifier.weight(1f)); FormField("State", fields, "state", Modifier.weight(1f)); FormField("ZIP", fields, "zip", Modifier.weight(1f))
        }

        SectionTitle("Driver's license")
        FormRow { FormField("License number", fields, "licenseNumber", Modifier.weight(1f)); FormField("Issuing state", fields, "licenseState", Modifier.weight(1f)) }
        FormRow { FormField("Expiration (YYYY-MM-DD)", fields, "licenseExpiration", Modifier.weight(1f)) }
        DocUploadRow(viewModel, DocType.LICENSE_FRONT)
        DocUploadRow(viewModel, DocType.LICENSE_BACK)

        SectionTitle("Insurance")
        FormRow { FormField("Insurance provider", fields, "insuranceProvider", Modifier.weight(1f)); FormField("Policy number", fields, "insurancePolicyNum", Modifier.weight(1f)) }
        FormRow { FormField("Expiration (YYYY-MM-DD)", fields, "insuranceExpiration", Modifier.weight(1f)) }
        DocUploadRow(viewModel, DocType.INSURANCE_DOC)

        SectionTitle("Vehicle")
        FormRow { FormField("Make", fields, "vehicleMake", Modifier.weight(1f)); FormField("Model", fields, "vehicleModel", Modifier.weight(1f)) }
        FormRow { FormField("Year", fields, "vehicleYear", Modifier.weight(1f)); FormField("Color", fields, "vehicleColor", Modifier.weight(1f)) }
        FormRow { FormField("License plate", fields, "licensePlate", Modifier.weight(1f)); FormField("VIN", fields, "vin", Modifier.weight(1f)) }
        DocUploadRow(viewModel, DocType.REGISTRATION_DOC)
        DocUploadRow(viewModel, DocType.VEHICLE_PHOTO_FRONT)
        DocUploadRow(viewModel, DocType.VEHICLE_PHOTO_BACK)
        DocUploadRow(viewModel, DocType.VEHICLE_PHOTO_LEFT)
        DocUploadRow(viewModel, DocType.VEHICLE_PHOTO_RIGHT)

        error?.let {
            Text(it, color = NexgenStop, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = {
                isSubmitting = true
                viewModel.saveApplicationDraft(fields.toMap())
                viewModel.submitApplication()
                isSubmitting = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp)
        ) {
            Text(if (isSubmitting) "Submitting…" else "Submit application", fontWeight = FontWeight.SemiBold)
        }

        Text(
            "Submitting starts a background check through Checkr. You'll finish that step \u2014 including entering your SSN \u2014 directly on Checkr's own secure page; it never passes through Nexgen.",
            color = NexgenTextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(), color = NexgenAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
    )
}

@Composable
private fun FormRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun FormField(label: String, fields: MutableMap<String, String>, key: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = NexgenTextDim, fontSize = 11.sp)
        OutlinedTextField(
            value = fields[key] ?: "",
            onValueChange = { fields[key] = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NexgenSurface, unfocusedContainerColor = NexgenSurface,
                focusedTextColor = NexgenText, unfocusedTextColor = NexgenText
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DocUploadRow(viewModel: DriverViewModel, docType: DocType) {
    val application by viewModel.application.collectAsState()
    val uploadingDoc by viewModel.uploadingDoc.collectAsState()
    val context = LocalContext.current

    val hasFile = application.docUrls?.get(docType) != null
    val isUploading = uploadingDoc == docType

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            if (bytes != null) viewModel.uploadDocument(docType, bytes, "image/jpeg")
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).background(NexgenSurface, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(docType.label, color = NexgenText, fontSize = 13.sp)
            Text(
                if (isUploading) "Uploading\u2026" else if (hasFile) "Uploaded \u2713" else "Not uploaded",
                color = if (hasFile) NexgenGo else NexgenTextDim, fontSize = 11.sp
            )
        }
        Button(
            onClick = { launcher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = if (hasFile) NexgenGo else NexgenSurface2, contentColor = if (hasFile) NexgenGoText else NexgenText),
            enabled = !isUploading
        ) { Text(if (hasFile) "Replace" else "Upload", fontSize = 12.sp) }
    }
}
