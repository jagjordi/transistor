# Work Log

## Current state

Active task: **Gradle standardisation**, a cross-repo task whose plan lives in `workbench/PLAN_GRADLE_STANDARDISATION.md`, not here. Transistor is phase **P4c**, the last of the three rollout phases, planned 2026-09-09 as **seven iterations, P4c.1–P4c.7**. **P4c.1–P4c.4 done — next is P4c.5** (R8 to the `optimization` block). The `releaseRuntimeClasspath` baseline every later iteration diffs against was captured before the first edit and is **unchanged after P4c.1–P4c.4: 706 lines, sha256 `00ee2a8cc5ab727235510ecf639cd70eee998d8b918d78b9c4b4512a0c4e626a`**, zero `com.android.support` artifacts. Re-derive it with `./gradlew :app:dependencies --configuration releaseRuntimeClasspath -q` rather than trusting a stored copy. **P4c.5 measures against the post-P4c.4 state**: one `app-debug.apk` at **12,450,633 B**, unchanged since P4c.1, **37 actionable tasks**, and — measured for the first time in P4c.4 — `app-release-unsigned.apk` at **3,333,746 B** over 44 actionable tasks. **Transistor is release-frozen as of P4c.4 (2026-09-11)** — the freeze is active, not pending. Its F-Droid recipe seds `build.gradle` and `../settings.gradle`; both files are now `.kts`, so the seds fail rather than no-op and no release tag can go out until the recipe MR rides with it (P6). The freeze started cheap — v4.3.8 / code 115 shipped 2026-08-05 and nothing is queued behind it.

**Transistor keeps the standalone Kotlin plugin** — a documented exception to the shared target spec, not an oversight. `@Parcelize` is inert under AGP's built-in Kotlin; settled on Trackbook and inherited here, see Decisions below.

This is the first work log in this repo. There is no `.ai/WORK_PLAN.md` and there should not be one: the plan for this task is the cross-repo file named above.

## Open issues

- **P6 will have to rewrite Transistor's fdroiddata `prebuild`, not delete it.** The recipe runs two seds — `/media3-cast/d` on `build.gradle` and `/foojay/d` on `../settings.gradle`. The first is **already a no-op on master** (the Cast dependency lives on the `non-free` branch), but both name files that **stopped existing at P4c.4 (2026-09-11)**, and a sed against a missing file fails the build. Same shape as Trackbook's problem, different cause.
- **P5 follows this phase directly.** The `non-free` branch differs from master by **five lines in `app/build.gradle`** (verified 2026-09-09) — the Cast dependency and its plugin lines. After P4c.4 that merge is a small port into the converted `.kts`, not a re-conversion. Do it while the conversion is still fresh.
- **The `builtInKotlin=false` / `newDsl=false` exception expires at AGP 10.** The deprecation the Kotlin DSL surfaces states that `BaseAppModuleExtension` "will be removed in AGP 10.0", so the flag selects a class AGP has scheduled for deletion. At AGP 10 the choice returns: built-in Kotlin, which `@Parcelize` blocks, or hand-written `Parcelable`. Found on Trackbook 2026-09-07; it applies identically here, and the cross-repo plan's phrase "a stable end state" holds under AGP 9.4 only.
- **`.ai/PROJECT_BRIEF.md` justifies `dependenciesInfo.includeInApk = false` with F-Droid reproducibility, which does not hold for this app.** Only Stay Put's recipe carries `Binaries:` and `AllowedAPKSigningKeys`; Transistor is a plain source build that F-Droid signs itself, so output divergence is harmless. The setting is fine and stays — the *reason* recorded in the brief is wrong. Fix when the brief is next touched; not part of this task.
- **`.ai/SECURITY_AUDIT_STARTPROMPT.md` is seeded and not started.** Same file exists in Trackbook. Escapepod's audit findings on XML and download vectors are the ones meant to transfer.

## Decisions

