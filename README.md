# LX FM 5.1（精简移植）

把 [LX-FM](https://gitee.com/lx_studio/LX-FM)（Apache-2.0，泷芯码匠）砍到只剩"听电台"，跑在 Android 5.1（API 22）上。

## 与原版的差别

| | 原版 | 这里 |
|---|---|---|
| minSdk | 26 | **22** |
| 技术栈 | Kotlin + Compose 1.6 + Media3 1.3 | 同左（Compose 下限本就是 21） |
| 界面 | 流光/毛玻璃/环形频谱/多主题 | 一屏列表 + 播放条（blur/RenderEffect 在 API<31 本来就是空操作） |
| 功能 | ASR 识别、录音、歌词、车载 HUD、场景卡片、自更新 | 只留：省市电台列表、后台播放、通知栏控制 |
| 原生库 | sherpa-onnx + shazam .so | **无**，纯 Java/Kotlin，armeabi-v7a 直接跑 |
| 体积 | 16MB+ | **8.4MB** |

## 为 5.1 做的三处实改

1. `minSdk 22`，没有 .so 所以不需要 ABI 拆分和 flavor。
2. 不用 `java.time`（5.1 没有；用了就得开 core library desugaring）。
3. `net/Http.kt`：API<24 时把 ISRG Root X1 补进信任库 —— 5.1 的系统 CA 里没有 Let's Encrypt 的根，radio-browser 和部分 https 流会握手失败（`networkSecurityConfig` 要 API 24 才生效，用不上）。

## 电台来源

- 官方专线：`ytmsout.radio.cn`（省市分级 + 当前节目名），签名算法沿用原版。
- 兜底：radio-browser.info 的中国电台（专线取不到时用）。

## 编译

本地：`gradle assembleRelease`（JDK 17、Android SDK 34）。仓库根目录带 `app/lxfm5.jks`，口令 `lxfm5pass`。
云端：`.github/workflows/android.yml`（推 workflow 文件需要 token 的 `workflow` scope）。

## 已知限制

- 用仓库自带的 `app/lxfm5.jks` 签名（v1+v2，口令 lxfm5pass），仅自用侧载；要发布请换自己的 key。
- 5.1 的 TLS 只到 1.2，只支持 TLS 1.3 的流放不出来。
- 电台列表不缓存，每次启动要联网取。
- 依赖 Compose，在这颗 A53 上滚动不会特别跟手。
