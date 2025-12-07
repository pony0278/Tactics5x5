# 5x5 Tactics — Project Progress

This document tracks completed work and outlines the roadmap for future development.

---

## Current Status: V3 Phase 4D Complete + Code Health Refactoring

The game engine now supports all V3 BUFF types, game rules, and all Phase 4 skills (A-D):

### BUFF System (6 types)
- **POWER**: +3 ATK, +1 HP instant, blocks MOVE_AND_ATTACK, instant obstacle destroy
- **LIFE**: +3 HP instant
- **SPEED**: -1 ATK, grants 2 actions per turn
- **WEAKNESS**: -2 ATK, -1 HP instant
- **BLEED**: -1 HP per round (damage over time)
- **SLOW**: Actions delayed by 1 round
- **BLIND**: Cannot attack for 1 round (duration 1, from Smoke Bomb)

### Game Rules Implemented
- **Obstacle HP System**: Obstacles have 3 HP, any unit can attack via ATTACK action
- **POWER Instant Destroy**: POWER buff destroys obstacles in 1 hit
- **Active Player Wins**: On simultaneous death, attacker wins
- **Exhaustion Rule**: Opponent takes consecutive turns when one side exhausted
- **Minion Decay**: Minions lose 1 HP per round at round end
- **Round 8 Pressure**: All units lose 1 HP per round after R8
- **Guardian Passive**: TANK protects adjacent allied units (damage redirected to TANK)
- **Removed DESTROY_OBSTACLE**: Use ATTACK on obstacles instead

### Hero Skills Implemented (Phase 4A-D)
- **Endure** (Warrior): Gain 3 shield, remove BLEED
- **Spirit Hawk** (Huntress): 2 damage at range 4
- **Elemental Blast** (Mage): 3 damage, 50% random debuff
- **Trinity** (Cleric): Heal 3 HP, remove debuff, apply LIFE
- **Shockwave** (Warrior): 1 damage to adjacent + knockback
- **Nature's Power** (Huntress): +2 damage for 2 attacks, LIFE buff (bonus damage fully integrated)
- **Power of Many** (Cleric): Heal all 1 HP, +1 ATK for 1 round
- **Heroic Leap** (Warrior): Leap to tile, 2 damage to adjacent enemies on landing
- **Smoke Bomb** (Rogue): Teleport, invisible 1 round, blind adjacent enemies
- **Warp Beacon** (Mage): Place beacon or teleport to existing beacon
- **Spectral Blades** (Huntress): 1 damage to all enemies in a line (pierces)
- **Wild Magic** (Mage): 1 damage to ALL enemies + 33% random debuff each
- **Elemental Strike** (Duelist): 3 damage + player-chosen debuff (BLEED/SLOW/WEAKNESS)
- **Death Mark** (Rogue): Mark target 2 rounds, +2 damage taken, heal on kill
- **Ascended Form** (Cleric): Invulnerable 1 round, 2x healing, cannot attack
- **Shadow Clone** (Rogue): Spawn 1HP/1ATK clone for 2 rounds
- **Feint** (Duelist): Dodge next attack, counter 2 damage
- **Challenge** (Duelist): Mark enemy, 50% damage to others, counter on attack

### Code Health Refactoring
RuleEngine refactored from ~3,300 lines (monolithic) to clean facade pattern:

| Component | Lines | Responsibility |
|-----------|-------|----------------|
| RuleEngine.java | 98 | Facade - delegates to specialized components |
| ActionValidator.java | 764 | All validation logic |
| ActionExecutor.java | 1,164 | All apply/execution logic |
| SkillExecutor.java | ~1,100 | All skill implementations |

**Test Coverage**: 393 tests passing
- RuleEngineGuardianTest: 16 tests (GRD-series)
- RuleEngineAttritionTest: 11 tests (ATR-series)
- RuleEngineSkillTest: 24 tests (skill framework)
- RuleEngineSkillPhase4BTest: 31 tests (damage/heal skills, bonus damage consumption)
- RuleEngineSkillPhase4CTest: 26 tests (movement skills, BLIND, invisible)
- RuleEngineSkillPhase4DTest: 14 tests (complex skills, buff types)

---

## Completed Work

### Phase 1: Foundation & Architecture

| Status | Component | Description |
|--------|-----------|-------------|
| Done | TECH_ARCH.md | System architecture overview |
| Done | ENGINE_SKELETON_V1.md | Engine class/package skeleton |
| Done | GAME_RULES_V1.md | V1 game rules specification |
| Done | WS_PROTOCOL_V1.md | WebSocket protocol specification |

### Phase 2: Engine Implementation