- 2026-09-09 — **P4c inherits P4b's settled decisions rather than re-deriving them.** Why: Trackbook is the app that paid for them, and both apps sit on the same two-app exception, so re-litigating costs builds and risks diverging. Inherited verbatim: (a) **keep `kotlin-android`**, `kotlinOptions { jvmTarget }` and the explicit `kotlin-stdlib-jdk8`, because `kotlin-parcelize` is inert under AGP's built-in Kotlin — the plugin applies without error but generates nothing, and `kotlin-parcelize-runtime` fixes only the import; (b) **keep `android.builtInKotlin=false` and `android.newDsl=false`** in `gradle.properties`, **with the comments already on them** — Transistor is where those comments were written, so unlike Trackbook nothing has to be composed; (c) take the module straight from bare `id '…'` to `alias(libs.plugins…)`, skipping Escapepod's intermediate step; (d) keep `-keepattributes SourceFile,LineNumberTable` in `rules.keep`. Alternative considered and rejected: hand-write `Parcelable` in the model classes to reach the plain spec — app-code work outside this task, with regression risk in code nothing tests automatically.
- 2026-09-09 — **P4c.5 is one measured step, not P4b.6's two.** Why: `-keepattributes SourceFile,LineNumberTable` is **already active** in `app/proguard-rules.pro` here, so moving the file to `src/main/keepRules/rules.keep` introduces no behaviour change and needs no second baseline. P4b.6 had to split because it was activating the directive at the same time. Consequence: P4c.6 takes its size and `mapping.txt` numbers straight from the post-P4c.5 state.
- 2026-09-09 — **Number P4c's iterations 1–7 with no gap.** Why: the extra iteration that made P4b eight was the Kotlin-plugin cluster, which "Resolved by probe" settled before this phase was planned, so there is nothing to cancel in place. P4b.3's cancelled ID stays cancelled *in P4b* — that convention exists to keep cross-references resolving in a file that already published them, and it does not apply to a phase being planned fresh.

## History

### 2026-09-11 — P4c.4 — Convert to Kotlin DSL
- Outcome: done
- Files changed:
    - `settings.gradle` → `settings.gradle.kts`
    - `build.gradle` → `build.gradle.kts`
    - `app/build.gradle` → `app/build.gradle.kts`
- Notes: **One attempt, no retry** — the retry P4b.5 spent on Trackbook was pre-paid by the plan predicting the `kotlinOptions` move. Debug APK **12,450,633 B**, unchanged from P4c.1; `releaseRuntimeClasspath` byte-identical for the **fourth** time (706 lines, `00ee2a8c…`). **First release build of the phase: `app-release-unsigned.apk` at 3,333,746 B** — the number P4c.5 baselines against. `assembleRelease` runs 44 actionable tasks to debug's 37. **Device test passed (y20k): install, adding stations, playback.**
    - **`settings.gradle.kts` and `build.gradle.kts` came out byte-identical to Trackbook's** — `diff` returns nothing for either. Voxscribe differs only by `rootProject.name`, which Transistor has never carried and none was added.
    - `kotlinOptions { jvmTarget = '17' }` left `android` for a top-level `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }` with `import org.jetbrains.kotlin.gradle.dsl.JvmTarget` at the head — Trackbook's placement and spelling. **The two Groovy space-assignment deprecations are retired** with the file format, as predicted.
    - Remaining warnings are the two-app exception only — `builtInKotlin=false`, `newDsl=false`, KGP's deprecated-plugin notice. No `e:` errors, so the plan's "grep `> Configure project` for `e:`" precaution never got used.
    - **`--warning-mode all` reports nothing when the configuration cache hits** — the configuration phase never runs, so there is nothing to warn about. It needs `--no-configuration-cache` to be meaningful. Cost one confused re-run here; worth knowing for P4c.6.
    - **The module file reads as delete+add at git's default 50% similarity threshold**, and as a rename from 30% down; the other two are clean renames. `git log --follow` still traces it.
    - The commented-out debug shrinking block was converted along with the live code (`//            isMinifyEnabled = true`), so uncommenting it still works. Trackbook dropped those lines instead; Transistor keeps them.

### 2026-09-11 — Quick fix — `gradlew` executable
- Outcome: done
- Files changed:
    - `gradlew` (mode only, `100644 → 100755`)
