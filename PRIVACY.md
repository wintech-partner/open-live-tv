# Privacy

Open Live TV has no account, no analytics, no advertising and no crash reporting.

- Network: when you play a channel the app requests that channel's stream from the broadcaster's server named in `channels.json`. The broadcaster and its CDN can see your IP address and the stream you request, as with any web player. The app contacts nothing else.
- On-device storage: your favorites and the last channel (Android SharedPreferences). `allowBackup` is off.
- Permissions: INTERNET only.
