# 5x5 Tactics Engine

## Project Overview
A 5x5 tactical board game featuring a game engine, WebSocket server, and cross-platform client.

## Tech Stack
- Java 17
- Maven 3.8+
- JUnit 5.10
- Jetty 11 (WebSocket Server)
- LibGDX 1.12+ (Client)
- TeaVM 0.9+ (Web Export - legacy)
- GWT 2.10.0 (Web Export - new)
- java-websocket 1.5+ (WebSocket Client)

## Project Structure
```
├── docs/                    # Specification documents (must read)
├── src/main/java/com/tactics/
│   ├── engine/              # Game core (pure Java, no external dependencies)
│   │   ├── model/           # GameState, Unit, Board, Position
│   │   ├── action/          # Action, ActionType
│   │   ├── buff/            # BuffInstance, BuffModifier, BuffFlags
│   │   ├── rules/           # RuleEngine, ActionValidator, ActionExecutor
│   │   ├── skill/           # SkillExecutor, SkillDefinition, SkillRegistry
│   │   └── util/            # GameStateFactory, Serializer, RngProvider
│   └── server/              # WebSocket server
│       ├── core/            # Match, MatchService, MatchRegistry
│       ├── dto/             # Message objects
│       └── ws/              # WebSocket handlers
├── src/test/java/           # Tests (1010 passing)
├── client/                  # Legacy web frontend (HTML/CSS/JS)
└── client-libgdx/           # LibGDX Client (NEW - Gradle subproject)
    ├── core/                # Shared client code
    │   └── src/main/java/com/tactics/client/
    │       ├── screens/     # DraftScreen, BattleScreen, ResultScreen
    │       ├── ui/          # UI components (buttons, dialogs)
    │       ├── net/         # WebSocket client
    │       └── render/      # Board, unit rendering
    ├── desktop/             # Desktop launcher (dev/test)
    ├── android/             # Android launcher
    ├── teavm/               # Web export (TeaVM - legacy)
    ├── html/                # Web export (GWT - new, official LibGDX)
    └── assets/              # Sprites, fonts (placeholder first)
```

## Package Names
| Module | Package |
|--------|---------|
| Engine | `com.tactics.engine` |
| Server | `com.tactics.server` |
| **Client** | `com.tactics.client` |

## Server Configuration
| Setting | Value |
|---------|-------|
| WebSocket URL | `ws://localhost:8080/match` |
| Start Server | `mvn exec:java` |

## Common Commands
```bash
# Engine/Server (Maven)
mvn compile                    # Compile
mvn test                       # Run all tests
mvn test -Dtest=ClassName      # Run single test class
mvn test -Dtest=Class#method   # Run single test method
mvn clean package              # Package
mvn exec:java                  # Start server

# LibGDX Client (Gradle)
cd client-libgdx
./gradlew desktop:run          # Run desktop version
./gradlew android:assembleDebug # Build Android APK
./gradlew teavm:build          # Build web version (TeaVM)
./gradlew html:compileGwt      # Build web version (GWT)
./gradlew html:dist            # Create GWT distribution
```

---

## 🗓️ Development Roadmap

**Current Phase**: Phase E - LibGDX + TeaVM Client (1010 tests passing)

| Phase | Description | Est. Time | Status |
|-------|-------------|-----------|--------|
| ~~C~~ | ~~Complete Remaining Tests~~ | ~~6-10 hours~~ | ✅ Complete |
| ~~D~~ | ~~End-to-End Testing~~ | ~~4-6 hours~~ | ✅ Complete |
| **E** | **LibGDX + TeaVM Client** | 35-45 hours | ⬜ Pending |
| F | Supabase Integration | 8-12 hours | ⬜ Pending |

### Phase E Tasks (Current)

