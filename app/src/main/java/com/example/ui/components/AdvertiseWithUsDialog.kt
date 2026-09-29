package com.example.ui.components

import androidx.compose.runtime.Composable

@Composable
fun AdvertiseWithUsDialog(
    onDismissRequest: () -> Unit
) {
    AdCampaignWizardDialog(
        viewModel = null,
        business = null,
        onDismissRequest = onDismissRequest
    )
}
