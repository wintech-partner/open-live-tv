# Open Live TV

A small Android live-TV player for free-to-air streams that broadcasters publish themselves, on their own servers or CDNs, for the public.
No account, no ads, no analytics, no DRM handling, no Google services. Plays with AndroidX Media3 (ExoPlayer), minSdk 23, works on phones, tablets and Android TV.

## What is in the list, and what is not

Included: public-service and international broadcasters whose own site offers the live stream without login, payment or DRM
(ARD family, 3sat, Deutsche Welle, France 24, Euronews, TV5Monde, Al Jazeera, TRT, RTVE).

Not included, on purpose: restreams of other people's streams, pay-TV channels, FAST/aggregator feeds, playlists from third-party collections,
DRM-protected streams, channels under EU broadcasting sanctions, and anything whose terms for third-party players are unclear (for example the SRG SSR API).

This is a selection rule, not legal advice: a broadcaster may change its terms, restrict a stream to some countries or switch it off. The app does nothing to get around geo-blocks.
Entries are in `app/src/main/assets/channels.json`; removal requests from a broadcaster are honoured by deleting the entry.

## Channels (30)

| Channel | Language | Stream host |
|---|---|---|
| Das Erste | Deutsch | `daserste-live.ard-mcdn.de` |
| tagesschau24 | Deutsch | `tagesschau.akamaized.net` |
| ARD-alpha | Deutsch | `mcdn.br.de` |
| NDR Fernsehen | Deutsch | `mcdn.ndr.de` |
| BR Fernsehen | Deutsch | `mcdn.br.de` |
| hr-fernsehen | Deutsch | `hr-live.ard-mcdn.de` |
| WDR Fernsehen | Deutsch | `wdr-live.ard-mcdn.de` |
| 3sat | Deutsch | `zdf-hls-18.akamaized.net` |
| DW | Deutsch | `dwamdstream102.akamaized.net` |
| DW English | English | `dwamdstream102.akamaized.net` |
| DW Español | Español | `dwamdstream104.akamaized.net` |
| DW (عربي) | العربية | `dwamdstream103.akamaized.net` |
| Euronews Deutsch | Deutsch | `cdn-euronews.akamaized.net` |
| Euronews Français | Français | `cdn-euronews.akamaized.net` |
| Euronews English | English | `cdn-euronews.akamaized.net` |
| Euronews Español | Español | `cdn-euronews.akamaized.net` |
| Africanews | English | `cdn-euronews.akamaized.net` |
| France 24 | Français | `live.france24.com` |
| France 24 English | English | `live.france24.com` |
| France 24 Español | Español | `live.france24.com` |
| France 24 (عربي) | العربية | `live.france24.com` |
| TV5Monde Info | Français | `ott.tv5monde.com` |
| TV5Monde (France Belgique Suisse) | Français | `ott.tv5monde.com` |
| Al Jazeera English | English | `live-hls-apps-aje-fa.getaj.net` |
| Al Jazeera (عربي) | العربية | `live-hls-apps-aja-fa.getaj.net` |
| TRT World | English | `tv-trtworld.medya.trt.com.tr` |
| TRT Arabi | العربية | `tv-trtarabi.medya.trt.com.tr` |
| TRT 2 | Türkçe | `tv-trt2.medya.trt.com.tr` |
| TRT Haber | Türkçe | `tv-trthaber.medya.trt.com.tr` |
| RTVE 24 Horas | Español | `ztnr.rtve.es` |

## Build

```
./gradlew assembleRelease     # JDK 17, Android SDK platform 36
```

## Privacy

The app only contacts the stream hosts above when you play a channel (the broadcaster sees your IP address like for any web player). It stores your favorites and last channel on the device. See PRIVACY.md.

## License

Apache License 2.0. AndroidX Media3 is Apache 2.0 as well.
