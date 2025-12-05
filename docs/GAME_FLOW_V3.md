# GAME_FLOW_V3.md — Complete Game Flow

## Overview

This document describes the complete flow of a Tactics 5×5 match from start to finish.

---

## 1. Match Phases

```
┌─────────────────┐
│  DRAFT PHASE    │  Players select minions & skill
└────────┬────────┘
         ▼
┌─────────────────┐
│  SETUP PHASE    │  Units placed on starting positions
└────────┬────────┘
         ▼
┌─────────────────┐
│  BATTLE PHASE   │  Alternating unit actions
└────────┬────────┘
         ▼
┌─────────────────┐
│  VICTORY        │  One hero is defeated
└─────────────────┘
```

---

## 2. Draft Phase

### 2.1 Minion Selection
```
For each player (can be simultaneous):
1. Display 3 minion types: TANK, ARCHER, ASSASSIN
2. Player selects 2 minions
3. Selection is hidden from opponent
4. Time limit: 30 seconds (optional)
```

### 2.2 Skill Selection
```
For each player (can be simultaneous):
1. Load player's SPD hero class
2. Display 3 skills for that class
3. Player selects 1 skill
4. Selection is hidden from opponent
```

### 2.3 Draft Data Structure
```java
class DraftSelection {
    String playerId;
    HeroClass heroClass;           // From SPD character
    List<MinionType> minions;      // 2 selected
    String selectedSkillId;
}
```

---

## 3. Setup Phase

### 3.1 Starting Positions
```
Player 1 (Bottom):
  Row 0: [M1] [__] [HERO] [__] [M2]
  
Player 2 (Top):
  Row 4: [M1] [__] [HERO] [__] [M2]

  M1, M2 = Selected minions
  HERO = Player's hero
```

### 3.2 Initial State
```java
class InitialGameState {
    // Board
    int boardWidth = 5;
    int boardHeight = 5;
    
    // Players
    Player player1, player2;
    
    // Units (6 total)
    List<Unit> units;  // 3 per player
    
    // Game state
    int currentRound = 1;
    String currentPlayer = player1.id;
    String activeUnitId = null;  // First unit to act
    
    // Empty at start
    Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
    List<Obstacle> obstacles = new ArrayList<>();
    List<BuffTile> buffTiles = new ArrayList<>();
}
```

---

## 4. Battle Phase — Turn Structure

### 4.1 Alternating Action Order
```
Round N:
  P1 Unit A acts → P2 Unit A acts →
  P1 Unit B acts → P2 Unit B acts →
  P1 Unit C acts → P2 Unit C acts →
  Round End Processing →
  Round N+1
```

### 4.2 Exhaustion Rule
When one player runs out of units to move:
- The opponent takes **consecutive turns** for remaining units
- A Round ends only when **ALL living units** have acted once

```
Example (P1 has 2 units, P2 has 3 units):
  P1 Unit1 → P2 Unit1 →
  P1 Unit2 → P2 Unit2 →
  (P1 exhausted) → P2 Unit3 →
  Round End
```

### 4.3 Unit Action Order
Each player's units act in order:
1. Hero
2. Minion 1 (by unit ID)
3. Minion 2 (by unit ID)

Dead units are skipped.

### 4.4 Action Timer
```
For each unit action:
1. Start 10-second timer
2. Player selects action
3. If timeout:
   - Player's HERO loses 1 HP (not the acting unit)
   - Unit's turn is skipped (END_TURN)
4. If action selected:
   - Validate action
   - If invalid: player can retry (timer continues)
   - If valid: execute action
```

---

## 5. Action Execution Flow

### 5.1 Action Types
```java
enum ActionType {
    MOVE,              // Move unit to tile
    ATTACK,            // Attack enemy
    MOVE_AND_ATTACK,   // Move then attack adjacent
    USE_SKILL,         // Hero skill (heroes only)
    DESTROY_OBSTACLE,  // Remove obstacle (POWER buff only)
    END_TURN           // Pass
}
```

### 5.2 Action Validation
```
1. Check game not over
2. Check correct player's turn
3. Check unit is alive
4. Check unit can act (not stunned, not already acted)
5. Check action-specific rules:
   - MOVE: target valid, in range, unoccupied
   - ATTACK: target valid, in range, enemy
   - USE_SKILL: cooldown ready, target valid
   - etc.
6. Return ValidationResult
```

### 5.3 Action Execution
```
1. Execute primary effect:
   - MOVE: Update position
   - ATTACK: Deal damage, check death
   - USE_SKILL: Apply skill effects
   
2. Trigger on-action effects:
   - BUFF tile trigger (if moved onto)
   - Guardian intercept (if attack)
   - Counter-attack (Challenge, Feint)
   
3. Check death:
   - Mark dead units
   - Trigger death mechanics (obstacle/BUFF tile choice)
   
4. Check victory:
   - If hero dead → game over
   
5. Advance turn:
   - Mark unit as acted
   - Switch to opponent's next unit
```

---

## 6. Round End Processing

When all units have acted (or round timer expires):

