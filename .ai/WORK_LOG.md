# Work Log

## Current state

Active task: **Gradle standardisation**, a cross-repo task whose plan lives in `workbench/PLAN_GRADLE_STANDARDISATION.md`, not here. Transistor is phase **P4c**, the last of the three rollout phases, planned 2026-09-09 as **seven iterations, P4c.1–P4c.7, none started**. Next is **P4c.1** (Gradle 9.6.1 + AGP 9.4.0, `/app/release` into `.gitignore`), which must **capture `./gradlew :app:dependencies --configuration releaseRuntimeClasspath -q` before its first edit** — that output is the baseline every later iteration diffs against, and P4c.6 greps it for `com.android.support`. **P4c.4 release-freezes Transistor**: its F-Droid recipe seds `build.gradle` and `../settings.gradle`, and the Kotlin DSL rename makes both fail rather than no-op, so no release tag until the recipe MR rides with it (P6). The freeze starts cheap — v4.3.8 / code 115 shipped 2026-08-05 and nothing is queued behind it.

**Transistor keeps the standalone Kotlin plugin** — a documented exception to the shared target spec, not an oversight. `@Parcelize` is inert under AGP's built-in Kotlin; settled on Trackbook and inherited here, see Decisions below.

This is the first work log in this repo. There is no `.ai/WORK_PLAN.md` and there should not be one: the plan for this task is the cross-repo file named above.

## Open issues

- **P6 will have to rewrite Transistor's fdroiddata `prebuild`, not delete it.** The recipe runs two seds — `/media3-cast/d` on `build.gradle` and `/foojay/d` on `../settings.gradle`. The first is **already a no-op on master** (the Cast dependency lives on the `non-free` branch), but both name files that stop existing at P4c.4, and a sed against a missing file fails the build. Same shape as Trackbook's problem, different cause.
- **P5 follows this phase directly.** The `non-free` branch differs from master by **five lines in `app/build.gradle`** (verified 2026-09-09) — the Cast dependency and its plugin lines. After P4c.4 that merge is a small port into the converted `.kts`, not a re-conversion. Do it while the conversion is still fresh.
- **The `builtInKotlin=false` / `newDsl=false` exception expires at AGP 10.** The deprecation the Kotlin DSL surfaces states that `BaseAppModuleExtension` "will be removed in AGP 10.0", so the flag selects a class AGP has scheduled for deletion. At AGP 10 the choice returns: built-in Kotlin, which `@Parcelize` blocks, or hand-written `Parcelable`. Found on Trackbook 2026-09-07; it applies identically here, and the cross-repo plan's phrase "a stable end state" holds under AGP 9.4 only.
- **`.ai/PROJECT_BRIEF.md` justifies `dependenciesInfo.includeInApk = false` with F-Droid reproducibility, which does not hold for this app.** Only Stay Put's recipe carries `Binaries:` and `AllowedAPKSigningKeys`; Transistor is a plain source build that F-Droid signs itself, so output divergence is harmless. The setting is fine and stays — the *reason* recorded in the brief is wrong. Fix when the brief is next touched; not part of this task.
- **`.ai/SECURITY_AUDIT_STARTPROMPT.md` is seeded and not started.** Same file exists in Trackbook. Escapepod's audit findings on XML and download vectors are the ones meant to transfer.

## Decisions

- 2026-09-09 — **P4c inherits P4b's settled decisions rather than re-deriving them.** Why: Trackbook is the app that paid for them, and both apps sit on the same two-app exception, so re-litigating costs builds and risks diverging. Inherited verbatim: (a) **keep `kotlin-android`**, `kotlinOptions { jvmTarget }` and the explicit `kotlin-stdlib-jdk8`, because `kotlin-parcelize` is inert under AGP's built-in Kotlin — the plugin applies without error but generates nothing, and `kotlin-parcelize-runtime` fixes only the import; (b) **keep `android.builtInKotlin=false` and `android.newDsl=false`** in `gradle.properties`, **with the comments already on them** — Transistor is where those comments were written, so unlike Trackbook nothing has to be composed; (c) take the module straight from bare `id '…'` to `alias(libs.plugins…)`, skipping Escapepod's intermediate step; (d) keep `-keepattributes SourceFile,LineNumberTable` in `rules.keep`. Alternative considered and rejected: hand-write `Parcelable` in the model classes to reach the plain spec — app-code work outside this task, with regression risk in code nothing tests automatically.
- 2026-09-09 — **P4c.5 is one measured step, not P4b.6's two.** Why: `-keepattributes SourceFile,LineNumberTable` is **already active** in `app/proguard-rules.pro` here, so moving the file to `src/main/keepRules/rules.keep` introduces no behaviour change and needs no second baseline. P4b.6 had to split because it was activating the directive at the same time. Consequence: P4c.6 takes its size and `mapping.txt` numbers straight from the post-P4c.5 state.
- 2026-09-09 — **Number P4c's iterations 1–7 with no gap.** Why: the extra iteration that made P4b eight was the Kotlin-plugin cluster, which "Resolved by probe" settled before this phase was planned, so there is nothing to cancel in place. P4b.3's cancelled ID stays cancelled *in P4b* — that convention exists to keep cross-references resolving in a file that already published them, and it does not apply to a phase being planned fresh.

## History

_None yet._
