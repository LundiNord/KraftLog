package de.nyxnord.kraftlog.ui.friends

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.ExerciseShareRecord
import de.nyxnord.kraftlog.data.ShareableStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(app: KraftLogApplication) {
    val context = LocalContext.current
    val btManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val btAdapter = remember { btManager?.adapter }

    val vm: FriendsViewModel = viewModel(
        factory = FriendsViewModel.factory(
            app.userPreferences,
            app.exerciseRepository,
            app.workoutRepository,
            btAdapter
        )
    )

    val uiState by vm.uiState.collectAsState()
    val displayName by vm.displayName.collectAsState()

    fun hasPermissions() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.BLUETOOTH_SCAN
    ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    // Tracks what to do after permissions / discoverability are granted
    var pendingAction by remember { mutableStateOf<String?>(null) }

    val discoverableIntent = remember {
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.startWaiting()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            when (pendingAction) {
                "wait" -> discoverableLauncher.launch(discoverableIntent)
                "find" -> {
                    vm.startScanning()
                    btAdapter?.startDiscovery()
                }
            }
        }
        pendingAction = null
    }

    // BroadcastReceiver for Bluetooth device discovery
    val isScanning = uiState is FriendsUiState.Scanning || uiState is FriendsUiState.DevicesFound
    DisposableEffect(isScanning) {
        if (isScanning) {
            val receiver = object : BroadcastReceiver() {
                @SuppressLint("MissingPermission")
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action == BluetoothDevice.ACTION_FOUND) {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let { vm.onDeviceFound(it) }
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            onDispose { context.unregisterReceiver(receiver) }
        } else {
            onDispose { }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Friends") }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is FriendsUiState.Idle -> IdleContent(
                    displayName = displayName,
                    onNameChange = { vm.saveDisplayName(it) },
                    onWaitForFriend = {
                        pendingAction = "wait"
                        if (hasPermissions()) {
                            discoverableLauncher.launch(discoverableIntent)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                            )
                        }
                    },
                    onFindFriend = {
                        pendingAction = "find"
                        if (hasPermissions()) {
                            vm.startScanning()
                            btAdapter?.startDiscovery()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                            )
                        }
                    }
                )
                is FriendsUiState.Waiting -> WaitingContent(onCancel = { vm.reset() })
                is FriendsUiState.Scanning -> ScanningContent(
                    devices = emptyList(),
                    onCancel = { btAdapter?.cancelDiscovery(); vm.reset() },
                    onConnect = { vm.connectTo(it) }
                )
                is FriendsUiState.DevicesFound -> ScanningContent(
                    devices = state.devices,
                    onCancel = { btAdapter?.cancelDiscovery(); vm.reset() },
                    onConnect = { vm.connectTo(it) }
                )
                is FriendsUiState.Connecting -> ConnectingContent()
                is FriendsUiState.Connected -> ComparisonContent(
                    myStats = state.myStats,
                    friendStats = state.friendStats,
                    onDone = { vm.reset() }
                )
                is FriendsUiState.Error -> ErrorContent(
                    message = state.message,
                    onBack = { vm.reset() }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    displayName: String,
    onNameChange: (String) -> Unit,
    onWaitForFriend: () -> Unit,
    onFindFriend: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Exchange stats with a friend over Bluetooth and see how you compare.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text("Your display name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onWaitForFriend, modifier = Modifier.fillMaxWidth()) {
            Text("Wait for Friend")
        }
        OutlinedButton(onClick = onFindFriend, modifier = Modifier.fillMaxWidth()) {
            Text("Find a Friend")
        }
    }
}

@Composable
private fun WaitingContent(onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        CircularProgressIndicator()
        Text("Waiting for a friend to connect…", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Ask your friend to open KraftLog and tap \"Find a Friend\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ScanningContent(
    devices: List<BluetoothDevice>,
    onCancel: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Scanning for nearby devices…")
        }
        if (devices.isEmpty()) {
            Text(
                "Make sure your friend taps \"Wait for Friend\" and enables discoverability.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            devices.forEach { device ->
                Card(
                    onClick = { onConnect(device) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(device.name ?: "Unknown device") },
                        supportingContent = { Text(device.address) }
                    )
                }
            }
        }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        CircularProgressIndicator()
        Text("Connecting and exchanging stats…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ComparisonContent(
    myStats: ShareableStats,
    friendStats: ShareableStats,
    onDone: () -> Unit
) {
    val sharedExercises = remember(myStats, friendStats) {
        val friendNames = friendStats.exerciseRecords.map { it.exerciseName }.toSet()
        myStats.exerciseRecords.filter { it.exerciseName in friendNames }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    myStats.displayName.ifBlank { "You" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    friendStats.displayName.ifBlank { "Friend" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        if (sharedExercises.isEmpty()) {
            item {
                Text(
                    "No shared exercises yet — log some workouts and try again!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sharedExercises) { mine ->
                val theirs = friendStats.exerciseRecords.first { it.exerciseName == mine.exerciseName }
                ExerciseComparisonRow(mine = mine, theirs = theirs)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Recent sessions (last 10)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${myStats.recentSessions.size}",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (myStats.recentSessions.size >= friendStats.recentSessions.size)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${friendStats.recentSessions.size}",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (friendStats.recentSessions.size >= myStats.recentSessions.size)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ExerciseComparisonRow(mine: ExerciseShareRecord, theirs: ExerciseShareRecord) {
    val myScore = mine.bestEstimated1RM ?: mine.maxWeightKg ?: mine.maxRepsInSet?.toFloat() ?: 0f
    val theirScore = theirs.bestEstimated1RM ?: theirs.maxWeightKg ?: theirs.maxRepsInSet?.toFloat() ?: 0f
    val iWin = myScore >= theirScore

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                mine.exerciseName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBlock(record = mine, winner = iWin, align = Alignment.Start)
                Text("vs", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatBlock(record = theirs, winner = !iWin, align = Alignment.End)
            }
        }
    }
}

@Composable
private fun StatBlock(record: ExerciseShareRecord, winner: Boolean, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        val mainText = record.bestEstimated1RM?.let { "${formatWeight(it)} kg est. 1RM" }
            ?: record.maxWeightKg?.let { "${formatWeight(it)} kg" }
            ?: record.maxRepsInSet?.let { "$it reps" }
            ?: "—"
        Text(
            mainText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (winner) FontWeight.Bold else FontWeight.Normal,
            color = if (winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            "${record.totalSessions} sessions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Connection failed", style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onBack) { Text("Back") }
    }
}

private fun formatWeight(kg: Float): String =
    if (kg % 1f == 0f) "${kg.toInt()}" else "${"%.1f".format(kg)}"
