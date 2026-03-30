# API 使用说明（OpenAI 兼容图像生成）

本文档说明 Local Dream 里“API 模型”功能需要的接口格式，以及如何在应用内配置。

> 目标：让你的服务可被 Local Dream 作为 **OpenAI-compatible image API** 调用。

---

## 1. 需要实现的接口

根据当前代码实现，应用会调用以下接口：

1. `GET /v1/models`（用于拉取可选模型 ID）
2. `POST /v1/images/generations`（用于图像生成）
3. `POST /v1/images/edits`（用于图生图 / 局部重绘，OpenAI 标准）
4. `POST /v1/images/variations`（用于变体生成，OpenAI 标准）

另外本地服务还提供一个实用扩展：

- `POST /upscale`（二进制 RGB 放大接口，**非 OpenAI 官方标准**）

并且会带上鉴权头：

```http
Authorization: Bearer <API_KEY>
```

---

## 2. `GET /v1/models`

### 请求

```bash
curl -X GET "https://your-api.example.com/v1/models" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

### 期望响应（最小格式）

```json
{
  "data": [
    { "id": "gpt-image-1" },
    { "id": "dall-e-3" }
  ]
}
```

应用会读取 `data[].id` 作为模型下拉列表。

> 如果你使用的是应用内本地服务（`127.0.0.1:8081`），现在也支持该端点：
>
> ```bash
> curl http://127.0.0.1:8081/v1/models
> ```

---

## 3. `POST /v1/images/generations`

### 请求体（应用实际发送）

```json
{
  "model": "gpt-image-1",
  "prompt": "a cute cat in watercolor style",
  "n": 1,
  "size": "1024x1024",
  "response_format": "b64_json"
}
```

字段说明：

- `model`: 你在 API 模型里配置的 model id
- `prompt`: 用户提示词
- `n`: 图片数量（应用侧会限制在 1~4）
- `size`: 例如 `512x512`、`1024x1024`、`1024x1792`、`1792x1024`
- `response_format`: 固定为 `b64_json`

### cURL 示例

```bash
curl -X POST "https://your-api.example.com/v1/images/generations" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model":"gpt-image-1",
    "prompt":"a cute cat in watercolor style",
    "n":1,
    "size":"1024x1024",
    "response_format":"b64_json"
  }'
```

### 期望响应（最小格式）

```json
{
  "data": [
    {
      "b64_json": "iVBORw0KGgoAAAANSUhEUgAA...",
      "revised_prompt": "a cute cat in watercolor style"
    }
  ]
}
```

应用会读取 `data[].b64_json` 并解码成图片，`revised_prompt` 为兼容 OpenAI 风格扩展字段。

> 本地服务同样支持：
>
> ```bash
> curl -X POST "http://127.0.0.1:8081/v1/images/generations" \
>   -H "Content-Type: application/json" \
>   -d '{
>     "model":"local-dream",
>     "prompt":"a cute cat in watercolor style",
>     "n":1,
>     "size":"1024x1024",
>     "response_format":"b64_json"
>   }'
> ```

> 注意：本地后端是按当前启动的运行时分辨率工作的。`size` 必须与当前后端实际分辨率一致（常见为 `512x512`，或你当前 patch 对应的分辨率），否则会返回 400。

---

## 4. `POST /v1/images/edits`（图生图 / 局部重绘）

该接口遵循 OpenAI Image API 风格，采用 `multipart/form-data`：

- 必填文件：`image`
- 可选文件：`mask`
- 必填文本：`prompt`
- 可选文本：`n`、`size`、`seed`、`steps`、`cfg`、`scheduler`、`denoise_strength`、`input_fidelity`

> `input_fidelity` 取值应在 `[0,1]`，会映射为内部去噪强度（`denoise_strength = 1 - input_fidelity`，并限制在 `[0,1]`）。

### cURL 示例

```bash
curl -X POST "http://127.0.0.1:8081/v1/images/edits" \
  -H "Authorization: Bearer local" \
  -F "image=@./input.png" \
  -F "mask=@./mask.png" \
  -F "prompt=a futuristic city at sunset" \
  -F "n=1" \
  -F "size=512x512" \
  -F "input_fidelity=0.6"
```

### 期望响应（最小格式）

```json
{
  "created": 1710000000,
  "data": [
    {
      "b64_json": "iVBORw0KGgoAAAANSUhEUgAA...",
      "revised_prompt": "a futuristic city at sunset"
    }
  ]
}
```

---

## 5. `POST /v1/images/variations`（变体生成）

该接口同样使用 `multipart/form-data`：

- 必填文件：`image`
- 可选文本：`prompt`、`n`、`size`、`seed`、`steps`、`cfg`、`scheduler`、`denoise_strength`、`input_fidelity`

> 变体接口默认 `denoise_strength=0.8`（比 edits 默认值更高），用于更明显的风格/细节变化；如需更保真可显式传更低值，或传 `input_fidelity` 覆盖。

### cURL 示例

```bash
curl -X POST "http://127.0.0.1:8081/v1/images/variations" \
  -H "Authorization: Bearer local" \
  -F "image=@./input.png" \
  -F "n=2" \
  -F "size=512x512"
```

---

## 6. 错误返回建议

当接口失败时，建议返回 OpenAI 风格错误：

```json
{
  "error": {
    "message": "Invalid API key"
  }
}
```

应用会优先展示 `error.message`。

---

## 7. 在 Local Dream 里配置

1. 打开应用，进入 **API 模型** 页面
2. 填写：
   - `API Endpoint`：例如 `https://your-api.example.com`（不要加结尾 `/`）
   - `API Key`
   - `Model ID`（可点刷新按钮从 `/v1/models` 拉取）
3. 保存后进入该 API 模型运行页，输入提示词生成

### 一键配置（本地已有模型）

如果你使用应用内“一键配置已有模型”（本地 `127.0.0.1` 端口方案），相关参数现在放在**设置页统一管理**：

- 本地 API 开关
- 本地端口（默认 `8081`）
- API Key

一键配置时会复用上述统一设置，而不是每个模型分别手动填写。

---

## 8. 常见问题

- **401/403**：检查 `Authorization: Bearer` 是否正确
- **404**：确认服务实现了 `/v1/models`、`/v1/images/generations`，如需图生图还需 `/v1/images/edits`
- **生成失败但无详细信息**：先用上面的 cURL 直接调用，确认服务返回格式正确
- **图片过大报错**：应用侧会拒绝超大 `b64_json`（约 12 MiB 字符级阈值）
