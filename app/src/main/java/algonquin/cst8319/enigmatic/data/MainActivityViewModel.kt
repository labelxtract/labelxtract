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

import android.net.Uri
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for managing UI state in MainActivity.
 *
 * This class:
 * - Stores and manages UI-related data that survives configuration changes
 * - Uses LiveData for observable data patterns to update the UI
 * - Controls visibility states of different UI components
 * - Manages the scanned image URI
 *
 * It follows the MVVM (Model-View-ViewModel) pattern to separate UI logic
 * from the UI controller (MainActivity).
 * @author Team ENIGMatic
 */
class MainActivityViewModel : ViewModel() {
    /**
     * LiveData holding the current text to display in the result view.
     */
    val currentText: MutableLiveData<String> = MutableLiveData()

    /**
     * LiveData holding the header text for the bottom sheet.
     */
    val headerText: MutableLiveData<String> = MutableLiveData()

    /**
     * LiveData controlling the visibility of the camera preview.
     * Initially visible (View.VISIBLE).
     */
    val previewViewVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    /**
     * LiveData controlling the visibility of the result container.
     * Initially gone (View.GONE).
     */
    val resultContainerVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    /**
     * LiveData controlling the visibility of the image view.
     * Initially gone (View.GONE).
     */
    val imageViewVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    /**
     * LiveData holding the URI of the scanned image.
     * Initially null.
     */
    val scannedImage: MutableLiveData<Uri?> = MutableLiveData(null)

    /**
     * LiveData controlling the visibility of the progress bar.
     * Initially visible (View.VISIBLE).
     */
    val progressBarVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    /**
     * Updates the scanned image URI and adjusts visibility states accordingly.
     *
     * @param bitmap The URI of the captured label image
     */
    fun setScannedImage(bitmap: Uri) {
        scannedImage.value = bitmap
        imageViewVisibility.value = View.VISIBLE
        resultContainerVisibility.value = View.VISIBLE
    }
}