# Development Roadmap

This document tracks the upcoming development phases for the 5x5 Tactics Engine.

**Last Updated**: 2025-12-09
**Current Tests**: 1010 passing
**Current Phase**: Phase E - LibGDX + TeaVM Client (Pending)

---

## Overview

| Phase | Description | Est. Time | Status |
|-------|-------------|-----------|--------|
| ~~C~~ | ~~Complete Remaining Tests~~ | ~~6-10 hours~~ | ✅ Complete (922 tests) |
| ~~D~~ | ~~End-to-End Testing~~ | ~~4-6 hours~~ | ✅ Complete (1010 tests) |
| **E** | **LibGDX + TeaVM Client** | 35-45 hours | ⬜ Pending |
| F | Supabase Integration | 8-12 hours | ⬜ Pending |

**Total Estimated Time**: 43-57 hours (~6-8 working days)

---

## Phase C: Complete Remaining Tests ✅ COMPLETE

**Status**: ✅ Complete - 922 tests passing (+160 tests added)

| Task | Description | Tests Added | Status |
|------|-------------|-------------|--------|
| C-1 | handleJoinMatch() refactor | - | ✅ Complete |
| C-2 | SKILL_SYSTEM tests | +107 | ✅ Complete |
| C-3 | BUFF tests | +53 | ✅ Complete |

---

## Phase D: End-to-End Testing ✅ COMPLETE

**Status**: ✅ Complete - 1010 tests passing (+88 tests added)

| Task | Description | Tests Added | Status |
|------|-------------|-------------|--------|
| D-1 | EndToEndTest.java | +25 | ✅ Complete |
| D-2 | WebSocketProtocolTest.java | +36 | ✅ Complete |
| D-3 | ErrorHandlingTest.java | +27 | ✅ Complete |

---

## Phase E: LibGDX + TeaVM Client

**Goal**: Create cross-platform client with Web as primary target.
**Estimated Time**: 35-45 hours

### Target Platforms

| Platform | Priority | Technology | Status |
|----------|----------|------------|--------|
| **Web** | 🔴 High | TeaVM | Primary target |
| **Desktop** | 🟡 Medium | LWJGL | Dev/testing |
| **Android** | 🟡 Medium | Native | Secondary |
| **iOS** | ❌ None | - | Not supported |

### Architecture

```
client-libgdx/
├── core/                    # Shared client code
│   └── src/main/java/com/tactics/client/
│       ├── screens/         # DraftScreen, BattleScreen, ResultScreen
│       ├── ui/              # Buttons, dialogs, HUD
│       ├── net/             # WebSocket client
│       └── render/          # BoardRenderer, UnitRenderer
├── desktop/                 # Desktop launcher
├── android/                 # Android launcher
├── teavm/                   # Web export (TeaVM)
└── assets/                  # Placeholder graphics first
```

### Tasks

| Task | Description | Est. Time | Priority | Status |
|------|-------------|-----------|----------|--------|
| E-1 | LibGDX + TeaVM Project Setup | 3-4 hours | 🔴 High | ⬜ |
| E-2 | WebSocket Client | 4-6 hours | 🔴 High | ⬜ |
| E-3 | Screen Framework | 4-6 hours | 🔴 High | ⬜ |
| E-4 | Draft UI (Placeholder) | 6-8 hours | 🔴 High | ⬜ |
| E-5 | Battle UI (Placeholder) | 8-10 hours | 🔴 High | ⬜ |
| E-6 | Web Export Test (TeaVM) | 2-3 hours | 🔴 High | ⬜ |
| E-7 | Android Export | 2-3 hours | 🟡 Medium | ⬜ |
| E-8 | Animations & Effects | 8-10 hours | 🟡 Medium | ⬜ |
| E-9 | Art Asset Replacement | TBD | 🟢 Low | ⬜ |
| E-10 | Ads Integration | TBD | 🟢 Low | ⬜ |

---

### E-1: LibGDX + TeaVM Project Setup

**Estimated Time**: 3-4 hours

```
Create LibGDX project with TeaVM web support.

Location: client-libgdx/ (inside existing project repo)
Package: com.tactics.client

Requirements:
1. Use gdx-liftoff or manual Gradle setup
2. Modules: core, desktop, android, teavm
3. Dependencies:
   - LibGDX 1.12+
   - TeaVM 0.9+
   - java-websocket 1.5+ (desktop/android)
4. Verify all platforms build:
   - cd client-libgdx && ./gradlew desktop:run
   - ./gradlew android:assembleDebug
   - ./gradlew teavm:build
5. Create "Hello Tactics" screen

Do NOT implement game logic yet.
```

---

### E-2: WebSocket Client

**Estimated Time**: 4-6 hours

