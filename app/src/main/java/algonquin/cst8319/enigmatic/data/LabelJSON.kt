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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data class representing the structured information extracted from a shipping label.
 *
 * This class:
 * - Stores all fields extracted from a Canada Post shipping label
 * - Provides getters and setters for each field
 * - Includes functionality to serialize the data to a formatted JSON string
 *
 * The class uses Kotlinx Serialization (@Serializable) to enable easy conversion
 * to and from JSON format.
 *
 * @property productType Type of shipping service (Priority, Xpresspost, etc.)
 * @property toAddress Recipient's address
 * @property destPostalCode Destination postal code
 * @property trackPin Tracking number/PIN
 * @property barCode Barcode value from the label
 * @property fromAddress Sender's address
 * @property productDimension Package dimensions (format: LxWxHcm)
 * @property productWeight Package weight with unit
 * @property productInstruction Special handling instructions
 * @property reference Reference number or customer ID
 *
 * @author Team ENIGMatic
 */
@Serializable
data class LabelJSON(
    private var productType: String,
    private var toAddress: String,
    private var destPostalCode: String,
    private var trackPin: String,
    private var barCode: String,
    private var fromAddress: String,
    private var productDimension: String,
    private var productWeight: String,
    private var productInstruction: String,
    private var reference: String
)

{
    /**
     * Gets the product type extracted from the label.
     * @return The product type as a String.
     */
    fun getProductType() = this.productType

    /**
     * Sets the product type for this label.
     * @param productType The product type to set.
     */
    fun setProductType(productType: String) {
        this.productType = productType
    }

    /**
     * Gets the recipient address extracted from the label.
     * @return The recipient address as a String.
     */
    fun getToAddress() = this.toAddress

    /**
     * Sets the recipient address for this label.
     * @param toAddress The recipient address to set.
     */
    fun setToAddress(toAddress: String) {
        this.toAddress = toAddress
    }

    /**
     * Gets the destination postal code extracted from the label.
     * @return The destination postal code as a String.
     */
    fun getDestPostalCode() = this.destPostalCode

    /**
     * Sets the destination postal code for this label.
     * @param destPostalCode The destination postal code to set.
     */
    fun setDestPostalCode(destPostalCode: String) {
        this.destPostalCode = destPostalCode
    }

    /**
     * Gets the track pin extracted from the label.
     * @return The track pin as a String.
     */
    fun getTrackPin() = this.trackPin

    /**
     * Sets the track pin for this label.
     * @param trackPin The track pin to set.
     */
    fun setTrackPin(trackPin: String) {
        this.trackPin = trackPin
    }

    /**
     * Gets the human-readable barcode extracted from the label.
     * @return The human-readable barcode as a String.
     */
    fun getBarCode() = this.barCode

    /**
     * Sets the human-readable barcode for this label.
     * @param barCode The human-readable barcode to set.
     */
    fun setBarCode(barCode: String) {
        this.barCode = barCode
    }

    /**
     * Gets the sender address extracted from the label.
     * @return The sender address as a String.
     */
    fun getFromAddress() = this.fromAddress

    /**
     * Sets the sender address for this label.
     * @param fromAddress The sender address to set.
     */
    fun setFromAddress(fromAddress: String) {
        this.fromAddress = fromAddress
    }

    /**
     * Gets the product dimensions extracted from the label.
     * @return The product dimensions as a String.
     */
    fun getProductDimension() = this.productDimension

    /**
     * Sets the product dimension for this label.
     * @param productDimension The product dimension to set.
     */
    fun setProductDimension(productDimension: String) {
        this.productDimension = productDimension
    }

    /**
     * Gets the product weight extracted from the label.
     * @return The product weight as a String.
     */
    fun getProductWeight() = this.productWeight

    /**
     * Sets the product weight for this label.
     * @param productWeight The product weight to set.
     */
    fun setProductWeight(productWeight: String) {
        this.productWeight = productWeight
    }

    /**
     * Gets the product instructions extracted from the label.
     * @return The product instructions as a String.
     */
    fun getProductInstruction() = this.productInstruction

    /**
     * Sets the product instruction for this label.
     * @param productInstruction The product instruction to set.
     */
    fun setProductInstruction(productInstruction: String) {
        this.productInstruction = productInstruction
    }

    /**
     * Gets the reference extracted from the label.
     * @return The reference as a String.
     */
    fun getReference() = this.reference

    /**
     * Sets the reference for this label.
     * @param reference The reference to set.
     */
    fun setReference(reference: String) {
        this.reference = reference
    }

    /**
     * Converts the object into a properly formatted JSON string.
     * Uses Kotlinx Serialization for accurate JSON representation.
     * @return A formatted JSON string representing all label fields.
     */
    fun toJson(): String {
        return Json { prettyPrint = true }.encodeToString(this)
    }
}

