package com.resq.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.resq.data.model.MapMarker
import com.resq.map.KarnatakaMapState
import com.resq.map.NearbyRescueService
import com.resq.map.RescueDistance
import com.resq.map.RescueServiceCategory
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import kotlin.math.roundToInt

@Composable
fun KarnatakaMapScreen(
    state: KarnatakaMapState,
    markers: List<MapMarker>,
    mapFile: File?,
    currentLatitude: Double?,
    currentLongitude: Double?,
    locationIsStale: Boolean,
    liveLocationActive: Boolean,
    onGetLocation: () -> Unit,
    onImport: (android.net.Uri) -> Unit,
    schematicMap: @Composable () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    if (state.installed && mapFile != null) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            ResQHeader("Karnataka Rescue Map")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Default.CloudDone, null) },
                    label = { Text("Offline • ${formatSize(state.sizeBytes)}") }
                )
                TextButton(onClick = { picker.launch(arrayOf("application/octet-stream", "application/x-pmtiles", "*/*")) }) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Replace map")
                }
            }
            RealKarnatakaMap(
                file = mapFile,
                markers = markers,
                currentLatitude = currentLatitude,
                currentLongitude = currentLongitude,
                locationIsStale = locationIsStale,
                liveLocationActive = liveLocationActive,
                onGetLocation = onGetLocation,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Text("© OpenStreetMap contributors • Protomaps • offline directional paths", fontSize = 10.sp)
            Spacer(Modifier.height(8.dp))
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ResQHeader("Karnataka Rescue Map")
                ResQCard(Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Import the Karnataka map package", style = MaterialTheme.typography.titleMedium)
                    Text("Select karnataka.pmtiles (maximum 1 GB). It is copied into ResQ and works without internet.")
                    if (state.importing) {
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Importing ${state.progressPercent}% • ${formatSize(state.sizeBytes)}", fontSize = 12.sp)
                    } else {
                        Button(
                            onClick = { picker.launch(arrayOf("application/octet-stream", "application/x-pmtiles", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileOpen, null)
                            Spacer(Modifier.width(7.dp))
                            Text("Import Karnataka Map")
                        }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
                Text("The schematic map remains available until a real map is imported.", fontSize = 11.sp)
            }
            Box(Modifier.weight(1f)) { schematicMap() }
        }
    }
}

private val rescueLayerIds = RescueServiceCategory.entries.map { it.layerId }.distinct().toTypedArray()

@Composable
private fun RealKarnatakaMap(
    file: File,
    markers: List<MapMarker>,
    currentLatitude: Double?,
    currentLongitude: Double?,
    locationIsStale: Boolean,
    liveLocationActive: Boolean,
    onGetLocation: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedPoi by remember { mutableStateOf<NearbyRescueService?>(null) }
    var nearbyResults by remember { mutableStateOf<List<NearbyRescueService>>(emptyList()) }
    var findMenuOpen by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var showRoute by remember { mutableStateOf(false) }
    var hasCenteredOnDevice by remember(file.absolutePath) { mutableStateOf(false) }
    val deviceLocationIcon = remember(context) { createDeviceLocationIcon(context) }
    val latestLatitude by rememberUpdatedState(currentLatitude)
    val latestLongitude by rememberUpdatedState(currentLongitude)

    val mapView = remember(file.absolutePath) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { readyMap ->
                readyMap.setStyle(Style.Builder().fromJson(karnatakaStyle(file))) {
                    val start = if (latestLatitude != null && latestLongitude != null) {
                        LatLng(latestLatitude!!, latestLongitude!!)
                    } else {
                        LatLng(12.9716, 77.5946)
                    }
                    readyMap.cameraPosition = CameraPosition.Builder().target(start).zoom(14.5).build()
                    readyMap.addOnMapClickListener { tapped ->
                        val feature = readyMap.queryRenderedFeatures(
                            readyMap.projection.toScreenLocation(tapped),
                            *rescueLayerIds
                        ).firstOrNull()
                        val service = feature?.toService(latestLatitude, latestLongitude)
                        if (service != null) {
                            selectedPoi = service
                            nearbyResults = listOf(service)
                            showRoute = false
                            true
                        } else {
                            false
                        }
                    }
                    map = readyMap
                }
            }
        }
    }

    DisposableEffect(mapView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(map, markers, currentLatitude, currentLongitude, selectedPoi, locationIsStale, liveLocationActive) {
        map?.let { mapLibre ->
            mapLibre.clear()
            markers.forEach { marker ->
                mapLibre.addMarker(
                    MarkerOptions().position(LatLng(marker.lat, marker.lng))
                        .title(marker.title)
                        .snippet("${marker.markerType}: ${marker.description}")
                )
            }
            selectedPoi?.let { destination ->
                mapLibre.addMarker(
                    MarkerOptions().position(LatLng(destination.latitude, destination.longitude))
                        .title(destination.name)
                        .snippet("Selected ${destination.category.label}")
                )
            }
            if (currentLatitude != null && currentLongitude != null) {
                mapLibre.addMarker(
                    MarkerOptions()
                        .position(LatLng(currentLatitude, currentLongitude))
                        .icon(deviceLocationIcon)
                        .title("YOU • This device")
                        .snippet(
                            when {
                                locationIsStale -> "Last known GPS location"
                                liveLocationActive -> "Live GPS location"
                                else -> "Current GPS location"
                            }
                        )
                )
                if (!hasCenteredOnDevice) {
                    mapLibre.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(currentLatitude, currentLongitude),
                            16.0
                        )
                    )
                    hasCenteredOnDevice = true
                }
            }
        }
    }

    LaunchedEffect(map, currentLatitude, currentLongitude, showRoute, selectedPoi) {
        val mapLibre = map ?: return@LaunchedEffect
        val source = mapLibre.style?.getSourceAs<GeoJsonSource>("resq-route") ?: return@LaunchedEffect
        val destination = selectedPoi
        if (showRoute && currentLatitude != null && currentLongitude != null && destination != null) {
            source.setGeoJson(
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        listOf(
                            Point.fromLngLat(currentLongitude, currentLatitude),
                            Point.fromLngLat(destination.longitude, destination.latitude)
                        )
                    )
                )
            )
            val bounds = LatLngBounds.Builder()
                .include(LatLng(currentLatitude, currentLongitude))
                .include(LatLng(destination.latitude, destination.longitude))
                .build()
            mapLibre.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        } else {
            source.setGeoJson("""{"type":"FeatureCollection","features":[]}""")
        }
    }

    fun recenter() {
        onGetLocation()
        if (currentLatitude != null && currentLongitude != null) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLatitude, currentLongitude), 15.0))
        }
    }

    fun findNearby(category: RescueServiceCategory) {
        findMenuOpen = false
        showRoute = false
        val latitude = currentLatitude
        val longitude = currentLongitude
        val mapLibre = map
        if (latitude == null || longitude == null || mapLibre == null) {
            searchMessage = "Get your current location first, then retry."
            onGetLocation()
            return
        }
        searchMessage = "Searching nearby ${category.label.lowercase()} locations…"
        mapLibre.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.0))
        mapView.postDelayed({
            val features = mapLibre.queryRenderedFeatures(
                RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat()),
                category.layerId
            )
            val results = features.mapNotNull { it.toService(latitude, longitude) }
                .filter { it.category == category }
                .distinctBy { "${it.name}-${"%.5f".format(it.latitude)}-${"%.5f".format(it.longitude)}" }
                .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
                .take(5)
            nearbyResults = results
            selectedPoi = results.firstOrNull()
            searchMessage = if (results.isEmpty()) {
                "No ${category.label.lowercase()} is visible nearby. Pan or zoom slightly and retry."
            } else {
                "${results.size} nearby result${if (results.size == 1) "" else "s"} found"
            }
        }, 1_200L)
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        RescueLegend(
            hasLocation = currentLatitude != null && currentLongitude != null,
            liveLocationActive = liveLocationActive,
            locationIsStale = locationIsStale,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            FilledTonalButton(onClick = { findMenuOpen = !findMenuOpen }) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(5.dp))
                Text("Find Nearby")
            }
            DropdownMenu(expanded = findMenuOpen, onDismissRequest = { findMenuOpen = false }) {
                RescueServiceCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.label) },
                        onClick = { findNearby(category) },
                        leadingIcon = { Icon(category.icon(), null, tint = category.color()) }
                    )
                }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            searchMessage?.let {
                Surface(shape = MaterialTheme.shapes.small, tonalElevation = 3.dp) {
                    Text(it, Modifier.padding(8.dp), fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
            }
            selectedPoi?.let { poi ->
                SelectedServiceCard(
                    poi = poi,
                    results = nearbyResults,
                    routeVisible = showRoute,
                    onSelect = { selectedPoi = it; showRoute = false },
                    onRoute = {
                        if (currentLatitude == null || currentLongitude == null) {
                            searchMessage = "Current GPS location is required for a path."
                            onGetLocation()
                        } else {
                            showRoute = true
                        }
                    }
                )
                Spacer(Modifier.height(7.dp))
            }
            SmallFloatingActionButton(onClick = ::recenter) {
                Icon(Icons.Default.MyLocation, "My Location")
            }
        }
    }
}

