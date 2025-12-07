# DRAFT_PHASE_TESTPLAN.md — Draft Phase Test Plan

## Overview

This document defines the test cases for Phase 6: Draft Phase implementation.

The Draft Phase handles:
1. **Minion Selection**: Each player picks 2 minions from TANK, ARCHER, ASSASSIN
2. **Skill Selection**: Each player picks 1 skill from their hero's 3 available skills
3. **Setup Phase**: Place units on starting positions and create initial GameState

---

## 1. Data Structures

### 1.1 DraftState

```java
class DraftState {
    PlayerId playerId;
    HeroClass heroClass;              // Player's hero class
    List<MinionType> selectedMinions; // 0-2 minions selected
    String selectedSkillId;           // null or selected skill
    boolean minionSelectionComplete;  // true when 2 minions selected
    boolean skillSelectionComplete;   // true when skill selected
}
```

### 1.2 DraftResult

```java
class DraftResult {
    DraftState player1Draft;
    DraftState player2Draft;
    boolean isComplete;               // true when both players complete
}
```

---

## 2. Test Categories

| Category | Prefix | Description |
|----------|--------|-------------|
| Draft Model | DM | DraftState/DraftResult model tests |
| Minion Selection | MS | Minion selection validation and logic |
| Skill Selection | SS | Skill selection validation and logic |
| Draft Validation | DV | Overall draft validation |
| Setup Phase | SP | GameState creation from draft results |
| Starting Positions | PO | Unit placement on board |
| Integration | DI | Full draft flow tests |

---

## 3. Draft Model Tests (DM-Series)

### DM1: DraftState Creation

| ID | Test Case | Expected |
|----|-----------|----------|
| DM1.1 | Create empty DraftState for P1 | playerId=P1, heroClass set, empty selections |
| DM1.2 | Create empty DraftState for P2 | playerId=P2, heroClass set, empty selections |
| DM1.3 | DraftState with WARRIOR hero | heroClass=WARRIOR, 3 skills available |
| DM1.4 | DraftState with MAGE hero | heroClass=MAGE, 3 skills available |

### DM2: DraftState Immutability

| ID | Test Case | Expected |
|----|-----------|----------|
| DM2.1 | Adding minion returns new state | Original unchanged |
| DM2.2 | Setting skill returns new state | Original unchanged |
| DM2.3 | selectedMinions list is unmodifiable | Throws on modification |

### DM3: DraftResult

| ID | Test Case | Expected |
|----|-----------|----------|
| DM3.1 | Create DraftResult with two states | Contains both player drafts |
| DM3.2 | isComplete when both complete | true |
| DM3.3 | isComplete when P1 incomplete | false |
| DM3.4 | isComplete when P2 incomplete | false |

---

## 4. Minion Selection Tests (MS-Series)

### MS1: Valid Minion Selection

| ID | Test Case | Expected |
|----|-----------|----------|
| MS1.1 | Select TANK as first minion | selectedMinions = [TANK] |
| MS1.2 | Select ARCHER as first minion | selectedMinions = [ARCHER] |
| MS1.3 | Select ASSASSIN as first minion | selectedMinions = [ASSASSIN] |
| MS1.4 | Select TANK then ARCHER | selectedMinions = [TANK, ARCHER] |
| MS1.5 | Select ASSASSIN then TANK | selectedMinions = [ASSASSIN, TANK] |
| MS1.6 | Select same type twice (TANK, TANK) | selectedMinions = [TANK, TANK] |

### MS2: Minion Selection Completion

| ID | Test Case | Expected |
|----|-----------|----------|
| MS2.1 | 0 minions selected | minionSelectionComplete = false |
| MS2.2 | 1 minion selected | minionSelectionComplete = false |
| MS2.3 | 2 minions selected | minionSelectionComplete = true |

### MS3: Minion Selection Validation

| ID | Test Case | Expected |
|----|-----------|----------|
| MS3.1 | Select null minion type | Rejected, error message |
| MS3.2 | Select 3rd minion after 2 selected | Rejected, already complete |
| MS3.3 | Valid minion type accepted | Added to selectedMinions |

### MS4: Available Minion Types

| ID | Test Case | Expected |
|----|-----------|----------|
| MS4.1 | Get available minion types | [TANK, ARCHER, ASSASSIN] |
| MS4.2 | All 3 types always available | Selection doesn't remove from pool |

---

## 5. Skill Selection Tests (SS-Series)

### SS1: Available Skills Per Class

| ID | Test Case | Hero Class | Expected Skills |
|----|-----------|------------|-----------------|
| SS1.1 | WARRIOR skills | WARRIOR | heroic_leap, shockwave, endure |
| SS1.2 | MAGE skills | MAGE | elemental_blast, warp_beacon, wild_magic |
| SS1.3 | ROGUE skills | ROGUE | smoke_bomb, death_mark, shadow_clone |
| SS1.4 | HUNTRESS skills | HUNTRESS | spirit_hawk, spectral_blades, natures_power |
| SS1.5 | DUELIST skills | DUELIST | challenge, elemental_strike, feint |
| SS1.6 | CLERIC skills | CLERIC | trinity, power_of_many, ascended_form |