- Notes: `c68b876a`. **One move, not the two predicted**: `core.filemode` is `true` in this clone, so a plain `chmod +x` fixed the working file and gave git the mode change to record — `git update-index --chmod=+x` was never needed, and the staged version of it found on 2026-09-11 was the harder path to the same result. `./gradlew -v` runs without the `sh` prefix. **Builds from P4c.4 on drop the workaround.** Transistor now matches Trackbook, Escapepod, Stay Put and Voxscribe at `100755`. The `non-free` branch still carries `100644` and picks this up at the P5 merge.

### 2026-09-11 — P4c.3 — Repositories into settings, root reduced, module aliases
- Outcome: done
- Files changed:
    - `settings.gradle`
    - `build.gradle`
    - `app/build.gradle`
- Notes: One attempt, no retry. **38 → 37 actionable tasks**, the drop the plan predicted, and `clean` survives its own deletion — the base plugin supplies it, so removing the legacy `task clean(type: Delete)` costs nothing. `app-debug.apk` **12,450,633 B**, unchanged from P4c.1/P4c.2; `releaseRuntimeClasspath` byte-identical to the baseline for the **third** time (706 lines, sha256 `00ee2a8c…`), `com.android.support` still zero.
    - **`settings.gradle` was already edited and uncommitted in the working tree when this session started** — not committed, not logged, not mentioned in the plan. Verified against the three sibling apps before adopting it: identical byte-for-byte apart from Groovy's `include ':app'`. Adopted unchanged rather than reverted and redone. **The lesson is the state, not the edit: a correct change nobody recorded is indistinguishable from an abandoned experiment until it is diffed against a sibling.**
    - Root `build.gradle` is now Trackbook's file with `kotlin.android` where Escapepod has `ksp`. It **gained a trailing newline** Transistor never had, so the diff shows `\ No newline at end of file` on the removed side.
    - The module's `kotlin-parcelize` **stays a bare `id`** and needs no catalog alias — the root `plugins` block puts KGP on the shared classpath, settled in P4b.4. The two plugin aliases P4c.2 left declared and unreferenced are now consumed, closing that loop.
    - `grep -r foojay` matches only `gradle/gradle-daemon-jvm.properties`, which stays, and this log's own prose about it.
    - **A staged `git update-index --chmod=+x gradlew` was also found at session start** and was unstaged to keep an out-of-scope mode change out of this diff. `gradlew` is still `100644`; the phase continues under `sh gradlew`.

### 2026-09-09 — P4c.2 — Version catalog
- Outcome: done
- Files changed:
    - `gradle/libs.versions.toml` (new)
    - `app/build.gradle`
- Notes: One attempt, no retry. **18 versions, 21 libraries, 2 plugins** — the counts the plan predicted, verified against the module first. `clean assembleDebug` succeeded, still **38 actionable tasks**, and `app-debug.apk` came out at **12,450,633 B, byte-identical to P4c.1's**. `releaseRuntimeClasspath` byte-identical to the P4c.1 baseline for the second time (706 lines, sha256 `00ee2a8c…`, empty diff). Only `versionName "4.3.8"` remains as a literal in `app/build.gradle`, as specified; AGP's and Kotlin's literals stay in the root `buildscript`, which sees no catalog accessors.
    - **Escapepod's catalog covered 17 of the 21 aliases name-for-name at identical versions**, so almost nothing was invented. Four exceptions: `constraintlayout` stays on Transistor's **2.2.2** (Escapepod is on 2.2.1 — this iteration changes no versions); `androidx-media3-exoplayer-hls` is new, since Escapepod has no HLS; `kotlin-stdlib-jdk8` takes **Trackbook's** naming and `kotlin` version ref; and `kotlin = "2.3.21"`, not Trackbook's 2.3.10. `[versions]` follows Escapepod's ordering, `[libraries]` too with the stdlib first as in Trackbook, so all five catalogs stay comparable.
    - **`libs.androidx.media3.exoplayer` and `libs.androidx.media3.exoplayer.hls` coexist** — an alias that is a strict prefix of another. Gradle generates the nested accessor and the leaf value together; no precedent for this in the sibling catalogs, and it needed no special handling.
    - **Byte-identical resolution is what verifies the alias set**, not reading the file: a misnamed accessor either fails configuration or silently resolves elsewhere, and 706 unchanged lines rule both out for all 21.
    - **The Gradle 10 hard error found in P4c.1 is retired**, as predicted — zero `implicit lookup` hits under `--warning-mode all`. `$kotlinVersion` was the module's only read of a root `ext` property, so the catalog closed it as a side effect and it needed no iteration of its own.
    - `agp = "9.4.0"`, `kotlin`, and the `android-application` / `kotlin-android` plugin aliases are **declared and unreferenced**, waiting for P4c.3. The commented-out `fileTree` line went with the three `def` locals; dependency order and blank-line grouping are unchanged, so the diff is coordinate-for-coordinate.

