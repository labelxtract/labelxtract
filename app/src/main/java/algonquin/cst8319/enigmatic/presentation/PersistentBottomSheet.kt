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

package algonquin.cst8319.enigmatic.presentation

import algonquin.cst8319.enigmatic.R
import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Fragment that provides a persistent bottom sheet UI for displaying label scanning results.
 *
 * This fragment creates a bottom sheet dialog that:
 * - Displays the JSON output of the scanned label
 * - Can be expanded or collapsed as needed
 * - Provides a consistent user interface for viewing results
 *
 * The bottom sheet is configured in onCreateDialog and uses the bottom_sheet layout file.
 *
 * @author Team ENIGMatic
 */
class PersistentBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(
        savedInstanceState: Bundle?,
    ): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        bottomSheetDialog.setContentView(R.layout.bottom_sheet)

        // Set behavior attributes here...

        return bottomSheetDialog
    }
}