# Development Roadmap

This document tracks the upcoming development phases for the 5x5 Tactics Engine.

**Last Updated**: 2025-12-09
**Current Tests**: 827 passing
**Current Phase**: Phase C - Complete Remaining Tests (In Progress)

---

## Overview

| Phase | Description | Est. Time | Status |
|-------|-------------|-----------|--------|
| C | Complete Remaining Tests | 6-10 hours | 🔄 In Progress (827/~950 tests) |
| D | End-to-End Testing | 4-6 hours | ⬜ Pending |
| E | LibGDX Client | 20-30 hours | ⬜ Pending |
| F | Supabase Integration | 8-12 hours | ⬜ Pending |

**Total Estimated Time**: 38-58 hours (~5-8 working days)

---

## Phase C: Complete Remaining Tests

**Goal**: Achieve comprehensive test coverage before client development.

**Estimated Time**: 6-10 hours

### Tasks

| Task | Description | Est. Time | Status |
|------|-------------|-----------|--------|
| C-1 | MatchWebSocketHandler.handleJoinMatch() refactor | 30 min | ⬜ |
| C-2 | Remaining SKILL_SYSTEM tests (~150 tests) | 4-6 hours | 🔄 In Progress (~135 added) |
| C-3 | Remaining BUFF tests (~100 tests) | 3-4 hours | ⬜ |

### C-2 Progress (Skill Tests)

| Series | Description | Tests Added | Status |
|--------|-------------|-------------|--------|
| SCL | Cleric Skills | 12 | ✅ Complete |
| SC | Cooldown System | 12 | ✅ Complete |
| SV | Skill Validation | 18 | ✅ Complete |
| SMG | Mage Wild Magic + Edge Cases | 7 | ✅ Complete |
| SH | Huntress Spirit Hawk + Spectral Blades | 9 | ✅ Complete |
| SW | Warrior Endure + Edge Cases | 7 | ✅ Complete |
| **Total Added** | | **65** | |

**Bug Fixed**: Shield absorption in `Unit.withDamage()` - shield now correctly absorbs damage before HP.

### C-1: handleJoinMatch Refactor

**Current**: 61 lines with multiple responsibilities  
**Target**: < 20 lines with extracted helpers

```
Refactor MatchWebSocketHandler.handleJoinMatch() method.

Split into:
- validateJoinRequest() - validate incoming data
- assignPlayerToMatch() - assign player to match
- sendJoinConfirmation() - send confirmation message
- broadcastGameStart() - broadcast when both players ready

Follow CODE_HEALTH_TODO.md section 2.3.
Ensure all existing tests pass after refactoring.
```

### C-2: SKILL_SYSTEM Tests

**Reference**: `docs/SKILL_SYSTEM_V3_TESTPLAN.md`

```
Analyze test coverage gaps between docs/SKILL_SYSTEM_V3_TESTPLAN.md and existing skill tests.

Steps:
1. Review existing skill tests in src/test/java/
2. Compare against SKILL_SYSTEM_V3_TESTPLAN.md
3. List missing test cases by series
4. Implement missing tests using TDD

Priority:
1. Edge cases for existing skills
2. Skill interaction tests
3. Cooldown edge cases

Run mvn test after each batch to verify.
```

### C-3: BUFF Tests

**Reference**: `docs/BUFF_SYSTEM_V3_TESTPLAN.md`

```
Analyze test coverage gaps between docs/BUFF_SYSTEM_V3_TESTPLAN.md and existing buff tests.

Steps:
1. Review existing buff tests (BuffFactoryTest, BuffTileTest, etc.)
2. Compare against BUFF_SYSTEM_V3_TESTPLAN.md
3. List missing test cases
4. Implement missing tests

Focus areas:
1. Buff stacking edge cases
2. Buff expiration timing
3. Buff interaction combinations
4. Buff + skill interactions

Run mvn test after each batch to verify.
```

---

## Phase D: End-to-End Testing

**Goal**: Verify complete game flow before building new client.

**Estimated Time**: 4-6 hours

