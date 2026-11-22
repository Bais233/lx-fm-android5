# Bais FM（Android 5.1）

面向 Android 5.1（API 22）的开放电台播放器，基于 [LX-FM](https://gitee.com/lx_studio/LX-FM)（Apache-2.0）精简移植。

- 作者：[Bais233](https://github.com/Bais233)
- 仓库：https://github.com/Bais233/lx-fm-android5
- 更新：应用内「设置 → 检查新版本」自动同步本仓库 GitHub Releases

## 功能

- 省市电台列表（官方专线 ytmsout.radio.cn，兜底 radio-browser.info）
- 后台播放 + 通知栏控制
- 睡眠定时（基于 elapsedRealtime，休眠不漂移）
- 主题：固定 / 定时 / 日出日落切换、自选主题色、界面字体大小、自定义字体
- 纯 Kotlin/Java，无 .so，armeabi-v7a 直接安装

## 编译

云端：GitHub Actions `.github/workflows/build.yml`，推送后自动构建 release APK。
本地：`gradle assembleRelease`（JDK 17、Android SDK 34）。
签名：仓库自带 `app/lxfm5.jks`（v1+v2），仅自用侧载；发布请换自己的 key。

## 已知限制

- 5.1 的 TLS 只到 1.2，仅支持 TLS 1.3 的流放不出来。
- 电台列表不缓存，每次启动要联网取。
- `net/Http.kt`：API<24 时把 ISRG Root X1 补进信任库（5.1 系统 CA 缺 Let's Encrypt 根证书）。
