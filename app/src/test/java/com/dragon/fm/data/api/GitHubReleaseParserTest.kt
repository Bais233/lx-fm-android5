package com.dragon.fm.data.api

private fun expectError(fragment: String, block: () -> Unit) {
    val error = runCatching(block).exceptionOrNull()
    check(error is IllegalArgumentException && error.message?.contains(fragment) == true) {
        "Expected error containing: $fragment"
    }
}

fun main() {
    val release = GitHubReleaseParser.parse(
        """
        {
          "tag_name": "v1.1.0-5.1",
          "name": "LX-FM 1.1.0",
          "body": "versionCode: 2\nFix playback",
          "published_at": "2026-11-22T12:00:00Z",
          "assets": [
            {"name":"LX-FM-arm64-v8a.apk","browser_download_url":"https://example.com/arm64.apk"},
            {"name":"LX-FM-universal.apk","browser_download_url":"https://example.com/universal.apk"}
          ]
        }
        """.trimIndent()
    )
    check(release.versionCode == 2)
    check(release.versionName == "1.1.0-5.1")
    check(release.changelog.lines() == listOf("versionCode: 2", "Fix playback"))
    check(release.releaseDate == "2026-11-22T12:00:00Z")
    check(release.downloadUrl == "https://example.com/universal.apk")
    check(release.abiUrls["arm64-v8a"] == "https://example.com/arm64.apk")

    val tagWithoutV = GitHubReleaseParser.parse(
        """{"tag_name":"1.1.0-5.1","body":"versionCode: 2","assets":[{"name":"app.apk","browser_download_url":"https://example.com/app.apk"}]}"""
    )
    check(tagWithoutV.versionName == "1.1.0-5.1")
    expectError("versionCode") {
        GitHubReleaseParser.parse("""{"tag_name":"v1.1.0","name":"fallback","body":"","assets":[]}""")
    }
    expectError("APK") {
        GitHubReleaseParser.parse("""{"tag_name":"1.1.0","body":"versionCode: 2","assets":[]}""")
    }
    println("GitHubReleaseParserTest: all checks passed")
}
