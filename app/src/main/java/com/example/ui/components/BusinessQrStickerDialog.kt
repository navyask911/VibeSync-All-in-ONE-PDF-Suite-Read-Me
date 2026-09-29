@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BusinessEntity
import com.example.ui.DatingViewModel
import com.example.util.BusinessStickerExporter
import com.example.util.QrCodeGeneratorHelper
import com.example.util.StickerShape
import com.example.util.StickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BusinessQrStickerDialog(
    business: BusinessEntity,
    viewModel: DatingViewModel,
    onDismiss: () -> Unit,
    onTestScan: (BusinessEntity) -> Unit = {}
) {
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedShape by remember { mutableStateOf(StickerShape.ROUNDED_SQUARE) }
    var selectedTheme by remember { mutableStateOf(StickerTheme.VIBESYNC_CORAL) }
    var tableLabel by remember { mutableStateOf("Table #01") }
    var customCta by remember { mutableStateOf("Scan with Camera or Tap NFC to Chat, View Offers & Drop a GPS-Verified Review") }
    var showLogo by remember { mutableStateOf(true) }
    var showOfferHighlight by remember { mutableStateOf(business.activeOfferSummary.isNotBlank()) }
    var showNfcBadge by remember { mutableStateOf(true) }

    var stickerBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingBitmap by remember { mutableStateOf(true) }

    var selectedPackType by remember { mutableIntStateOf(0) }
    var shippingAddress by remember { mutableStateOf("${business.address}, ${business.city}") }
    var recipientPhone by remember { mutableStateOf(business.phoneNumber.ifBlank { "+91 98765 43210" }) }
    var isOrderPlaced by remember { mutableStateOf(false) }

    LaunchedEffect(selectedShape, selectedTheme, tableLabel, customCta, showLogo, showOfferHighlight, showNfcBadge, business) {
        isGeneratingBitmap = true
        withContext(Dispatchers.Default) {
            val bmp = BusinessStickerExporter.createPrintableStickerBitmap(
                business = business,
                shape = selectedShape,
                theme = selectedTheme,
                tableLabel = tableLabel,
                customCta = customCta,
                showLogo = showLogo,
                showOfferHighlight = showOfferHighlight,
                showNfcBadge = showNfcBadge,
                size = 1200
            )
            withContext(Dispatchers.Main) {
                stickerBitmap = bmp
                isGeneratingBitmap = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("dialog_business_qr_sticker"),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE91E63)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Printable Sticker & Counter QR",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${business.name} • 4x4\" Vector Generator",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("btn_close_qr_sticker_dialog")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("🎨 Sticker Studio", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("📦 Order Physical", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("📲 Test Deep-Link", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> StickerStudioTab(
                        business = business,
                        stickerBitmap = stickerBitmap,
                        isGenerating = isGeneratingBitmap,
                        selectedShape = selectedShape,
                        onSelectShape = { selectedShape = it },
                        selectedTheme = selectedTheme,
                        onSelectTheme = { selectedTheme = it },
                        tableLabel = tableLabel,
                        onTableLabelChange = { tableLabel = it },
                        customCta = customCta,
                        onCustomCtaChange = { customCta = it },
                        showLogo = showLogo,
                        onToggleLogo = { showLogo = it },
                        showOfferHighlight = showOfferHighlight,
                        onToggleOfferHighlight = { showOfferHighlight = it },
                        showNfcBadge = showNfcBadge,
                        onToggleNfcBadge = { showNfcBadge = it },
                        onPrint = {
                            stickerBitmap?.let { bmp ->
                                QrCodeGeneratorHelper.printBitmapDirectly(context, bmp, "VibeSync_${business.name}_Sticker")
                            }
                        },
                        onSavePng = {
                            stickerBitmap?.let { bmp ->
                                QrCodeGeneratorHelper.saveBitmapToGallery(context, bmp, "VibeSync_${business.name.replace(" ", "_")}_4x4_Sticker")
                            }
                        },
                        onShare = {
                            stickerBitmap?.let { bmp ->
                                QrCodeGeneratorHelper.shareBitmap(context, bmp, "${business.name} Printable Counter Sticker")
                            }
                        }
                    )

                    1 -> OrderPhysicalStickerPackTab(
                        business = business,
                        selectedPackType = selectedPackType,
                        onSelectPackType = { selectedPackType = it },
                        shippingAddress = shippingAddress,
                        onAddressChange = { shippingAddress = it },
                        phone = recipientPhone,
                        onPhoneChange = { recipientPhone = it },
                        isOrderPlaced = isOrderPlaced,
                        onPlaceOrder = {
                            isOrderPlaced = true
                            Toast.makeText(context, "🎉 Physical Sticker Order Confirmed! Dispatched within 24-48 hours.", Toast.LENGTH_LONG).show()
                        }
                    )

                    2 -> SmartDeepLinkTestTab(
                        business = business,
                        onSimulateScan = {
                            onDismiss()
                            onTestScan(business)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerStudioTab(
    business: BusinessEntity,
    stickerBitmap: Bitmap?,
    isGenerating: Boolean,
    selectedShape: StickerShape,
    onSelectShape: (StickerShape) -> Unit,
    selectedTheme: StickerTheme,
    onSelectTheme: (StickerTheme) -> Unit,
    tableLabel: String,
    onTableLabelChange: (String) -> Unit,
    customCta: String,
    onCustomCtaChange: (String) -> Unit,
    showLogo: Boolean,
    onToggleLogo: (Boolean) -> Unit,
    showOfferHighlight: Boolean,
    onToggleOfferHighlight: (Boolean) -> Unit,
    showNfcBadge: Boolean,
    onToggleNfcBadge: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onSavePng: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Print Preview (300 DPI Vector Output)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "4x4 INCH • READY TO PRINT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .shadow(8.dp, if (selectedShape == StickerShape.ROUND_CIRCLE) CircleShape else RoundedCornerShape(16.dp))
                        .clip(if (selectedShape == StickerShape.ROUND_CIRCLE) CircleShape else RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating || stickerBitmap == null) {
                        CircularProgressIndicator(color = Color(0xFFE91E63), modifier = Modifier.size(36.dp))
                    } else {
                        Image(
                            bitmap = stickerBitmap.asImageBitmap(),
                            contentDescription = "Printable Sticker Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPrint,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_print_sticker_direct"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Wi-Fi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSavePng,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_sticker_png"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PNG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_sticker"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sticker Geometry & Branding Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("1. Select Physical Sticker Format", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StickerShape.values().forEach { shape ->
                val isSelected = selectedShape == shape
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .clickable { onSelectShape(shape) }
                        .testTag("shape_${shape.name}")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = shape.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = shape.description,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("2. Select Color Theme", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StickerTheme.values().forEach { theme ->
                val isSelected = selectedTheme == theme
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .clickable { onSelectTheme(theme) }
                        .testTag("theme_${theme.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(theme.primaryColor))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = theme.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = tableLabel,
            onValueChange = onTableLabelChange,
            label = { Text("Table Number or Location Label") },
            placeholder = { Text("e.g. Table #04, Main Counter, Entrance Door, Bar Stand") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_sticker_table_label")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = customCta,
            onValueChange = onCustomCtaChange,
            label = { Text("Sticker Call-to-Action Headline") },
            placeholder = { Text("e.g. Scan with Camera or Tap NFC to View Menu, Offers & Review") },
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_sticker_cta")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show VibeSync Logo in QR Center", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Embeds official VibeSync heart emblem into the QR code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showLogo,
                        onCheckedChange = onToggleLogo,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE91E63))
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Highlight Active Venue Offer Banner", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Displays '${business.activeOfferSummary.ifBlank { "Couples Discount" }}' on sticker", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showOfferHighlight,
                        onCheckedChange = onToggleOfferHighlight,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32))
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Contactless NFC Tap Indicator", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Indicates customers can tap their phone via NFC or scan QR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showNfcBadge,
                        onCheckedChange = onToggleNfcBadge,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderPhysicalStickerPackTab(
    business: BusinessEntity,
    selectedPackType: Int,
    onSelectPackType: (Int) -> Unit,
    shippingAddress: String,
    onAddressChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isOrderPlaced: Boolean,
    onPlaceOrder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE0F2F1),
            border = BorderStroke(1.dp, Color(0xFF80CBC4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Official Weatherproof Vinyl & Acrylic Stands",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF004D40)
                    )
                    Text(
                        text = "UV-resistant, waterproof commercial stickers with embedded NTAG213 NFC chips delivered directly to your venue address.",
                        fontSize = 11.sp,
                        color = Color(0xFF00796B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Physical Merchandise Package", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedPackType == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, if (selectedPackType == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectPackType(0) }
                .testTag("pack_starter")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🏷️ Starter Venue Pack (5 Stickers + 2 Stands)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 5x 4x4\" UV Gloss Vinyl Table Stickers\n• 2x L-Shaped Clear Acrylic Counter Stands\n• Free Domestic 2-Day Shipping", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹199", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedPackType == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, if (selectedPackType == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectPackType(1) }
                .testTag("pack_pro")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("👑 Master Venue Pack (20 Stickers + 5 Stands + NFC)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 20x 4x4\" Heavy-Duty Vinyl Decals\n• 5x Premium Clear Acrylic Table Tents\n• Embedded NTAG213 Smart NFC Chips\n• Priority Express Delivery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹499", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Delivery Destination", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = shippingAddress,
            onValueChange = onAddressChange,
            label = { Text("Registered Venue Shipping Address") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Delivery Contact Phone Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (!isOrderPlaced) {
            Button(
                onClick = onPlaceOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_order_physical_pack"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Order (₹${if (selectedPackType == 0) 199 else 499}) • Pay on Delivery / UPI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Order #VIBE-STK-9921 Placed Successfully!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estimated delivery within 2 business days to: $shippingAddress. Tracking SMS sent to $phone.",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartDeepLinkTestTab(
    business: BusinessEntity,
    onSimulateScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Nfc, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant QR Scan & NFC Smart Deep-Link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When customers scan this sticker or tap with an NFC-enabled Android/iOS handset:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("1. ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE91E63))
                        Text("App Installed: Deep-links directly to ${business.name}'s profile page, active deals & in-app chat.", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("2. ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                        Text("GPS Check-in Trigger: Automatically verifies proximity and prompts customer: 'You are at ${business.name}! Drop a review & earn +50 points'.", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("3. ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1976D2))
                        Text("Web Fallback: Displays mobile-optimized venue showcase with 1-tap Google Play download.", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Generated Smart QR & NFC Payload:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "https://vibesync.app/biz/${business.id}?src=table_sticker&lat=${business.latitude}&lng=${business.longitude}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSimulateScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_simulate_qr_scan"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📲 Simulate Customer QR Scan / NFC Tap", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
