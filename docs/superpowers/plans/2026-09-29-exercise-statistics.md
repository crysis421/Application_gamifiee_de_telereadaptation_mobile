# Exercise Statistics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the US-5.3 Android statistics screen with a color-coded interactive muscle body, a global perfection rate, and drill-down statistics by muscle group and exercise.

**Architecture:** Pure Java domain and aggregation classes calculate statistics from the existing exercise catalogue and a replaceable local session repository. An Android ViewModel exposes immutable screen data to a Java/XML Activity, while a custom `MuscleBodyView` draws and selects the front-facing muscle map. The existing bottom navigation gains a third statistics destination.

**Tech Stack:** Java 11, Android SDK 36, AndroidX AppCompat/Lifecycle, Material 3, JUnit 4, XML layouts and vector drawables.

**Spec:** `docs/superpowers/specs/2026-09-29-exercise-statistics-design.md`

## Global Constraints

- Keep the existing catalogue, exercise creation and exercise player behavior intact.
- Use one stylized front-facing mannequin; do not add a rear or 3D model.
- Treat a session score greater than or equal to 90 as perfect.
- Clamp scores to the inclusive range 0–100 and ignore sessions whose exercise ID is unknown.
- Use red for 0–39%, orange for 40–69%, green for 70–100%, and gray when no data exists.
- Use local sample data only; no API, database, camera, or movement analysis belongs in this ticket.
- All visible copy must be stored in Android string resources and remain in French.

## Review Focus

- Empty history: show 0% globally and gray/no-data muscle states without division by zero; covered in Task 1 tests.
- Invalid scores: clamp negative values to 0 and values above 100 to 100; covered in Task 1 tests.
- Unknown exercise IDs: exclude them from global, exercise and muscle calculations; covered in Task 1 tests.
- Muscle selection with no matching sessions: retain the selection and show an explicit empty state; covered in Task 2 tests.
- Boundary taps on the mannequin: return no selection outside a region and the correct group inside each region; covered in Task 3 tests.

---

### Task 1: Statistics domain and calculations

**Files:**
- Create: `app/src/main/java/com/uphf/saes5/ExerciseSession.java`
- Create: `app/src/main/java/com/uphf/saes5/MuscleGroups.java`
- Create: `app/src/main/java/com/uphf/saes5/ExerciseStatistic.java`
- Create: `app/src/main/java/com/uphf/saes5/MuscleStatistic.java`
- Create: `app/src/main/java/com/uphf/saes5/StatisticsSummary.java`
- Create: `app/src/main/java/com/uphf/saes5/StatisticsCalculator.java`
- Create: `app/src/main/java/com/uphf/saes5/StatisticsRepository.java`
- Test: `app/src/test/java/com/uphf/saes5/StatisticsCalculatorTest.java`

**Interfaces:**
- Consumes: `Exercise#getId()`, `Exercise#getName()`, `Exercise#getMuscleGroup()` and lists from `ExerciseRepository#getAll()`.
- Produces: `StatisticsCalculator.calculate(List<Exercise>, List<ExerciseSession>) -> StatisticsSummary`; `StatisticsRepository.getAll() -> List<ExerciseSession>`.

- [ ] **Step 1: Write failing aggregation tests**

Add focused JUnit tests with these exact cases:

- scores `[89, 90, 100]` produce `67%` perfect;
- scores `[50, 51]` produce a rounded average of `51`;
- session scores `-5` and `140` are exposed as `0` and `100`;
- an unknown exercise ID does not affect any count or percentage;
- empty history returns global `0`, zero exercise rows, and five no-data muscle rows;
- the fixed group order is `Dos`, `Pectoraux`, `Bras`, `Tronc`, `Jambes`.