```
Implement WebSocket client with platform abstraction.

Location: client-libgdx/core/src/main/java/com/tactics/client/net/

Files:
1. IWebSocketClient.java (interface)
2. WebSocketListener.java (interface)
3. DesktopWebSocketClient.java (java-websocket)
4. TeaVMWebSocketClient.java (browser WebSocket)
5. WebSocketFactory.java (platform detection)
6. GameMessageHandler.java (JSON parsing)

Features:
- Auto-reconnect with exponential backoff
- Message queue when disconnected
- JSON per WS_PROTOCOL_V1.md

Server URL: ws://localhost:8080/match
Test with: mvn exec:java (in project root)
```

---

### E-3: Screen Framework

**Estimated Time**: 4-6 hours

```
Create screen framework with placeholder UI.

Location: client-libgdx/core/src/main/java/com/tactics/client/screens/

Screens:
1. BaseScreen.java - common functionality
2. ScreenManager.java - transitions
3. ConnectScreen.java - server connection
4. DraftScreen.java - hero/minion selection
5. BattleScreen.java - main game
6. ResultScreen.java - victory/defeat

Use colored rectangles for all UI.
Follow GAME_FLOW_V3.md for transitions.
```

---

### E-4: Draft UI (Placeholder)

**Estimated Time**: 6-8 hours

```
Draft phase with placeholder graphics.

UI Elements:
1. Hero selection (6 buttons)
2. Skill preview area
3. Minion selection (3 buttons, pick 2)
4. 60s timer
5. Ready button
6. Opponent status

Interactions:
- Tap hero → select
- Tap minion → add to team
- Ready → send DRAFT_PICK
- GAME_STATE phase=BATTLE → BattleScreen
```

---

### E-5: Battle UI (Placeholder)

**Estimated Time**: 8-10 hours

```
Battle phase with placeholder graphics.

UI Elements:
1. 5x5 board grid
2. Units (colored rectangles)
3. HP bars
4. Action buttons (Move, Attack, Skill, End Turn)
5. Turn indicator
6. 10s timer
7. Unit info panel
8. Death Choice dialog

Interactions:
- Tap unit → select
- Tap tile → move/attack
- Action buttons → send ACTION
- GAME_OVER → ResultScreen
```

---

### E-6: Web Export Test (TeaVM)

**Estimated Time**: 2-3 hours

```
Verify TeaVM web export.

Tests:
1. Build: ./gradlew teavm:build
2. WebSocket connects
3. Full game flow works
4. Test browsers: Chrome, Firefox, Safari

Targets:
- Build size < 5MB
- Load time < 3s
```

---

### E-7: Android Export

**Estimated Time**: 2-3 hours

```
Verify Android build.

Steps:
1. ./gradlew android:assembleDebug
2. Install on device/emulator
3. Test full game flow
4. Verify touch input
```

---

### E-8: Animations & Effects

**Estimated Time**: 8-10 hours

```
Visual feedback (300-500ms animations):
1. Unit movement
2. Attack + damage numbers
3. Skill effects
4. Buff application
5. Unit death
6. Turn transition
7. Timer warning
8. Victory/Defeat
```

---

### E-9: Art Asset Replacement (Deferred)

Replace placeholders with real sprites later.

---

### E-10: Ads Integration (Deferred)

```
Preparation now:
- Define AdsController interface
- Create NoOpAdsController

Implementation later:
- Android: AdMob
- Web: AppLixir or similar
```

---

## Phase F: Supabase Integration

**Goal**: Player accounts, auth, persistent data.
**Estimated Time**: 8-12 hours

| Task | Description | Est. Time | Status |
|------|-------------|-----------|--------|
| F-1 | Supabase setup | 2 hours | ⬜ |
| F-2 | Server Auth/Repository | 4 hours | ⬜ |
| F-3 | Client login UI | 4 hours | ⬜ |
| F-4 | Leaderboard & history | 4 hours | ⬜ |

---

## Milestone Checklist

### Milestone 1: Backend Complete ✅
- [x] All core game logic
- [x] Code Health complete
- [x] 692 tests passing

### Milestone 2: Test Coverage Complete ✅
- [x] Phase C complete (+160 tests)
- [x] Phase D complete (+88 tests)
- [x] 1010 tests passing

### Milestone 3: LibGDX Playable (Web Demo)
- [ ] E-1 ~ E-6 complete
- [ ] **Full game playable in browser**

### Milestone 4: Multi-Platform
- [ ] E-7: Android working
- [ ] E-8: Animations complete

### Milestone 5: Production Ready
- [ ] E-9: Art assets
- [ ] E-10: Ads integrated
- [ ] Phase F: Supabase complete

---

## Progress Log

| Date | Phase | Task | Notes |
|------|-------|------|-------|
| 2025-12-08 | - | Roadmap created | Initial planning |
| 2025-12-09 | C | **Phase C Complete** | 922 tests |
| 2025-12-09 | D | **Phase D Complete** | 1010 tests |
| 2025-12-09 | E | Phase E Planning | LibGDX + TeaVM |

---

*This document should be updated as phases are completed.*
