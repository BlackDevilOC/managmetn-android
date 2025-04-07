# GitHub Release Setup Guide for Auto-Update

This guide will walk you through the process of uploading your APK to GitHub and setting up the auto-update system.

## Step 1: Build Your APK

1. Open your project in Android Studio
2. Make sure your app version is set to 1.1 (to match update.json) in app/build.gradle.kts:
   ```kotlin
   defaultConfig {
       applicationId = "com.substituemanagment.managment"
       minSdk = 24
       targetSdk = 34
       versionCode = 2  // This should match versionCode in update.json
       versionName = "1.1"  // This should match versionName in update.json
       // ...
   }
   ```
3. Build the APK: Click on Build > Build Bundle(s) / APK(s) > Build APK(s)
4. Android Studio will show a notification when the build is complete. Click on "locate" to find the APK file
5. The APK is usually located at: app/build/outputs/apk/debug/app-debug.apk

## Step 2: Create a GitHub Release

1. Go to your GitHub repository: https://github.com/BlackDevilOC/managmetn-android
2. Click on the "Releases" section in the right sidebar
3. Click on "Create a new release"
4. Enter release information:
   - Tag version: v1.1 (must match what's in your update.json download URL)
   - Release title: Version 1.1
   - Description: Add details about what's new in this version
5. Drag and drop your APK file into the "Attach binaries" section
6. Make sure the file is named "app-release.apk" to match the URL in update.json
7. Click "Publish release"

## Step 3: Verify Auto-Update Configuration

1. Make sure the `update.json` file is pushed to your repository
2. The download URL in update.json should be: `https://github.com/BlackDevilOC/managmetn-android/releases/download/v1.1/app-release.apk`
3. The URL in AppUpdateManager.kt should point to: `https://raw.githubusercontent.com/BlackDevilOC/managmetn-android/new/update.json`
4. Make sure the versionCode in update.json (2) is higher than your current app version code (1)

## Step 4: Test the Auto-Update

1. Install the original APK (version 1.0) on your device
2. Launch the app
3. The app should detect the new version (1.1) and show the update dialog
4. Click "Download & Install" to download and install the update

## Troubleshooting

If the auto-update doesn't work:

1. Check that your release tag (v1.1) matches the path in the downloadUrl
2. Verify that the APK file in the release is named correctly (app-release.apk)
3. Make sure the raw URL to update.json is correct
4. Check that the versionCode in update.json is higher than the installed app's versionCode
5. Look at the Android logs for error messages related to "AppUpdateManager"

## For Future Updates

When releasing a new version:

1. Increment the versionCode and versionName in app/build.gradle.kts
2. Update the update.json file with the new version information and release notes
3. Create a new GitHub release with a new tag (e.g., v1.2)
4. Upload the new APK file to the release
5. Push all changes to GitHub 