- [ ] **Step 2: Run the tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests com.uphf.saes5.StatisticsCalculatorTest`
Expected: compilation failure because the statistics types do not exist.

- [ ] **Step 3: Implement the immutable domain types and calculator**

`ExerciseSession(String exerciseId, long completedAtEpochMillis, int qualityScore)` clamps its score. `MuscleGroups.ALL` defines `Dos`, `Pectoraux`, `Bras`, `Tronc`, `Jambes`. `StatisticsSummary` exposes the global percentage plus immutable exercise and muscle statistics lists. Calculate values with integer rounding and stable catalogue/group ordering; groups without exercises or sessions remain present with `hasData == false`.

- [ ] **Step 4: Add representative local sample sessions**

Seed every existing exercise with multiple dated scores spanning red, orange and green states. Return defensive copies from `StatisticsRepository.getAll()`.

- [ ] **Step 5: Run the focused and complete unit suites**

Run: `.\gradlew.bat testDebugUnitTest --tests com.uphf.saes5.StatisticsCalculatorTest`
Expected: PASS.

Run: `.\gradlew.bat test`
Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add exercise statistics calculations`

### Task 2: Statistics screen state and muscle filtering

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/uphf/saes5/StatisticsScreenState.java`
- Create: `app/src/main/java/com/uphf/saes5/StatisticsViewModel.java`
- Test: `app/src/test/java/com/uphf/saes5/StatisticsViewModelTest.java`

**Interfaces:**
- Consumes: `StatisticsCalculator.calculate(...)`, `StatisticsRepository.getAll()` and `ExerciseRepository.getAll()`.
- Produces: `StatisticsViewModel#getState() -> LiveData<StatisticsScreenState>`; `selectMuscle(String)`, `showMuscleMode()`, and `showExerciseMode()`.

- [ ] **Step 1: Add AndroidX core-testing and write failing ViewModel tests**

Use `InstantTaskExecutorRule`. Assert that initial mode is `MUSCLES` with no selection; `showExerciseMode()` yields `EXERCISES`; selecting `Jambes` leaves only the `Squat` and `Fentes` rows; selecting `Jambes` again clears the filter; selecting `Bras` retains `Bras` as selected and yields an empty exercise list.

- [ ] **Step 2: Run the tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests com.uphf.saes5.StatisticsViewModelTest`
Expected: compilation failure because the ViewModel and state do not exist.

- [ ] **Step 3: Implement immutable `StatisticsScreenState` and `StatisticsViewModel`**

The state contains the summary, display mode enum, selected muscle name and currently visible exercise statistics. Every selection or mode change publishes a new state.

- [ ] **Step 4: Run focused and complete unit suites**

Run the focused ViewModel test and then `.\gradlew.bat test`.
Expected: PASS for both.

- [ ] **Step 5: Commit**

Commit message: `feat: expose statistics screen state`

### Task 3: Interactive muscle body

**Files:**
- Create: `app/src/main/java/com/uphf/saes5/MuscleBodyGeometry.java`
- Create: `app/src/main/java/com/uphf/saes5/MuscleBodyView.java`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/uphf/saes5/MuscleBodyGeometryTest.java`

**Interfaces:**
- Consumes: muscle percentages keyed by `Dos`, `Pectoraux`, `Bras`, `Tronc`, `Jambes`; optional selected group.
- Produces: `MuscleBodyGeometry.groupAt(float normalizedX, float normalizedY) -> String`; `MuscleBodyView#setMuscleStatistics(List<MuscleStatistic>)`; `setSelectedMuscle(String)`; `setOnMuscleSelectedListener(...)`.

- [ ] **Step 1: Write failing normalized hit-region tests**

Assert that normalized representative points map as follows: `(0.50, 0.19)` to `Dos`, `(0.50, 0.30)` to `Pectoraux`, `(0.24, 0.38)` to `Bras`, `(0.50, 0.47)` to `Tronc`, `(0.43, 0.78)` to `Jambes`, and `(0.05, 0.05)` to no group. Boundary points use inclusive left/top and exclusive right/bottom edges.

