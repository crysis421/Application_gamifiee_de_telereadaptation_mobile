# Reference Movement Comparison Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Compare the user's camera pose with the reference stored for the launched exercise and provide a live score plus threshold-based validation.

**Architecture:** Keep Android capture/lifecycle in a dedicated camera controller used by `ExerciseActivity`, and keep pose math in pure Java. Persist the reference identifier and threshold on `Exercise`; feed normalized frames into a bounded angle/DTW engine whose one-shot validation drives the existing session counter.

**Tech Stack:** Java 11, AndroidX CameraX, MediaPipe Holistic, Material 3, JUnit 4, SharedPreferences/JSON.

**Spec:** `docs/superpowers/specs/2026-11-18-reference-movement-comparison-design.md`

## Global Constraints

- Similarity thresholds are integers from 1 to 100; the default is 80.
- Sampling remains at 100 ms and CameraX keeps only the latest frame.
- Existing catalogue JSON and existing normalized movement files remain readable.
- No new network or database dependency.
- Missing references and refused camera permission must not crash the session.

## Review Focus

- A legacy exercise without the new JSON fields loads with reference ID equal to its exercise ID and threshold 80.
- Translation, scale and horizontal mirroring do not materially change an angle score.
- Missing or malformed pose frames are ignored instead of poisoning the rolling comparison.
- A continuously high score cannot increment the repetition counter more than once per captured sequence.
- Activity teardown releases CameraX, MediaPipe and executor resources even during an active analysis.

---

### Task 1: Exercise reference metadata

**Files:**
- Modify: `app/src/main/java/com/uphf/saes5/CatalogueExercice/Exercise.java`
- Modify: `app/src/main/java/com/uphf/saes5/CatalogueExercice/ExerciseRepository.java`
- Modify: `app/src/test/java/com/uphf/saes5/ExerciseRepositoryTest.java`

**Interfaces:**
- Produces: `Exercise#getReferenceMovementId()`, `Exercise#getSimilarityThreshold()`, `Exercise.DEFAULT_SIMILARITY_THRESHOLD`.

- [ ] **Step 1: Write failing repository tests** for round-trip persistence, threshold clamping and legacy JSON defaults.
- [ ] **Step 2: Run `ExerciseRepositoryTest`** and confirm the new assertions fail to compile or fail.
- [ ] **Step 3: Add metadata to `Exercise` and JSON serialization** while keeping all existing constructors compatible.
- [ ] **Step 4: Run `ExerciseRepositoryTest`** and expect all tests to pass.
- [ ] **Step 5: Commit** with `feat: associate reference models with exercises`.

### Task 2: Angle-based similarity math

**Files:**
- Modify: `app/src/main/java/com/uphf/saes5/MovementMath.java`
- Create: `app/src/test/java/com/uphf/saes5/MovementMathTest.java`

**Interfaces:**
- Produces: `float[] MovementMath.angleFeatures(float[] normalizedPose)` and `int MovementMath.angleScore(List<float[]> reference, List<float[]> attempt)`.

- [ ] **Step 1: Write failing tests** for identical poses, translated/scaled poses, mirrored poses, changed elbow/knee angles, time-warped sequences and unusable frames.
- [ ] **Step 2: Run `MovementMathTest`** and verify missing APIs fail.
- [ ] **Step 3: Implement eight joint angles and DTW scoring** with differences normalized over 180 degrees.
- [ ] **Step 4: Run `MovementMathTest`** and expect all tests to pass.
- [ ] **Step 5: Commit** with `feat: compare movements using joint angles`.

### Task 3: Bounded live comparison engine

**Files:**
- Create: `app/src/main/java/com/uphf/saes5/MovementComparisonEngine.java`
- Create: `app/src/main/java/com/uphf/saes5/MovementComparisonResult.java`
- Create: `app/src/test/java/com/uphf/saes5/MovementComparisonEngineTest.java`

**Interfaces:**
- Consumes: `MovementMath.angleFeatures` and `MovementMath.angleScore`.
- Produces: `MovementComparisonEngine(List<float[]> referenceFrames, int threshold)`, `addFrame(float[])`, `reset()`; immutable result getters for live score, movement score, readiness, validity and one-shot repetition validation.

- [ ] **Step 1: Write failing engine tests** for immediate posture score, bounded window, invalid/valid decisions, malformed frames and one validation per sequence.
- [ ] **Step 2: Run `MovementComparisonEngineTest`** and verify missing classes fail.
- [ ] **Step 3: Implement the rolling engine** and clear its sequence buffer after a validation.
- [ ] **Step 4: Run the engine and math tests** and expect all tests to pass.
- [ ] **Step 5: Commit** with `feat: add live movement comparison engine`.

