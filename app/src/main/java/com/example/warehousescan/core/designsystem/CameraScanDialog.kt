package com.example.warehousescan.core.designsystem

import android.Manifest
import android.view.MotionEvent
import android.util.Size
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.warehousescan.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanner
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private class CameraResources {
    @Volatile var provider: ProcessCameraProvider? = null
    @Volatile var scanner: BarcodeScanner? = null
    @Volatile var camera: Camera? = null
    @Volatile var disposed: Boolean = false
}

@Composable
fun CameraScanDialog(
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String, String) -> Unit
) {
    val context = LocalContext.current
    val cameraResources = remember { CameraResources() }
    var continuous by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionDenied = true
            Timber.w("Camera permission denied by user")
        }
    }

    DisposableEffect(cameraResources) {
        onDispose {
            cameraResources.disposed = true
            cameraResources.provider?.unbindAll()
            cameraResources.scanner?.close()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    CameraPreviewView(
                        resources = cameraResources,
                        onBarcodeScanned = { barcode, symbology ->
                            onBarcodeScanned(barcode, symbology)
                            if (continuous) Toast.makeText(context, "Captured $barcode", Toast.LENGTH_SHORT).show()
                            else onDismissRequest()
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.camera_permission_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.camera_permission_explanation),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            if (permissionDenied) {
                                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(settingsIntent)
                            } else permissionLauncher.launch(Manifest.permission.CAMERA)
                        }, modifier = Modifier.height(56.dp)) {
                            Text(stringResource(if (permissionDenied) R.string.open_app_settings else R.string.grant_camera_permission))
                        }
                    }
                }

                // Overlay Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Barcode / QR Code",
                        color = Color.White,
                        fontSize = 22.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasCameraPermission) {
                            IconButton(onClick = {
                                torchOn = !torchOn
                                cameraResources.camera?.cameraControl?.enableTorch(torchOn)
                            }, modifier = Modifier.size(56.dp)) {
                                Icon(Icons.Default.FlashOn, contentDescription = stringResource(R.string.toggle_torch), tint = Color.White)
                            }
                        }
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(56.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.close_scanner), tint = Color.White)
                        }
                    }
                }

                if (hasCameraPermission) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { continuous = !continuous }, modifier = Modifier.height(56.dp)) {
                            Text(stringResource(if (continuous) R.string.continuous_scan_on else R.string.continuous_scan_off), color = Color.White)
                        }
                        Button(onClick = onDismissRequest, modifier = Modifier.height(56.dp)) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }

                if (hasCameraPermission) {
                    Text(
                        text = stringResource(R.string.point_camera_at_code),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 160.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewView(
    resources: CameraResources,
    onBarcodeScanned: (String, String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanLock = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                if (resources.disposed) return@addListener
                resources.provider = cameraProvider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A
                    )
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)
                resources.scanner = barcodeScanner

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !scanLock.get()) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue ?: barcode.displayValue
                                    if (!rawValue.isNullOrBlank() && scanLock.compareAndSet(false, true)) {
                                        Timber.i("ML Kit camera detected barcode: $rawValue")
                                        val symbology = when (barcode.format) {
                                            Barcode.FORMAT_QR_CODE -> "QRCODE"
                                            Barcode.FORMAT_DATA_MATRIX -> "DATAMATRIX"
                                            Barcode.FORMAT_CODE_128 -> "CODE128"
                                            Barcode.FORMAT_CODE_39 -> "CODE39"
                                            Barcode.FORMAT_EAN_13 -> "EAN13"
                                            Barcode.FORMAT_EAN_8 -> "EAN8"
                                            Barcode.FORMAT_UPC_A -> "UPCA"
                                            else -> "UNKNOWN"
                                        }
                                        onBarcodeScanned(rawValue, symbology)
                                        Handler(Looper.getMainLooper()).postDelayed({ scanLock.set(false) }, 500)
                                        break
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Barcode analysis failed")
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    resources.camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                            runCatching {
                                resources.camera?.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(point).build()
                                )
                            }.onFailure { Timber.d(it, "Tap-to-focus could not be applied") }
                        }
                        view.performClick()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Camera binding failed")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

