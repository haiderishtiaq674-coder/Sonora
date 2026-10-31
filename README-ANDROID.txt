SONORA ANDROID - FIRST BUILD

This is a native Android starter app. It asks Android for music-library permission on first launch, then scans audio indexed by Android MediaStore. It does not upload music anywhere.

Build:
- Upload these project files into the Sonora GitHub repository, keeping the folder paths.
- GitHub Actions > Build Sonora Android APK > Run workflow.
- When the run finishes, open its Artifacts section and download Sonora-Android-debug.
- Extract the ZIP and install app-debug.apk on Android. Android may ask you to allow installs from the browser/file manager.

Notes:
- This first native version includes permission request, automatic library scan, song list, play/pause, previous/next, and rescan.
- It is a starter implementation, not yet feature-equivalent to the website. Lock-screen/media notification controls, playlists, favourites, search, and iOS support are future work.
- The existing GitHub Pages website files are not replaced by this project.