### SS2: Valid Skill Selection

| ID | Test Case | Expected |
|----|-----------|----------|
| SS2.1 | WARRIOR selects heroic_leap | selectedSkillId = "warrior_heroic_leap" |
| SS2.2 | WARRIOR selects endure | selectedSkillId = "warrior_endure" |
| SS2.3 | MAGE selects wild_magic | selectedSkillId = "mage_wild_magic" |
| SS2.4 | CLERIC selects trinity | selectedSkillId = "cleric_trinity" |

### SS3: Skill Selection Validation

| ID | Test Case | Expected |
|----|-----------|----------|
| SS3.1 | Select null skill | Rejected, error message |
| SS3.2 | Select invalid skill ID | Rejected, error message |
| SS3.3 | WARRIOR selects mage_wild_magic | Rejected, wrong class |
| SS3.4 | MAGE selects warrior_endure | Rejected, wrong class |
| SS3.5 | Select skill when already selected | Rejected, already complete |

### SS4: Skill Selection Completion

| ID | Test Case | Expected |
|----|-----------|----------|
| SS4.1 | No skill selected | skillSelectionComplete = false |
| SS4.2 | Skill selected | skillSelectionComplete = true |

---

## 6. Draft Validation Tests (DV-Series)

### DV1: Overall Draft Completion

| ID | Test Case | Expected |
|----|-----------|----------|
| DV1.1 | No selections made | isComplete = false |
| DV1.2 | Only minions selected | isComplete = false |
| DV1.3 | Only skill selected | isComplete = false |
| DV1.4 | Both minions and skill selected | isComplete = true |

### DV2: Draft State Transitions

| ID | Test Case | Expected |
|----|-----------|----------|
| DV2.1 | Select minion before skill | Valid |
| DV2.2 | Select skill before minions | Valid |
| DV2.3 | Interleave minion and skill selection | Valid |

### DV3: Both Players Complete

| ID | Test Case | Expected |
|----|-----------|----------|
| DV3.1 | P1 complete, P2 incomplete | DraftResult.isComplete = false |
| DV3.2 | P1 incomplete, P2 complete | DraftResult.isComplete = false |
| DV3.3 | Both complete | DraftResult.isComplete = true |

---

## 7. Setup Phase Tests (SP-Series)

### SP1: GameState Creation

| ID | Test Case | Expected |
|----|-----------|----------|
| SP1.1 | Create GameState from complete draft | Valid GameState returned |
| SP1.2 | Create from incomplete draft | Error/exception |
| SP1.3 | GameState has 6 units (3 per player) | units.size() == 6 |
| SP1.4 | GameState round starts at 1 | currentRound == 1 |
| SP1.5 | GameState player 1 starts | currentPlayer == PLAYER_1 |
| SP1.6 | GameState not game over | isGameOver == false |

### SP2: Hero Creation

| ID | Test Case | Expected |
|----|-----------|----------|
| SP2.1 | P1 Hero created with correct class | category=HERO, heroClass matches draft |
| SP2.2 | P2 Hero created with correct class | category=HERO, heroClass matches draft |
| SP2.3 | Hero has selected skill | selectedSkillId matches draft |
| SP2.4 | Hero skill cooldown starts at 0 | skillCooldown == 0 |
| SP2.5 | Hero HP = 5 | hp == 5, maxHp == 5 |
| SP2.6 | Hero ATK = 1 | attack == 1 |
| SP2.7 | Hero moveRange = 1 | moveRange == 1 |
| SP2.8 | Hero attackRange = 1 | attackRange == 1 |

### SP3: Minion Creation

| ID | Test Case | Expected |
|----|-----------|----------|
| SP3.1 | TANK minion created | HP=5, ATK=1, move=1, attackRange=1 |
| SP3.2 | ARCHER minion created | HP=3, ATK=1, move=1, attackRange=3 |
| SP3.3 | ASSASSIN minion created | HP=2, ATK=2, move=4, attackRange=1 |
| SP3.4 | Minion has correct owner | owner matches player |
| SP3.5 | Minion category is MINION | category == MINION |
| SP3.6 | Minion has correct type | minionType matches selection |

### SP4: Duplicate Minion Selection

| ID | Test Case | Expected |
|----|-----------|----------|
| SP4.1 | P1 selects TANK, TANK | Creates 2 TANK minions |
| SP4.2 | P2 selects ARCHER, ARCHER | Creates 2 ARCHER minions |
| SP4.3 | Duplicate minions have unique IDs | id1 != id2 |

---

## 8. Starting Positions Tests (PO-Series)

### PO1: Player 1 Positions (Bottom Row)

```
Row 0: [M1(0,0)] [__] [HERO(2,0)] [__] [M2(4,0)]
```

| ID | Test Case | Expected Position |
|----|-----------|-------------------|
| PO1.1 | P1 Hero at center | (2, 0) |
| PO1.2 | P1 Minion 1 at left | (0, 0) |
| PO1.3 | P1 Minion 2 at right | (4, 0) |

### PO2: Player 2 Positions (Top Row)