| Status | Component | Commit | Description |
|--------|-----------|--------|-------------|
| Done | Model Classes | — | GameState, Unit, Board, Position, PlayerId, Action |
| Done | RuleEngine | — | validateAction() and applyAction() |
| Done | V1 Tests | — | RuleEngineValidateActionTest, RuleEngineApplyActionTest |

### Phase 3: Server Implementation

| Status | Component | Commit | Description |
|--------|-----------|--------|-------------|
| Done | SERVER_SKELETON_V1.md | 1aa3189 | Server class skeleton specification |
| Done | Server Skeleton | 0f9a1b0 | MatchRegistry, MatchService, WebSocket handlers |
| Done | MatchRegistry/MatchService | 4df1ade | Core server logic |
| Done | SERVER_CORE_TESTPLAN_V1 | 823b7bf | Test plan for server core |
| Done | Core Tests | 37b0668 | MatchRegistry and MatchService tests |
| Done | SERVER_WS_TESTPLAN_V1 | d6a4ea5 | WebSocket layer test plan |
| Done | WebSocket Layer | 486f23e | ConnectionRegistry, MatchWebSocketHandler |
| Done | WebSocket Tests | 08bf30c | MatchWebSocketHandler tests |
| Done | Serializer Tests | cba8d64 | GameStateSerializer tests |
| Done | Serializer Impl | e26353f | toJsonMap/fromJsonMap methods |
| Done | Code Review Fixes | 35b59e0 | Refactoring from code review |
| Done | Jetty 11 Fix | dd33163 | WebSocket API compatibility |
| Done | No-arg Constructors | 5906159 | For dependency injection |
| Done | Player Slot Assignment | f7d73fa | Server-side multiplayer slots |
| Done | Action PlayerId Fix | 72b7c76 | Fixed null playerId bug |

### Phase 4: Client Implementation

| Status | Component | Commit | Description |
|--------|-----------|--------|-------------|
| Done | CLIENT_WIREFRAME_V1.md | c626c58 | Client specification |
| Done | Frontend Skeleton | eff278d | HTML/CSS/JS skeleton |
| Done | Playable Client | 7e7a569 | Full game flow working |

### Phase 5: V2 Features (Unit Types & Ranges)

| Status | Component | Commit | Description |
|--------|-----------|--------|-------------|
| Done | UNIT_TYPES_V1.md | 47c5e34 | Unit types specification |
| Done | GameStateFactory | 13fa254 | Uses UNIT_TYPES_V1 stats |
| Done | UNIT_TYPES_TESTPLAN_V1 | 6ede88c | Test plan for unit types |
| Done | GameStateFactory Tests | 84b5eb9 | Unit type tests |
| Done | GAME_RULES_V2.md | f652d2d | V2 rules with moveRange/attackRange |
| Done | V2 Test Plan | 1e62f77 | RULEENGINE_VALIDATE_V2_TESTPLAN |
| Done | Unit Model V2 | 698eb0c | moveRange/attackRange fields |
| Done | V2 Validation Tests | a91f924 | RuleEngineValidateActionV2Test |
| Done | RuleEngine V2 | 95991ab | V2 validation with ranges |
| Done | Client Range Display | b23406c | Visual move/attack range highlights |

### Phase 6: Build & Test Fixes

| Status | Component | Description |
|--------|-----------|-------------|
| Done | Java-WebSocket Dependency | Added missing `org.java-websocket:Java-WebSocket:1.5.4` to pom.xml |
| Done | WebSocketClientConnection | Implemented missing `ClientConnection` interface methods (matchId, playerId) |
| Done | JUnit Stub Cleanup | Removed conflicting custom JUnit stubs from test directory |
| Done | Test Expectations | Updated tests to match current `GameStateFactory` behavior (4 units) |
| Done | Test Expectations | Fixed player slot assignment test expectations |

### Phase 7: V3 Model Layer Extension

| Status | Component | Description |
|--------|-----------|-------------|
| Done | UnitCategory enum | HERO, MINION distinction |
| Done | MinionType enum | TANK, ARCHER, ASSASSIN with stats |
| Done | HeroClass enum | WARRIOR, MAGE, ROGUE, HUNTRESS, DUELIST, CLERIC |
| Done | BuffType enum | POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW |
| Done | BuffTile class | Position, buffType, duration, triggered state |
| Done | Obstacle class | Position-based obstacle for movement blocking |
| Done | DeathChoice class | Pending choice after minion death (SPAWN_OBSTACLE/SPAWN_BUFF_TILE) |
| Done | Unit V3 Extension | category, minionType, heroClass, maxHp, selectedSkillId, skillCooldown |
| Done | Unit V3 Extension | shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState |
| Done | Unit V3 Extension | actionsUsed, preparing, preparingAction (for SPEED/SLOW buffs) |
| Done | GameState V3 Extension | buffTiles, obstacles, currentRound, pendingDeathChoice |
| Done | GameState V3 Extension | player1TurnEnded, player2TurnEnded flags |
| Done | ActionType V3 Extension | USE_SKILL, DEATH_CHOICE (DESTROY_OBSTACLE removed) |
| Done | Action V3 Extension | actingUnitId, skillTargetUnitId, deathChoiceType |
| Done | GameStateSerializer | Updated for all new V3 fields |