| Task | Description | Est. Time | Priority | Status |
|------|-------------|-----------|----------|--------|
| E-1 | LibGDX + TeaVM Project Setup | 3-4 hours | 🔴 High | ✅ Complete |
| E-2 | WebSocket Client | 4-6 hours | 🔴 High | ✅ Complete |
| E-3 | Screen Framework | 4-6 hours | 🔴 High | ✅ Complete |
| E-4 | Draft UI (Placeholder) | 6-8 hours | 🔴 High | ✅ Complete |
| E-5 | Battle UI (Placeholder) | 8-10 hours | 🔴 High | ✅ Complete |
| E-CH | Code Health Check | 1-2 hours | 🟡 Medium | ✅ Complete |
| E-R1 | Code Health: Split BattleScreen | 1-2 hours | 🟡 Medium | ✅ Complete |
| E-R2 | Code Health: Centralize Colors | 0.5 hours | 🟡 Medium | ✅ Complete |
| E-6 | Web Export Test (TeaVM) | 2-3 hours | 🔴 High | ✅ Complete |
| E-6.5 | GWT Web Export (Official LibGDX) | 2-3 hours | 🔴 High | ✅ Complete |
| E-7 | Android Export | 2-3 hours | 🟡 Medium | ⬜ Pending |
| E-8 | Animations & Effects | 8-10 hours | 🟡 Medium | ⬜ Pending |
| E-9 | Art Asset Replacement | TBD | 🟢 Low | ⬜ Pending |
| E-10 | Ads Integration | TBD | 🟢 Low | ⬜ Pending |

**📄 Full roadmap details**: `/docs/docs_ROADMAP.md`

---

## 🚧 Completed Development Phases

| Status | Phase | Description |
|--------|-------|-------------|
| ✅ | Phase 0-3 | V1/V2/V3 Foundation, Guardian |
| ✅ | Phase 4 | Hero Skill System (18 skills) |
| ✅ | Phase 5 | Death Choice Flow |
| ✅ | Phase 6 | Draft Phase |
| ✅ | Phase 7 | Timer System (108 tests) |
| ✅ | Phase 8 | Unit-by-Unit Turn System |
| ✅ | Code Health | All refactoring complete |
| ✅ | Phase C | Test Coverage (+160 tests) |
| ✅ | Phase D | E2E Testing (+88 tests) |

**Test Status**: 1010 tests passing

---

## 🎮 LibGDX Development Guidelines

### Target Platforms
| Platform | Priority | Technology | Status |
|----------|----------|------------|--------|
| **Web** | 🔴 High | GWT (official) | ✅ Primary target |
| **Web** | 🟡 Medium | TeaVM | ✅ Alternative |
| **Desktop** | 🟡 Medium | LWJGL | ✅ Dev/testing |
| **Android** | 🟡 Medium | Native | ⬜ Secondary |
| **iOS** | ❌ None | - | Not supported (RoboVM deprecated) |

### Development Principles
1. **Web First** - Test in browser frequently
2. **Placeholder Graphics** - Colored rectangles first, art later
3. **Platform Abstraction** - Use interfaces for platform-specific code
4. **No Engine Dependency** - Client only uses WebSocket, not engine directly

### Graphics Strategy
```
Phase 1 (E-1 to E-6): Placeholder
- Units: Colored rectangles (Red=enemy, Blue=ally)
- Board: Simple grid lines
- UI: Basic shapes with text labels

Phase 2 (E-9): Real Assets
- Replace placeholders with actual sprites
- Add visual polish
```

### Ads Integration (Deferred)
```java
// Define interface now, implement later
public interface AdsController {
    void showRewardedAd(Runnable onReward);
    void showInterstitial();
    boolean isAdReady();
}

// Use NoOp implementation during development
public class NoOpAdsController implements AdsController {
    public void showRewardedAd(Runnable onReward) { onReward.run(); }
    public void showInterstitial() { /* no-op */ }
    public boolean isAdReady() { return false; }
}
```

---

## 📦 LibGDX Project Dependencies

### Core Dependencies
```groovy
// build.gradle (core module)
dependencies {
    api "com.badlogicgames.gdx:gdx:$gdxVersion"
    api "org.java-websocket:Java-WebSocket:1.5.4" // Desktop/Android only
}
```

### TeaVM Dependencies
```groovy
// build.gradle (teavm module)
dependencies {
    implementation "org.teavm:teavm-classlib:$teavmVersion"
    implementation "org.teavm:teavm-jso:$teavmVersion"
    implementation "org.teavm:teavm-jso-apis:$teavmVersion"
}
```

