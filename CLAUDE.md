# 5x5 Tactics Engine

## Project Overview
A 5x5 tactical board game featuring a game engine, WebSocket server, and web client.

## Tech Stack
- Java 17
- Maven 3.8+
- JUnit 5.10
- Jetty 11 (WebSocket)
- Vanilla JavaScript client

## Project Structure
```
├── docs/                    # Specification documents (must read)
├── src/main/java/com/tactics/
│   ├── engine/              # Game core (pure Java, no external dependencies)
│   │   ├── model/           # GameState, Unit, Board, Position
│   │   ├── action/          # Action, ActionType
│   │   ├── buff/            # BuffInstance, BuffModifier, BuffFlags
│   │   ├── rules/           # RuleEngine, ValidationResult
│   │   └── util/            # GameStateFactory, Serializer, RngProvider
│   └── server/              # WebSocket server
│       ├── core/            # Match, MatchService, MatchRegistry
│       ├── dto/             # Message objects
│       └── ws/              # WebSocket handlers
├── src/test/java/           # Tests
└── client/                  # Web frontend (HTML/CSS/JS)
```

## Common Commands
```bash
# Compile
mvn compile

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=RuleEngineBuffFlowTest

# Run single test method
mvn test -Dtest=RuleEngineBuffFlowTest#testStunnedUnitCannotMove

# Package
mvn clean package

# Start server
java -jar target/tactics-engine-1.0-SNAPSHOT.jar
# or
mvn exec:java

# Client URL
http://localhost:8080/client/index.html
```

---

## ⚠️ VERSION NOTE

**V3 is the current development target.**

V1/V2 files are legacy reference. For new features, always refer to V3 documents.

---

## 🚧 Current Development Phase

| Status | Phase | Description |
|--------|-------|-------------|
| ✅ | Phase 0 | Complete V1 BUFF Foundation |
| ✅ | Phase 1 | Model Layer Extension |
| ✅ | Phase 2 | V3 BUFF System |
| 🔄 | **Phase 3** | Guardian Passive |
| ⬜ | Phase 4 | Hero Skill System |
| ⬜ | Phase 5 | Game Flow Extension |
| ⬜ | Phase 6 | Draft Phase |

**Next Step**: Implement TANK Guardian passive (protect adjacent allies)

**Detailed Roadmap**: `/docs/V3_IMPLEMENTATION_ROADMAP.md`

---

## ✅ Recently Implemented Rule Changes

These rules have been implemented:

| Rule | Status | Description |
|------|--------|-------------|
| Obstacle HP | ✅ | Obstacles have 3 HP, any unit can attack via ATTACK action |
| POWER instant destroy | ✅ | POWER buff destroys obstacles in 1 hit |
| Active player wins | ✅ | On simultaneous death, attacker wins |
| Exhaustion Rule | ✅ | Opponent takes consecutive turns when one side exhausted |
| Remove DESTROY_OBSTACLE | ✅ | Removed - use ATTACK on obstacles instead |
| Minion Decay | ✅ | Minions lose 1 HP per round at round end |
| Round 8 Pressure | ✅ | All units lose 1 HP per round after R8 |

---

## Core Documents — V3 (Current)

### Start Here
- `/docs/README.md` - Document index and reading order

### Game Rules (V3)
- `/docs/GAME_RULES_V3.md` - Core gameplay rules (hero, minions, victory)
- `/docs/GAME_FLOW_V3.md` - Complete game phases and flow
- `/docs/UNIT_TYPES_V3.md` - Hero & Minion definitions
- `/docs/BUFF_SYSTEM_V3.md` - 6 BUFF types (POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW)
- `/docs/SKILL_SYSTEM_V3.md` - Hero skill architecture (18 skills)
- `/docs/DESIGN_DECISIONS_V3.md` - Confirmed design decisions

### Quick Reference
- `/docs/V3_QUICK_REFERENCE.md` - Key numbers and stats
- `/docs/HERO_SKILLS_REFERENCE.md` - Skill quick reference (中英對照)

### Test Plans (V3)
- `/docs/BUFF_SYSTEM_V3_TESTPLAN.md` - 141 test cases for BUFF system
- `/docs/GUARDIAN_TESTPLAN.md` - 81 test cases for Guardian passive
- `/docs/SKILL_SYSTEM_V3_TESTPLAN.md` - 201 test cases for Skill system

### Development Roadmap
- `/docs/V3_IMPLEMENTATION_ROADMAP.md` - Phase-by-phase development checklist

---

## Legacy Documents — V1/V2 (Reference Only)

### Architecture & Skeletons
- `/docs/TECH_ARCH.md` - System architecture overview
- `/docs/ENGINE_SKELETON_V1.md` - Engine class skeleton
- `/docs/SERVER_SKELETON_V1.md` - Server class skeleton

### Game Rules (Legacy)
- `/docs/GAME_RULES_V1.md` - V1 basic rules
- `/docs/GAME_RULES_V2.md` - V2 range rules (moveRange/attackRange)
- `/docs/UNIT_TYPES_V1.md` - Unit type definitions
- `/docs/BUFF_SYSTEM_V1.md` - V1 Buff system specification

