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

import android.util.Log
import com.google.mlkit.vision.text.Text.TextBlock

/**
 * FieldExtractor class is responsible for extracting specific fields from a list of scanned text blocks,
 * obtained from an OCR process. It identifies and extracts information such as product type,
 * addresses, postal codes, tracking numbers, and other relevant details from a Canada Post
 * shipping label as per standard format/layout in March 2025.
 *
 * @param scannedTextBlocks A list of TextBlock objects representing the scanned text.
 * @constructor Creates a field extractor with a new list of text blocks.
 * @author Team ENIGMatic
 */
class FieldExtractor(
    private var scannedTextBlocks: List<TextBlock>
) {

    // String variables to store the extracted fields, matching JSON field requirements
    private lateinit var productType: String
    private lateinit var toAddress: String
    private lateinit var destPostalCode: String
    private lateinit var trackPin: String
    private lateinit var fromAddress: String
    private lateinit var productDimension: String
    private lateinit var productWeight: String
    private lateinit var productInstruction: String
    private lateinit var reference: String

    // Lists to store sorted & extracted Strings from instance's list of text blocks
    private var cleanScannedText = mutableListOf<MutableList<String>>()
    private var extractedFields = mutableListOf<String>()

    /*
    Keyword lists and Regex useful to find required fields when parsing the scanned label text.
    These can be tweaked as required to improve field retrieval.
    A good resource to construct and validate Regex is https://regex101.com/
     */
    // product types have limited options, so looking for exact match from a list
    private val productTypes = listOf("Priority", "Regular Parcel", "Xpresspost", "Expedited Parcel")
    // 'to address' field always follows the header 'TO: / À:', so looking for that pattern in a forgiving fashion
    private val toAddressHeaderRegex = Regex("TO.*[AÀÅ]", RegexOption.IGNORE_CASE)
    // Canadian postal code pattern
    // note: added the letter 'O' to the matcher for digits, since OCR recognizer often reads a 0 as O
    private val postalCodeRegex = Regex("""[a-zA-Z][O0-9][a-zA-Z][\\ \\-]{0,1}[O0-9][a-zA-Z][O0-9]""")
    // track pin / tracking number pattern - looking for 4 sets of 4 digits, each separated by space
    private val trackPinRegex = Regex("""\d{4}\s\d{4}\s\d{4}\s\d{4}""")
    // 'from address' field always follows the header 'FROM: / DE:', so looking for that pattern in a forgiving fashion
    private val fromAddressHeaderRegex = Regex("FROM.*DE", RegexOption.IGNORE_CASE)
    // product dimensions are shown in pattern 'LxWxHcm'
    // note: regex handles both integer and decimal values, and optional single whitespace before 'cm'
    private val productDimensionRegex = Regex("""\d*(\.\d+)?x\d*(\.\d+)?x\d*(\.\d+)?(\s)?cm""")
    // product weight is expressed in 'KG' unit, so looking for this pattern
    private val productWeightRegex = Regex("KG")
    // product weight value is listed as decimal value
    private val productWeightValueRegex = Regex("""\d+[.]\d+""")
    // product instructions have limited options, so looking for exact match from a list
    private val productInstructions = listOf("SIGNATURE", "18+ SIGNATURE", "19+ SIGNATURE", "21+ SIGNATURE", "CARD FOR PICKUP", "DELIVER TO PO", "LEAVE AT DOOR", "DO NOT SAFE DROP")
    // reference at the bottom of the label follows the key 'Ref./Réf.
    private val referenceRegex = Regex("Ref.*R[eé]f", RegexOption.IGNORE_CASE)

    // Variables holding the index of the found fields
    private var foundProductTypeIndex = -1
    private var foundToAddressHeaderBlockIndex = -1
    private var foundToAddressHeaderLineIndex = -1
    private var foundPostalCodeIndex = -1
    private var foundTrackPinIndex = -1
    private var foundFromAddressHeaderBlockIndex = -1
    private var foundFromAddressHeaderLineIndex = -1
    private var foundProductDimensionIndex = -1
    private var foundProductWeightIndex = -1
    private var foundProductInstructionIndex = -1
    private var foundReferenceIndex = -1


    // All getters for private fields that need to be accessed by other classes
    /**
     * Gets the product type extracted from the scanned text.
     * @return The extracted product type.
     */
    fun getProductType(): String {return productType}
    /**
     * Gets the "to" address extracted from the scanned text.
     * @return The extracted "to" address.
     */
    fun getToAddress(): String {return toAddress}
    /**
     * Gets the destination postal code extracted from the scanned text.
     * @return The extracted destination postal code.
     */
    fun getDestPostalCode(): String {return destPostalCode}
    /**
     * Gets the tracking PIN extracted from the scanned text.
     * @return The extracted tracking PIN.
     */
    fun getTrackPin(): String {return trackPin}
    /**
     * Gets the "from" address extracted from the scanned text.
     * @return The extracted "from" address.
     */
    fun getFromAddress(): String {return fromAddress}
    /**
     * Gets the product dimensions extracted from the scanned text.
     * @return The extracted product dimensions.
     */
    fun getProductDimension(): String {return productDimension}
    /**
     * Gets the product weight extracted from the scanned text.
     * @return The extracted product weight.
     */
    fun getProductWeight(): String {return productWeight}
    /**
     * Gets the product instructions extracted from the scanned text.
     * @return The extracted product instructions.
     */
    fun getProductInstruction(): String {return productInstruction}
    /**
     * Gets the reference information extracted from the scanned text.
     * @return The extracted reference information.
     */
    fun getReference(): String {return reference}

    /**
     * Sole public function for this class, when called it sorts the instance's list of text
     * blocks and then calls each private function relevant to field extraction, finally it
     * returns a list of extracted fields.
     *
     * @return A mutable list of extracted fields as strings.
     */
    fun extractAllFields() : MutableList<String> {
        if (scannedTextBlocks.isNotEmpty()) {
            // sort all text blocks
            cleanScannedText = sortScannedTextBlocks(scannedTextBlocks)

            // parse all text blocks to find each field reference index
            findAllFieldPositions()

            // extract each field using their found reference position
            productType = extractProductType()
            toAddress = extractToAddress()
            destPostalCode = extractDestPostalCode()
            trackPin = extractTrackPin()
            fromAddress = extractFromAddress()
            productDimension = extractProductDimension()
            productWeight = extractProductWeight()
            productInstruction = extractProductInstruction()
            reference = extractReference()

        }

        return extractedFields
    }

    /**
     * Sorts the scanned text blocks based on their vertical position.
     *
     * @param scannedTextBlocks The list of TextBlock objects to sort.
     * @return A mutable list of mutable lists of strings, representing sorted text blocks.
     */
    private fun sortScannedTextBlocks(scannedTextBlocks: List<TextBlock>) : MutableList<MutableList<String>> {
        val sortedScannedTextStrings = mutableListOf<MutableList<String>>()

        // sorting all text blocks by their vertical position, i.e. boundingBox.top value
        val sortedTextBlocks = scannedTextBlocks.sortedWith(compareBy { it.boundingBox?.top })

        // iterating through each block and reordering lines by their vertical position
        for (block in sortedTextBlocks) {
            val boundingBox = block.boundingBox

            val sortedBlock = sortBlockLines(block)

            sortedScannedTextStrings.add(sortedBlock)

            // useful logging for debugging only
            if (boundingBox != null) {
                Log.d(
                    "OCR", "Sorted text block: ${sortedBlock}\n" +
                            "bounding box: Left:${boundingBox.left}, Top:${boundingBox.top}, " +
                            "Right: ${boundingBox.right}, Bottom: ${boundingBox.bottom}"
                )
            }
        }

        return sortedScannedTextStrings
    }

    /**
     * Sorts the lines within a text block based on their vertical position.
     *
     * @param textBlock The TextBlock object containing lines to sort.
     * @return A mutable list of strings representing sorted lines within the block.
     */
    private fun sortBlockLines(textBlock: TextBlock) : MutableList<String> {
        val sortedBlock = mutableListOf<String>()

        // sorting lines
        val sortedTextLines = textBlock.lines.sortedBy { it.boundingBox?.top }

        for (line in sortedTextLines) {
            sortedBlock.add(line.text)
        }

        return sortedBlock
    }

    /**
     * Finds the positions (indices) of various fields within the scanned text blocks.
     * This method parses through the text blocks to locate headers or actual field values
     * using regular expressions and keyword matching.
     */
    private fun findAllFieldPositions() {
        for (block in cleanScannedText) {
            for (line in block) {
                // some fields are expected to be in single-line blocks
                if (block.size == 1) {
                    if (foundPostalCodeIndex < 0 && postalCodeRegex.matches(line)) {
                        foundPostalCodeIndex = cleanScannedText.indexOf(block)
                    }
                    else if (foundTrackPinIndex < 0 && line.contains(trackPinRegex)) {
                        foundTrackPinIndex = cleanScannedText.indexOf(block)
                    }
                    else if (foundProductDimensionIndex < 0 && line.contains(productDimensionRegex)) {
                        foundProductDimensionIndex = cleanScannedText.indexOf(block)
                    }
                }

                if (foundProductTypeIndex < 0) {
                    for (productType in productTypes) {
                        if (line.contains(productType)) {
                            foundProductTypeIndex = cleanScannedText.indexOf(block)
                            break
                        }
                    }
                }

                if (foundToAddressHeaderBlockIndex < 0 && line.contains(toAddressHeaderRegex)) {
                    foundToAddressHeaderBlockIndex = cleanScannedText.indexOf(block)
                    foundToAddressHeaderLineIndex = block.indexOf(line)
                }

                if (foundFromAddressHeaderBlockIndex < 0 && line.contains(fromAddressHeaderRegex)) {
                    foundFromAddressHeaderBlockIndex = cleanScannedText.indexOf(block)
                    foundFromAddressHeaderLineIndex = block.indexOf(line)
                }

                if (foundProductWeightIndex < 0 && line.contains(productWeightRegex)) {
                    foundProductWeightIndex = cleanScannedText.indexOf(block)
                }

                if (foundProductInstructionIndex < 0) {
                    for (instruction in productInstructions) {
                        if (line.equals(instruction, true)) {
                            foundProductInstructionIndex = cleanScannedText.indexOf(block)
                            break
                        }
                    }
                }

                if (foundReferenceIndex < 0 && line.contains(referenceRegex)) {
                    foundReferenceIndex = cleanScannedText.indexOf(block)
                }
            }
        }
    }

    /**
     * Extracts the product type from the scanned text blocks.
     *
     * @return The extracted product type as a String.
     */
    private fun extractProductType() : String {
        var extractedProductType = ""

        if (foundProductTypeIndex >= 0) {
            for (line in cleanScannedText[foundProductTypeIndex]) {
                for (productType in productTypes) {
                    if (line.contains(productType)) {
                        extractedProductType = productType
                        break
                    }
                }
            }
        }

        extractedFields.add("productType: $extractedProductType")
        return extractedProductType
    }

    /**
     * Extracts the "to" address from the scanned text blocks.
     *
     * @return The extracted "to" address as a String.
     */
    private fun extractToAddress() : String {
        var extractedToAddress = ""

        if(foundToAddressHeaderBlockIndex>=0) {
            // making sure we include 'to address' details if embedded in same block as
            // the header, by extracting any remaining lines the header block
            if ((foundToAddressHeaderLineIndex + 1) < cleanScannedText[foundToAddressHeaderBlockIndex].size) {
                for (lineIndex in (foundToAddressHeaderLineIndex + 1)..<cleanScannedText[foundToAddressHeaderBlockIndex].size) {
                    extractedToAddress += "${cleanScannedText[foundToAddressHeaderBlockIndex][lineIndex]}, "
                }
            }

            var nextBlockIndex = foundToAddressHeaderBlockIndex + 1

            // continue until postal code was found so we get complete address
            var blockIsAddressRelated = true
            while (nextBlockIndex < cleanScannedText.size &&
                    !extractedToAddress.contains(postalCodeRegex) &&
                    blockIsAddressRelated) {
                for (line in cleanScannedText[nextBlockIndex]) {
                    // sometimes postal code in address is not recognized properly,
                    // i.e. digit read as character, so loop continues through next blocks
                    // so adding check to make sure the product instruction (ie '18+ SIGNATURE')
                    // which is the next block after 'to address' does not slip into the extracted address
                    for (instruction in productInstructions) {
                        if (line.equals(instruction, true)) {
                            blockIsAddressRelated = false
                            break
                        }
                    }
                    if (blockIsAddressRelated) {
                        extractedToAddress += "${line}, "
                    }
                    else {
                        break
                    }
                }
                nextBlockIndex += 1
            }

        }

        // clean up by removing last comma
        if (extractedToAddress.endsWith(", ")) {
            extractedToAddress = extractedToAddress.substringBeforeLast(",")
        }
        extractedFields.add("toAddress: $extractedToAddress")
        return extractedToAddress
    }

    /**
     * Extracts the destination postal code from the scanned text blocks.
     *
     * @return The extracted destination postal code as a String.
     */
    private fun extractDestPostalCode(): String {
        var extractedDestPostalCode = ""

        if (foundPostalCodeIndex >= 0) {
            extractedDestPostalCode = cleanScannedText[foundPostalCodeIndex][0]
        }

        extractedFields.add("destPostalCode: $extractedDestPostalCode")
        return extractedDestPostalCode

    }

    /**
     * Extracts the tracking PIN from the scanned text blocks.
     *
     * @return The extracted tracking PIN as a String.
     */
    private fun extractTrackPin(): String {
        var extractedTrackPin = ""

        if (foundTrackPinIndex >= 0) {
            val trackPinBlock = cleanScannedText[foundTrackPinIndex]
            // sometimes found block will be the 'PIN/NIP:' near bottom of label
            if (trackPinBlock[0].contains(":")) {
                extractedTrackPin = trackPinBlock[0].substringAfterLast(":")
            }
            else {
                extractedTrackPin = trackPinBlock[0]
            }
        }

        extractedFields.add("trackPin: $extractedTrackPin")
        return extractedTrackPin

    }

    /**
     * Extracts the "from" address from the scanned text blocks.
     *
     * @return The extracted "from" address as a String.
     */
    private fun extractFromAddress() : String {
        var extractedFromAddress = ""

        if(foundFromAddressHeaderBlockIndex>=0) {
            // making sure we include 'from address' details if embedded in same block as
            // the header, by extracting any remaining lines the header block
            if ((foundFromAddressHeaderBlockIndex + 1) < cleanScannedText[foundFromAddressHeaderBlockIndex].size) {
                for (lineIndex in (foundFromAddressHeaderLineIndex + 1)..<(cleanScannedText[foundFromAddressHeaderBlockIndex].size)) {
                    extractedFromAddress += "${cleanScannedText[foundFromAddressHeaderBlockIndex][lineIndex]}, "
                }
            }

            var nextBlockIndex = foundFromAddressHeaderBlockIndex + 1

            // continue until postal code was found so we get complete address
            while (nextBlockIndex < cleanScannedText.size && !extractedFromAddress.contains(postalCodeRegex)) {
                // using a hack here: there is often other text blocks in between 'from' header and
                // the 'address' text block, i.e. dimension or weight or MANIFEST, so skipping those blocks
                // is required
                var isAddressRelated = true
                for (line in cleanScannedText[nextBlockIndex]) {
                    if (line.contains(productDimensionRegex) ||
                        line.contains(productWeightRegex) ||
                        line.contains(productWeightValueRegex) ||
                        line.contains("MANIFEST", true)) {
                        isAddressRelated = false
                        break
                    }
                }
                if (isAddressRelated) {
                    for (line in cleanScannedText[nextBlockIndex]) {
                        extractedFromAddress += "${line}, "
                    }
                }

                nextBlockIndex += 1
            }

        }

        // clean up by removing last comma
        if (extractedFromAddress.endsWith(", ")) {
            extractedFromAddress = extractedFromAddress.substringBeforeLast(",")
        }
        extractedFields.add("fromAddress: $extractedFromAddress")
        return extractedFromAddress
    }

    /**
     * Extracts the product dimensions from the scanned text blocks.
     *
     * @return The extracted product dimensions as a String.
     */
    private fun extractProductDimension(): String {
        var extractedProductDimension = ""

        if (foundProductDimensionIndex >= 0) {
            extractedProductDimension = cleanScannedText[foundProductDimensionIndex][0]
        }

        extractedFields.add("productDimension: $extractedProductDimension")
        return extractedProductDimension
    }

    /**
     * Extracts the product weight from the scanned text blocks.
     *
     * @return The extracted product weight as a String.
     */
    private fun extractProductWeight(): String {
        var extractedProductWeightValue = ""
        val extractedProductWeightUnit = "kg"
        var extractedProductWeight = ""

        if(foundProductWeightIndex>=0) {
            val productWeightBlock = cleanScannedText[foundProductWeightIndex]

            // first line of block is usually weight value, but sometimes the value is
            // in a previous block, so first checking if first line matches product weight value format
            if (productWeightBlock[0].contains(productWeightValueRegex)) {
                extractedProductWeightValue = productWeightBlock[0]
            }
            // else, the product weight value is expected in one of the three previous block
            else {
                var previousBlockIndex = foundProductWeightIndex - 1

                while (previousBlockIndex >= foundProductWeightIndex - 3) {
                    // check that block it's not the product dimensions or 'from/to' header
                    if (!intArrayOf(
                            foundProductDimensionIndex,
                            foundFromAddressHeaderBlockIndex
                        ).contains(previousBlockIndex)
                    ) {
                        for (line in cleanScannedText[previousBlockIndex]) {
                            if (line.contains(productWeightValueRegex)) {
                                extractedProductWeightValue = line
                                break
                            }
                        }
                    }
                    if (extractedProductWeightValue != "") {
                        break
                    } else {
                        previousBlockIndex -= 1
                    }
                }
            }
        }

        // add the weight unit to the extracted product weight value
        if (extractedProductWeightValue != "") {
            extractedProductWeight = extractedProductWeightValue+extractedProductWeightUnit
        }
        extractedFields.add("productWeight: ${extractedProductWeight}")
        return extractedProductWeight
    }

    /**
     * Extracts the product instructions from the scanned text blocks.
     *
     * @return The extracted product instructions as a String.
     */
    private fun extractProductInstruction(): String {
        var extractedProductInstruction = ""

        if (foundProductInstructionIndex >= 0) {
            for (line in cleanScannedText[foundProductInstructionIndex]) {
                for (instruction in productInstructions) {
                    if (line.equals(instruction, true)) {
                        extractedProductInstruction = instruction
                        break
                    }
                }
            }
        }

        extractedFields.add("productInstruction: ${extractedProductInstruction}")
        return extractedProductInstruction
    }

    /**
     * Extracts the reference from the scanned text blocks.
     *
     * @return The extracted reference as a String.
     */
    private fun extractReference(): String {
        var extractedReference = ""

        if (foundReferenceIndex >= 0) {
            for (line in cleanScannedText[foundReferenceIndex]) {
                if (line.contains(referenceRegex)) {
                    extractedReference = line.substringAfterLast(":")
                    break
                }
            }
        }

        extractedFields.add("reference: ${extractedReference}")
        return extractedReference
    }

}