### GWT Dependencies
```groovy
// build.gradle (html module)
dependencies {
    implementation project(":core")
    implementation "com.badlogicgames.gdx:gdx:$gdxVersion:sources"
    implementation "com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion"
    implementation "com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion:sources"
}
```

### Platform-Specific WebSocket
| Platform | Library | Notes |
|----------|---------|-------|
| Desktop | java-websocket | Standard Java library |
| Android | java-websocket | Same as desktop |
| Web/TeaVM | Browser WebSocket | Via JSBody annotation |
| Web/GWT | Browser WebSocket | Via JSNI |

---

## 📚 Key Reference Documents

### Game Rules (V3 - Current)
| Document | Description |
|----------|-------------|
| `/docs/GAME_RULES_V3.md` | Core gameplay rules |
| `/docs/BUFF_SYSTEM_V3.md` | 6 BUFF types |
| `/docs/SKILL_SYSTEM_V3.md` | 18 hero skills |
| `/docs/GAME_FLOW_V3.md` | Complete game phases |

### Protocol & Integration
| Document | Description |
|----------|-------------|
| `/docs/WS_PROTOCOL_V1.md` | WebSocket message format |
| `/docs/docs_ROADMAP.md` | Full development roadmap |

---

## 🏗️ Architecture Overview

### Layer Separation
```
┌─────────────────────────────────────────────────┐
│  CLIENT (client-libgdx/)                        │  ← UI + WebSocket only
│  ├── screens/ → Game screens                    │
│  ├── net/     → WebSocket client                │
│  └── render/  → Visual rendering                │
├─────────────────────────────────────────────────┤
│  SERVER (server/)                               │  ← Orchestration
│  ├── ws/   → WebSocket handlers                 │
│  ├── core/ → Match, Timer management            │
│  └── dto/  → Data transfer objects              │
├─────────────────────────────────────────────────┤
│  ENGINE (engine/)                               │  ← Pure game logic
│  ├── rules/ → RuleEngine (facade)               │
│  ├── skill/ → SkillExecutor (per hero)          │
│  └── model/ → GameState, Unit, Buff             │
└─────────────────────────────────────────────────┘
```

### Layer Rules
| From | Can Access | Cannot Access |
|------|------------|---------------|
| CLIENT | WebSocket only | SERVER, ENGINE directly |
| SERVER | ENGINE | CLIENT internals |
| ENGINE | Nothing external | SERVER, CLIENT |

### Client-Server Communication
```
CLIENT                          SERVER
  │                               │
  │──── JOIN_MATCH ──────────────>│
  │<─── GAME_STATE ───────────────│
  │                               │
  │──── DRAFT_PICK ──────────────>│
  │<─── GAME_STATE ───────────────│
  │                               │
  │──── ACTION (MOVE/ATTACK) ────>│
  │<─── GAME_STATE ───────────────│
  │                               │
  │<─── GAME_OVER ────────────────│
```

---

## 🎮 V3 Key Concepts

