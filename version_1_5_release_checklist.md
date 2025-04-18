# Version 1.5 Release Checklist

## Pre-release Steps
- [x] Update app/build.gradle.kts with versionCode = 5 and versionName = "1.5"
- [x] Update update.json with new version information and download URL
- [x] Update AppUpdateManager.kt URL to point to the main branch
- [x] Improved responsive layout for attendance page
- [x] Added tab functionality for filtering teachers by status
- [x] Enhanced UI for better experience on smaller screens
- [x] Fixed layout issues on older/smaller devices
- [x] Optimized touch targets for better usability
- [x] Made status indicators clickable for quicker attendance management

## Build and Package
- [ ] Run the app to verify everything works correctly
- [ ] Build a release APK:
  ```
  ./gradlew assembleRelease
  ```
- [ ] Test the APK on a real device before publishing
- [ ] Make sure all new features are working as expected

## GitHub Release
- [ ] Create a new release on GitHub
- [ ] Set the tag to "v1.5"
- [ ] Set the release title to "Version 1.5"
- [ ] Add release notes with all new features and improvements
- [ ] Upload the APK file to the release
- [ ] Publish the release

## Update Testing
- [ ] Install version 1.4 on a test device (if not already installed)
- [ ] Launch the app and verify it detects the version 1.5 update
- [ ] Test the update dialog
- [ ] Click the download button and verify the update downloads correctly
- [ ] Install the update and verify the new version is installed correctly

## Post-release Verification
- [ ] Verify all features work after the update:
  - [ ] Test attendance page responsiveness on different screen sizes
  - [ ] Verify tab functionality works correctly
  - [ ] Test clickable status indicators for attendance toggling
  - [ ] Test responsive layout on a smaller device
- [ ] Verify crash reporting is working correctly
- [ ] Monitor for any post-release issues

## Notes
Remember: The auto-update system will handle all these steps automatically for your users. Once a user with version 1.4 opens the app, they will be prompted to update to version 1.5. 
