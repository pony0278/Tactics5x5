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

## Core Documents (Must Read Before Modifying)

### Architecture & Skeletons
- `/docs/TECH_ARCH.md` - System architecture overview
- `/docs/ENGINE_SKELETON_V1.md` - Engine class skeleton
- `/docs/SERVER_SKELETON_V1.md` - Server class skeleton

### Game Rules
- `/docs/GAME_RULES_V1.md` - V1 basic rules
- `/docs/GAME_RULES_V2.md` - V2 range rules (moveRange/attackRange)
- `/docs/UNIT_TYPES_V1.md` - Unit type definitions
- `/docs/BUFF_SYSTEM_V1.md` - Buff system specification

### Test Plans
- `/docs/RULEENGINE_TESTPLAN_V1.md` - RuleEngine tests
- `/docs/RULEENGINE_VALIDATE_V2_TESTPLAN.md` - V2 validation tests
- `/docs/BUFF_SYSTEM_V1_TESTPLAN.md` - Buff system tests

### Protocol
- `/docs/WS_PROTOCOL_V1.md` - WebSocket message format

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

## Current Progress

See `/docs/PROGRESS.md` for details.

### Completed
- [x] V1 basic rules
- [x] V2 range rules (moveRange/attackRange)
- [x] WebSocket server
- [x] Web client
- [x] Unit types (SWORDSMAN, ARCHER, TANK)

### In Progress
- [ ] Buff System V1 (stun, root, poison, bonusAttack, etc.)

## Test Coverage

| Test Class | Coverage |
|------------|----------|
| RuleEngineValidateActionTest | V1 validation logic |
| RuleEngineApplyActionTest | Action execution logic |
| RuleEngineValidateActionV2Test | V2 range validation |
| RuleEngineBuffFlowTest | Buff system (in progress) |
| MatchServiceTest | Game flow |
| GameStateSerializerTest | JSON serialization |

## AI Development Guidelines

See `/prompt/CLAUDE.md` for detailed system prompt rules.

Key principles:
- Must read relevant specification documents before modifying code
- Do not invent rules or fields not defined in documents
- Do not violate layering architecture
- Must run tests to verify after modifications