### Task 4: Reference recording tied to the exercise

**Files:**
- Modify: `app/src/main/java/com/uphf/saes5/MovementStorage.java`
- Modify: `app/src/main/java/com/uphf/saes5/HolisticLauncher.java`
- Modify: `app/src/main/java/com/uphf/saes5/HolisticCameraActivity.java`
- Modify: `app/src/main/java/com/uphf/saes5/CatalogueExercice/CreateExerciseActivity.java`
- Modify: `app/src/main/res/layout/activity_create_exercise.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/com/uphf/saes5/ExerciseRepositoryTest.java`

**Interfaces:**
- Consumes: exercise reference ID and threshold metadata.
- Produces: `MovementStorage.exists(Context, String)`, record/test launcher methods taking an explicit reference ID.

- [ ] **Step 1: Extend model tests** so creation/editing preserves the explicit reference and threshold.
- [ ] **Step 2: Run the focused tests** and confirm the new expectations fail.
- [ ] **Step 3: Generate a stable draft exercise ID, bind recording/testing to it and add an 80-default threshold slider.**
- [ ] **Step 4: Move new UI text into resources and expose reference availability in the form.**
- [ ] **Step 5: Run repository tests and `assembleDebug`** and expect success.
- [ ] **Step 6: Commit** with `feat: record references for catalogue exercises`.

### Task 5: Camera pipeline inside the session

**Files:**
- Create: `app/src/main/java/com/uphf/saes5/PoseCameraController.java`
- Modify: `app/src/main/java/com/uphf/saes5/accueil/ui/ExerciseActivity.java`
- Modify: `app/src/main/res/layout/activity_exercise.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `HolisticLandmarkerHelper`, `OverlayView`, `MovementComparisonEngine`, exercise reference metadata.
- Produces: lifecycle-aware pose frame callbacks to `ExerciseActivity` and a score/availability UI.

- [ ] **Step 1: Add `PreviewView`, `OverlayView`, score and reference-status views** while retaining manual buttons as the missing-reference fallback.
- [ ] **Step 2: Implement `PoseCameraController`** with `start()`, `stop()` and a listener receiving result bundles/errors/empty poses.
- [ ] **Step 3: Add permission handling and load the exercise reference in `ExerciseActivity`.**
- [ ] **Step 4: Feed sampled normalized frames to the engine; update the banner and call the existing validated callback only on one-shot validation.**
- [ ] **Step 5: Run unit tests and `assembleDebug`** and expect success.
- [ ] **Step 6: Commit** with `feat: compare live pose during exercise sessions`.

### Task 6: Similarity score in summary and research log

**Files:**
- Modify: `app/src/main/java/com/uphf/saes5/accueil/ui/ExerciseActivity.java`
- Modify: `app/src/main/java/com/uphf/saes5/accueil/ui/SummaryActivity.java`
- Modify: `app/src/main/java/com/uphf/saes5/SessionEntry.java`
- Modify: `app/src/test/java/com/uphf/saes5/SessionLogTest.java`

**Interfaces:**
- Consumes: completed movement scores emitted by the engine.
- Produces: rounded session average or `SessionEntry.SCORE_UNAVAILABLE` when no comparison completed.

- [ ] **Step 1: Write failing log tests** for a measured score and the unavailable fallback.
- [ ] **Step 2: Run `SessionLogTest`** and verify the new flow is not represented yet.
- [ ] **Step 3: Pass the average score to `SummaryActivity` and persist it in `SessionEntry`.**
- [ ] **Step 4: Run `SessionLogTest` and the complete unit suite** and expect success.
- [ ] **Step 5: Commit** with `feat: log exercise similarity scores`.

### Task 7: End-to-end verification

**Files:**
- Verify only; fix only ticket-related findings.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: a tested debug APK and documented manual evidence.

- [ ] **Step 1: Run `test assembleDebug` with the project Java 21 runtime.**
- [ ] **Step 2: Run `lintDebug` and distinguish new findings from the known catalogue tint baseline error.**
- [ ] **Step 3: Install the debug APK on the emulator and verify permission refusal, missing reference fallback, reference recording and live valid/invalid feedback.**
- [ ] **Step 4: Confirm `git diff --check` and inspect the branch diff for unrelated files.**
