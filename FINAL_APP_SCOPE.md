# Smart صنعاء — final native app scope

This branch is the Android-native rebuild of Smart Manager for direct local MikroTik API access.

## Required parity
- Native RTL phone UI matching Smart Manager cards, colors, navigation and pages.
- RouterOS v6 and v7 detection with Hotspot and User Manager backends.
- Dashboard: RouterOS, CPU, uptime, storage, Hotspot, PPPoE and User Manager totals.
- Cards: list, search, profile filtering, add single user, batch vouchers, serial sequence, preview and print/PDF.
- Sessions: active Hotspot/PPP/User Manager sessions and safe removal.
- Expired: snapshot, page/all selection, disable and delete with unknown/un-used cards protected.
- Reports, profiles, templates, backups, database maintenance and settings.
- TLS option, local API connection, session protection and clear confirmations for mutations.

The app must not be called final until these operations are connected to RouterOS and tested on both v6.49.x and v7.x.