### Tasks

| Task | Description | Est. Time | Status |
|------|-------------|-----------|--------|
| D-1 | Draft → Battle → Victory complete flow | 2 hours | ⬜ |
| D-2 | WebSocket message validation | 2 hours | ⬜ |
| D-3 | Error handling & edge cases | 2 hours | ⬜ |

### D-1: Complete Flow Test

```
Create EndToEndTest.java that tests complete game flow:

1. Two players connect via WebSocket
2. Both complete Draft phase (select hero, minions)
3. Battle until one Hero dies
4. Verify victory message sent correctly

Use mock WebSocket sessions.
Test both player perspectives.
```

### D-2: WebSocket Message Validation

```
Verify all WebSocket messages match docs/WS_PROTOCOL_V1.md:

Client → Server:
- JOIN_MATCH
- DRAFT_PICK
- ACTION (MOVE, ATTACK, USE_SKILL, END_TURN)
- DEATH_CHOICE

Server → Client:
- GAME_STATE
- YOUR_TURN (with availableUnitIds for unit-by-unit)
- TIMER_SYNC
- GAME_OVER

Create WebSocketProtocolTest.java to validate message formats.
```

### D-3: Error Handling Tests

```
Test error scenarios:

1. Invalid actions during opponent's turn
2. Disconnection mid-game
3. Timer expiration handling
4. Invalid Draft selections
5. Malformed WebSocket messages

Ensure graceful error handling and appropriate error messages.
```

---

## Phase E: LibGDX Client

**Goal**: Replace HTML/CSS/JS client with LibGDX for cross-platform support.

**Estimated Time**: 20-30 hours

### Architecture

```
client-libgdx/
├── core/                    # Shared game logic
│   ├── screens/             # DraftScreen, BattleScreen, ResultScreen
│   ├── ui/                  # UI components (buttons, dialogs)
│   ├── net/                 # WebSocket client
│   ├── render/              # Board, unit rendering
│   └── assets/              # Sprites, fonts, sounds
├── desktop/                 # Desktop launcher
└── android/                 # Android launcher (optional)
```

### Tasks

| Task | Description | Est. Time | Status |
|------|-------------|-----------|--------|
| E-1 | Project structure setup (Gradle multi-module) | 2 hours | ⬜ |
| E-2 | WebSocket client implementation | 3 hours | ⬜ |
| E-3 | Screen framework (Draft, Battle, Result) | 4 hours | ⬜ |
| E-4 | Draft UI (hero selection, minion draft) | 6 hours | ⬜ |
| E-5 | Battle UI (board, units, actions) | 8 hours | ⬜ |
| E-6 | Animations & effects | 8 hours | ⬜ |

### E-1: Project Setup

```
Create LibGDX client project structure for Tactics 5x5.

Requirements:
1. Use Gradle multi-module setup
2. Core module depends on engine module (for shared models)
3. Desktop launcher for development
4. Include java-websocket library for WebSocket client

Do NOT implement rendering yet - just project structure and basic screens.
```

### E-2: WebSocket Client

```
Implement TacticsWebSocketClient.java that:

1. Connects to ws://server:8080/match
2. Sends/receives JSON messages per docs/WS_PROTOCOL_V1.md
3. Handles reconnection with exponential backoff
4. Notifies listeners on message received
5. Queues messages if disconnected

Include unit tests for message parsing.
```

### E-3: Screen Framework

```
Create base screen structure:

1. BaseScreen.java - common functionality
2. DraftScreen.java - hero/minion selection
3. BattleScreen.java - main game board
4. ResultScreen.java - victory/defeat display
5. LoadingScreen.java - connection/loading state

Implement screen transitions and basic UI layout.
```

### E-4: Draft UI

```
Implement Draft phase UI:

1. Hero class selection (6 heroes with portraits)
2. Skill preview on hover
3. Minion draft (TANK, ARCHER, ASSASSIN)
4. 60s countdown timer display
5. Opponent draft status indicator
6. Ready/Waiting states

Follow GAME_FLOW_V3.md Draft Phase specification.
```

