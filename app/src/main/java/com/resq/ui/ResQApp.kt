package com.resq.ui

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.resq.ui.home.HomeScreen
import com.resq.ui.messages.PacketHistoryScreen
import com.resq.ui.mesh.MeshScreen
import com.resq.ui.map.OfflineMapScreen
import com.resq.ui.map.KarnatakaMapScreen
import com.resq.ui.report.ReportScreen
import com.resq.ui.rescue.RescueModeScreen
import com.resq.ui.wifi.WifiDirectScreen
import com.resq.ui.decision.DecisionScreen
import com.resq.ui.ai.AiAnalysisScreen
import com.resq.ui.settings.SettingsScreen
import com.resq.ui.sos.SosConfirmationScreen
import com.resq.ui.theme.ResQTheme
import com.resq.ui.theme.ThemeMode

private object Routes {
    const val HOME = "home"
    const val REPORT = "report"
    const val SOS = "sos"
    const val MESSAGES = "messages"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val MESH = "mesh"
    const val RESCUE = "rescue"
    const val WIFI = "wifi"
    const val DECISION = "decision"
    const val AI = "ai"
}

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun ResQApp() {
    val context = LocalContext.current
    val viewModel: ResQViewModel = viewModel(
        factory = ResQViewModel.factory(context.applicationContext as Application)
    )
    val location by viewModel.location.collectAsState()
    val packets by viewModel.packets.collectAsState()
    val meshState by viewModel.meshState.collectAsState()
    val forwardingLogs by viewModel.forwardingLogs.collectAsState()
    val rescueMode by viewModel.rescueMode.collectAsState()
    val selectedPacketId by viewModel.selectedPacketId.collectAsState()
    val wifiState by viewModel.wifiState.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val imageAnalysisState by viewModel.imageAnalysis.collectAsState()
    val analysisState by viewModel.analysis.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val karnatakaMapState by viewModel.karnatakaMapState.collectAsState()
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var requestLiveAfterPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            if (requestLiveAfterPermission) viewModel.startLiveLocation() else viewModel.refreshLocation()
        }
        else Toast.makeText(context, "Location permission denied. Enable it in Android Settings to use GPS.", Toast.LENGTH_LONG).show()
        requestLiveAfterPermission = false
    }
    val getLocation = {
        if (viewModel.hasLocationPermission()) viewModel.refreshLocation()
        else {
            requestLiveAfterPermission = false
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val startLiveLocation = {
        if (viewModel.hasLocationPermission()) viewModel.startLiveLocation()
        else {
            requestLiveAfterPermission = true
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val bluetoothPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.refreshBluetooth()
            viewModel.startBluetoothServer()
        } else Toast.makeText(context, "Nearby devices permission is required", Toast.LENGTH_LONG).show()
    }
    val ensureBluetoothPermissions = {
        val granted = bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            viewModel.refreshBluetooth()
            viewModel.startBluetoothServer()
        } else bluetoothPermissionLauncher.launch(bluetoothPermissions)
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetooth()
        viewModel.startBluetoothServer()
    }
    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.startBluetoothServer() }
    val wifiPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) Toast.makeText(context, "Nearby Wi-Fi permission is required", Toast.LENGTH_LONG).show()
    }
    val ensureWifiPermissions = {
        val granted = wifiPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) wifiPermissionLauncher.launch(wifiPermissions)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startSpeechInput()
        else Toast.makeText(context, "Microphone permission denied; type the emergency instead", Toast.LENGTH_LONG).show()
    }
    val startSpeech = {
        if (speechState.listening) viewModel.stopSpeechInput()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startSpeechInput()
        } else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val locationText = when {
        location.loading -> "Finding location…"
        location.fix != null -> buildString {
            append("${"%.5f".format(location.fix!!.latitude)}, ${"%.5f".format(location.fix!!.longitude)}")
            location.fix!!.accuracyMeters?.let { append(" • ±${it.toInt()} m") }
            append(if (location.fix!!.isStale) " • last known" else if (location.live) " • live" else " • current")
        }
        location.error != null -> location.error!!
        else -> "Location not captured"
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) viewModel.refreshLocation()
        viewModel.events.collect { event ->
            Toast.makeText(context, event.message, if (event.success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    ResQTheme(themeMode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val current = backStackEntry?.destination
        val bottomDestinations = listOf(
            BottomDestination(Routes.HOME, "Home", Icons.Default.Home),
            BottomDestination(Routes.MESSAGES, "Messages", Icons.Default.Message),
            BottomDestination(Routes.MAP, "Map", Icons.Default.Map),
            BottomDestination(Routes.SETTINGS, "Settings", Icons.Default.Settings)
        )
        val showBottomBar = current?.route in bottomDestinations.map { it.route }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomDestinations.forEach { item ->
                            NavigationBarItem(
                                selected = current?.hierarchy?.any { it.route == item.route } == true,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        deviceId = viewModel.deviceId,
                        storedPacketCount = packets.size,
                        locationText = locationText,
                        onGetLocation = getLocation,
                        onMesh = { navController.navigate(Routes.MESH) },
                        onRescue = { navController.navigate(Routes.RESCUE) },
                        onWifi = { navController.navigate(Routes.WIFI) },
                        onDecision = { navController.navigate(Routes.DECISION) },
                        onSos = { viewModel.triggerDirectSos() },
                        onReport = { navController.navigate(Routes.REPORT) },
                        onMap = { navController.navigate(Routes.MAP) }
                    )
                }
                composable(Routes.REPORT) {
                    LaunchedEffect(Unit) { viewModel.clearEmergencyImage() }
                    ReportScreen(
                        locationText = locationText,
                        hasLocation = location.fix != null,
                        onGetLocation = getLocation,
                        speechState = speechState,
                        imageState = imageAnalysisState,
                        onSpeak = startSpeech,
                        onAnalyzeImage = viewModel::analyzeEmergencyImage,
                        onClearImage = viewModel::discardEmergencyImage,
                        onBack = { navController.popBackStack() },
                        onContinue = {
                            viewModel.analyzeDraft(it)
                            navController.navigate(Routes.AI)
                        }
                    )
                }
                composable(Routes.AI) {
                    AiAnalysisScreen(
                        state = analysisState,
                        locationText = locationText,
                        onBack = { navController.popBackStack() },
                        onCreatePacket = {
                            viewModel.createAnalyzedPacket()
                            navController.navigate(Routes.MESSAGES) {
                                popUpTo(Routes.HOME)
                            }
                        }
                    )
                }
                composable(Routes.SOS) {
                    SosConfirmationScreen(
                        deviceId = viewModel.deviceId,
                        locationText = locationText,
                        hasLocation = location.fix != null,
                        onGetLocation = getLocation,
                        onBack = { navController.popBackStack() },
                        onConfirmed = {
                            viewModel.triggerDirectSos()
                            navController.popBackStack()
                        }
                    )
                }
                composable(Routes.MESH) {
                    LaunchedEffect(Unit) { ensureBluetoothPermissions() }
                    MeshScreen(
                        state = meshState,
                        packets = packets,
                        selectedPacketId = selectedPacketId,
                        onBack = { navController.popBackStack() },
                        onEnableBluetooth = {
                            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        },
                        onMakeDiscoverable = {
                            discoverableLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(
                                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300
                                )
                            )
                        },
                        onScan = viewModel::scanForPeers,
                        onSelectPacket = viewModel::selectPacket,
                        onSend = viewModel::sendSelectedPacket
                    )
                }
                composable(Routes.RESCUE) {
                    LaunchedEffect(Unit) { ensureBluetoothPermissions() }
                    RescueModeScreen(
                        enabled = rescueMode,
                        packets = packets,
                        onEnabledChange = viewModel::setRescueMode,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.WIFI) {
                    LaunchedEffect(Unit) { ensureWifiPermissions() }
                    WifiDirectScreen(
                        state = wifiState,
                        packets = packets,
                        selectedPacketId = selectedPacketId,
                        onBack = { navController.popBackStack() },
                        onHost = viewModel::startWifiHost,
                        onDiscover = viewModel::discoverWifiPeers,
                        onSelectPacket = viewModel::selectPacket,
                        onSend = viewModel::sendSelectedPacketWifi
                    )
                }
                composable(Routes.DECISION) {
                    DecisionScreen(
                        decision = viewModel.currentDecision(),
                        onBack = { navController.popBackStack() },
                        onOpenBluetooth = { navController.navigate(Routes.MESH) },
                        onOpenWifi = { navController.navigate(Routes.WIFI) }
                    )
                }
                composable(Routes.MESSAGES) { PacketHistoryScreen(packets, forwardingLogs) }
                composable(Routes.MAP) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_START -> startLiveLocation()
                                Lifecycle.Event.ON_STOP -> viewModel.stopLiveLocation()
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        startLiveLocation()
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            viewModel.stopLiveLocation()
                        }
                    }
                    KarnatakaMapScreen(
                        state = karnatakaMapState,
                        markers = mapMarkers,
                        mapFile = viewModel.currentKarnatakaMapFile(),
                        currentLatitude = location.fix?.latitude,
                        currentLongitude = location.fix?.longitude,
                        locationIsStale = location.fix?.isStale == true,
                        liveLocationActive = location.live,
                        onGetLocation = getLocation,
                        onImport = viewModel::importKarnatakaMap
                    ) {
                        OfflineMapScreen(
                            markers = mapMarkers,
                            centerLatitude = location.fix?.latitude,
                            centerLongitude = location.fix?.longitude,
                            onSeedMarkers = viewModel::seedOfflineMapMarkers,
                            onGetLocation = getLocation
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(viewModel.deviceId, themeMode) { themeMode = it }
                }
            }
        }
    }
}