```
Row 4: [M1(0,4)] [__] [HERO(2,4)] [__] [M2(4,4)]
```

| ID | Test Case | Expected Position |
|----|-----------|-------------------|
| PO2.1 | P2 Hero at center | (2, 4) |
| PO2.2 | P2 Minion 1 at left | (0, 4) |
| PO2.3 | P2 Minion 2 at right | (4, 4) |

### PO3: Position Validation

| ID | Test Case | Expected |
|----|-----------|----------|
| PO3.1 | All 6 units at unique positions | No position conflicts |
| PO3.2 | All positions within 5x5 board | All in bounds |
| PO3.3 | Minion order matches selection order | First selected = (0,y), Second = (4,y) |

---

## 9. Integration Tests (DI-Series)

### DI1: Full Draft Flow

| ID | Test Case | Expected |
|----|-----------|----------|
| DI1.1 | P1 completes draft, P2 completes draft | DraftResult complete |
| DI1.2 | Create GameState from DraftResult | Valid game ready to play |
| DI1.3 | Units can perform actions | MOVE, ATTACK work |

### DI2: Different Hero Class Combinations

| ID | Test Case | Expected |
|----|-----------|----------|
| DI2.1 | P1=WARRIOR, P2=MAGE | Both heroes created correctly |
| DI2.2 | P1=ROGUE, P2=CLERIC | Both heroes created correctly |
| DI2.3 | Same class for both (P1=WARRIOR, P2=WARRIOR) | Both work independently |

### DI3: Different Minion Combinations

| ID | Test Case | Expected |
|----|-----------|----------|
| DI3.1 | P1=[TANK,ARCHER], P2=[ASSASSIN,TANK] | All 4 minions created |
| DI3.2 | Both players same minions | Works, all have unique IDs |
| DI3.3 | All ASSASSIN team (P1=[ASSASSIN,ASSASSIN]) | Creates 2 assassins |

### DI4: Draft then Battle

| ID | Test Case | Expected |
|----|-----------|----------|
| DI4.1 | Complete draft, P1 hero uses skill | Skill works, cooldown set |
| DI4.2 | Complete draft, TANK guardian works | Guardian intercepts attacks |
| DI4.3 | Complete draft, play full round | Round end processing works |

---

## 10. Error Handling Tests (EH-Series)

### EH1: Invalid Draft Operations

| ID | Test Case | Expected |
|----|-----------|----------|
| EH1.1 | Create GameState before draft complete | Error/null |
| EH1.2 | Select minion for wrong player | Error |
| EH1.3 | Select skill for wrong player | Error |

### EH2: Edge Cases

| ID | Test Case | Expected |
|----|-----------|----------|
| EH2.1 | Empty minion selection list | Cannot create GameState |
| EH2.2 | null heroClass | Error during draft creation |
| EH2.3 | null selectedSkillId | Error during setup |

---

## 11. Unit ID Generation (ID-Series)

### ID1: Unique IDs

| ID | Test Case | Expected |
|----|-----------|----------|
| ID1.1 | All 6 units have unique IDs | No duplicates |
| ID1.2 | Hero IDs follow pattern | "p1_hero", "p2_hero" |
| ID1.3 | Minion IDs follow pattern | "p1_minion_1", "p1_minion_2", etc. |

---

## 12. Test Count Summary

| Category | Count |
|----------|-------|
| Draft Model (DM) | 10 |
| Minion Selection (MS) | 12 |
| Skill Selection (SS) | 14 |
| Draft Validation (DV) | 8 |
| Setup Phase (SP) | 16 |
| Starting Positions (PO) | 9 |
| Integration (DI) | 11 |
| Error Handling (EH) | 5 |
| Unit ID (ID) | 3 |
| **Total** | **88** |

---

## 13. Implementation Priority

### Phase 6A: Core Draft Model
1. DraftState class
2. DraftResult class
3. DM-series tests

### Phase 6B: Minion Selection
1. Minion selection logic
2. MS-series tests

### Phase 6C: Skill Selection
1. Skill selection logic (uses existing SkillRegistry)
2. SS-series tests

### Phase 6D: Setup Phase
1. GameState creation from DraftResult
2. Unit placement on starting positions
3. SP-series, PO-series tests

### Phase 6E: Integration
1. Full flow testing
2. DI-series tests

---

## 14. Dependencies

| Component | Dependency |
|-----------|------------|
| DraftState | HeroClass, MinionType enums |
| Skill Selection | SkillRegistry (already exists) |
| Setup Phase | Unit, GameState, Board classes |
| Unit Creation | MinionType stats from UNIT_TYPES_V3.md |

---

## 15. Notes

1. **Hidden Selection**: Draft selections are hidden from opponent. This is a server/client concern, not engine. Engine just validates and stores.

2. **Timer**: 30-second draft timer is server/client responsibility, not tested here.

3. **SPD Integration**: Hero class comes from external SPD character. For engine tests, we assume heroClass is provided.

4. **Skill Availability**: Each hero class has exactly 3 skills defined in SkillRegistry.

---

*Created: 2025-12-07*
*Last Updated: 2025-12-07*
