/**
 *  Copyright 2025 ENIGMatic
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the “Software”),
 *  to deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 *  sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 *  PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 *  CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 *  OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.data.MainActivityViewModel
import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
import algonquin.cst8319.enigmatic.processing.ImageAnalyzer
import algonquin.cst8319.enigmatic.processing.LabelDetectedCallback
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Main entry point for the LabelXtract application.
 *
 * This activity is responsible for:
 * - Setting up the camera preview using CameraX
 * - Managing user permissions for camera access
 * - Coordinating the label detection and scanning process
 * - Displaying scanning results to the user via a bottom sheet
 * - Providing clipboard functionality for the extracted JSON data
 *
 * The class implements LabelDetectedCallback to receive notifications
 * when a shipping label is detected in the camera feed.
 *
 * @author Team ENIGMatic
 */
@ExperimentalGetImage class MainActivity : AppCompatActivity(), LabelDetectedCallback {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var binding : ActivityMainBinding
    private lateinit var imageAnalyzer: ImageAnalyzer

    private lateinit var textView: TextView
    private lateinit var bottomSheetHeader: TextView
    private lateinit var closeEfab: ExtendedFloatingActionButton
    private lateinit var copyEfab: ExtendedFloatingActionButton
    private lateinit var progressBar: ProgressBar

    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var clipboardManager: ClipboardManager

    private val viewModel: MainActivityViewModel by viewModels<MainActivityViewModel>()

    // Status flags
    private var isScanningInProgress = false

