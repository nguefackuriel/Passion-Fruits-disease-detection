package ai.passio.platformsdksandboxapp

import ai.passio.passiosdk.core.camera.PassioCameraViewProvider
import ai.passio.passiosdk.core.config.PassioMode
import ai.passio.passiosdk.platform.*
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class MainActivity : AppCompatActivity(), PassioCameraViewProvider {

    private val resultView: TextView by lazy { findViewById(R.id.mainResult) }
    private val versionView: TextView by lazy { findViewById(R.id.mainVersion) }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onCameraPermissionGranted()
            } else {
                onCameraPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = PassioConfiguration(
            this.applicationContext,
            ""
        )
        PassioSDK.instance.configure(config) { status ->
            when (status.mode) {
                PassioMode.NOT_READY -> Toast.makeText(this, "SDK NOT READY", Toast.LENGTH_SHORT).show()
                PassioMode.FAILED_TO_CONFIGURE -> Toast.makeText(this, "SDK FAILED TO CONFIGURE", Toast.LENGTH_SHORT).show()
                PassioMode.IS_BEING_CONFIGURED -> Toast.makeText(this, "SDK BEING CONFIGURED", Toast.LENGTH_SHORT).show()
                PassioMode.IS_READY_FOR_DETECTION -> {
                    Toast.makeText(this, "SDK READY", Toast.LENGTH_SHORT).show()
                    val versionString = "SDK: ${PassioSDK.instance.getSDKVersion()}\nModels: ${status.activeModels ?: 0}"
                    versionView.text = versionString
                }
                PassioMode.IS_DOWNLOADING_MODELS -> Toast.makeText(this, "SDK NOT READY", Toast.LENGTH_SHORT).show()
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onCameraPermissionGranted()
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun onCameraPermissionGranted() {
        PassioSDK.instance.startCamera(this)
    }

    override fun requestPreviewView(): PreviewView {
        return findViewById(R.id.mainPreviewView)
    }

    override fun requestCameraLifecycleOwner(): LifecycleOwner = this

    private fun onCameraPermissionDenied() {

    }

    override fun onStart() {
        super.onStart()
        PassioSDK.instance.startDetection(listener)
    }

    override fun onStop() {
        PassioSDK.instance.stopDetection()
        super.onStop()
    }

    private val listener = object : ClassificationListener {
        override fun onClassificationResult(candidate: ClassificationCandidate, bitmap: Bitmap) {
            resultView.text = "${candidate.label.displayName}, ${String.format("%.02f", (candidate.confidence * 100))}%"
        }
    }

    private fun getDownloadedModels(): List<Uri> {
        val list = mutableListOf<Uri>()
        val path = this.cacheDir.absolutePath + File.separator + "download"
        val folder = File(path)

        folder?.listFiles()?.forEach { file ->
            list.add(Uri.parse(file.path))
        }

        return list
    }
}