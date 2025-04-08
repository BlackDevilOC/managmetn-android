# Auto-Update System for Your Android App

This guide explains how to set up and use the auto-update system that has been implemented in your app.

## How It Works

The auto-update system works as follows:

1. When your app starts, it checks for updates from your GitHub repository
2. If an update is available, it shows a dialog to the user with release notes
3. If the user agrees to update, the app downloads the new APK file
4. After downloading, it prompts the user to install the update

## Setup Instructions

### 1. Host the update.json file

1. Create a GitHub repository if you don't already have one
2. Upload the `update.json` file to the main branch of your repository
3. Update the `UPDATE_METADATA_URL` in `AppUpdateManager.kt` to point to your raw GitHub URL:

```kotlin
private val UPDATE_METADATA_URL = "https://raw.githubusercontent.com/YOURUSERNAME/YOURREPO/main/update.json"
```

### 2. Update the JSON File

Each time you release a new version, update the `update.json` file with:

- New version code (must be higher than previous)
- New version name
- Download URL for the APK
- Release notes

Example:
```json
{
  "versionCode": 2,
  "versionName": "1.1",
  "downloadUrl": "https://github.com/YOURUSERNAME/YOURREPO/releases/download/v1.1/app-release.apk",
  "releaseNotes": "New features and bug fixes in this version:\n- Added auto-update functionality\n- Fixed UI issues\n- Improved performance"
}
```

### 3. Upload APK Files

1. Build your APK file using Android Studio (Build > Build Bundle(s) / APK(s) > Build APK(s))
2. Create a new Release on your GitHub repository
3. Upload the APK file to the release
4. Make sure the download URL in your update.json matches the URL of this release

## Important Notes

- Always increment the `versionCode` in your app's build.gradle.kts file for each new version
- Make sure the `downloadUrl` is publicly accessible
- Test the update process on a real device before releasing to users
- Consider implementing optional updates vs. mandatory updates based on your needs

## Troubleshooting

If users experience issues with the auto-update:

1. Check that the update.json file is accessible
2. Verify that the APK download URL is correct and publicly accessible
3. Ensure the version code in update.json is higher than the user's current version
4. Check the Android logs for any errors related to downloading or installing

## Security Considerations

- Consider using HTTPS URLs for all downloads
- You may want to implement signature verification for the downloaded APK
- Consider using Google Play's in-app updates if publishing through the Play Store 