### Test Plans (Legacy)
- `/docs/RULEENGINE_TESTPLAN_V1.md` - RuleEngine tests
- `/docs/RULEENGINE_VALIDATE_V2_TESTPLAN.md` - V2 validation tests
- `/docs/BUFF_SYSTEM_V1_TESTPLAN.md` - V1 Buff system tests

### Protocol
- `/docs/WS_PROTOCOL_V1.md` - WebSocket message format

---

## Architecture Rules (Important!)

### Layering Principles
1. **ENGINE is fully self-contained** - `engine/` must not reference `server/` or `client/`
2. **SERVER operates through RuleEngine** - Must not directly mutate engine class state
3. **CLIENT depends only on WebSocket protocol** - Must not embed game rule logic

### Code Standards
- **Determinism** - All engine logic must be deterministic; randomness only via `RngProvider`
- **Immutability** - `GameState`, `Unit`, etc. are immutable; modifications require new instances
- **No unauthorized additions** - Do not add classes/methods/fields not defined in skeleton docs
- **Test verification** - All existing tests must pass after modifications

---

## V3 Key Concepts

### Victory Condition
**Kill the enemy Hero = Win** (minion deaths don't end the game)

### Team Composition
- 1 Hero (based on SPD character)
- 2 Minions (drafted from TANK, ARCHER, ASSASSIN)

### New Mechanics (V3)
| Mechanic | Description |
|----------|-------------|
| Hero Skills | Each hero has 1 skill (cooldown: 2 rounds) |
| BUFF Tiles | Spawn on minion death, trigger on step |
| Obstacles | 3 HP, any unit can attack, POWER = instant destroy |
| Guardian | TANK protects adjacent friendly units |
| Minion Decay | Minions lose 1 HP per round |
| Round 8 Pressure | All units lose 1 HP per round after R8 |
| Exhaustion Rule | Opponent takes consecutive turns when one side has no units left |
| Simultaneous Death | Active player (attacker) wins |
| SLOW Buff | Actions delayed by 1 round |
| SPEED Buff | Grants 2 actions per round |

### 6 BUFF Types
| BUFF | ATK | HP | Special |
|------|-----|----|---------|
| POWER | +3 | +1 | Blocks MOVE_AND_ATTACK, **1-hit obstacle destroy** |
| LIFE | — | +3 | — |
| SPEED | -1 | — | Double action per round |
| WEAKNESS | -2 | -1 | — |
| BLEED | — | -1/round | Damage over time |
| SLOW | — | — | Actions delayed 1 round |

---

## Current Progress

See `/docs/PROGRESS.md` for details.

### Completed
- [x] V1 basic rules
- [x] V2 range rules (moveRange/attackRange)
- [x] WebSocket server
- [x] Web client
- [x] Unit types (SWORDSMAN, ARCHER, TANK)
- [x] V3 Model Layer (UnitCategory, MinionType, HeroClass, BuffType enums)
- [x] V3 BUFF System (6 types: POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW)
- [x] V3 SPEED buff (2 actions per turn)
- [x] V3 SLOW buff (delayed actions)
- [x] V3 Buff Tile Triggering
- [x] V3 Round Tracking

### In Progress
- [ ] V3 Guardian Passive (TANK protects adjacent allies) - **16 tests passing**

### Recently Completed
- [x] Obstacle HP system (3 HP, attackable via ATTACK)
- [x] POWER buff instant obstacle destroy
- [x] Simultaneous death: active player wins
- [x] Exhaustion Rule
- [x] Minion Decay (-1 HP/round)
- [x] Round 8 Pressure (-1 HP/round to all)
- [x] Removed DESTROY_OBSTACLE (use ATTACK instead)

---

## Test Coverage

**Total: 298 tests passing**

### Existing Tests
| Test Class | Coverage |
|------------|----------|
| RuleEngineValidateActionTest | V1 validation logic |
| RuleEngineApplyActionTest | Action execution logic |
| RuleEngineValidateActionV2Test | V2 range validation |
| RuleEngineBuffFlowTest | V1 Buff system |
| MatchServiceTest | Game flow |
| GameStateSerializerTest | JSON serialization |

### V3 Tests (Implemented)
| Test Class | Coverage | Tests |
|------------|----------|-------|
| BuffFactoryTest | BM-Series: BuffType, modifiers | 12 |
| BuffTileTest | BT-Series: Tile trigger, instant HP | 10 |
| RuleEngineSpeedBuffTest | BSP-Series: 2 actions, tracking | 7 |
| RuleEngineSlowBuffTest | BSL-Series: Preparing state | 7 |

### V3 Tests (To Be Implemented)
| Test Plan | Test Count |
|-----------|------------|
| GUARDIAN_TESTPLAN.md | 81 |
| SKILL_SYSTEM_V3_TESTPLAN.md | 201 |
| Remaining BUFF tests | ~100 |

---

## AI Development Guidelines

See `/prompt/CLAUDE.md` for detailed system prompt rules.

Key principles:
- **Must read relevant V3 specification documents before modifying code**
- Do not invent rules or fields not defined in documents
- Do not violate layering architecture
- Must run tests to verify after modifications
- For V3 features, always reference V3 documents (not V1/V2)
