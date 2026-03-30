# API 使用说明（OpenAI 兼容图像生成）

本文档说明 Local Dream 里“API 模型”功能需要的接口格式，以及如何在应用内配置。

> 目标：让你的服务可被 Local Dream 作为 **OpenAI-compatible image API** 调用。

---

## 1. 需要实现的接口

根据当前代码实现，应用会调用以下两个接口：

1. `GET /v1/models`（用于拉取可选模型 ID）
2. `POST /v1/images/generations`（用于图像生成）

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
      "b64_json": "iVBORw0KGgoAAAANSUhEUgAA..."
    }
  ]
}
```

应用会读取 `data[].b64_json` 并解码成图片。

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

---

## 4. 错误返回建议

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

## 5. 在 Local Dream 里配置

1. 打开应用，进入 **API 模型** 页面
2. 填写：
   - `API Endpoint`：例如 `https://your-api.example.com`（不要加结尾 `/`）
   - `API Key`
   - `Model ID`（可点刷新按钮从 `/v1/models` 拉取）
3. 保存后进入该 API 模型运行页，输入提示词生成

### 一键配置（本地已有模型）

如果你使用应用内“一键配置已有模型”（本地 `127.0.0.1` 端口方案），现在也需要填写：

- 本地端口（默认 `8081`）
- API Key（会保存到该模型配置中）

---

## 6. 常见问题

- **401/403**：检查 `Authorization: Bearer` 是否正确
- **404**：确认服务实现的是 `/v1/images/generations` 与 `/v1/models`
- **生成失败但无详细信息**：先用上面的 cURL 直接调用，确认服务返回格式正确
- **图片过大报错**：应用侧会拒绝超大 `b64_json`（约 12 MiB 字符级阈值）
