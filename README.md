# LabelXtract

**Team ENIGMatic**  
*(Algonquin College – CST8319 Software Development Project)*

  ![LabelXtract Logo](app/src/main/ic_launcher-playstore.png)

## Demo

![LabelXtract Demo](app/src/main/Demo.gif)

---

## Table of Contents
1. [Quickstart](#quickstart)  
2. [Project Overview](#project-overview)  
3. [Features](#features)  
4. [Project Requirements Summary](#project-requirements-summary)  
5. [Project Structure](#project-structure)  
6. [Technologies Used](#technologies-used)  
7. [Installation & Setup](#installation--setup)  
8. [Usage](#usage)  
9. [Sample Output](#sample-output)  
10. [Contribution Guidelines](#contribution-guidelines)
11. [Contributors](#contributors)
12. [License](#license)  

---

## Quickstart
1. Clone the repo
2. Open in Android Studio
3. Run on an Android 11+ device (API level 30+)
4. Point your camera at a Canada Post shipping label 
5. View extracted data in JSON format

## Project Overview
**Label Xtract** is an Android application designed to read shipping labels using the device’s rear-facing camera and automatically extract key information (addresses, postal codes, barcodes, and more) via Optical Character Recognition (OCR). The data is then displayed in JSON format. This project was developed by **Team ENIGMatic** to address inefficiencies in handling non-conformant packages at Canada Post, where a small percentage of parcel labels fail to be read by traditional barcode scanners.

**Core Goals:**
- Provide depot clerks with a tool to quickly extract all relevant shipping label info.
- Handle damaged or partially unreadable labels that traditional scanners struggle with
- Produce structured data for further processing
- Create an intuitive, user-friendly interface for quick scanning in a busy environment

---

## Features
- **Intelligent Label Detection:** Automatically detects Canadian postal codes to identify shipping labels
- **High-Quality Document Scanning:** Uses Google's Document Scanner API for clear label captures
- **Advanced OCR Processing:** Extracts text from images using Google ML Kit's Text Recognition
- **Field Extraction Logic:** Parses detected text to identify specific shipping label fields:
    - Product type (Priority, Xpresspost, Regular Parcel, etc.)
    - Sender and recipient addresses
    - Postal codes
    - Tracking numbers
    - Package dimensions and weight
    - Special handling instructions
- **Barcode Scanning:** Identifies and reads barcodes even if partially damaged
- **JSON Output:** Presents all extracted data in a clean, structured format
- **Copy to Clipboard:** One-tap copying of the JSON output for easy use in other applications
- **Intuitive UI:** Simple camera-based interface with clear result display
- **Error Handling:** Validates extracted data and provides audio feedback for missing fields

---

## Project Requirements Summary
This application aligns with the core requirements stated in the **Project Requirements Specification (PRS)**:

1. **Functional Requirements**  
   - OCR scanning of shipping labels to extract data
   - Barcode detection and text extraction
   - JSON output for recognized fields  
   - Handling of partially damaged labels  
   - Intuitive UI and user flow

2. **Non-Functional Requirements**  
   - Compatible with Android 11 (API level 30) or higher  
   - Sub-second processing time for scans  
   - Developed using Agile methodology  
   - Hosted on a public GitHub repository under a permissive license

---

## Project Structure
The application follows the MVC architecture pattern and is organized into the following packages:

```plaintext
.
└── algonquin.cst8319.enigmatic/
    ├── MainActivity.kt                # Main UI and camera setup
    ├── data/
    │   ├── FieldExtractor.kt          # Extracts structured data from OCR text
    │   ├── LabelJSON.kt               # Data model for label information
    │   ├── MainActivityViewModel.kt   # ViewModel for UI state management
    │   └── Validator.kt               # Validates extracted fields
    ├── presentation/
    │   └── PersistentBottomSheet.kt   # UI component for showing results
    ├── processing/
    │    ├── ImageAnalyzer.kt           # Processes camera frames for OCR & barcode
    │    └── LabelDetectedCallback.kt   # Interface for label detection events
    ├── xml layouts, resources, etc.
    └── ...
```

### Key Classes

---

#### `MainActivity`
The entry point of the application, responsible for:
- Camera setup and permissions handling
- UI management and user interaction
- Launching the document scanner when a label is detected
- Displaying scan results

#### `ImageAnalyzer`
Core processing class that:
- Analyzes camera frames in real-time
- Detects text using ML Kit's Text Recognition
- Scans for barcodes using ML Kit's Barcode Scanner
- Identifies when a shipping label is present (by detecting postal codes)
- Coordinates the extraction and validation of label fields

#### `FieldExtractor`
Specialized class for extracting shipping label information:
- Uses regex patterns to identify specific fields in OCR text
- Sorts and processes text blocks based on their position
- Extracts addresses, postal codes, tracking numbers, etc.
- Handles the challenges of OCR errors and formatting variations

#### `LabelJSON`
Data class that:
- Stores all extracted shipping label fields
- Provides getters and setters for each field
- Serializes the data to a formatted JSON string

#### `Validator`
Ensures data quality by:
- Checking for missing or invalid fields
- Validating field length and format where possible
- Providing error feedback (including audio alerts)

---

## Technologies Used

Below is a comprehensive list of the core technologies and libraries utilized by Label Xtract:

- **![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)**  
  Primary programming language for implementing the application logic, leveraging modern Kotlin features such as data classes, and extension functions.

- **![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)**  
  Build automation tool used to manage project configurations, dependencies, and tasks. The Gradle wrapper ensures consistent build environments across different systems.

- **![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android-studio&logoColor=white)** 
  Recommended integrated development environment (IDE) for Android app development, offering code editing, debugging, and built-in Gradle support.

- **Android SDK / AndroidX Libraries**  
  - **CameraX**: Provides a simple and consistent API to access the device camera.  
  - **Lifecycle** / **ViewModel**: Architecture components managing UI-related data in a lifecycle-conscious way.  
  - **Activity KTX** / **Fragment KTX** / **Core KTX**: Kotlin extension libraries that simplify common Android tasks.  
  - **ConstraintLayout**: Flexible layout manager for UI design.  
  - **Material Components**: Implements Material Design widgets and behaviors (e.g., Bottom Sheets).

- **![Google ML Kit](https://img.shields.io/badge/Google%20ML%20Kit-4285F4?style=for-the-badge&logo=google&logoColor=white)** 
  - **ML Kit Text Recognition**: Automatically detects and extracts text from images.  
  - **ML Kit Barcode Scanning**: Identifies and reads barcodes (including partially damaged ones).  
  - **Play Services Document Scanner**: Enables higher-fidelity image capture flows via `GmsDocumentScanning`.

- **Kotlinx Serialization (Json)**  
  Used for converting recognized label data into structured JSON output. Annotated classes (e.g., `@Serializable`) allow easy parsing and generation of JSON strings.

- **![Material Design](https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white)**  
  Offers ready-made UI components (buttons, text fields, bottom sheets, etc.) and ensures a modern, consistent user experience aligned with Google’s Material Design guidelines.

These technologies work together to deliver a user-friendly experience. All build dependencies are specified in the app’s `build.gradle` file, making it straightforward to manage and update the libraries as needed.

---

## Installation & Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Kotlin 2.0.0 or newer
- **![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)** device running Android 11 (API level 30) or higher
- Camera permissions enabled on the test device

### Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/LabelXtract/labelxtract.git

2. **Open in Android Studio**
   - Open **Android Studio**.
   - Select **Open an Existing Project**.
   - Navigate to the cloned folder and open it.

3. **Sync and Install Dependencies**
   - Allow **Gradle** to sync automatically, or go to **File > Sync Project with Gradle Files**.
   - Required **ML Kit** libraries and **AndroidX** components will be installed automatically via Gradle.

4. **Run on a Device or Emulator**
   - Connect your Android device via USB with **Developer Mode** enabled.
   - Press the **Run** button in Android Studio to build and deploy the app.

5. **Permissions**
   - On the first launch, the app will request **Camera** permissions. Grant these permissions so the application can access the camera for live scanning.

---

## Usage

1. **Launch the App**
    - Open LabelXtract on your Android device
    - The app will request camera permissions if not already granted

2. **Scan a Label**
    - Point your device's camera at a Canada Post shipping label
    - Hold the device steady until the app detects a postal code
    - Once detected, the document scanner will launch automatically

3. **Capture the Label**
    - Follow the on-screen guides to position the label within the frame
    - The document scanner will automatically capture the image when ready
    - For manual capture, tap the shutter button

4. **View Results**
    - Confirm or retake the displayed captured image
    - After processing, the app displays the extracted information
    - A bottom sheet shows all detected fields in JSON format
    - The scanned image appears above the JSON data for reference

5. **Copy or Continue**
    - Tap "Copy" to copy the JSON data to your clipboard
    - Tap "Close" to dismiss the results and scan another label

### Tips for Best Results

- **Good Lighting:** Ensure the label is well-lit for accurate OCR
- **Avoid Glare:** Minimize reflections on glossy label surfaces
- **Flat Surface:** Try to keep the label flat to avoid distortion
- **Complete View:** Make sure the entire label is visible in the frame
- **Steady Hand:** Hold the device steady during scanning

---

## Sample Output

Below is a sample JSON output captured by the application:

```json
{
  "productType": "Priority",
  "toAddress": "Julie Tester, 4811 Churchill Place, Laval, QC, H7W 4H4",
  "destPostalCode": "H7W 4H4",
  "trackPin": "7023 2102 3528 2700",
  "barCode": "PHWH7447023210235282270000200",
  "fromAddress": "IIQA CUST DO NOT USE, 2 SAINTE-CATHERINE ST East, MONTREAL QC H2X",
  "productDimension": "33x33x33cm",
  "productWeight": "7.190kg",
  "productInstruction": "MANIFESTREQ",
  "reference": "QC-DJ002"
}
```

Each field represents specific information from the shipping label:
- `productType`: The Canada Post service type (Priority, Xpresspost, etc.)
- `toAddress`: The recipient's complete address
- `destPostalCode`: The destination postal code
- `trackPin`: The tracking number, typically 16 digits
- `barCode`: The barcode value, if successfully scanned
- `fromAddress`: The sender's complete address
- `productDimension`: Package dimensions (LxWxH)
- `productWeight`: Package weight with unit
- `productInstruction`: Special handling instructions (SIGNATURE, DO NOT SAFE DROP, etc.)
- `reference`: Reference number or customer ID

---

## Contribution Guidelines

We welcome contributions to improve **LabelXtract**! Whether you find a bug, have an idea for a new feature, or want to enhance the existing code, here’s how you can get involved:

- **Reporting Issues or Suggestions**:  
  If you encounter any problems using **LabelXtract** or have recommendations for enhancements, please open an issue on the project's issue tracker (GitHub Issues). Provide details about the problem or idea, including steps to reproduce bugs or reasoning behind feature requests. This helps maintainers and contributors understand and prioritize the work.

- **Contributing Code**:  
  We appreciate pull requests from the community. To contribute code or documentation:

  1. **Fork the Repository**:  
     Click the **"Fork"** button on GitHub to create your own copy of the **Label Xtract** repository.
  2. **Create a Branch**:  
     In your forked repository, create a new branch for your fix or feature. Use a descriptive branch name, for example: `fix/image-loading-bug` or `feature/add-json-export`.
  3. **Make Changes**:  
     Develop your feature or bug fix on that branch. Follow the coding style of the project (consistent naming, formatting, and adequate comments).
  4. **Test Your Changes**:  
     Ensure that the project still builds and all tests pass after your changes.
  5. **Commit and Push**:  
     Commit your changes with a clear and concise commit message. Push the branch to your GitHub fork.
  6. **Open a Pull Request**:  
     Go to the original repository and open a PR from your forked branch. In the pull request description, clearly explain the changes you’ve made and **why** (link to the issue it fixes, if applicable). Include any relevant screenshots or logs if the changes affect the UI or performance.
  7. **Code Review**:  
     The ENIGMatic team will review your pull request as soon as we can. Please be open to feedback or requests for adjustments. We aim to collaborate to maintain code quality and project vision.

- **Code Style and Guidelines**:  
  Try to adhere to any style guidelines used in the project. Write clear comments for any functions you introduce. This makes it easier for others to understand and maintain the code in the future.

By following these guidelines, you help us ensure that the project remains stable and useful. All contributors will be acknowledged for their work. **Thank you** for helping improve **Label Xtract**!

---

## Contributors

The **LabelXtract** project was developed by the following team members as part of a software development course project:

| **Name**       | **Role**            | **GitHub**                                             |
|----------------|---------------------|-------------------------------------------------------|
| Ekene Ndubueze  | Developer, QA, Tester     | [GitHub Profile](https://github.com/Ozi-Tech)     |
| Naomi Bell     | Developer, UI/UX, Tester         | [GitHub Profile](https://github.com/bell0418)     |
| Ian Philips  | Developer, Business Analyst, Tester         | [GitHub Profile](https://github.com/phil0440)     |
| Gulnur Ospanova  | Developer, Architect/Researcher, Tester      | [GitHub Profile](https://github.com/gulnurkaztai)     |
| Milan Neven  | Developer, Architect, Tester | [GitHub Profile](https://github.com/MilanNeven)     |

---

## License

This project is licensed under the [MIT License](LICENSE).  
For detailed terms and conditions, please see the `LICENSE` file in the root directory of this repository.