    // Document Scanner options
    private val documentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1) // or 2 if multiple scans per session are required
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG // or PDF if preferred
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    // Client that launches the document scanner flow
    private val docScannerClient = GmsDocumentScanning.getClient(documentScannerOptions)

    // Define scannerLauncher:
    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            // Retrieve the scanning result
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)

            // For JPEG pages
            if (scanResult != null) {
                scanResult.pages?.let { pages ->
                    for ((index, page) in pages.withIndex()) {
                        val imageUri = page.imageUri
                        Log.d("DocScanner", "JPEG page $index => $imageUri")

                        // Send scanner image for analysis and display results
                        val scannedImage = InputImage.fromFilePath(this,imageUri)
                        imageAnalyzer.analyzeDocScannerImage(scannedImage) {
                            displayResults(imageUri)
                        }
                    }
                }
            }
        } else if (activityResult.resultCode == RESULT_CANCELED) {
            Log.d("DocScanner", "Scanning was cancelled by the user")
            resumeCameraX()
        }
    }

    /**
     * Initializes the activity, sets up the UI, and prepares the camera.
     *
     * This method:
     * 1. Inflates the layout
     * 2. Sets up the bottom sheet behavior and UI elements
     * 3. Configures click listeners for buttons
     * 4. Sets up LiveData observers for the ViewModel
     * 5. Initializes the camera executor and checks permissions
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *        shut down, this contains the data it most recently supplied in onSaveInstanceState.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the BottomSheet view from layout
        bottomSheet = findViewById(R.id.bottom_sheet_layout)
        textView = findViewById(R.id.textView)
        bottomSheetHeader = findViewById(R.id.bottom_sheet_header)
        closeEfab = findViewById(R.id.close_efab)
        copyEfab = findViewById(R.id.copy_efab)
        progressBar = findViewById(R.id.progressBar)

        // Set up BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isDraggable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        textView.movementMethod = ScrollingMovementMethod()
        bottomSheetHeader.text = getString(R.string.scanning)

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create the observers which update the UI.
        val textObserver = Observer<String> { newText ->
            textView.text = newText
        }

        val headerObserver = Observer<String> { newText ->
            bottomSheetHeader.text = newText
        }

        val previewViewVisibilityObserver = Observer<Int> { visibility ->
            binding.previewView.visibility = visibility
        }

        val resultContainerVisibilityObserver = Observer<Int> { visibility ->
            binding.resultContainer.visibility = visibility
        }

        val imageViewVisibilityObserver = Observer<Int> { visibility ->
            binding.imageView.visibility = visibility
        }

        val progressBarVisibilityObserver = Observer<Int> { visibility ->
            progressBar.visibility = visibility
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        viewModel.currentText.observe(this, textObserver)
        viewModel.headerText.observe(this, headerObserver)
        viewModel.previewViewVisibility.observe(this, previewViewVisibilityObserver)
        viewModel.resultContainerVisibility.observe(this, resultContainerVisibilityObserver)
        viewModel.imageViewVisibility.observe(this, imageViewVisibilityObserver)
        viewModel.progressBarVisibility.observe(this, progressBarVisibilityObserver)
        viewModel.scannedImage.observe(this) { uri ->
            uri?.let {
                binding.imageView.setImageURI(it)
            }
        }

        // FloatingActionButton for "Close"
        closeEfab.setOnClickListener {
            viewModel.previewViewVisibility.value = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            viewModel.progressBarVisibility.value = View.VISIBLE

            // Update viewModel
            viewModel.headerText.value = getString(R.string.scanning)
            // Clearing the textView causes the bottom sheet to be hidden on "close".
            // Not necessary while hidden anyways.
            // viewModel.currentText.value = getString(R.string.empty_string)

            viewModel.resultContainerVisibility.value = View.GONE
            viewModel.imageViewVisibility.value = View.GONE

            startCamera()
        }

        // FloatingActionButton for "Copy"
        copyEfab.setOnClickListener {
            val copyText = viewModel.currentText.value
            val clipData = ClipData.newPlainText("text", copyText)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkCameraPermission()
        // start the camera
        startCamera()
    }

    /**
     * Cleans up resources when the activity is destroyed.
     * Specifically, shuts down the camera executor to release resources.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Checks if the app has camera permission and requests it if needed.
     * Camera permission is essential for the app's core functionality.
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                1001
            )
        }
    }

    /**
     * Initializes and starts the camera preview.
     *
     * This method:
     * 1. Gets a ProcessCameraProvider instance
     * 2. Creates a Preview use case for the viewfinder
     * 3. Creates and configures an ImageAnalyzer for processing frames
     * 4. Binds all use cases to the device's back camera
     * 5. Handles any exceptions that may occur during setup
     */
    private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)

        processCameraProvider.addListener({
            cameraProvider = processCameraProvider.get()

            // Set up the Preview use case
            val preview = Preview.Builder().build().also {
                val previewView = binding.previewView
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                // Bind the camera to the lifecycle
                cameraProvider.unbindAll()

                // instantiate the ImageAnalyzer and bind it to the cameraProvider
                imageAnalyzer = ImageAnalyzer(this)
                val imageAnalysis = imageAnalyzer.createImageAnalysis(cameraExecutor)

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.d("ERROR", e.message.toString())

            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Resumes the camera preview after document scanning is complete.
     * Resets the scanning flag and restarts the camera.
     */
    private fun resumeCameraX() {
        isScanningInProgress = false
        startCamera()
    }

    /**
     * Callback method triggered when a shipping label is detected in the camera feed.
     *
     * This method:
     * 1. Checks if scanning is already in progress to prevent duplicate launches
     * 2. Unbinds the camera provider to pause camera preview
     * 3. Launches the document scanner for high-quality image capture
     */
    override fun onLabelDetected() {
        // trying to prevent duplicate launches ¯\_(ツ)_/¯
        if (isScanningInProgress) {
            Log.d("LabelDetectedCallback", "Doc scanner already launched, skipping...")
            return
        }

        Log.d("LabelDetectedCallback", "A label was detected. Launching doc scanner...")

        isScanningInProgress = true

        // Unbind (optional) or set a pause flag in the analyzer
        cameraProvider.unbindAll()  // or leave the preview bound if you want
        // 2) Start document scanner
        startDocumentScanner()
    }

    /**
     * Launches Google's document scanner interface to capture a high-quality
     * image of the detected shipping label.
     *
     * The scanner provides edge detection, perspective correction, and
     * enhanced image quality for better OCR results.
     */
    private fun startDocumentScanner() {
        docScannerClient.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("DocScanner", "Failed to launch doc scanner: ${e.message}", e)
            }
    }

    /**
     * Displays the results of a successful label scan.
     *
     * This method:
     * 1. Updates the UI to show the scanned image
     * 2. Expands the bottom sheet to display the JSON results
     * 3. Updates the ViewModel with the new data
     * 4. Passes the processed label data to outputProcessedLabelData
     *
     * @param image URI of the captured label image
     */
    private fun displayResults(image: Uri) {
        isScanningInProgress = false

        // Update UI to show the scanned image and extracted fields
        viewModel.previewViewVisibility.value = View.GONE
        viewModel.resultContainerVisibility.value = View.VISIBLE
        viewModel.imageViewVisibility.value = View.VISIBLE
        viewModel.setScannedImage(image)

        textView.text = getString(R.string.empty_string)
        binding.imageView.setImageURI(image)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        viewModel.progressBarVisibility.value = View.GONE

        val finalValidatedOutput = imageAnalyzer.getFinalValidatedOutput()

        if(finalValidatedOutput.contains("MISSING_FIELDS:")) {

            val missingFields = finalValidatedOutput.removePrefix("MISSING_FIELDS:")
            val errorMessage = getString(R.string.error_missing_fields, missingFields)
            outputProcessedLabelData(errorMessage)

        } else {
            outputProcessedLabelData(finalValidatedOutput)
        }
    }

    /**
     * Updates the UI with the processed and validated label data.
     *
     * This method:
     * 1. Updates the bottom sheet header text
     * 2. Displays the processed label data in the text view
     * 3. Expands the bottom sheet to show results
     * 4. Updates the ViewModel with the current UI state
     *
     * @param processedLabelData The validated JSON string to display
     */
    private fun outputProcessedLabelData(processedLabelData: String) {
        runOnUiThread {
            bottomSheetHeader.text = getString(R.string.label_information)
            textView.text = getString(R.string.empty_string)
            textView.text = processedLabelData
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            // Update viewModel
            viewModel.headerText.value = bottomSheetHeader.text.toString()
            viewModel.currentText.value = textView.text.toString()
        }
    }

}
