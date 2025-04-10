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

package algonquin.cst8319.enigmatic.processing

import algonquin.cst8319.enigmatic.data.FieldExtractor
import algonquin.cst8319.enigmatic.data.LabelJSON
import algonquin.cst8319.enigmatic.data.Validator
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService

/**
 * Processes camera frames to detect and analyze shipping labels.
 *
 * This class implements ImageAnalysis.Analyzer to receive camera frames
 * and uses Google ML Kit to:
 * - Detect text using OCR (Optical Character Recognition)
 * - Recognize postal codes to identify shipping labels
 * - Scan for barcodes
 * - Extract structured data from the detected text
 *
 * When a shipping label is detected (identified by a postal code),
 * the class notifies the activity via the LabelDetectedCallback.
 *
 * @param labelDetectedCallback Interface to notify when a label is detected
 * @author Team ENIGMatic
 */
@ExperimentalGetImage class ImageAnalyzer(
    /**
     * Callback interface to notify MainActivity when a label is detected.
     */
    private val labelDetectedCallback: LabelDetectedCallback,
) : ImageAnalysis.Analyzer {

    /**
     * ML Kit's TextRecognizer instance, used for detecting and extracting text from images.
     * Configured with default options.
     */
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Instance of FieldExtractor that processes recognized text blocks to extract
     * structured data. Initialized in the processLabelText method.
     */
    private lateinit var fieldExtractor: FieldExtractor

    /**
     * Status flag indicating whether text recognition processing is complete.
     * Used to coordinate with other processing steps.
     */
    private var isTextProcessingComplete = false

    /**
     * Status flag indicating whether barcode processing is complete.
     * Used to coordinate with other processing steps.
     */
    private var isBarcodeProcessingComplete = false

    /**
     * Status flag indicating whether barcode processing is currently in progress.
     * Prevents multiple simultaneous barcode processing operations.
     */
    private var isBarcodeProcessing = false

    /**
     * Status flag indicating whether the analyzer is paused.
     * When true, camera frames will be ignored.
     */
    private var isPaused = false

    /**
     * Stores the value of the barcode detected in the shipping label.
     */
    private var barcodeValue = ""

    /**
     * Stores the list of extracted field strings from the OCR process.
     */
    private var extractedFields = mutableListOf<String>()

    /**
     * Structured data model that holds all extracted shipping label information.
     */
    private lateinit var labelJSON: LabelJSON

    /**
     * The final validated JSON output string ready for display to the user.
     */
    private lateinit var finalValidatedOutput: String

    /**
     * Configuration options for the barcode scanner.
     * Set to recognize all barcode formats for maximum compatibility.
     */
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    /**
     * ML Kit's BarcodeScanner instance, used for detecting and reading barcodes in images.
     */
    private val barcodeScanner = BarcodeScanning.getClient(options)

    /**
     * Gets the final validated output as a String.
     * @return The validated and processed label data in JSON format.
     */
    fun getFinalValidatedOutput() : String {return finalValidatedOutput}

    /**
     * Creates an ImageAnalysis use case with the desired settings and analyzer.
     * @param cameraExecutor The executor used to process image frames in the background.
     * @return The configured ImageAnalysis use case.
     */
    fun createImageAnalysis(cameraExecutor: ExecutorService): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Keeps only the latest frame
            .build().apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    analyze(imageProxy) // Send the frame to ML Kit
                }
            }
        return imageAnalyzer
    }

    /**
     * This method is called for every frame that the ImageAnalysis use case processes.
     * Converts the ImageProxy to an InputImage and performs text recognition on it.
     * @param imageProxy The camera frame to analyze.
     */
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detectLabel(image)
        }
        imageProxy.close()
    }

    /**
     * Processes a document scanner image to extract label information.
     *
     * This method coordinates the full processing pipeline:
     * 1. Text recognition using OCR
     * 2. Barcode scanning
     * 3. JSON creation from extracted fields
     * 4. Validation of the final output
     *
     * It uses nested callbacks to ensure each step completes before
     * the next begins, and finally calls onComplete when all processing is done.
     *
     * @param image The InputImage from the document scanner
     * @param onComplete Callback to invoke when processing is complete
     */
    fun analyzeDocScannerImage(image: InputImage, onComplete: () -> Unit) {
        processLabelText(image) {
            // Once text recognition is done, process barcode
            processBarcode(image) {
                // Barcode is done — now create label JSON
                createLabelJSON {
                    // JSON created — now call onComplete
                    validateJSON {
                        onComplete()
                    }
                }
            }
        }
    }

    /**
     * Detects a shipping label from the given InputImage using ML Kit's TextRecognizer.
     * If a postal code is found within the recognized text, the scanning process is paused,
     * and a callback is triggered to notify the Activity.
     * @param image The InputImage to be processed for label detection.
     */
    private fun detectLabel(image: InputImage) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                isTextProcessingComplete = false

                val isLabelDetected = detectPostalCode(visionText)
                if (isLabelDetected) {
                    // Pause further analysis
                    isPaused = true
                    // Notify the Activity
                    labelDetectedCallback.onLabelDetected()
                }
                Log.i("Label", "Shipping Label detected")
            }
            .addOnFailureListener { e ->
                //Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true
            }
    }

    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     * @param onComplete Callback invoked when text recognition is complete, providing
      * a list of extracted field strings.
     */
    private fun processLabelText(image: InputImage, onComplete: (List<String>) -> Unit) {
        isTextProcessingComplete = false

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("OCR", "Full detected text: ${visionText.text}")
                // clear list of extracted fields from previous image
                extractedFields.clear()
                // use FieldExtractor to get all fields
                fieldExtractor = FieldExtractor(visionText.textBlocks)
                extractedFields = fieldExtractor.extractAllFields()
                Log.d("OCR", extractedFields.toString())

                onComplete(extractedFields)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
                // avoids null handling. safer than returning null.
                onComplete(emptyList())
            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true
            }
    }


    /**
     * Processes barcode scanning on the given [InputImage].
     *
     * If a barcode scan is already in progress, this function will skip processing
     * the current frame to avoid overlapping scans, and immediately invoke [onComplete]
     * to ensure that the calling flow can continue without hanging.
     *
     * The provided [onComplete] callback will be called after barcode scanning completes
     * successfully, fails, or is skipped.
     *
     * @param image The [InputImage] to scan for barcodes.
     * @param onComplete Callback invoked when barcode processing is finished or skipped.
     */
    private fun processBarcode(image: InputImage, onComplete: () -> Unit) {
        if (isBarcodeProcessing) {
            Log.d("Barcode", "Barcode processing already in progress; skipping this frame.")
            onComplete() // call it to prevent hanging :)
            return
        }

        isBarcodeProcessing = true
        isBarcodeProcessingComplete = false

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodeValue = ""
                if (barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {
                        barcodeValue = barcode.displayValue ?: ""
                        Log.d("Barcode", "Detected barcode: $barcodeValue")
                    }
                } else {
                   // Log.d("Barcode", "No barcode detected in this frame.")
                }
            }
            .addOnFailureListener { e ->
               // Log.e("Barcode", "Barcode scanning failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Reset flag so next frame can trigger barcode scanning
                isBarcodeProcessing = false
                // Mark barcode processing as complete
                isBarcodeProcessingComplete = true
                onComplete()
            }
    }

    /**
     * Checks if the provided visionText contains a Canadian postal code pattern.
     * This is used to determine if a shipping label is present in the current image.
     *
     * @param visionText The text recognized by ML Kit's TextRecognizer
     * @return True if a postal code pattern is found, false otherwise
     */
    private fun detectPostalCode(visionText: Text): Boolean {
        val postalCodeRegex = Regex("""[a-zA-Z][O0-9][a-zA-Z][\\ \\-]{0,1}[O0-9][a-zA-Z][O0-9]""")
        // Iterate over all text blocks, lines, or elements
        for (block in visionText.textBlocks) {
            val blockText = block.text
            // Check if this block contains a valid Canadian postal code
            if (postalCodeRegex.containsMatchIn(blockText)) {
                return true // Found at least one match
            }
        }
        return false // No match found
    }

    /**
     * Creates a LabelJSON object from the extracted fields.
     * This consolidates all the data extracted by the FieldExtractor and any barcode data.
     *
     * @param onComplete Callback invoked when JSON creation is complete
     */
    private fun createLabelJSON(onComplete: () -> Unit) {
        labelJSON = LabelJSON(
            fieldExtractor.getProductType(),
            fieldExtractor.getToAddress(),
            fieldExtractor.getDestPostalCode(),
            fieldExtractor.getTrackPin(),
            barcodeValue,
            fieldExtractor.getFromAddress(),
            fieldExtractor.getProductDimension(),
            fieldExtractor.getProductWeight(),
            fieldExtractor.getProductInstruction(),
            fieldExtractor.getReference())

        onComplete()
    }

    /**
     * Validates the created LabelJSON object to ensure all required fields are present
     * and properly formatted. Stores the final validated output for later retrieval.
     *
     * @param onComplete Callback invoked when validation is complete
     */
    private fun validateJSON(onComplete: () -> Unit) {
        val validate = Validator()
        finalValidatedOutput = validate.validateAndConvert(labelJSON)

        onComplete()
    }

}