### Phase 8: V3 BUFF System Implementation

| Status | Component | Description |
|--------|-----------|-------------|
| Done | BuffInstance V3 | Added BuffType, instantHpBonus, withDecreasedDuration() |
| Done | BuffFlags V3 | Added powerBuff, speedBuff, slowBuff, bleedBuff flags |
| Done | BuffFactory | Factory methods for all 6 buff types with correct modifiers |
| Done | POWER buff | +3 ATK, +1 HP instant, blocks MOVE_AND_ATTACK, instant obstacle destroy |
| Done | LIFE buff | +3 HP instant |
| Done | SPEED buff | -1 ATK, allows 2 actions per turn (actionsUsed tracking) |
| Done | WEAKNESS buff | -2 ATK, -1 HP instant |
| Done | BLEED buff | -1 HP per round at turn end |
| Done | SLOW buff | Delays actions by 1 round (preparing state) |
| Done | SLOW execution | Execute preparing actions at round start, attack misses if target moved |
| Done | Round tracking | player1TurnEnded/player2TurnEnded flags |
| Done | Round increment | Increment round after both players END_TURN |
| Done | actionsUsed reset | Reset at round start for all units |
| Done | Buff tile trigger | Trigger on movement, random buff via RngProvider |
| Done | RngProvider integration | Seeded randomness for deterministic replays |

### Phase 9: V3 Test Coverage

| Status | Component | Description |
|--------|-----------|-------------|
| Done | BuffFactoryTest | 12 tests: BM-series (BuffType enum, modifiers, flags, duration, immutability) |
| Done | BuffTileTest | 10 tests: BT-series (tile trigger, instant HP effects, RngProvider determinism) |
| Done | RuleEngineSpeedBuffTest | 7 tests: BSP-series (2 actions/turn, -1 ATK, action tracking, round reset) |
| Done | RuleEngineSlowBuffTest | 7 tests: BSL-series (preparing state, delayed execution, attack miss on target move) |

### Phase 10: V3 Game Rules & Guardian Passive

| Status | Component | Description |
|--------|-----------|-------------|
| Done | Obstacle HP System | Obstacles have 3 HP (Obstacle.DEFAULT_HP), can be attacked by any unit |
| Done | Obstacle Attack | ATTACK action validates/applies for both units and obstacles |
| Done | POWER Instant Destroy | POWER buff destroys obstacles in 1 hit (applyAttackObstacle) |
| Done | Active Player Wins | checkGameOver() accepts activePlayer param for simultaneous death |
| Done | Exhaustion Rule | getNextActingPlayer() handles consecutive turns |
| Done | Minion Decay | applyMinionDecay() - all minions lose 1 HP at round end |
| Done | Round 8 Pressure | applyRound8Pressure() - all units lose 1 HP when round >= 8 |
| Done | Guardian Passive | TANK protects adjacent friendly units via findGuardian() |
| Done | Remove DESTROY_OBSTACLE | Removed action type - use ATTACK on obstacles instead |
| Done | RuleEngineGuardianTest | 16 tests: GRD-series (basic intercept, adjacency, edge cases, MOVE_AND_ATTACK) |
| Done | RuleEngineAttritionTest | 11 tests: ATR-series (minion decay, round 8 pressure, survival duration) |

### Phase 10.5: Code Refactoring

| Status | Component | Description |
|--------|-----------|-------------|
| Done | GameState.with*() | 11 helper methods for immutable state updates |
| Done | Unit.with*() | Added withHpBonus(), enhanced withDamage(), withActionUsed() |
| Done | UnitTransformer | Functional interface for unit transformations |
| Done | updateUnitInList() | Helper to reduce repetitive iteration patterns |
| Done | updateUnitsInList() | Batch unit updates with Map<String, UnitTransformer> |
| Done | PlayerId constants | PLAYER_1, PLAYER_2, isPlayer1(), isPlayer2() |
| Done | Obstacle.ID_PREFIX | Constant for obstacle ID generation |
| Done | RuleEngine cleanup | Initial reduction from 2141 to 1906 lines |

