<div align="center">

# Local Dream OSS <img src="./assets/icon.png" width="32" alt="Local Dream">

**Android Stable Diffusion with Snapdragon NPU acceleration**  
_Also supports CPU/GPU inference_

<img src="./assets/demo1.jpg" alt="App Demo" width="800">

</div>

## About this Repo

This is the **open-source version** of [Local Dream](https://github.com/xororz/local-dream) — an Android app for on-device Stable Diffusion image generation. The OSS version is **completely free** and adds developer-oriented features on top of the original app.

Source repository: **[xororz/local-dream](https://github.com/xororz/local-dream)**

If you like it, please consider [sponsoring](https://github.com/xororz/local-dream?tab=readme-ov-file#-support-this-project) the original project.

## 🆕 OSS-Exclusive Features

### 🔌 OpenAI-Compatible Local API Server

The OSS version embeds a lightweight HTTP server that exposes the on-device inference engine as an **OpenAI-compatible image generation API**, letting other apps and tools use your phone as a local AI backend.

**Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/v1/models` | List available model IDs |
| `POST` | `/v1/images/generations` | Generate images (OpenAI format) |
| `POST` | `/v1/images/edits` | Image edits / img2img / inpaint (OpenAI format, multipart) |
| `POST` | `/v1/images/variations` | Image variations (OpenAI format, multipart) |
| `POST` | `/upscale` | Local Dream custom extension (binary RGB upscale, non-OpenAI-standard) |

**Highlights:**
- Bearer token authentication (`Authorization: Bearer <API_KEY>`)
- Returns images as Base64-encoded JSON (`b64_json`)
- Configurable port (default: **8081**) and API key (default: `local`)
- CORS enabled for browser/cross-origin clients

### 📡 API Models Tab

A dedicated **API Models** tab in the model list lets you:
- Add any OpenAI-compatible remote or local API endpoint
- Configure custom base URLs, API keys, and model IDs
- Auto-fetch available models from any `/v1/models` endpoint
- One-click **Quick Setup** to wire the local server to the in-app model selector

### ⚙️ Local API Settings

Toggle and configure the built-in server directly from the app:
- **Enable / disable** the local API server
- Set the **listening port**
- Set the **API key**

> [!NOTE]
> Currently focus on SD1.5 models. SD2.1 models are no longer maintained due to poor quality and not popular. SDXL/Flux models are too large for most devices. So will not support them for now.
>
> Most users don't get how to properly use highres mode. Please check [here](#npu-high-resolution-support).
>
> Now you can import your own NPU models converted using our easy-to-follow [NPU Model Conversion Guide](https://github.com/xororz/local-dream/tree/master/convert). And you can also download some pre-converted models from [xororz/sd-qnn](https://huggingface.co/xororz/sd-qnn/tree/main) or [Mr-J-369](https://huggingface.co/Mr-J-369). Download `_min` if you are using non-flagship chips. Download `_8gen1` if you are using 8gen1. Download `_8gen2` if you are using 8gen2/3/4/5. We recommend checking the instructions on the original model page to set up prompts and parameters.
>
> You can join our [telegram group](https://t.me/local_dream) for discussion or help with testing.

## 🚀 Quick Start

1. **Download**: Get the APK from [Releases](https://github.com/xororz/local-dream/releases) or [Google Play](https://play.google.com/store/apps/details?id=io.github.xororz.localdream)(NSFW filtered)
2. **Install**: Install the APK on your Android device
3. **Select Models**: Open the app and download the model(s) you want to use

## ✨ Features

- 🎨 **txt2img** - Generate images from text descriptions
- 🖼️ **img2img** - Transform existing images
- 🎭 **inpaint** - Redraw selected areas of images
- **custom models** - Import your own SD1.5 models for CPU (in app) or NPU (follow [conversion guide](https://github.com/xororz/local-dream/tree/master/convert)). You can get some pre-converted models from [xororz/sd-qnn](https://huggingface.co/xororz/sd-qnn) or [Mr-J-369](https://huggingface.co/Mr-J-369)
- **lora support** - Support adding LoRA weights to custom CPU models when importing.
- **prompt weights** - Emphasize certain words in prompts. E.g., `(masterpiece:1.5)`. Same format as [Automatic1111](https://github.com/AUTOMATIC1111/stable-diffusion-webui)
- **embeddings** - Support for custom embeddings like [EasyNegative](https://civitai.com/models/7808/easynegative). SafeTensor format is required. Convert `pt` to `safetensors` using [this](https://chino.icu/local-dream/pt2sf.py)
- **upscalers** - 4x upscaling with [realesrgan_x4plus_anime_6b](https://github.com/xinntao/Real-ESRGAN/) and [4x-UltraSharpV2_Lite](https://huggingface.co/Kim2091/UltraSharpV2)

## 🔧 Build Instructions

> **Note**: Building on Linux/WSL is recommended. Other platforms are not verified.

### Prerequisites

The following tools are required for building:

- **Rust** - Install [rustup](https://rustup.rs/), then run:
  ```bash
  # rustup default stable
  rustup default 1.84.0 # Please use 1.84.0 for compatibility. Newer versions may cause build failures.
  rustup target add aarch64-linux-android
  ```
- **Ninja** - Build system
- **CMake** - Build configuration

### 1. Clone Repository

```bash
git clone --recursive https://github.com/xororz/local-dream.git
```

### 2. Prepare SDKs

1. **Download QNN SDK**: Get [QNN_SDK_2.39](https://apigwx-aws.qualcomm.com/qsc/public/v1/api/download/software/sdks/Qualcomm_AI_Runtime_Community/All/2.39.0.250926/v2.39.0.250926.zip) and extract
2. **Download Android NDK**: Get [Android NDK](https://developer.android.com/ndk/downloads) and extract
3. **Configure paths**:
   - Update `QNN_SDK_ROOT` in `app/src/main/cpp/CMakeLists.txt`
   - Update `ANDROID_NDK_ROOT` in `app/src/main/cpp/CMakePresets.json`

### 3. Build Libraries

**Linux**

```bash
cd app/src/main/cpp/
bash ./build.sh
```

<details>
<summary><strong>Windows</strong></summary>

```powershell
# Install dependencies if needed:
# winget install Kitware.CMake
# winget install Ninja-build.Ninja
# winget install Rustlang.Rustup

cd app\src\main\cpp\

# Convert patch file (install dos2unix if needed: winget install -e --id waterlan.dos2unix)
dos2unix SampleApp.patch
.\build.bat
```

</details>

<details>
<summary><strong>macOS</strong></summary>

```bash
# Install dependencies with Homebrew:
# brew install cmake rust ninja

# Fix CMake version compatibility
sed -i '' '2s/$/ -DCMAKE_POLICY_VERSION_MINIMUM=3.5/' build.sh
bash ./build.sh
```

</details>

### 4. Build APK

Open this project in Android Studio and navigate to:
**Build → Generate App Bundles or APKs → Generate APKs**

## 🔑 CI / Release Signing Setup

The CI pipeline signs release APKs automatically. Before pushing to `main` / `master` / `dev`, you must add four repository secrets; otherwise the **"Validate release signing secrets"** step will fail with:

```
Missing secret: ANDROID_RELEASE_KEYSTORE_BASE64
Error: Process completed with exit code 1.
```

### Step 1 – Create an Android signing keystore

Run the following command on your local machine (requires JDK):

```bash
keytool -genkeypair \
  -keystore release-signing.jks \
  -alias my-key-alias \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Your Name, OU=Your Org, O=Your Company, L=City, ST=State, C=US"
```

> Keep `release-signing.jks` somewhere safe. **Never commit it to the repository.**

### Step 2 – Encode the keystore to Base64

**Linux / macOS:**

```bash
base64 -w 0 release-signing.jks
```

**Windows (PowerShell):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-signing.jks"))
```

Copy the entire output string — you will need it in the next step.

### Step 3 – Add secrets to your GitHub repository

Go to your repository on GitHub:  
**Settings → Secrets and variables → Actions → New repository secret**

Add all four secrets:

| Secret name | Value |
|---|---|
| `ANDROID_RELEASE_KEYSTORE_BASE64` | The Base64 string from Step 2 |
| `ANDROID_RELEASE_STORE_PASSWORD` | The `-storepass` value you chose |
| `ANDROID_RELEASE_KEY_ALIAS` | The `-alias` value you chose (e.g. `my-key-alias`) |
| `ANDROID_RELEASE_KEY_PASSWORD` | The `-keypass` value you chose |

Once all four secrets are in place, push again — the CI will decode the keystore, sign the APK, and create a GitHub Release automatically.

## Technical Implementation

### NPU Acceleration

- **SDK**: Qualcomm QNN SDK leveraging Hexagon NPU
- **Quantization**: W8A16 static quantization for optimal performance
- **Resolution**: Fixed 512×512 model shape
- **Performance**: Extremely fast inference speed

### CPU/GPU Inference

- **Framework**: Powered by MNN framework
- **Quantization**: W8 dynamic quantization
- **Resolution**: Flexible sizes (128×128, 256×256, 384×384, 512×512)
- **Performance**: Moderate speed with high compatibility

## NPU High Resolution Support

> [!IMPORTANT]
> Please note that quantized high-resolution(>768x768) models may produce images with poor layout. We recommend first generating at 512 resolution (optionally you can upscale it), then using the high-resolution model for img2img (which is essentially Highres.fix). The suggested img2img denoise_strength is around 0.8. After that, you can get images with better layout and details.

## Device Compatibility

### NPU Acceleration Support

Compatible with devices featuring:

- **Snapdragon 8 Gen 1/8+ Gen 1**
- **Snapdragon 8 Gen 2**
- **Snapdragon 8 Gen 3**
- **Snapdragon 8 Elite**
- **Snapdragon 8 Elite Gen 5/8 Gen 5**
- Non-flagship chips with Hexagon V68 or above (e.g., Snapdragon 7 Gen 1, 8s Gen 3)

> **Note**: Other devices cannot download NPU models

### CPU/GPU Support

- **RAM Requirement**: ~2GB available memory
- **Compatibility**: Most Android devices from recent years

## Available Models

The following models are built-in and can be downloaded directly in the app:

<div align="center">

| Model                | Type  | CPU/GPU | NPU | Clip Skip | Source                                                                      |
| -------------------- | ----- | :-----: | :-: | :-------: | --------------------------------------------------------------------------- |
| **AnythingV5**       | SD1.5 |   ✅    | ✅  |     2     | [CivitAI](https://civitai.com/models/9409?modelVersionId=30163)             |
| **ChilloutMix**      | SD1.5 |   ✅    | ✅  |     1     | [CivitAI](https://civitai.com/models/6424/chilloutmix?modelVersionId=11732) |
| **Absolute Reality** | SD1.5 |   ✅    | ✅  |     2     | [CivitAI](https://civitai.com/models/81458?modelVersionId=132760)           |
| **QteaMix**          | SD1.5 |   ✅    | ✅  |     2     | [CivitAI](https://civitai.com/models/50696/qteamix-q?modelVersionId=94654)  |
| **CuteYukiMix**      | SD1.5 |   ✅    | ✅  |     2     | [CivitAI](https://civitai.com/models/28169?modelVersionId=265102)           |

</div>

## 🎲 Seed Settings

Custom seed support for reproducible image generation:

- **CPU Mode**: Seeds guarantee identical results across different devices with same parameters
- **GPU Mode**: Results may differ from CPU mode and can vary between different devices
- **NPU Mode**: Seeds ensure consistent results only on devices with identical chipsets

## Credits & Acknowledgments

### C++ Libraries

- **[Qualcomm QNN SDK](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)** - NPU model execution
- **[alibaba/MNN](https://github.com/alibaba/MNN/)** - CPU model execution
- **[xtensor-stack](https://github.com/xtensor-stack)** - Tensor operations & scheduling
- **[mlc-ai/tokenizers-cpp](https://github.com/mlc-ai/tokenizers-cpp)** - Text tokenization
- **[yhirose/cpp-httplib](https://github.com/yhirose/cpp-httplib)** - HTTP server
- **[nothings/stb](https://github.com/nothings/stb)** - Image processing
- **[facebook/zstd](https://github.com/facebook/zstd)** - Model compression
- **[nlohmann/json](https://github.com/nlohmann/json)** - JSON processing

### Android Libraries

- **[square/okhttp](https://github.com/square/okhttp)** - HTTP client
- **[coil-kt/coil](https://github.com/coil-kt/coil)** - Image loading & processing
- **[MoyuruAizawa/Cropify](https://github.com/MoyuruAizawa/Cropify)** - Image cropping
- **AOSP, Material Design, Jetpack Compose** - UI framework

### Models

- **[CompVis/stable-diffusion](https://github.com/CompVis/stable-diffusion)** and all other model creators
- **[xinntao/Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN)** - Image upscaling
- **[Kim2091/UltraSharpV2](https://huggingface.co/Kim2091/UltraSharpV2)** - Image upscaling
- **[bhky/opennsfw2](https://github.com/bhky/opennsfw2)** - NSFW content filtering

---

## 💖 Support This Project

If you find Local Dream useful, please consider supporting its development:

### What Your Support Helps With:

- **Additional Models** - More AI model integrations
- **New Features** - Enhanced functionality and capabilities
- **Bug Fixes** - Continuous improvement and maintenance

<a href="https://ko-fi.com/xororz">
    <img height="36" style="border:0px;height:36px;" src="https://storage.ko-fi.com/cdn/kofi2.png?v=3" border="0" alt="Buy Me a Coffee at ko-fi.com" />
</a>
<a href="https://afdian.com/a/xororz">
    <img height="36" style="border-radius:12px;height:36px;" src="https://pic1.afdiancdn.com/static/img/welcome/button-sponsorme.jpg" alt="在爱发电支持我" />
</a>

Your sponsorship helps maintain and improve Local Dream for everyone!
