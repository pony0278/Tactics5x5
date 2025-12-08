# 5x5 Tactics Engine

## Project Overview
A 5x5 tactical board game featuring a game engine, WebSocket server, and web client.

## Tech Stack
- Java 17
- Maven 3.8+
- JUnit 5.10
- Jetty 11 (WebSocket)
- Vanilla JavaScript client (將替換為 LibGDX)

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
├── src/test/java/           # Tests (692 passing)
└── client/                  # Web frontend (HTML/CSS/JS)
```

## Common Commands
```bash
mvn compile                    # Compile
mvn test                       # Run all tests
mvn test -Dtest=ClassName      # Run single test class
mvn test -Dtest=Class#method   # Run single test method
mvn clean package              # Package
mvn exec:java                  # Start server
```

---

## 🗓️ Development Roadmap

**Current Phase**: Phase C - Complete Remaining Tests

| Phase | Description | Est. Time | Status |
|-------|-------------|-----------|--------|
| **C** | Complete Remaining Tests | 6-10 hours | 🔄 Next |
| D | End-to-End Testing | 4-6 hours | ⬜ Pending |
| E | LibGDX Client | 20-30 hours | ⬜ Pending |
| F | Supabase Integration | 8-12 hours | ⬜ Pending |

### Phase C Tasks (Current)

| Task | Description | Est. Time | Status |
|------|-------------|-----------|--------|
| C-1 | handleJoinMatch() refactor | 30 min | ⬜ |
| C-2 | SKILL_SYSTEM tests (~150) | 4-6 hours | ⬜ |
| C-3 | BUFF tests (~100) | 3-4 hours | ⬜ |

**📄 Full roadmap details**: `/docs/ROADMAP.md`

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

**Test Status**: 692 tests passing

---

## 📚 Key Reference Documents

### Game Rules (V3 - Current)
| Document | Description |
|----------|-------------|
| `/docs/GAME_RULES_V3.md` | Core gameplay rules |
| `/docs/BUFF_SYSTEM_V3.md` | 6 BUFF types |
| `/docs/SKILL_SYSTEM_V3.md` | 18 hero skills |
| `/docs/GAME_FLOW_V3.md` | Complete game phases |

### Test Plans
| Document | Description |
|----------|-------------|
| `/docs/SKILL_SYSTEM_V3_TESTPLAN.md` | Skill tests (201 cases) |
| `/docs/BUFF_SYSTEM_V3_TESTPLAN.md` | BUFF tests (141 cases) |
| `/docs/TIMER_TESTPLAN.md` | Timer tests (80 cases) |

### Development
| Document | Description |
|----------|-------------|
| `/docs/ROADMAP.md` | Full development roadmap |
| `/docs/CODE_HEALTH_TODO.md` | Code health tracking |
| `/docs/WS_PROTOCOL_V1.md` | WebSocket message format |

---

## 🏗️ Architecture Overview

### Layer Separation
```
┌─────────────────────────────────────────────────┐
│  CLIENT (client/ → LibGDX)                      │  ← UI only
├─────────────────────────────────────────────────┤
│  SERVER (server/)                               │  ← Orchestration
│  ├── ws/   → WebSocket handlers                 │
│  ├── core/ → Match, Timer management            │
│  └── dto/  → Data transfer objects              │
├─────────────────────────────────────────────────┤
│  ENGINE (engine/)                               │  ← Pure game logic
│  ├── rules/ → RuleEngine (facade)               │
│  │   ├── ActionValidator, ActionExecutor        │
│  │   ├── MoveExecutor, AttackExecutor           │
│  │   └── TurnManager, GameOverChecker           │
│  ├── skill/ → SkillExecutor (per hero)          │
│  └── model/ → GameState, Unit, Buff             │
└─────────────────────────────────────────────────┘
```

### Layer Rules
| From | Can Access | Cannot Access |
|------|------------|---------------|
| CLIENT | WebSocket only | SERVER, ENGINE |
| SERVER | ENGINE | CLIENT internals |
| ENGINE | Nothing external | SERVER, CLIENT |

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
3. **Layer Separation** - ENGINE → SERVER → CLIENT
4. **Immutability** - GameState, Unit are immutable

### Code Standards
| Rule | Limit |
|------|-------|
| Class size | < 500 lines (must split if > 1000) |
| Method size | < 30 lines (max 50) |
| Parameters | Max 3-4 per method |
| Duplicate code | Extract after 3 occurrences |

### Common Patterns (Reference existing implementations)

| Pattern | Purpose | Project Example |
|---------|---------|-----------------|
| **Facade** | Simplify complex subsystems | `RuleEngine` → delegates to `ActionValidator`, `ActionExecutor` |
| **Strategy/Dispatch** | Swappable algorithms by type | `SkillExecutor` → dispatches to `WarriorSkillExecutor`, `MageSkillExecutor`, etc. |
| **Immutable + withX()** | State immutability | `Unit.withHp()`, `GameState.withUnits()` |
| **Dependency Injection** | Reduce coupling | `MatchService(RuleEngine engine)` constructor injection |
| **Helper Extraction** | Remove duplicate code | `RuleEngineHelper.findUnitById()`, `hasSpeedBuff()` |

**Note**: Interfaces are NOT required for this project scale. Use concrete classes to avoid over-engineering.

### Testing (TDD)
```
1. Check test plan in docs/
2. Write failing test first
3. Write minimal code to pass
4. Refactor if needed
5. Run: mvn test
```

### Code Review Checklist
- [ ] Layer boundaries respected
- [ ] Single responsibility per class
- [ ] Methods < 30 lines
- [ ] All tests pass
- [ ] No duplicate code

---

## 📊 Test Coverage Summary

**Total: 692 tests passing**

### By Feature
| Feature | Tests |
|---------|-------|
| Core Engine (validation, actions) | ~120 |
| BUFF System | ~50 |
| Skill System (18 skills) | ~95 |
| Guardian Passive | 16 |
| Draft Phase | 110 |
| Timer System | 108 |
| Unit Turn System | 38 |
| Serialization | ~40 |
| WebSocket/Server | ~50 |

### Remaining Tests
| Test Plan | Remaining |
|-----------|-----------|
| SKILL_SYSTEM_V3_TESTPLAN.md | ~150 |
| BUFF_SYSTEM_V3_TESTPLAN.md | ~100 |

---

## 🚀 Quick Start Commands for Claude CLI

### Phase C-1: Refactor handleJoinMatch
```
Refactor MatchWebSocketHandler.handleJoinMatch() method.
Split into: validateJoinRequest(), assignPlayerToMatch(), sendJoinConfirmation(), broadcastGameStart().
Ensure all tests pass.
```

### Phase C-2: Analyze SKILL tests gap
```
Analyze test coverage gaps between docs/SKILL_SYSTEM_V3_TESTPLAN.md and existing skill tests.
List missing test cases by series.
Do NOT implement yet - just provide analysis.
```

### Phase C-3: Analyze BUFF tests gap
```
Analyze test coverage gaps between docs/BUFF_SYSTEM_V3_TESTPLAN.md and existing buff tests.
List missing test cases.
Do NOT implement yet - just provide analysis.
```

---

*Last updated: 2025-12-08*  
*Tests: 692 passing*
