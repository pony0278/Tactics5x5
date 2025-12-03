# 5x5 Tactics — Project Progress

This document tracks completed work and outlines the roadmap for future development.

---

## Current Status: V3 Phase 2 Complete

The game engine now supports V3 BUFF system with all 6 buff types:
- **POWER**: +3 ATK, +1 HP instant, blocks MOVE_AND_ATTACK, enables DESTROY_OBSTACLE
- **LIFE**: +3 HP instant
- **SPEED**: -1 ATK, grants 2 actions per turn
- **WEAKNESS**: -2 ATK, -1 HP instant
- **BLEED**: -1 HP per round (damage over time)
- **SLOW**: Actions delayed by 1 round

Additional V3 features implemented:
- Round tracking with `player1TurnEnded`/`player2TurnEnded` flags
- Round increment after both players END_TURN
- Buff tile triggering with RngProvider for randomness
- SLOW buff preparing state and execution at round start

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
| Done | ActionType V3 Extension | USE_SKILL, DESTROY_OBSTACLE, DEATH_CHOICE |
| Done | Action V3 Extension | actingUnitId, skillTargetUnitId, deathChoiceType |
| Done | GameStateSerializer | Updated for all new V3 fields |

### Phase 8: V3 BUFF System Implementation

| Status | Component | Description |
|--------|-----------|-------------|
| Done | BuffInstance V3 | Added BuffType, instantHpBonus, withDecreasedDuration() |
| Done | BuffFlags V3 | Added powerBuff, speedBuff, slowBuff, bleedBuff flags |
| Done | BuffFactory | Factory methods for all 6 buff types with correct modifiers |
| Done | POWER buff | +3 ATK, +1 HP instant, blocks MOVE_AND_ATTACK |
| Done | POWER buff | Enables DESTROY_OBSTACLE action |
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

**Total: 235 tests passing**

---

## Roadmap: V3 Development Phases

### Phase 3: Guardian Passive (Next)

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| High | TANK Guardian | TANK protects adjacent allied units from attacks | Medium |
| High | Attack Redirection | Attacks on protected units redirect to TANK | Medium |

### Phase 4: Hero Skill System

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| High | Skill Validation | USE_SKILL action validation (cooldown, range, target) | Medium |
| High | Skill Execution | 18 hero skills with various effects | High |
| High | Cooldown Tracking | 2-round cooldown after skill use | Low |

### Phase 5: Game Flow Extension

| Priority | Feature | Description | Complexity |
|----------|---------|-------------|------------|
| High | Minion Decay | Minions lose 1 HP per round | Low |
| High | Round 8 Pressure | All units lose 1 HP per round after R8 | Low |
| Medium | Death Choice Flow | SPAWN_OBSTACLE or SPAWN_BUFF_TILE on minion death | Medium |

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
| V3.0-Phase2 | 2025-12-03 | V3 Model Layer + BUFF System complete (6 buff types, SPEED/SLOW mechanics, round tracking) |
| V2.1 | 2025-12-02 | Build fixes, 235 tests passing, buff system tests |
| V2.0 | Earlier | Ranged attacks, client range visualization |
| V1.0 | Earlier | Basic game with melee-only combat |

---

*Last updated: 2025-12-03*