- [ ] **Step 2: Run the geometry test and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests com.uphf.saes5.MuscleBodyGeometryTest`
Expected: compilation failure because `MuscleBodyGeometry` does not exist.

- [ ] **Step 3: Implement geometry and verify GREEN**

Keep all hit testing in pure normalized geometry so it is independent of screen size and directly testable.

- [ ] **Step 4: Implement `MuscleBodyView`**

Draw a neutral silhouette, distinct muscle regions, selection outline and compact legend using theme-safe colors. Map touches through `MuscleBodyGeometry`, emit only valid selections, and expose a French accessibility summary.

- [ ] **Step 5: Run all unit tests and compile Android resources**

Run: `.\gradlew.bat test assembleDebug`
Expected: PASS and `app/build/outputs/apk/debug/app-debug.apk` created.

- [ ] **Step 6: Commit**

Commit message: `feat: add interactive muscle progress view`

### Task 4: Statistics Activity, detail lists and navigation

**Files:**
- Create: `app/src/main/java/com/uphf/saes5/StatisticsActivity.java`
- Create: `app/src/main/java/com/uphf/saes5/StatisticsListAdapter.java`
- Create: `app/src/main/res/layout/activity_statistics.xml`
- Create: `app/src/main/res/layout/item_statistic.xml`
- Create: `app/src/main/res/drawable/ic_statistics.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/res/values/ids.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/uphf/saes5/MainActivity.java`
- Modify: `app/src/main/java/com/uphf/saes5/ExerciseCatalogActivity.java`
- Modify: `app/src/main/java/com/uphf/saes5/ExercisePlayerActivity.java`

**Interfaces:**
- Consumes: `StatisticsViewModel` state/actions and `MuscleBodyView` setters/listener.
- Produces: a registered `StatisticsActivity`, `nav_statistics` bottom-navigation route and reusable detail rows for muscle/exercise modes.

- [ ] **Step 1: Add the layouts, resources and manifest entry**

Create a scrollable screen containing title, global percentage card, custom mannequin, mode controls, contextual heading/empty state, RecyclerView and bottom navigation. Use resource strings for all copy and content descriptions.

- [ ] **Step 2: Implement the detail adapter and Activity binding**

Render muscle rows with score/session count and exercise rows with average/perfect/session values. Bind muscle touches and mode controls to the ViewModel and refresh all visible state consistently.

- [ ] **Step 3: Wire the statistics destination through every existing bottom navigation listener**

Select `nav_statistics` on the new Activity and route the new item from Home, Catalogue and Player without changing existing routes.

- [ ] **Step 4: Build and run the complete test suite**

Run: `.\gradlew.bat test assembleDebug lintDebug`
Expected: tests and build pass; lint has no new fatal errors.

- [ ] **Step 5: Inspect the APK and repository diff**

Confirm the debug APK exists, only ticket-related tracked files changed, `local.properties` remains ignored, and no user `.idea` changes entered the branch.

- [ ] **Step 6: Commit**

Commit message: `feat: add exercise statistics screen`

### Task 5: End-to-end verification and handoff

**Files:**
- Modify only files required by defects uncovered during verification.
- Test: relevant focused regression test for every defect fixed.

**Interfaces:**
- Consumes: the complete statistics flow.
- Produces: a verified debug APK and user testing instructions.

- [ ] **Step 1: Run clean verification**

Run: `.\gradlew.bat clean test assembleDebug lintDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Verify app flow on an available emulator/device**

Open Home → Statistiques, confirm the global card and mannequin render, tap each muscle region, switch both list modes, and navigate Statistics → Catalogue → Statistics. If no Android target is available, report that manual limitation explicitly while retaining build/test evidence.

- [ ] **Step 3: Fix any discovered defect test-first and repeat verification**

Add a failing focused regression test before each fix, then rerun Step 1.

- [ ] **Step 4: Commit final verification fixes if any**

Commit message: `fix: polish exercise statistics flow`