@Composable
private fun SelectedServiceCard(
    poi: NearbyRescueService,
    results: List<NearbyRescueService>,
    routeVisible: Boolean,
    onSelect: (NearbyRescueService) -> Unit,
    onRoute: () -> Unit
) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 5.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(poi.name, fontWeight = FontWeight.Bold)
            Text(poi.category.label, color = poi.category.color())
            Text(
                "${formatDistance(poi.distanceMeters)} • ${"%.5f".format(poi.latitude)}, ${"%.5f".format(poi.longitude)}",
                fontSize = 11.sp
            )
            if (results.size > 1) {
                results.take(3).forEach { result ->
                    TextButton(onClick = { onSelect(result) }, contentPadding = PaddingValues(0.dp)) {
                        Text("${result.name} — ${formatDistance(result.distanceMeters)}", fontSize = 11.sp)
                    }
                }
            }
            Button(onClick = onRoute, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Route, null)
                Spacer(Modifier.width(6.dp))
                Text(if (routeVisible) "Offline path shown" else "Show offline path")
            }
            if (routeVisible) {
                Text(
                    "Directional straight-line guide only. PMTiles does not contain a routing graph or turn-by-turn instructions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RescueLegend(
    hasLocation: Boolean,
    liveLocationActive: Boolean,
    locationIsStale: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text("Rescue services", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                when {
                    !hasLocation -> "GPS unavailable • retry"
                    locationIsStale -> "GPS: last known"
                    liveLocationActive -> "GPS: live"
                    else -> "GPS: tap My Location"
                },
                fontSize = 10.sp
            )
            LegendItem("You / this device", ComposeColor(0xFF006CFF))
            LegendItem("Medical", ComposeColor(0xFFD92D3A))
            LegendItem("Fire", ComposeColor(0xFFF47B20))
            LegendItem("Police", ComposeColor(0xFF1976D2))
            LegendItem("Supplies", ComposeColor(0xFF168A55))
            LegendItem("Shelter", ComposeColor(0xFF7B4BB7))
        }
    }
}