### Victory Condition
**Kill the enemy Hero = Win** (minion deaths don't end the game)

### Team Composition
- 1 Hero (6 classes: Warrior, Mage, Rogue, Cleric, Huntress, Duelist)
- 2 Minions (TANK, ARCHER, ASSASSIN)

### 6 BUFF Types
| BUFF | Effect | Special |
|------|--------|---------|
| POWER | +3 ATK, +1 HP | 1-hit obstacle destroy |
| LIFE | +3 HP | — |
| SPEED | -1 ATK | 2 actions per round |
| WEAKNESS | -2 ATK, -1 HP | — |
| BLEED | -1 HP/round | Damage over time |
| SLOW | — | Actions delayed 1 round |

### Timer System
| Timer | Duration | Timeout |
|-------|----------|---------|
| Action | 10s | Hero -1 HP + auto END_TURN |
| Death Choice | 5s | Default Obstacle |
| Draft | 60s | Random selection |

---

## 🔧 Development Guidelines for Claude CLI

### Architecture Principles
1. **High Cohesion** - Each class has ONE clear responsibility
2. **Low Coupling** - Minimize dependencies, use injection
3. **Layer Separation** - ENGINE → SERVER → CLIENT (via WebSocket)
4. **Immutability** - GameState, Unit are immutable
5. **Platform Abstraction** - Use interfaces for platform-specific code

### Code Standards
| Rule | Limit |
|------|-------|
| Class size | < 500 lines (must split if > 1000) |
| Method size | < 30 lines (max 50) |
| Parameters | Max 3-4 per method |
| Duplicate code | Extract after 3 occurrences |

### Common Patterns

| Pattern | Purpose | Project Example |
|---------|---------|-----------------|
| **Facade** | Simplify complex subsystems | `RuleEngine` |
| **Strategy/Dispatch** | Swappable algorithms by type | `SkillExecutor` |
| **Immutable + withX()** | State immutability | `Unit.withHp()` |
| **Platform Factory** | Platform-specific implementations | `WebSocketFactory.create()` |
| **Interface Abstraction** | Deferred implementation | `AdsController` |

### Testing (TDD)
```
1. Check test plan in docs/
2. Write failing test first
3. Write minimal code to pass
4. Refactor if needed
5. Run: mvn test (engine) or ./gradlew test (client)
```

### Code Review Checklist
- [ ] Layer boundaries respected
- [ ] Single responsibility per class
- [ ] Methods < 30 lines
- [ ] All tests pass
- [ ] No duplicate code
- [ ] Platform abstraction used where needed

---

## 📊 Test Coverage Summary

**Total: 1010 tests passing**

### By Feature
| Feature | Tests |
|---------|-------|
| Core Engine (validation, actions) | ~120 |
| BUFF System | ~100 |
| Skill System (18 skills) | ~200 |
| Guardian Passive | 16 |
| Draft Phase | 110 |
| Timer System | 108 |
| Unit Turn System | 38 |
| Serialization | ~40 |
| WebSocket/Server | ~50 |
| E2E Tests | 88 |

---

## 🚀 Quick Start Commands for Claude CLI

### E-1: LibGDX + TeaVM Project Setup
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

Do NOT implement game logic yet - just project structure.
```

### E-2: WebSocket Client
```
Implement WebSocket client with platform abstraction.

Create in client-libgdx/core/src/main/java/com/tactics/client/net/:

1. IWebSocketClient.java (interface)
   - connect(String url)
   - send(String message)
   - disconnect()
   - setListener(WebSocketListener listener)
   - isConnected()

2. WebSocketListener.java (interface)
   - onConnected()
   - onMessage(String message)
   - onDisconnected()
   - onError(String error)

3. DesktopWebSocketClient.java
   - Uses java-websocket library
   - Auto-reconnect with exponential backoff

4. TeaVMWebSocketClient.java
   - Uses browser WebSocket via @JSBody
   - Same interface as desktop

5. WebSocketFactory.java
   - create() returns platform-specific implementation

6. GameMessageHandler.java
   - Parse JSON messages per WS_PROTOCOL_V1.md
   - Dispatch to appropriate handlers

Server URL: ws://localhost:8080/match
Test with: mvn exec:java (in project root)
```

### E-3: Screen Framework
```
Create screen framework with placeholder UI.

Create in client-libgdx/core/src/main/java/com/tactics/client/screens/:

1. BaseScreen.java
   - Common functionality (input, camera, batch)
   - Abstract render() and update() methods

2. ScreenManager.java
   - Screen stack management
   - Transitions between screens

3. ConnectScreen.java
   - Server URL input (or hardcoded for now)
   - "Connect" button
   - Connection status display

4. DraftScreen.java
   - Placeholder layout for hero/minion selection
   - Timer display area

5. BattleScreen.java
   - Placeholder 5x5 grid
   - Action button areas

6. ResultScreen.java
   - Victory/Defeat display
   - "Play Again" button

Use colored rectangles for all UI elements.
Follow GAME_FLOW_V3.md for screen transitions.
```

---

*Last updated: 2025-12-10*
*Tests: 1010 passing*
*Current Phase: E - LibGDX + TeaVM/GWT Client*
*GWT Build: ✅ Successful (html/build/dist/ ~5MB)*
