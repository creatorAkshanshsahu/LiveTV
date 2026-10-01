LiveTgTV Embed Android TV App

This version uses the documented LiveTgTV EMBED endpoints instead of trying to play the raw V1 MPD itself.

V1:
https://livetgtv.lovable.app/embed/{id}

V2:
https://livetgtv.lovable.app/v2/embed/{id}

The app fetches the public catalogue natively, so the old WebView does not need to run the catalogue API JavaScript. Selecting a channel opens the provider's ready-made embed player.

V1 embeds are expected to handle the MPEG-DASH/ClearKey details themselves.
V2 embeds include their server switcher.

Build with GitHub Actions:
1. Upload this project's contents to the repository.
2. Keep .github/workflows/build-apk.yml.
3. Actions -> Build LiveTgTV Embed APK.
4. Download artifact LiveTgTV-Embed-Android9.
5. Extract app-debug.apk and install it.

The app has V1 and V2 tabs, search, D-pad focusable channel buttons, and BACK to return from player to channel list.