### E-5: Battle UI

```
Implement Battle phase UI:

1. 5x5 board rendering with tile highlights
2. Unit sprites with HP bars
3. Action selection (Move, Attack, Skill, End Turn)
4. Valid target highlighting
5. Turn indicator (Your Turn / Opponent's Turn)
6. 10s action timer display
7. Unit info panel (stats, buffs)
8. Death Choice dialog

Follow GAME_RULES_V3.md for visual feedback requirements.
```

### E-6: Animations & Effects

```
Implement visual feedback:

1. Unit movement animation
2. Attack animation with damage numbers
3. Skill effect animations (per skill)
4. Buff application/removal effects
5. Death animation
6. Victory/Defeat celebration
7. Timer warning effects (< 3s)

Keep animations short (< 500ms) to maintain game pace.
```

---

## Phase F: Supabase Integration

**Goal**: Add player accounts, authentication, and persistent data.

**Estimated Time**: 8-12 hours

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│  LibGDX Client                                          │
│  └── Login UI, Game UI                                  │
├─────────────────────────────────────────────────────────┤
│  Java Server (Jetty WebSocket)                          │
│  ├── MatchService (game logic)                          │
│  ├── AuthService (validate Supabase JWT)                │
│  └── PlayerRepository (PostgreSQL via Supabase)         │
├─────────────────────────────────────────────────────────┤
│  Supabase                                               │
│  ├── Auth (Email, Google, Discord login)                │
│  ├── PostgreSQL (players, matches, leaderboard)         │
│  └── Realtime (optional: live leaderboard)              │
└─────────────────────────────────────────────────────────┘
```

### Database Schema

```sql
-- Players table
CREATE TABLE players (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auth_id UUID REFERENCES auth.users(id),
    username VARCHAR(50) UNIQUE NOT NULL,
    rating INT DEFAULT 1000,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Match history
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    player1_id UUID REFERENCES players(id),
    player2_id UUID REFERENCES players(id),
    winner_id UUID REFERENCES players(id),
    player1_hero VARCHAR(20),
    player2_hero VARCHAR(20),
    rounds INT,
    duration_seconds INT,
    replay_data JSONB,  -- Optional: store game states for replay
    created_at TIMESTAMP DEFAULT NOW()
);

-- Leaderboard view
CREATE VIEW leaderboard AS
SELECT 
    username, 
    rating, 
    wins, 
    losses,
    ROUND(wins::DECIMAL / NULLIF(wins + losses, 0) * 100, 1) as win_rate
FROM players
WHERE wins + losses >= 10  -- Minimum games for ranking
ORDER BY rating DESC
LIMIT 100;

-- Indexes
CREATE INDEX idx_players_rating ON players(rating DESC);
CREATE INDEX idx_matches_created ON matches(created_at DESC);
```

### Server Additions

```
server/
├── auth/
│   ├── SupabaseAuthService.java    # JWT validation
│   ├── AuthFilter.java             # WebSocket auth filter
│   └── JwtPayload.java             # JWT claims model
├── repository/
│   ├── PlayerRepository.java       # Player CRUD
│   ├── MatchRepository.java        # Match history CRUD
│   └── LeaderboardRepository.java  # Leaderboard queries
└── config/
    └── DatabaseConfig.java         # HikariCP connection pool
```

### Tasks

| Task | Description | Est. Time | Status |
|------|-------------|-----------|--------|
| F-1 | Supabase setup + database tables | 2 hours | ⬜ |
| F-2 | Java Server Auth/Repository modules | 4 hours | ⬜ |
| F-3 | Client login UI | 4 hours | ⬜ |
| F-4 | Leaderboard & match history | 4 hours | ⬜ |

### F-1: Supabase Setup

```
Setup Supabase project:

1. Create new Supabase project
2. Run SQL migrations (players, matches tables)
3. Configure Auth providers (Email, Google)
4. Get connection string and JWT secret
5. Test connection from Java

