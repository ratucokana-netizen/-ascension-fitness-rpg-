# ASCENSION - Hunter Fitness RPG

A mobile-first, offline-capable workout tracker inspired by RPG leveling mechanics. No copyrighted Solo Leveling assets are included.

## Run in a browser
Serve this folder over localhost or HTTPS (service workers do not work from file://). For example:

```bash
python3 -m http.server 8080
```

Open http://localhost:8080. On Android Chrome, choose **Add to Home screen** / **Install app** to use as a standalone PWA. To install on a phone, host this folder on an HTTPS static website first.

## Build an Android APK with Android Studio

Requirements: Node.js 20+, Android Studio with Android SDK and Java 17.

1. In this directory, run `npm install`.
2. Run `npx cap add android`.
3. Run `npx cap sync android`.
4. Run `npx cap open android`.
5. In Android Studio select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. The debug APK is generally at `android/app/build/outputs/apk/debug/app-debug.apk`.
7. For release distribution, generate a signed APK via Android Studio's **Generate Signed Bundle / APK** wizard.

Note: Capacitor expects a web asset directory separate from the project root in some versions. If sync rejects `webDir: "."`, create a `www` folder, copy `index.html`, `app.js`, `sw.js`, `manifest.webmanifest`, `icon-192.png`, `icon-512.png` into it, change `webDir` to `"www"`, and rerun sync.

## Training rules
Monday Push A; Wednesday Pull A; Thursday Legs A; Friday cycles Push B, Pull B, Legs B each week; Saturday easy run; Tuesday and Sunday rest. Each strength exercise has 3 sets of 8-12 reps. Check off after logging all 3 sets and working weight. Targets rise by the configured percentage every two weeks from the saved starting baseline; actual increases are optional and should be based on technique.

## Storage
All data stays in local browser/WebView storage. Export a JSON backup in Settings. Clearing app storage deletes progress unless backed up.
