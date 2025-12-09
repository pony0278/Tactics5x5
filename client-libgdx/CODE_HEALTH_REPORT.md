# Code Health Report - LibGDX Client

**Date**: 2025-12-09
**Total Java Files**: 14
**Total Lines**: 3,504

---

## 1. Summary Statistics

### File Size Analysis

| File | Lines | Status |
|------|-------|--------|
| BattleScreen.java | 1,034 | 🔴 **CRITICAL** - Needs splitting |
| DraftScreen.java | 577 | 🟡 Medium - Approaching limit |
| GameMessageHandler.java | 321 | 🟢 OK |
| ConnectScreen.java | 311 | 🟢 OK |
| BaseScreen.java | 232 | 🟢 OK |
| TeaVMWebSocketClient.java | 182 | 🟢 OK |
| DesktopWebSocketClient.java | 173 | 🟢 OK |
| ScreenManager.java | 156 | 🟢 OK |
| ResultScreen.java | 138 | 🟢 OK |
| HelloTacticsScreen.java | 111 | 🟢 OK (legacy) |
| WebSocketFactory.java | 72 | 🟢 OK |
| IWebSocketClient.java | 51 | 🟢 OK |
| TacticsGame.java | 42 | 🟢 OK |
| WebSocketListener.java | 30 | 🟢 OK |

### Thresholds
- 🟢 OK: < 300 lines
- 🟡 Medium: 300-500 lines
- 🔴 Critical: > 500 lines

---

## 2. Issues Found

### 🔴 HIGH Priority Issues

#### H-1: BattleScreen.java is 1,034 lines (Critical)

**Problem**: BattleScreen combines too many responsibilities:
- Grid rendering (~45 lines)
- Unit rendering (~40 lines)
- HP bar rendering (~30 lines)
- Buff rendering (~35 lines)
- Action buttons (~65 lines)
- Unit info panel (~60 lines)
- Death choice dialog (~50 lines)
- Turn indicator (~40 lines)
- Input handling (~150 lines)
- WebSocket listener callbacks (~80 lines)
- Action mode logic (~80 lines)

**Recommended Split**:
1. Extract `BoardRenderer.java` - grid, units, HP bars, buffs (~150 lines)
2. Extract `BattleUI.java` - action buttons, info panel, turn indicator (~180 lines)
3. Extract `DeathChoiceDialog.java` - death choice modal (~80 lines)
4. Keep `BattleScreen.java` - coordination, input, WebSocket (~400 lines)

### 🟡 MEDIUM Priority Issues

#### M-1: Duplicate Color Definitions

**Problem**: Same colors defined in multiple files:
- BattleScreen: 23 inline `new Color()` calls
- DraftScreen: 19 inline `new Color()` calls
- Buff colors defined in BattleScreen but needed in DraftScreen too

**Recommendation**: Create `ui/GameColors.java` with shared constants:
```java
public class GameColors {
    // Heroes
    public static final Color WARRIOR = new Color(0.8f, 0.3f, 0.3f, 1);
    public static final Color MAGE = new Color(0.3f, 0.3f, 0.8f, 1);
    // ... etc

    // Buffs
    public static final Color BUFF_POWER = new Color(0.9f, 0.5f, 0.1f, 1);
    // ... etc

    // UI
    public static final Color PANEL_BG = new Color(0.15f, 0.15f, 0.2f, 1);
    public static final Color DISABLED = new Color(0.2f, 0.2f, 0.2f, 1);
}
```

#### M-2: Timer Logic Duplicated

**Problem**: Similar timer countdown logic in:
- DraftScreen.drawTimer() - 15 lines
- BattleScreen.drawTurnIndicator() (timer portion) - 12 lines
- BattleScreen (death choice timer) - 10 lines

**Recommendation**: Create `ui/TimerDisplay.java`:
```java
public class TimerDisplay {
    public void render(float time, float warningThreshold, float x, float y, ...);
}
```

#### M-3: WebSocket Listener Boilerplate

**Problem**: Identical WebSocket callback implementations in:
- ConnectScreen (4 callbacks)
- DraftScreen (4 callbacks)
- BattleScreen (4 callbacks)

`onConnected`, `onDisconnected`, `onError`, `onMessage` are nearly identical.

**Recommendation**: Create abstract base class or use default methods:
```java
public abstract class NetworkedScreen extends BaseScreen
    implements WebSocketListener, GameMessageHandler.GameMessageListener {

    // Default implementations for common callbacks
    @Override
    public void onConnected() {
        Gdx.app.log(getTag(), "Connected");
    }
    // ... etc
}
```

#### M-4: Buff Data Duplicated