@Suppress("DEPRECATION")
private fun createDeviceLocationIcon(context: Context): Icon {
    val density = context.resources.displayMetrics.density
    val width = (62f * density).roundToInt().coerceAtLeast(62)
    val height = (74f * density).roundToInt().coerceAtLeast(74)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val centerX = width / 2f
    val circleY = height * 0.39f
    val outerRadius = width * 0.40f
    val innerRadius = width * 0.33f

    paint.color = Color.argb(80, 0, 0, 0)
    canvas.drawCircle(centerX + density * 1.5f, circleY + density * 2f, outerRadius, paint)

    val pointer = Path().apply {
        moveTo(centerX - width * 0.14f, circleY + innerRadius * 0.62f)
        lineTo(centerX, height * 0.96f)
        lineTo(centerX + width * 0.14f, circleY + innerRadius * 0.62f)
        close()
    }
    paint.color = Color.rgb(0, 108, 255)
    canvas.drawPath(pointer, paint)

    paint.color = Color.WHITE
    canvas.drawCircle(centerX, circleY, outerRadius, paint)
    paint.color = Color.rgb(0, 108, 255)
    canvas.drawCircle(centerX, circleY, innerRadius, paint)

    paint.color = Color.WHITE
    canvas.drawCircle(centerX, circleY - innerRadius * 0.28f, innerRadius * 0.22f, paint)
    canvas.drawRoundRect(
        centerX - innerRadius * 0.30f,
        circleY + innerRadius * 0.02f,
        centerX + innerRadius * 0.30f,
        circleY + innerRadius * 0.46f,
        innerRadius * 0.12f,
        innerRadius * 0.12f,
        paint
    )

    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

@Composable
private fun LegendItem(label: String, color: ComposeColor) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.size(9.dp)) {}
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp)
    }
}

