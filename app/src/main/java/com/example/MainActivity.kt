package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.ShortcutContact
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.SmsSender
import com.example.viewmodel.SmsShortcutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SmsShortcutDialApp()
                }
            }
        }
    }
}

// Custom Colors mapped to Elegant Dark Design Theme
val DeepMidnight = Color(0xFF1C1B1F)      // Background bg-[#1C1B1F]
val SurfaceDark = Color(0xFF25232A)       // Card background bg-[#25232A]
val BannerDark = Color(0xFF313033)        // Hint banner bg-[#313033]
val ElegantBorder = Color(0xFF49454F)     // Border outline border-[#49454F]

val PersianTurquoise = Color(0xFFD0BCFF)    // Changed to ElegantPrimary color [#D0BCFF]
val PersianTurquoiseLight = Color(0xFFEADDFF) // Changed to ElegantOnPrimaryContainer [#EADDFF]
val WarmGold = Color(0xFFF9D15D)          // Golden accent
val LightGrayText = Color(0xFFCAC4D0)     // Main body/text [#CAC4D0]
val SubtleGrayText = Color(0xFF938F99)    // Secondary small text [#938F99]
val ErrorRed = Color(0xFFF2B8B5)          // Light Crimson error
val SuccessGreen = Color(0xFFB1F1CB)      // Soft green success
val ElegantPrimary = Color(0xFFD0BCFF)
val ElegantOnPrimary = Color(0xFF381E72)
val ElegantPrimaryContainer = Color(0xFF4F378B)
val ElegantOnPrimaryContainer = Color(0xFFEADDFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsShortcutDialApp() {
    val context = LocalContext.current
    val viewModel: SmsShortcutViewModel = viewModel()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    
    // Permission State Managers
    val permissions = remember {
        val list = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list.add(Manifest.permission.READ_PHONE_STATE)
        }
        list.toTypedArray()
    }

    var isPermissionsGranted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    var isRoleGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                roleManager?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
            } else {
                true
            }
        )
    }

    // Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isPermissionsGranted = result.values.all { it }
        if (isPermissionsGranted) {
            Toast.makeText(context, "دسترسی‌های پیامک با موفقیت تایید شدند.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "برای کارکرد خودکار میانبر، تایید دسترسی پیامک الزامی است.", Toast.LENGTH_LONG).show()
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            isRoleGranted = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
        }
    }

    // Dialog form triggers
    var isAddOrEditOpen by remember { mutableStateOf(false) }
    var selectedForEdit by remember { mutableStateOf<ShortcutContact?>(null) }
    var isDeleteConfirmOpen by remember { mutableStateOf<ShortcutContact?>(null) }
    var isAboutOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepMidnight),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "آیکون برنامه",
                            tint = PersianTurquoiseLight
                        )
                        Text(
                            text = "میانبر پیامکی شماره‌گیر",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isAboutOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "درباره برنامه",
                            tint = PersianTurquoiseLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMidnight,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedForEdit = null
                    isAddOrEditOpen = true
                },
                containerColor = PersianTurquoise,
                contentColor = ElegantOnPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_shortcut_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "افزودن میانبر جدید",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = DeepMidnight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Setup & Configuration Banners
            if (!isPermissionsGranted) {
                PermissionWarningCard(
                    title = "تایید دسترسی پیامک",
                    description = "برای ارسال خودکار پیامک هنگام شماره‌گیری کدهای میانبر، نیاز به دسترسی پیامک داریم.",
                    buttonText = "اعطای دسترسی",
                    onClick = { permissionLauncher.launch(permissions) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isRoleGranted) {
                PermissionWarningCard(
                    title = "تنظیم دستیار تماس (سیستم عامل ۱۰ به بالا)",
                    description = "برای شنیدن و لغو کدهای دایورتر شماره‌گیر، لازم است برنامه را به عنوان دستیار هدایت تماس فعال کنید.",
                    buttonText = "فعال‌سازی دستیار تماس",
                    onClick = {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                        if (roleManager != null) {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                            roleLauncher.launch(intent)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Interactive Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                placeholder = { Text("جستجو میان نام، شماره یا کد...", color = LightGrayText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "آیکون جستجو", tint = PersianTurquoise) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "پاک کردن", tint = LightGrayText)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PersianTurquoise,
                    unfocusedBorderColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Area
            val filteredShortcuts = remember(shortcuts, searchQuery) {
                shortcuts.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery) ||
                    it.shortcutCode.contains(searchQuery)
                }
            }

            if (filteredShortcuts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "لیست خالی",
                            tint = LightGrayText.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "هیچ میانبری ثبت نشده است!" else "هیچ موردی یافت نشد!",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) 
                                "برای شروع، کلید افزودن (+) پائین را فشار دهید." 
                            else 
                                "کلمه‌ی دیگری را برای جستجو انتخاب کنید.",
                            color = LightGrayText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shortcuts_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredShortcuts, key = { it.id }) { shortcut ->
                        ShortcutItemCard(
                            shortcut = shortcut,
                            onEdit = {
                                selectedForEdit = shortcut
                                isAddOrEditOpen = true
                            },
                            onDelete = {
                                isDeleteConfirmOpen = shortcut
                            }
                        )
                    }
                }
            }

            // Quick Info Footer matching the "Elegant Dark" usage hint banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = BannerDark),
                border = BorderStroke(1.dp, ElegantBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElegantPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "راهنما",
                            tint = ElegantOnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "راهنما: کافیست کُد میانبر ثبت شده را در شماره‌گیر تلفن خود شماره‌گیری کنید تا پیامک تعیین شده فوراً ارسال گردد.",
                        color = LightGrayText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Modal Add & Edit Dialog
    if (isAddOrEditOpen) {
        AddOrEditShortcutDialog(
            shortcut = selectedForEdit,
            onDismiss = { isAddOrEditOpen = false },
            onSave = { name, phone, message, code ->
                if (selectedForEdit != null) {
                    viewModel.updateShortcut(
                        selectedForEdit!!.copy(
                            name = name,
                            phoneNumber = phone,
                            message = message,
                            shortcutCode = code
                        )
                    )
                    Toast.makeText(context, "تغییرات با موفقیت ذخیره شد.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addShortcut(name, phone, message, code)
                    Toast.makeText(context, "میانبر جدید ثبت شد.", Toast.LENGTH_SHORT).show()
                }
                isAddOrEditOpen = false
            }
        )
    }

    // Modal Delete Confirm
    if (isDeleteConfirmOpen != null) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmOpen = null },
            title = { Text("حذف میانبر", fontWeight = FontWeight.Bold) },
            text = { Text("آیا مطمئن هستید که می‌خواهید میانبر مخاطب (${isDeleteConfirmOpen!!.name}) را حذف نمایید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShortcut(isDeleteConfirmOpen!!)
                        isDeleteConfirmOpen = null
                        Toast.makeText(context, "میانبر با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmOpen = null }) {
                    Text("انصراف", color = Color.White)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = LightGrayText
        )
    }

    // Modal About App
    if (isAboutOpen) {
        AlertDialog(
            onDismissRequest = { isAboutOpen = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "درباره برنامه",
                        tint = ElegantPrimary
                    )
                    Text("درباره برنامه", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "این برنامه به شما امکان می‌دهد کدهای میانبر شماره‌گیر تلفن همراه را تعریف و مدیریت کنید. با شماره‌گیری هر میانبر در گوشی، عملیات فرآیند تماس قطع شده و پیامک تعیین‌شده مرتبط با آن خودکار ارسال می‌گردد.",
                        color = LightGrayText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ElegantBorder))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "تهیه کننده برنامه:",
                            color = ElegantPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "مهدی اسماعیلی",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "سرپرست فناوری اطلاعات و ارتباطات شرکت عمران آذرستان",
                            color = LightGrayText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ElegantBorder))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نسخه برنامه:",
                            color = ElegantPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "۱.۲.۰",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isAboutOpen = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary)
                ) {
                    Text("بستن", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = LightGrayText
        )
    }
}