### 2026-09-09 — P4c.1 — Gradle 9.6.1 + AGP 9.4.0
- Outcome: done
- Files changed:
    - `gradle/wrapper/gradle-wrapper.properties`
    - `build.gradle`
    - `.gitignore`
- Notes: Three lines, one attempt, no retry. Wrapper `9.6.0` → `9.6.1`, root `ext.gradlePluginVersion` `9.2.1` → `9.4.0`, `kotlinVersion` untouched at 2.3.21, `/app/release` appended to `.gitignore`. Baseline captured **before the first edit**, as the plan required: **706 lines, sha256 `00ee2a8c…`**. `clean assembleDebug` succeeded in 58s, **38 actionable tasks**, one `app-debug.apk` at **12,450,633 B** — no splits, so the single-file shape the plan predicted. `gradlew -v` confirms Gradle 9.6.1; `buildEnvironment` confirms **AGP 9.4.0**, which pulls KGP 2.2.10 and is forced up to **2.3.21** by the root `ext`.
    - **The AGP bump moved no dependency.** `releaseRuntimeClasspath` is byte-identical before and after — same 706 lines, same sha256, empty diff. Second app where the toolchain step is *itself* proven pure (P4b.1 was the first), so any resolution drift in P4c.2–.7 is attributable to that iteration rather than to AGP.
    - **`com.android.support` count on `releaseRuntimeClasspath`: zero.** That is P4c.6's `enableJetifier` check, answered here on the real tree rather than inherited. The plan called Transistor's pre-existing `enableJetifier=false` a provable no-op; this is the proof, and it matches P4b.1's result on Trackbook.
    - Editing `distributionUrl` by hand left `gradle/gradle-daemon-jvm.properties` untouched — still JetBrains vendor, `toolchainVersion=21`, ten foojay `toolchainUrl` lines. **P4c.3's `grep -r foojay` assumes exactly that**, so it is safe.
    - `.gitignore` **has no trailing newline** and was kept that way, so the diff shows `/captures` as touched while its content is unchanged — a `\ No newline at end of file` marker moving down one line. `app/release/` (`baselineProfiles/`, `output-metadata.json`) has dropped out of `git status`.
    - **Deprecation inventory under `--warning-mode all`, since it maps onto later iterations.** Expected and permanent: `builtInKotlin=false`, `newDsl=false`, and KGP's own "no longer required since AGP 9.0" notice — the two-app exception announcing itself, and the running signal that it expires at AGP 10. Retired by P4c.4: Groovy space-assignment at `app/build.gradle:8` (`namespace`) and `:42` (`shrinkResources`). Not ours: `testVariants` / `unitTestVariants` obsolete, from AGP's own legacy variant API.
    - **One deprecation is new and is not in P4b.1's notes: implicit property lookup in a parent project**, at `app/build.gradle:79` — `implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"` resolves `kotlinVersion` from the root project, which Gradle says "will fail with an error in Gradle 10". **P4c.2 retires it for free**, because the catalog alias replaces that `ext` reference. Worth recording as a second reason for the version-catalog move, not just tidiness.
    - **`gradlew` is not executable in this repo** — see Open issues. Every build in this phase runs as `sh gradlew`.