private fun org.maplibre.geojson.Feature.toService(userLat: Double?, userLng: Double?): NearbyRescueService? {
    val point = geometry() as? Point ?: return null
    val kind = runCatching { if (hasProperty("kind")) getStringProperty("kind") else null }.getOrNull()
        ?: return null
    val category = RescueServiceCategory.fromKind(kind) ?: return null
    val name = runCatching {
        if (hasProperty("name")) getStringProperty("name").takeIf { it.isNotBlank() } else null
    }.getOrNull()
    val latitude = point.latitude()
    val longitude = point.longitude()
    return NearbyRescueService(
        name = name ?: kind.replace('_', ' ').replaceFirstChar { it.uppercase() },
        kind = kind,
        category = category,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = if (userLat != null && userLng != null) {
            RescueDistance.meters(userLat, userLng, latitude, longitude)
        } else null
    )
}

private fun RescueServiceCategory.color(): ComposeColor = when (this) {
    RescueServiceCategory.MEDICAL -> ComposeColor(0xFFD92D3A)
    RescueServiceCategory.FIRE -> ComposeColor(0xFFF47B20)
    RescueServiceCategory.POLICE -> ComposeColor(0xFF1976D2)
    RescueServiceCategory.PHARMACY -> ComposeColor(0xFF168A55)
    RescueServiceCategory.SUPPLIES -> ComposeColor(0xFF168A55)
    RescueServiceCategory.SHELTER -> ComposeColor(0xFF7B4BB7)
}

private fun RescueServiceCategory.icon() = when (this) {
    RescueServiceCategory.MEDICAL -> Icons.Default.LocalHospital
    RescueServiceCategory.FIRE -> Icons.Default.LocalFireDepartment
    RescueServiceCategory.POLICE -> Icons.Default.LocalPolice
    RescueServiceCategory.PHARMACY -> Icons.Default.LocalPharmacy
    RescueServiceCategory.SUPPLIES -> Icons.Default.WaterDrop
    RescueServiceCategory.SHELTER -> Icons.Default.HomeWork
}

private fun formatDistance(meters: Double?): String = when {
    meters == null -> "Distance unavailable"
    meters < 1_000 -> "${meters.toInt()} m away"
    else -> "%.1f km away".format(meters / 1_000.0)
}