### Phase 11: Code Health Refactoring

| Status | Component | Description |
|--------|-----------|-------------|
| Done | SkillExecutor extraction | All 19 skill implementations moved to SkillExecutor.java (~1,100 lines) |
| Done | ActionValidator extraction | All validation logic moved to ActionValidator.java (764 lines) |
| Done | ActionExecutor extraction | All apply logic moved to ActionExecutor.java (1,164 lines) |
| Done | RuleEngine facade | RuleEngine reduced to 98 lines (clean facade pattern) |

**Final Architecture**:
```
rules/
├── RuleEngine.java        → 98 lines (facade)
├── ActionValidator.java   → 764 lines (validation)
├── ActionExecutor.java    → 1,164 lines (execution)
└── ValidationResult.java  → 23 lines (result type)

skill/
└── SkillExecutor.java     → ~1,100 lines (skill implementations)
```

---

## Test Coverage Summary

| Test Class | Coverage Area | Status |
|------------|---------------|--------|
| RuleEngineValidateActionTest | V1 action validation | Done |
| RuleEngineApplyActionTest | Action application | Done |
| RuleEngineValidateActionV2Test | V2 range validation | Done |
| RuleEngineBuffFlowTest | Buff system validation & apply | Done |
| MatchRegistryTest | Match management | Done |
| MatchServiceTest | Game orchestration | Done |
| MatchWebSocketHandlerTest | WebSocket messages | Done |
| GameStateSerializerTest | JSON serialization | Done |
| GameStateFactoryTest | Unit type creation | Done |
| BuffFactoryTest | V3 BuffFactory & BuffType | Done |
| BuffTileTest | V3 Buff tile triggering | Done |
| RuleEngineSpeedBuffTest | V3 SPEED buff mechanics | Done |
| RuleEngineSlowBuffTest | V3 SLOW buff mechanics | Done |
| RuleEngineGuardianTest | V3 Guardian passive (TANK protect) | Done |
| RuleEngineAttritionTest | V3 Minion decay & Round 8 pressure | Done |
| RuleEngineSkillTest | Skill framework, Endure, Spirit Hawk | Done |
| RuleEngineSkillPhase4BTest | Phase 4B damage/heal skills, bonus damage | Done |
| RuleEngineSkillPhase4CTest | Phase 4C movement skills, BLIND, invisible | Done |
| RuleEngineSkillPhase4DTest | Phase 4D complex skills, buff types | Done |

**Total: 393 tests passing**

---

## Roadmap: V3 Development Phases

### Phase 3: Guardian Passive ✅ COMPLETE

| Priority | Feature | Description | Status |
|----------|---------|-------------|--------|
| High | TANK Guardian | TANK protects adjacent allied units from attacks | ✅ Done |
| High | Attack Redirection | Attacks on protected units redirect to TANK | ✅ Done |

### Phase 4: Hero Skill System ✅ COMPLETE