Document environment variables needed:
- SUPABASE_URL
- SUPABASE_ANON_KEY
- SUPABASE_JWT_SECRET
- SUPABASE_DB_URL
```

### F-2: Server Auth/Repository

```
Implement server-side authentication:

1. SupabaseAuthService.java
   - Validate JWT tokens from Supabase
   - Extract user_id from token
   - Cache validated tokens (5 min TTL)

2. PlayerRepository.java
   - findByAuthId(UUID authId)
   - createPlayer(UUID authId, String username)
   - updateRating(UUID playerId, int newRating)
   - getLeaderboard(int limit)

3. MatchRepository.java
   - saveMatch(MatchResult result)
   - getPlayerHistory(UUID playerId, int limit)

4. DatabaseConfig.java
   - HikariCP connection pool
   - Environment-based configuration

Add authentication to WebSocket handshake.
```

### F-3: Client Login UI

```
Implement login flow in LibGDX client:

1. LoginScreen with options:
   - Email/Password login
   - Google OAuth button
   - Guest play (optional)

2. Registration flow:
   - Username selection
   - Email verification

3. Token management:
   - Store JWT securely
   - Auto-refresh before expiry
   - Handle token expiration gracefully

4. Profile display:
   - Username, rating, win/loss record
   - Logout button
```

### F-4: Leaderboard & Match History

```
Implement leaderboard and history features:

1. LeaderboardScreen
   - Top 100 players by rating
   - Current player's rank
   - Pull to refresh

2. ProfileScreen
   - Player stats (rating, wins, losses, win rate)
   - Recent match history (last 20 games)
   - Match details on tap (heroes used, rounds, duration)

3. Server endpoints:
   - GET /api/leaderboard
   - GET /api/players/{id}/history
   - GET /api/matches/{id}
```

---

## Deployment Options

### Recommended Stack (Free Tier Friendly)

| Service | Purpose | Free Tier |
|---------|---------|-----------|
| **Supabase** | Auth + PostgreSQL | 500MB DB, 50K MAU |
| **Railway** | Java WebSocket Server | $5/month after free trial |
| **Fly.io** | Alternative server hosting | 3 shared VMs free |

### Environment Variables

```bash
# Server
PORT=8080
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_JWT_SECRET=your-jwt-secret
DATABASE_URL=postgresql://...

# Client (build-time)
API_URL=wss://your-server.railway.app
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

---

## Milestone Checklist

### Milestone 1: Backend Complete ✅
- [x] All core game logic implemented
- [x] All Code Health refactoring complete
- [x] 692 tests passing

### Milestone 2: Test Coverage Complete
- [ ] Phase C-1: handleJoinMatch refactored
- [x] Phase C-2: SKILL_SYSTEM tests (827 tests, 65 new skill tests added)
- [ ] Phase C-3: BUFF tests complete
- [ ] Phase D: E2E tests passing
- [x] Target: 800+ total tests ✅ (827 achieved)

### Milestone 3: LibGDX Playable
- [ ] Phase E-1 ~ E-3: Basic framework
- [ ] Phase E-4: Draft UI working
- [ ] Phase E-5: Battle UI working
- [ ] Can play full game via LibGDX client

### Milestone 4: Production Ready
- [ ] Phase F: Supabase integration complete
- [ ] Player authentication working
- [ ] Match history saved
- [ ] Leaderboard functional
- [ ] Deployed to cloud

---

## Progress Log

| Date | Phase | Task | Notes |
|------|-------|------|-------|
| 2025-12-08 | - | Roadmap created | Initial planning |
| 2025-12-08 | C-2 | SCL-Series Cleric tests | 12 tests (774 total) |
| 2025-12-08 | C-2 | SC-Series Cooldown tests | 12 tests (786 total) |
| 2025-12-08 | C-2 | SV-Series Validation tests | 18 tests (804 total) |
| 2025-12-09 | C-2 | SMG/SH/SW Hero skill tests | 23 tests (827 total) |
| 2025-12-09 | C-2 | Shield absorption bug fix | Unit.withDamage() now uses shield |

---

*This document should be updated as phases are completed.*
