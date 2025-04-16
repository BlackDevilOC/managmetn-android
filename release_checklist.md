# Release Checklist for Auto-Update System

## Before Building the APK

- [x] Update AppUpdateManager.kt with correct GitHub URL (https://raw.githubusercontent.com/BlackDevilOC/managmetn-android/new/update.json)
- [x] Update update.json with correct version information and download URL
- [x] Update app/build.gradle.kts with new versionCode (2) and versionName (1.1)
- [ ] Make sure all your code changes are committed to the "new" branch

## Building the APK

- [ ] Build a release APK in Android Studio:
  - Select Build > Generate Signed Bundle / APK
  - Choose APK
  - Fill in your keystore information or create a new keystore
  - Select release build type
  - Click Finish to generate the APK

## Creating GitHub Release

- [ ] Go to your GitHub repository: https://github.com/BlackDevilOC/managmetn-android
- [ ] Click on "Releases" in the sidebar
- [ ] Click "Create a new release"
- [ ] Set the tag to "v1.1"
- [ ] Set the release title to "Version 1.1"
- [ ] Add release notes describing the changes
- [ ] Upload the generated APK file and rename it to "app-release.apk"
- [ ] Publish the release

## Testing the Update System

- [ ] Install version 1.0 of the app on a device
- [ ] Launch the app and check if it detects the update
- [ ] Test downloading and installing the update
- [ ] Verify the new version (1.1) is installed correctly

## Final Steps

- [ ] Push all your changes to GitHub
- [ ] Verify that update.json and AppUpdateManager.kt have the correct URLs
- [ ] Share the APK with your users

Remember to follow this checklist each time you release a new version of your app. 