private fun karnatakaStyle(file: File): String {
    val source = JSONObject.quote("pmtiles://file://${file.absolutePath}")
    return """
        {
          "version": 8,
          "name": "ResQ Karnataka Offline",
          "sources": {
            "karnataka": {"type": "vector", "url": $source, "attribution": "© OpenStreetMap contributors • Protomaps"},
            "resq-route": {"type": "geojson", "data": {"type":"FeatureCollection", "features":[]}}
          },
          "layers": [
            {"id":"background","type":"background","paint":{"background-color":"#eef3e8"}},
            {"id":"earth","type":"fill","source":"karnataka","source-layer":"earth","paint":{"fill-color":"#eef3e8"}},
            {"id":"landuse","type":"fill","source":"karnataka","source-layer":"landuse","paint":{"fill-color":"#dcebd2","fill-opacity":0.70}},
            {"id":"water","type":"fill","source":"karnataka","source-layer":"water","paint":{"fill-color":"#91cfe8"}},
            {"id":"buildings","type":"fill","source":"karnataka","source-layer":"buildings","minzoom":12,"paint":{"fill-color":"#d5cec4","fill-outline-color":"#beb5a8"}},
            {"id":"roads-case","type":"line","source":"karnataka","source-layer":"roads","paint":{"line-color":"#c3b9aa","line-width":["interpolate",["linear"],["zoom"],6,0.6,14,5.5]}},
            {"id":"roads","type":"line","source":"karnataka","source-layer":"roads","paint":{"line-color":"#ffffff","line-width":["interpolate",["linear"],["zoom"],6,0.3,14,3.5]}},
            {"id":"boundaries","type":"line","source":"karnataka","source-layer":"boundaries","paint":{"line-color":"#78848c","line-width":1.0,"line-dasharray":[3,2]}},
            {"id":"resq-route-line","type":"line","source":"resq-route","paint":{"line-color":"#e53935","line-width":5,"line-dasharray":[2,1]}},
            {"id":"poi-support","type":"circle","source":"karnataka","source-layer":"pois","minzoom":11,"filter":["in","kind","shelter","social_facility","community_centre","ranger_station","emergency_phone","ambulance_station","helipad"],"paint":{"circle-color":"#7b4bb7","circle-radius":["interpolate",["linear"],["zoom"],11,4,16,9],"circle-stroke-color":"#ffffff","circle-stroke-width":2,"circle-opacity":0.95}},
            {"id":"poi-supplies","type":"circle","source":"karnataka","source-layer":"pois","minzoom":11,"filter":["in","kind","pharmacy","drinking_water","water_point","fuel"],"paint":{"circle-color":"#168a55","circle-radius":["interpolate",["linear"],["zoom"],11,4,16,9],"circle-stroke-color":"#ffffff","circle-stroke-width":2,"circle-opacity":0.95}},
            {"id":"poi-police","type":"circle","source":"karnataka","source-layer":"pois","minzoom":11,"filter":["==","kind","police"],"paint":{"circle-color":"#1976d2","circle-radius":["interpolate",["linear"],["zoom"],11,5,16,10],"circle-stroke-color":"#ffffff","circle-stroke-width":2,"circle-opacity":0.95}},
            {"id":"poi-fire","type":"circle","source":"karnataka","source-layer":"pois","minzoom":11,"filter":["==","kind","fire_station"],"paint":{"circle-color":"#f47b20","circle-radius":["interpolate",["linear"],["zoom"],11,5,16,10],"circle-stroke-color":"#ffffff","circle-stroke-width":2,"circle-opacity":0.95}},
            {"id":"poi-medical","type":"circle","source":"karnataka","source-layer":"pois","minzoom":10,"filter":["in","kind","hospital","clinic","doctors","dentist"],"paint":{"circle-color":"#d92d3a","circle-radius":["interpolate",["linear"],["zoom"],10,5,16,11],"circle-stroke-color":"#ffffff","circle-stroke-width":2,"circle-opacity":0.95}}
          ]
        }
    """.trimIndent()
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "${bytes / 1024} KB"
}
