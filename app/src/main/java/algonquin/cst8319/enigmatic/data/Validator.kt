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

package algonquin.cst8319.enigmatic.data
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Validates the extracted label data before presenting it to the user.
 *
 * This class:
 * - Checks for missing or invalid fields in the LabelJSON object
 * - Ensures critical fields meet minimum length requirements
 * - Provides error feedback including audio alerts when validation fails
 * - Formats the data as JSON when validation succeeds
 *
 * Validation helps ensure data quality and completeness before the information
 * is presented to the user or potentially sent to other systems.
 * @author Team ENIGMatic
 */
class Validator {

    /**
     * Flag indicating whether any field validation has failed.
     */
    private var hasInvalidField: Boolean = false

    /**
     * Validates the fields of a [LabelJSON] object.
     *
     * Checks for the presence and minimum length of:
     * - toAddress (min 9 characters)
     * - fromAddress (min 11 characters)
     * - barCode (min 7 characters)
     *
     * If all fields are valid, returns the label as a JSON string.
     * If not, returns a formatted error message and plays an error tone.
     *
     * @param label The [LabelJSON] object to validate.
     * @return A JSON string if valid, or an error message if invalid.
     */
    fun validateAndConvert(label: LabelJSON): String {
        val missingFields = mutableListOf<String>()
        val minToAddressLength = 9
        val minBarcodeLength = 7
        val minFromAddressLength = 11

        // Check for null/blank or too short values
        if (label.getToAddress().isNullOrBlank() || label.getToAddress().length < minToAddressLength ) {
            missingFields.add("toAddress")

            hasInvalidField = true
        }

        if (label.getFromAddress().isNullOrBlank() || label.getFromAddress().length < minFromAddressLength ) {
            missingFields.add("fromAddress")
            hasInvalidField = true
        }

        if (label.getBarCode().isNullOrBlank() || label.getBarCode().length < minBarcodeLength) {
            missingFields.add("barCode")
            hasInvalidField = true
        }

        return if (missingFields.isEmpty()) {
            label.toJson()
        } else {
            playErrorBeep()
            "MISSING_FIELDS:" + missingFields.joinToString(", ")
        }
    }

    /**
     * Plays an error beep tone using the device’s alarm stream.
     * Called when invalid or missing fields are detected.
     */
    private fun playErrorBeep() {
         val toneGen1 = ToneGenerator(AudioManager.STREAM_ALARM, 100)
         toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 1000)
         toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 1000)
     }

}