@Composable
fun PermissionWarningCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "هشدار",
                    tint = WarmGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = LightGrayText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = buttonText, color = DeepMidnight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutItemCard(
    shortcut: ShortcutContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shortcut_card_${shortcut.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, ElegantBorder),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shortcut.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = shortcut.phoneNumber,
                        color = LightGrayText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Dial Shortcut Code badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElegantPrimaryContainer)
                        .border(BorderStroke(1.dp, ElegantPrimary.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "کد شماره‌گیری",
                            tint = ElegantOnPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = shortcut.shortcutCode,
                            color = ElegantOnPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SMS Body Message Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepMidnight, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start-side elegant solid accent bar representing the border-l-2
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElegantPrimary)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "پیامک تعیین شده:",
                            color = ElegantPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shortcut.message,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Send SMS Manually trigger
                Button(
                    onClick = {
                        // Triggers SMS immediately
                        SmsSender.sendSms(context, shortcut.phoneNumber, shortcut.message)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("send_sms_button_${shortcut.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "ارسال پیامک فوری",
                        tint = ElegantOnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ارسال پیامک فوری", color = ElegantOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Edit & Delete Icons package
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_button_${shortcut.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = LightGrayText
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_button_${shortcut.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditShortcutDialog(
    shortcut: ShortcutContact?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, message: String, code: String) -> Unit
) {
    var name by remember { mutableStateOf(shortcut?.name ?: "") }
    var phone by remember { mutableStateOf(shortcut?.phoneNumber ?: "") }
    var message by remember { mutableStateOf(shortcut?.message ?: "") }
    var code by remember { mutableStateOf(shortcut?.shortcutCode ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (shortcut != null) "ویرایش میانبر" else "افزودن میانبر جدید",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = it.trim().isEmpty()
                        },
                        label = { Text("نام مخاطب") },
                        isError = nameError,
                        modifier = Modifier.fillMaxWidth().testTag("field_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PersianTurquoise,
                            unfocusedBorderColor = LightGrayText.copy(alpha = 0.5f),
                            errorBorderColor = ErrorRed
                        ),
                        singleLine = true
                    )
                }

                item {
                    // Mobile Number Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = it.trim().isEmpty()
                        },
                        label = { Text("شماره موبایل") },
                        isError = phoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("field_phone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PersianTurquoise,
                            unfocusedBorderColor = LightGrayText.copy(alpha = 0.5f),
                            errorBorderColor = ErrorRed
                        ),
                        singleLine = true
                    )
                }

                item {
                    // Shortcut Code
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it
                            codeError = it.trim().isEmpty()
                        },
                        label = { Text("کد شماره‌گیری میانبر") },
                        placeholder = { Text("مثال: ۱۱۰ یا *۱#") },
                        isError = codeError,
                        modifier = Modifier.fillMaxWidth().testTag("field_code"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PersianTurquoise,
                            unfocusedBorderColor = LightGrayText.copy(alpha = 0.5f),
                            errorBorderColor = ErrorRed
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "هنگام شماره‌گیری این کُد در گوشی، فرآیند تماس قطع شده و پیامک مربوطه خودکار ارسال می‌گردد.",
                        color = LightGrayText,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                item {
                    // SMS Custom Message Message Box
                    OutlinedTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            messageError = it.trim().isEmpty()
                        },
                        label = { Text("متن پیامک سفارشی") },
                        isError = messageError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("field_message"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PersianTurquoise,
                            unfocusedBorderColor = LightGrayText.copy(alpha = 0.5f),
                            errorBorderColor = ErrorRed
                        ),
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.trim().isEmpty()
                    phoneError = phone.trim().isEmpty()
                    codeError = code.trim().isEmpty()
                    messageError = message.trim().isEmpty()

                    if (!nameError && !phoneError && !codeError && !messageError) {
                        onSave(name, phone, message, code)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PersianTurquoise,
                    contentColor = ElegantOnPrimary
                ),
                modifier = Modifier.testTag("save_button")
            ) {
                Text("ذخیره", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_button")
            ) {
                Text("لغو", color = PersianTurquoise)
            }
        },
        containerColor = SurfaceDark,
        titleContentColor = Color.White
    )
}

// Simple extension helper to avoid duplicate styling configurations
@Composable
fun Modifier.fillPaddingAndWidth(): Modifier {
    return this.fillMaxWidth()
}