### 6.1 Processing Order
```java
void processRoundEnd(GameState state) {
    // 1. Apply BLEED damage
    for (Unit unit : sortById(state.getAliveUnits())) {
        if (hasBleedBuff(unit)) {
            applyDamage(unit, 1, "BLEED");
        }
    }
    
    // 2. Apply minion decay
    for (Unit unit : sortById(state.getAliveMinions())) {
        applyDamage(unit, 1, "DECAY");
    }
    
    // 3. Apply Round 8+ pressure
    if (state.currentRound >= 8) {
        for (Unit unit : sortById(state.getAliveUnits())) {
            applyDamage(unit, 1, "PRESSURE");
        }
    }
    
    // 4. Check deaths (in ID order)
    for (Unit unit : sortById(state.getUnits())) {
        if (unit.hp <= 0 && unit.isAlive()) {
            processDeath(unit);
        }
    }
    
    // 5. Check victory
    checkVictory(state);
    
    // 6. Decrement BUFF durations
    for (Unit unit : state.getUnits()) {
        decrementBuffDurations(unit);
    }
    
    // 7. Decrement skill cooldowns
    for (Hero hero : state.getHeroes()) {
        if (hero.skillCooldown > 0) {
            hero.skillCooldown--;
        }
    }
    
    // 8. Decrement BUFF tile durations
    decrementBuffTileDurations(state);
    
    // 9. Reset unit action flags
    for (Unit unit : state.getAliveUnits()) {
        unit.hasActed = false;
    }
    
    // 10. Advance round
    state.currentRound++;
}
```

---

## 7. Death Processing

### 7.1 Hero Death
```
1. Mark hero as dead (alive = false)
2. Check victory immediately
3. Game ends — opponent wins
```

### 7.2 Minion Death
```
1. Mark minion as dead
2. Owning player chooses:
   Option A: Spawn Obstacle
   - Create obstacle at death position
   
   Option B: Spawn BUFF Tile
   - Create BUFF tile at death position
   - BUFF type is random (or determined at spawn)
   - Tile lasts 2 rounds or until triggered
   
3. Choice must be made within 5 seconds (optional timer)
4. Default: Spawn Obstacle (if timeout)
```

### 7.3 Simultaneous Death
```
If an action causes both heroes to die:
1. The ACTIVE PLAYER (who initiated the attack) WINS
2. Rationale: Rewards aggression over defense

Example:
- P1 uses Wild Magic, kills both heroes
- P1 WINS (P1 is active player)
```

---

## 8. Victory Determination

### 8.1 Victory Conditions
```
WIN: Enemy hero HP reaches 0
LOSE: Your hero HP reaches 0
```

### 8.2 Victory Check Timing
Check after:
- Every action execution
- Every damage application
- Round end processing

### 8.3 Simultaneous Death Resolution
If an action causes both Heroes to die at the same time:
- The **Active Player** (who initiated the attack) **WINS**
- Rationale: Rewards aggression over defense

```java
void checkVictory(GameState state, PlayerId activePlayer) {
    boolean p1HeroDead = !state.getHero(PLAYER_1).isAlive();
    boolean p2HeroDead = !state.getHero(PLAYER_2).isAlive();
    
    if (p1HeroDead && p2HeroDead) {
        // Both dead - active player wins (aggression rewarded)
        state.winner = activePlayer;
    } else if (p1HeroDead) {
        state.winner = PLAYER_2;
    } else if (p2HeroDead) {
        state.winner = PLAYER_1;
    }
}
```

---

## 9. State Transitions

```
DRAFT → SETUP: Both players completed draft
SETUP → BATTLE: Units placed, game initialized
BATTLE → VICTORY: Hero HP reaches 0

Within BATTLE:
  WAITING_FOR_ACTION → ACTION_EXECUTING → WAITING_FOR_ACTION
  ROUND_N → ROUND_END_PROCESSING → ROUND_N+1
  MINION_DEATH → WAITING_FOR_DEATH_CHOICE → CONTINUE
```

---

## 10. Timer Summary

| Phase | Timer | Timeout Effect |
|-------|-------|----------------|
| Draft (minions) | 30s | Random selection |
| Draft (skill) | 30s | Random selection |
| Action | 10s | Hero -1 HP, skip turn |
| Death choice | 5s | Spawn Obstacle (default) |

---

## 11. Example Full Round

```
Round 3:
├── P1 Hero acts
│   └── MOVE to (2,2)
├── P2 Hero acts
│   └── USE_SKILL: Elemental Blast on P1 Archer
│       └── P1 Archer takes 3 damage
├── P1 Tank acts
│   └── ATTACK P2 Assassin
│       └── P2 Assassin takes 1 damage
├── P2 Tank acts
│   └── END_TURN (pass)
├── P1 Archer acts
│   └── ATTACK P2 Hero (range 3)
│       └── P2 Hero takes 1 damage
├── P2 Assassin acts
│   └── MOVE_AND_ATTACK P1 Archer
│       └── P1 Archer takes 2 damage, dies!
│       └── P1 chooses: Spawn BUFF Tile
│       └── Random BUFF: BLEED
└── Round End
    ├── BLEED damage: none (no BLEED buffs yet)
    ├── Minion decay: All minions -1 HP
    ├── Pressure: none (round < 8)
    ├── Check deaths: P2 Assassin dies (HP was 2, now 1, survived)
    ├── BUFF durations: Elemental Blast skill on cooldown (2→1)
    └── Advance to Round 4
```

---

## 12. Implementation Checklist

- [ ] Draft phase UI and logic
- [ ] Setup phase unit placement
- [ ] 10-second action timer
- [ ] Action validation for all types
- [ ] Action execution for all types
- [ ] Death processing with player choice
- [ ] Round end processing (BLEED, decay, pressure)
- [ ] Victory detection
- [ ] Simultaneous death resolution
- [ ] State persistence and replay support

---

# End of GAME_FLOW_V3.md