| Sub-phase | Status | Description |
|-----------|--------|-------------|
| Phase 4A | ✅ Done | Skill framework, validation, cooldown, Endure, Spirit Hawk |
| Phase 4B | ✅ Done | Damage/heal skills (Elemental Blast, Trinity, Shockwave, Nature's Power, Power of Many) |
| Phase 4C | ✅ Done | Movement skills (Heroic Leap, Smoke Bomb, Warp Beacon, Spectral Blades) |
| Phase 4D | ✅ Done | Complex skills (Wild Magic, Elemental Strike, Death Mark, Ascended Form, Shadow Clone, Feint, Challenge) |

### Code Health Refactoring ✅ COMPLETE

| Priority | Feature | Description | Status |
|----------|---------|-------------|--------|
| High | SkillExecutor | Extract 19 skill implementations to SkillExecutor.java | ✅ Done |
| High | ActionValidator | Extract all validation logic to ActionValidator.java | ✅ Done |
| High | ActionExecutor | Extract all apply logic to ActionExecutor.java | ✅ Done |
| High | RuleEngine Facade | RuleEngine reduced to 98-line clean facade | ✅ Done |

### Phase 5: Game Flow Extension ✅ COMPLETE

| Priority | Feature | Description | Status |
|----------|---------|-------------|--------|
| High | Minion Decay | Minions lose 1 HP per round | ✅ Done |
| High | Round 8 Pressure | All units lose 1 HP per round after R8 | ✅ Done |
| Medium | Death Choice Flow | SPAWN_OBSTACLE or SPAWN_BUFF_TILE on minion death | Partial (validation exists) |

### Phase 6: Draft Phase

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| Medium | Hero Selection | Players choose hero class | Medium |
| Medium | Minion Draft | Alternating minion selection (TANK, ARCHER, ASSASSIN) | Medium |
| Medium | Initial Placement | Place units on starting positions | Medium |

---

## Roadmap: Future Features (Post-V3)

### Short Term (Next Steps)

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| High | Unit Type Display | Show unit type names in client (Swordsman/Archer/Tank) | Low |
| High | Range Stats Display | Show moveRange/attackRange in unit info panel | Low |
| Medium | Occupied Cell Filtering | Filter out occupied cells from move range highlights | Low |
| Medium | Cancel Action Button | Add button to cancel pending action and clear highlights | Low |

### Medium Term (Polish & UX)

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| High | Unit Type Icons/Colors | Visual distinction between unit types | Medium |
| Medium | Attack Animation | Visual feedback when attacks occur | Medium |
| Medium | Move Animation | Smooth unit movement transition | Medium |
| Medium | Damage Numbers | Show damage dealt floating above units | Low |
| Low | Sound Effects | Audio feedback for actions | Low |

### Long Term (New Gameplay)

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| High | Path Blocking | Units block movement paths (not just destination) | High |
| High | Line of Sight | Ranged attacks blocked by obstacles/units | High |
| Medium | New Unit Types | Additional units with unique abilities | Medium |
| Medium | Terrain Types | Different tile effects (slow, block, etc.) | High |
| Medium | Match History/Replay | Record and replay completed games | High |
| Low | Spectator Mode | Watch ongoing matches | Medium |
| Low | Lobby System | Create/join matches dynamically | High |

### Technical Debt & Improvements

| Priority | Task | Description |
|----------|------|-------------|
| Medium | Integration Tests | End-to-end WebSocket game flow tests |
| Medium | Error Handling | Improve client error messages and recovery |
| Low | Performance | Optimize rendering for larger boards |
| Low | Mobile Support | Touch-friendly UI adjustments |

---

## Documentation Status

| Document | Status | Last Updated |
|----------|--------|--------------|
| TECH_ARCH.md | Complete | — |
| ENGINE_SKELETON_V1.md | Complete | — |
| GAME_RULES_V1.md | Complete | — |
| GAME_RULES_V2.md | Complete | V2 ranges |
| WS_PROTOCOL_V1.md | Complete | — |
| SERVER_TECH_ARCH_V1.md | Complete | — |
| SERVER_SKELETON_V1.md | Complete | — |
| CLIENT_WIREFRAME_V1.md | Complete | — |
| UNIT_TYPES_V1.md | Complete | — |
| All Test Plans | Complete | — |

---

## How to Run

```bash
# Build the project
mvn clean package

# Run the server
java -jar target/tactics5x5-1.0-SNAPSHOT.jar

# Open client in browser
# Navigate to http://localhost:8080/client/index.html
```

---

## Version History

| Version | Date | Highlights |
|---------|------|------------|
| V3.0-CodeHealth | 2025-12-07 | Full RuleEngine refactoring (98 lines facade + ActionValidator/ActionExecutor/SkillExecutor), 393 tests |
| V3.0-Phase4D | 2025-12-07 | Complex skills (Wild Magic, Elemental Strike, Death Mark, Ascended Form, Shadow Clone, Feint, Challenge), 393 tests |
| V3.0-Phase4C | 2025-12-07 | Movement skills (Heroic Leap, Smoke Bomb, Warp Beacon, Spectral Blades), BLIND buff, invisible mechanic, 374 tests |
| V3.0-Phase4B | 2025-12-06 | Damage/heal skills (Elemental Blast, Trinity, Shockwave, Nature's Power, Power of Many), 348 tests |
| V3.0-Phase4A | 2025-12-06 | Skill framework, validation, cooldown, Endure, Spirit Hawk, 322 tests |
| V3.0-Phase3 | 2025-12-05 | Guardian passive, game rules (obstacle HP, decay, exhaustion), refactoring, 298 tests |
| V3.0-Phase2+Tests | 2025-12-04 | Added 36 V3 tests (BuffFactory, BuffTile, SPEED, SLOW), 271 total tests |
| V3.0-Phase2 | 2025-12-03 | V3 Model Layer + BUFF System complete (6 buff types, SPEED/SLOW mechanics, round tracking) |
| V2.1 | 2025-12-02 | Build fixes, 235 tests passing, buff system tests |
| V2.0 | Earlier | Ranged attacks, client range visualization |
| V1.0 | Earlier | Basic game with melee-only combat |

---

*Last updated: 2025-12-07*