**Problem**: BUFF types and skills defined in both screens:
- DraftScreen: HERO_SKILLS array (6 heroes × 3 skills)
- BattleScreen: BUFF colors (6 buffs)

These should come from shared data.

**Recommendation**: Create `data/GameData.java` with:
- HERO_CLASSES
- HERO_SKILLS
- HERO_COLORS
- BUFF_TYPES
- BUFF_COLORS
- MINION_TYPES

### 🟢 LOW Priority Issues

#### L-1: Magic Numbers in Layout

**Problem**: Layout values hardcoded throughout:
```java
private static final float CELL_SIZE = 80;
private static final float GRID_START_X = 50;
// Many more...
```

**Recommendation**: Create `ui/LayoutConfig.java` for consistent layout values.

#### L-2: Method Size

Most methods are appropriately sized. Largest methods:
- `drawActionButtons()` - 62 lines (acceptable)
- `drawUnitInfoPanel()` - 58 lines (acceptable)
- `calculateValidTargets()` - 57 lines (acceptable)
- `handleGridClick()` - 57 lines (acceptable)

No individual method exceeds 65 lines - **within acceptable limits**.

#### L-3: HelloTacticsScreen.java

**Problem**: Legacy test screen still present (111 lines).

**Recommendation**: Can be deleted once no longer needed for testing.

---

## 3. Recommended Refactorings

### Phase 1: Immediate (Before E-6)

| ID | Task | Priority | Impact |
|----|------|----------|--------|
| R-1 | Split BattleScreen into 3-4 classes | 🔴 High | Maintainability |
| R-2 | Create GameColors.java | 🟡 Medium | Consistency |

### Phase 2: Before E-8 (Animations)

| ID | Task | Priority | Impact |
|----|------|----------|--------|
| R-3 | Create BoardRenderer.java | 🟡 Medium | Animation support |
| R-4 | Create UnitRenderer.java | 🟡 Medium | Animation support |
| R-5 | Create TimerDisplay.java | 🟡 Medium | Reusability |

### Phase 3: Nice to Have

| ID | Task | Priority | Impact |
|----|------|----------|--------|
| R-6 | Create GameData.java | 🟢 Low | Single source of truth |
| R-7 | Create NetworkedScreen base | 🟢 Low | DRY |
| R-8 | Create LayoutConfig.java | 🟢 Low | Consistency |
| R-9 | Delete HelloTacticsScreen | 🟢 Low | Cleanup |

---

## 4. Package Structure

### Current Structure
```
com.tactics.client/
├── TacticsGame.java
├── screens/
│   ├── BaseScreen.java
│   ├── BattleScreen.java (1034 lines!)
│   ├── ConnectScreen.java
│   ├── DraftScreen.java
│   ├── HelloTacticsScreen.java
│   ├── ResultScreen.java
│   └── ScreenManager.java
└── net/
    ├── GameMessageHandler.java
    ├── IWebSocketClient.java
    ├── WebSocketFactory.java
    └── WebSocketListener.java
```

### Recommended Structure (After Refactoring)
```
com.tactics.client/
├── TacticsGame.java
├── screens/
│   ├── BaseScreen.java
│   ├── BattleScreen.java (~400 lines)
│   ├── ConnectScreen.java
│   ├── DraftScreen.java
│   ├── ResultScreen.java
│   └── ScreenManager.java
├── net/
│   ├── GameMessageHandler.java
│   ├── IWebSocketClient.java
│   ├── WebSocketFactory.java
│   └── WebSocketListener.java
├── ui/
│   ├── GameColors.java
│   ├── TimerDisplay.java
│   └── DeathChoiceDialog.java
├── render/
│   ├── BoardRenderer.java
│   └── UnitRenderer.java
└── data/
    └── GameData.java
```

---

## 5. Action Items

### Immediate Actions (🔴 High Priority)

1. **Split BattleScreen.java** - This is blocking further development
   - Create `render/BoardRenderer.java` for grid/unit rendering
   - Create `ui/GameColors.java` for shared color constants
   - Reduce BattleScreen to coordination/input handling only

### Pre-Animation Actions (🟡 Medium Priority)

2. **Prepare for animations**
   - Extract unit rendering to enable animation states
   - Create TimerDisplay component for reuse

### Deferred Actions (🟢 Low Priority)

3. **Cleanup tasks**
   - Create GameData.java when connecting to real server
   - Delete HelloTacticsScreen when no longer needed

---

## 6. Metrics to Track

| Metric | Current | Target |
|--------|---------|--------|
| Largest file | 1,034 lines | < 500 lines |
| Files > 500 lines | 2 | 0 |
| Inline Color calls | 42 | < 10 |
| Duplicate code blocks | ~5 | 0 |

---

*Generated as part of Phase E Code Health Check*
