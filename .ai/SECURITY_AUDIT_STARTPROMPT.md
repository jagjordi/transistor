# Security Audit — Start Prompt (Transistor)

Seeded from the **Escapepod** security audit (the pilot) so this run reuses the
findings instead of re-deriving them. Transistor is a near-sibling of Escapepod
(same y20k Android architecture: `helpers/`, `search/`, `BasePlayerService`,
DownloadManager, Media3, Gson) — expect **strong transfer**.

## Kick-off

Open a coding-android session (`activate workbench`) and say:

> Run a security audit of Transistor — input & download vectors — modeled on the
> Escapepod audit. Read this file and the Escapepod artifacts first, then start
> `JOB_PLAN_TASK` (framing = task "F: security audit"). Audit→Remediate split,
> findings in `.ai/SECURITY_AUDIT.md`, gated one-write-per-turn.

**Model: Opus 4.8.** Not Fable 5 — its cyber classifiers false-refuse on security
content and its bug-finding gains exclude security analysis.

## Prior art — read before starting

- `~/Desktop/Codeberg/y20k/escapepod/.ai/SECURITY_AUDIT.md` — findings F-1…F-24 (severity, untrusted path, repro, fix direction)
- `~/Desktop/Codeberg/y20k/escapepod/.ai/WORK_PLAN.md` (`## Archive`) + `WORK_LOG.md` — the audit (P1/P2) and remediation (P3: B1–B10, all committed to `master`)
- memory thread `coding-android/threads/escapepod-security-audit.md`

Escapepod's **B1** built the reusable validators in `NetworkHelper`:
`isAllowedScheme` (http/https, cheap, no DNS — safe on the main/keystroke thread)
and `isSafeRemoteUrl` (scheme allowlist + rejects private/link-local/loopback
resolved IPs + re-validates each redirect hop + connect/read timeouts, off-main).
**Introduce the same validators in Transistor first — they anchor every URL finding below.**

## Task framing (same as Escapepod)

- **Vector 1** = clipboard/keyboard text input: the add-station box + search field.
- **Vector 2** = downloads & interpreting of downloads: M3U/PLS playlists, station/collection JSON, stream ICY metadata, cover images.
- **Intent/IPC surface = out of scope** — note incidental, don't fix. (Transistor's manifest has broad `VIEW` intent-filters for `.m3u/.m3u8/.pls` over http/https. The **parsing** of an opened playlist is in scope; the intent plumbing that delivers it is not.)
- Audit phases emit findings only (no code) into `.ai/SECURITY_AUDIT.md`. Remediate as small `B<n>` iterations, ≤3 files, compile-and-run testable, via `JOB_IMPLEMENT_ITERATION.md`.

## Untrusted-input surface — verify on read, then audit

| Area | File(s) | Transferable findings |
|---|---|---|
| M3U/PLS parse | `helpers/FileHelper.readStationPlaylist` | F-1 unbounded (line/station count, field length); F-2 `substring`/`indexOf` NPE/OOB on malformed `#EXTINF:`/`Title=`/`File=` lines; **F-4 / F-20 weak `line.startsWith("http")` stream-URL check** (case-sensitive, accepts `httpjunk`, misses scheme) |
| Add-station input | `dialogs/FindStationDialog.kt`, `search/DirectInputCheck.kt` | F-20 pasted stream/playlist URL behind a weak check → subscription/SSRF pipeline |
| Search | `search/RadioBrowserSearch.kt`, `search/RadioBrowserResult.kt`, `search/SearchResultAdapter.kt` | F-16 / F-17 Gson null-safety + unguarded `fromJson` (main-thread Volley/callback crash); F-21 query spliced into request URL unencoded; F-22 no query length cap |
| Network / SSRF | `helpers/NetworkHelper.kt` | **F-15** — `createConnection` follows up to 5 redirects with **no host/IP validation** and (verify) **no timeouts**; reached via `downloadPlaylist`, `detectContentType`, `getRadioBrowserServer` |
| Streaming / playback | `BasePlayerService.kt` | **F-19** stream URI → ExoPlayer with no scheme guard — **LIVE and critical here** (radio = streaming); **F-18** no `onPlayerError` → silent playback failure; ICY now-playing metadata via `Html.fromHtml(...).toString()` (~:270) → F-24-style unbounded-parse (it is `toString`'d to plain text, so no link/XSS risk — lower) |
| Download / files | `helpers/DownloadHelper.kt`, `helpers/FileHelper.kt`, `helpers/CollectionHelper.kt`, `helpers/DownloadFinishedReceiver.kt` | F-11 / F-12 station-name & filename path traversal; F-13 free-space gate on enqueue; F-14 wrap DownloadManager cursors in `.use` + guard `moveToFirst()`/column index |
| Backup / import | `helpers/BackupHelper.kt`, `helpers/ImportHelper.kt`, `helpers/UpdateHelper.kt` | F-16 / F-17 JSON restore null-safety; F-7 unbounded / mass-import |

## Key difference from Escapepod

**Streaming is Transistor's primary path.** In Escapepod the streaming branch was
dead code (F-19 was defense-in-depth on a latent path); here **stream URIs flow to
ExoPlayer on the main path**, so F-19 (scheme guard on the media URI) is a real,
high-priority fix, not a formality.

## Does NOT transfer — don't chase

- **XXE / DOCTYPE / entity expansion (F-5, F-9) and the KXmlParser `FEATURE_PROCESS_DOCDECL` trap.** Transistor parses **no XML**: M3U/PLS is line-based text (`BufferedReader`); Radio Browser results + the collection are JSON (Gson). There is no `Xml.newPullParser()`.
- **Feed-HTML tappable links (F-23).** No HTML render sink with `LinkMovementMethod`; stream metadata is `toString`'d to plain text, not rendered as spans.

## Scope bounds (carried from Escapepod, user-approved)

- SSRF defense is **pragmatic**: block private/link-local/loopback + re-check each redirect hop + set timeouts; **not** DNS-rebinding/TOCTOU, **not** exhaustive IPv6.
- Per-file download **byte-cap deferred** (DownloadManager has no clean max-size API) — free-space gate only.
- Prefer a **user-visible Toast** for new error paths; each is a new English `strings.xml` string (translations flow via Weblate).
- Cleartext HTTP is intentional (radio streams) — target parser/decoder robustness, not transport.
