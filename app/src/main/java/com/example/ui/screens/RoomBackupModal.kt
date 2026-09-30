package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.room.RoomBackupSnapshotEntity
import com.example.ui.viewmodel.InvoiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoomBackupModal(
  viewModel: InvoiceViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()

  var showCreateDialog by remember { mutableStateOf(false) }
  var newBackupTitle by remember { mutableStateOf("") }
  var newBackupNote by remember { mutableStateOf("") }

  var snapshotToRestore by remember { mutableStateOf<RoomBackupSnapshotEntity?>(null) }
  var snapshotToDelete by remember { mutableStateOf<RoomBackupSnapshotEntity?>(null) }

  var showManualPasteDialog by remember { mutableStateOf(false) }
  var manualJsonText by remember { mutableStateOf("") }

  val clipboardManager = LocalClipboardManager.current

  // Save Backup directly to user storage location (Downloads, Documents, etc.)
  val saveFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    uri?.let {
      viewModel.saveBackupToUri(context, it)
    }
  }

  // File Picker for importing json backup from device storage
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let {
      viewModel.importBackupFromUri(context, it)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.loadRoomBackupData()
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      Card(
        modifier = Modifier
          .fillMaxWidth(0.96f)
          .fillMaxHeight(0.94f)
          .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          // Modal Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF28A745).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Storage,
                  contentDescription = null,
                  tint = Color(0xFF28A745)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "💾 النسخ الاحتياطي في Room محلياً",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF1B5E20)
                )
                Text(
                  text = "حماية بيانات الفواتير والعملاء محلياً على ذاكرة الجهاز",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

          // 1. Status Banner Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFD4EDDA)))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF28A745),
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "قاعدة بيانات Room نشطة ومتزامنة محلياً على الجهاز",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF155724)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
              ) {
                StatusItem(
                  title = "الفواتير الحالية",
                  value = "${uiState.savedInvoices.size}",
                  color = Color(0xFF5E258D)
                )
                StatusItem(
                  title = "العملاء والحسابات",
                  value = "${uiState.customers.size}",
                  color = Color(0xFF007BFF)
                )
                StatusItem(
                  title = "نسخ Room الاحتياطية",
                  value = "${uiState.roomSnapshots.size}",
                  color = Color(0xFF28A745)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 2. Action Buttons
          // Row 1: Create local snapshot / Save file to device
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                newBackupTitle = ""
                newBackupNote = ""
                showCreateDialog = true
              },
              modifier = Modifier
                .weight(1f)
                .height(42.dp),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
            ) {
              Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("➕ حفظ نسخة في Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                saveFileLauncher.launch("Mamlaka_Backup_$timeStamp.json")
              },
              modifier = Modifier
                .weight(1f)
                .height(42.dp),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17A2B8))
            ) {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("💾 حفظ ملف بالجهاز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Row 2: Share / Copy
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { viewModel.exportAndShareBackup(context) },
              modifier = Modifier
                .weight(1f)
                .height(40.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF17A2B8))
              Spacer(modifier = Modifier.width(4.dp))
              Text("📤 تصدير ومشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17A2B8))
            }

            OutlinedButton(
              onClick = {
                val json = viewModel.exportBackup()
                clipboardManager.setText(AnnotatedString(json))
                viewModel.showToast("📋 تم نسخ كود النسخة الاحتياطية إلى الحافظة بنجاح!")
              },
              modifier = Modifier
                .weight(1f)
                .height(40.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF5E258D))
              Spacer(modifier = Modifier.width(4.dp))
              Text("📋 نسخ كود النسخة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5E258D))
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Row 3: Import File / Paste Code
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
              modifier = Modifier
                .weight(1f)
                .height(40.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("📂 استيراد من ملف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                manualJsonText = ""
                showManualPasteDialog = true
              },
              modifier = Modifier
                .weight(1f)
                .height(40.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("📋 استعادة من كود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 3. Snapshots List Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "📋 سجل النسخ الاحتياطية المحفوظة في Room (${uiState.roomSnapshots.size})",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF343A40)
            )
            if (uiState.isRoomOperationInProgress) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // 4. Snapshots List
          if (uiState.roomSnapshots.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(10.dp))
                .padding(20.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = "📦 لا توجد نسخ احتياطية محفوظة حالياً في Room",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "اضغط على «➕ نسخة احتياطية جديدة» لإنشاء أول نسخة فوراً لحماية الفواتير والعملاء.",
                  fontSize = 12.sp,
                  color = Color.DarkGray
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(uiState.roomSnapshots, key = { it.id }) { snapshot ->
                SnapshotCard(
                  snapshot = snapshot,
                  onRestore = { snapshotToRestore = snapshot },
                  onDelete = { snapshotToDelete = snapshot }
                )
              }
            }
          }
        }
      }
    }
  }

  // Dialog 1: Create New Backup
  if (showCreateDialog) {
    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("💾 إنشاء نسخة احتياطية محلية في Room", fontWeight = FontWeight.Bold, color = Color(0xFF28A745)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "سيتم حفظ جميع الفواتير الحالية (${uiState.savedInvoices.size}) وحسابات العملاء (${uiState.customers.size}) في قاعدة بيانات Room المحلية بشكل آمن.",
            fontSize = 13.sp,
            color = Color.DarkGray
          )
          OutlinedTextField(
            value = newBackupTitle,
            onValueChange = { newBackupTitle = it },
            label = { Text("اسم النسخة (اختياري)") },
            placeholder = { Text("مثال: نسخة نهاية الشهر / قبل الجرد") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newBackupNote,
            onValueChange = { newBackupNote = it },
            label = { Text("ملاحظة إضافية (اختياري)") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.createLocalRoomBackup(newBackupTitle, newBackupNote)
            showCreateDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
        ) {
          Text("حفظ الآن", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Button(onClick = { showCreateDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog 2: Restore Snapshot Confirm
  snapshotToRestore?.let { snapshot ->
    AlertDialog(
      onDismissRequest = { snapshotToRestore = null },
      title = { Text("⚠️ تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold, color = Color(0xFFDC3545)) },
      text = {
        Text(
          text = "هل أنت متأكد من استعادة النسخة «${snapshot.title}» بتاريخ (${snapshot.timestamp})؟\n\nتحتوي على (${snapshot.invoiceCount}) فاتورة و (${snapshot.customerCount}) حساب عميل.\nسيتم تحديث البيانات الحالية في النظام بمحتوى هذه النسخة.",
          fontSize = 13.sp,
          color = Color.DarkGray
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.restoreFromRoomSnapshot(snapshot)
            snapshotToRestore = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545))
        ) {
          Text("نعم، استعادة الآن", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Button(onClick = { snapshotToRestore = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog 3: Delete Snapshot Confirm
  snapshotToDelete?.let { snapshot ->
    AlertDialog(
      onDismissRequest = { snapshotToDelete = null },
      title = { Text("🗑️ تأكيد حذف النسخة", fontWeight = FontWeight.Bold, color = Color(0xFFDC3545)) },
      text = {
        Text(
          text = "هل أنت متأكد من حذف النسخة الاحتياطية «${snapshot.title}»؟ لن تتمكن من التراجع عن هذا الإجراء.",
          fontSize = 13.sp,
          color = Color.DarkGray
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteRoomSnapshot(snapshot.id)
            snapshotToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545))
        ) {
          Text("حذف", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Button(onClick = { snapshotToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog 4: Manual Paste JSON
  if (showManualPasteDialog) {
    AlertDialog(
      onDismissRequest = { showManualPasteDialog = false },
      title = { Text("📋 استعادة من كود أو نص النسخة", fontWeight = FontWeight.Bold, color = Color(0xFF5E258D)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("الصق نص النسخة الاحتياطية (JSON) هنا للاستعادة الفورية:", fontSize = 12.sp, color = Color.Gray)
          OutlinedButton(
            onClick = {
              val clip = clipboardManager.getText()?.text
              if (!clip.isNullOrBlank()) {
                manualJsonText = clip
                viewModel.showToast("📋 تم لصق النص من الحافظة تلقائياً!")
              } else {
                viewModel.showToast("⚠️ الحافظة فارغة")
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("📋 لصق مباشر من الحافظة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          OutlinedTextField(
            value = manualJsonText,
            onValueChange = { manualJsonText = it },
            placeholder = { Text("الصق بيانات JSON هنا...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (manualJsonText.isNotBlank()) {
              val ok = viewModel.importBackup(manualJsonText)
              if (ok) {
                showManualPasteDialog = false
              }
            } else {
              viewModel.showToast("⚠️ يرجى لصق نص النسخة أولاً")
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
        ) {
          Text("استعادة الآن", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Button(onClick = { showManualPasteDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}

@Composable
fun StatusItem(title: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
    Text(text = title, fontSize = 11.sp, color = Color.Gray)
  }
}

@Composable
fun SnapshotCard(
  snapshot: RoomBackupSnapshotEntity,
  onRestore: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = snapshot.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212529)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "📅 ${snapshot.timestamp}",
            fontSize = 11.sp,
            color = Color.Gray
          )
          if (snapshot.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "📝 ${snapshot.note}",
              fontSize = 11.sp,
              color = Color(0xFF5E258D)
            )
          }
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Button(
            onClick = onRestore,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("استعادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "حذف",
              tint = Color(0xFFDC3545),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Data tags
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .background(Color(0xFFEDE7F6), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "📄 ${snapshot.invoiceCount} فاتورة",
            fontSize = 11.sp,
            color = Color(0xFF5E258D),
            fontWeight = FontWeight.Bold
          )
        }

        Box(
          modifier = Modifier
            .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "👥 ${snapshot.customerCount} حساب عميل",
            fontSize = 11.sp,
            color = Color(0xFF1976D2),
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
