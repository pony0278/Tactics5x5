# DESIGN_DECISIONS_V3.md — Confirmed Design Decisions

## Overview

This document records all confirmed design decisions for V3, replacing TBD placeholders in other docs.

---

## 1. Timeout & Penalties

### Q: Timeout penalty target — Hero or acting unit?
**A: Hero**

When a player exceeds the 10-second time limit:
- The **Hero** loses 1 HP (not the acting unit)
- This creates consistent pressure regardless of which unit is acting
- Prevents strategy of timing out on minions to avoid penalty

```
Implementation:
if (actionTimeout) {
    hero.hp -= 1;
    checkDeath(hero);
}
```

---

## 2. Late-Game Pressure

### Q: Round 8 pressure — Hero only or all units?
**A: All units**

Starting from Round 8:
- **All units** (Hero + Minions) lose 1 HP per round
- This stacks with minion decay (minions lose 2 HP/round total after R8)
- Creates urgency to end the game

```
Round 8+:
- Hero: -1 HP/round
- Minions: -1 HP (decay) + -1 HP (pressure) = -2 HP/round
```

---

## 3. Simultaneous Death

### Q: Both heroes die simultaneously — Who wins?
**A: Active player (attacker) wins**

If an action causes both Heroes to die at the same time:
- The **Active Player** (who initiated the attack) **WINS**
- Rationale: Rewards aggression over defense

```
Example scenarios:
- Wild Magic kills both: Active player WINS (rewarded for aggression)
- Counter-attack trade: Attacker WINS (they initiated)
- Reflection damage kills both: Attacker WINS
```

---

## 4. SLOW Buff Details

### Q: Can "preparing" units be attacked?
**A: Yes**

Units with SLOW buff in "preparing" state:
- **Can be targeted and attacked normally**
- Take damage as usual
- If killed while preparing: action is cancelled
- If survive: action executes next round

### Q: Can SLOW buff actions be cancelled?
**A: No**

Once an action is declared with SLOW buff:
- Action **cannot be cancelled** by the player
- Only cancelled if unit dies
- Unit is committed to the action

### Q: When does the preparing action execute?
**A: At the START of the next round, when it's that player's turn**

```
SLOW buff flow:
Round N: Unit with SLOW selects ATTACK on enemy at (3,2)
  - Unit enters "preparing" state
  - Store: preparingAction = {type: ATTACK, targetPosition: (3,2)}
  - Unit can be attacked/damaged
  
Round N+1 (player's turn start):
  - If unit alive: ATTACK executes at stored targetPosition
  - If target moved: attack hits empty tile (misses)
  - If unit dead: action cancelled (no effect)
```

### Q: What if target moves before delayed attack executes?
**A: Attack targets the original POSITION, not the unit**

- Attack hits the stored `targetPosition`
- If target moved away: attack misses (hits empty tile)
- If different unit now at position: that unit gets hit instead

---

## 4.1 SPEED Buff Details

### Q: How does the double action system work?
**A: Unit can do 2 actions before turn switches**

Track with `actionsUsed` field on Unit:
- Normal unit: Turn ends after 1 action (or MOVE_AND_ATTACK or END_TURN)
- SPEED unit: Turn ends after 2 actions OR explicit END_TURN

```java
// Unit fields
int actionsUsed = 0;      // Reset at round start
int maxActions = 1;       // 2 if has SPEED buff

// Valid action combinations for SPEED unit:
// MOVE → MOVE → end
// MOVE → ATTACK → end
// ATTACK → MOVE → end  
// MOVE_AND_ATTACK → (1 action left) → MOVE or ATTACK → end
// END_TURN → immediate end (forfeit remaining actions)
```

### Q: Does SPEED affect MOVE_AND_ATTACK?
**A: MOVE_AND_ATTACK counts as 1 action**

- SPEED unit can do MOVE_AND_ATTACK + one more action
- Or do MOVE, then ATTACK, then still have no actions left (2 used)

```
Example SPEED turn:
1. MOVE_AND_ATTACK (actionsUsed = 1)
2. MOVE (actionsUsed = 2) → turn ends
```

---

## 4.2 Round Tracking

### Q: When does currentRound increment?
**A: After BOTH players have ended their turn**

Implementation:
```java
// GameState fields
int currentRound = 1;
boolean player1TurnEnded = false;
boolean player2TurnEnded = false;

// When a player's turn ends:
void onTurnEnd(PlayerId player) {
    if (player == PLAYER_1) player1TurnEnded = true;
    else player2TurnEnded = true;
    
    if (player1TurnEnded && player2TurnEnded) {
        processRoundEnd();  // Decay, poison, BUFF duration, etc.
        currentRound++;
        player1TurnEnded = false;
        player2TurnEnded = false;
        // Start new round
    }
}
```

### Round-end processing order:
1. Decrement BUFF durations
2. Apply BLEED damage
3. Apply minion decay (-1 HP)
4. Apply Round 8+ pressure (if applicable)
5. Remove expired BUFFs
6. Remove expired BUFF tiles
7. Check deaths
8. Check victory

---

## 4.3 RngProvider for Randomness

### Q: How to handle random BUFF tile selection?
**A: Use existing RngProvider class**

Location: `src/main/java/com/tactics/engine/util/RngProvider.java`

```java
// When BUFF tile is triggered:
BuffType randomBuff = BuffType.values()[rngProvider.nextInt(6)];
applyBuff(unit, randomBuff, duration: 2);

// BUFF type mapping:
// 0 = POWER
// 1 = LIFE  
// 2 = SPEED
// 3 = WEAKNESS
// 4 = BLEED
// 5 = SLOW
```

Why RngProvider:
- **Determinism**: Same seed = same results (for replay/testing)
- **Testability**: Can mock for predictable tests
- Centralized randomness management

---

## 4.4 Exhaustion Rule

### Q: What happens when one player runs out of units to move?
**A: Opponent takes consecutive turns for remaining units**

When one player has no more unused units but opponent still has:
- Opponent takes **consecutive turns** for their remaining units
- A Round ends only when **ALL living units** have acted once

```
Example (P1 has 2 units, P2 has 3 units):
P1 Unit1 → P2 Unit1 → P1 Unit2 → P2 Unit2 → P2 Unit3 → Round End

Benefits:
- No wasted turns
- Rewards having more surviving units
- Encourages aggressive play to eliminate enemy units
```

---

## 4.5 Obstacle Properties

### Q: Can obstacles be destroyed? How?
**A: Yes — obstacles have HP and can be attacked**

| Property | Value |
|----------|-------|
| HP | 3 |
| Destructible | Yes — any unit can ATTACK an obstacle |
| POWER Crush | Units with POWER buff destroy obstacles in **1 hit** |
| Blocking | Blocks movement and line-of-sight while active |

**Rationale**: Prevents stalling/griefing. Players cannot create permanent blockades.

```
Destruction methods:
1. Normal attack: obstacle.hp -= attacker.attack
2. POWER buff: Instant destruction (ignores HP)

Example:
- Obstacle has 3 HP
- ARCHER (ATK 1) needs 3 attacks to destroy
- ASSASSIN (ATK 2) needs 2 attacks to destroy
- POWER buff unit destroys in 1 attack (instant)
```

---

## 5. SPD Skill Integration

### Q: Where does the skill list come from?
**A: Separate skill definition files, extensible architecture**

Skill system design:
- Skills defined in JSON/config files (not hardcoded)
- Each hero class has a skill directory
- Skills can be added/modified without engine changes
- See `SKILL_SYSTEM_V3.md` for full architecture

```
Directory structure:
skills/
├── warrior/
│   ├── heroic_leap.json
│   ├── shockwave.json
│   └── endure.json
├── mage/
│   ├── elemental_blast.json
│   ├── warp_beacon.json
│   └── wild_magic.json
├── rogue/
│   └── ...
├── huntress/
│   └── ...
├── duelist/
│   └── ...
└── cleric/
    └── ...
```

---

## 6. Additional Clarifications

### 6.1 Death Order for Round-End Effects
When multiple units die from round-end effects (poison, decay, pressure):

```
Order: Sort by unit.id ascending
- Process all damage first
- Then mark deaths
- First hero to reach 0 HP = that player loses
```

### 6.2 BUFF Tile Trigger Timing
BUFF tile triggers when unit **ends movement** on the tile:
- Not during movement (passing through)
- Only at final position
- Triggers before any attack action in MOVE_AND_ATTACK

### 6.3 Obstacle Destruction with POWER Buff
Units with POWER buff can destroy obstacles:
- New action type: `DESTROY_OBSTACLE`
- Target: Adjacent obstacle tile
- Effect: Remove obstacle from map
- Still counts as the unit's action for the turn

### 6.4 Guardian (TANK) Passive

TANK's Guardian passive intercepts damage to adjacent friendly units:

**What Guardian DOES intercept:**
- Normal attacks (ATTACK, MOVE_AND_ATTACK)
- Skill damage (all skill types)
- Counter-attack damage (Feint, Challenge)
- Ranged attacks (any range)

**What Guardian does NOT intercept:**
- BLEED damage (not an attack action)
- System damage (minion decay, Round 8 pressure)
- Instant HP effects (WEAKNESS -1 HP)
- Timeout penalty
- Self-damage
- Attacks on the TANK itself

**Guardian Rules:**
- Adjacent = Manhattan distance 1 (up/down/left/right, NOT diagonal)
- Damage transfer = 100% of original damage
- No intercept limit per round
- Only protects friendly units
- Multiple TANKs: Lowest unit ID intercepts
- Dead TANK cannot intercept
- TANK cannot protect itself

```java
// Adjacency check
boolean isAdjacent(Position a, Position b) {
    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) == 1;
}

// Get guardian for unit
Unit getGuardian(Unit target, GameState state) {
    return state.getUnits().stream()
        .filter(u -> u.isAlive())
        .filter(u -> u.getOwner().equals(target.getOwner()))
        .filter(u -> u.getMinionType() == TANK)
        .filter(u -> !u.equals(target))  // Cannot protect self
        .filter(u -> isAdjacent(u.getPosition(), target.getPosition()))
        .min(Comparator.comparing(Unit::getId))
        .orElse(null);
}
```

### 6.5 Invisible Units (from Smoke Bomb)
Invisible units:
- Cannot be **targeted** by enemies
- Can still take **AoE damage** (Wild Magic, Shockwave, etc.)
- Invisibility breaks on any action (attack, skill, or start of next turn)

---

## 7. Priority Resolution Order

When multiple effects trigger simultaneously:

```
1. Damage dealt (in action order)
2. Death checks
3. On-death effects (obstacle/BUFF tile choice)
4. BUFF applications
5. Cooldown decrements
6. Round-end poison/decay
7. Final death checks
8. Victory check
```

---

## 8. Victory Check Timing

Victory is checked:
- After every action resolves
- After round-end processing
- Immediately when a hero's HP reaches 0

```java
void checkVictory(GameState state, PlayerId activePlayer) {
    Hero player1Hero = getHero(PLAYER_1);
    Hero player2Hero = getHero(PLAYER_2);
    
    if (!player1Hero.isAlive() && !player2Hero.isAlive()) {
        // Both dead - active player WINS (aggression rewarded)
        state.winner = activePlayer;
    } else if (!player1Hero.isAlive()) {
        state.winner = PLAYER_2;
    } else if (!player2Hero.isAlive()) {
        state.winner = PLAYER_1;
    }
}
```

---

## 8.1 Skill System Decisions

### STUN and Skills
- **STUN blocks all skill usage**
- Stunned hero cannot declare USE_SKILL action

### SLOW and Skills
- **SLOW delays skills by 1 round** (same as other actions)
- Skill enters "preparing" state
- Cooldown starts immediately when skill is declared
- If hero dies while preparing, skill is cancelled (cooldown NOT refunded)

### ROOT and Skills
- ROOT blocks **movement skills only** (Heroic Leap, Smoke Bomb, Warp Beacon teleport)
- Non-movement skills (Elemental Blast, Trinity, etc.) are NOT blocked

### Guardian and Skills
- **Guardian DOES intercept skill damage** (changed from earlier assumption)
- AoE skills: Each target is checked for adjacent friendly Tank
- Counter-attack damage is normal attack type → also intercepted by Guardian

### Warp Beacon Special Rules
- First use (place beacon): Does NOT trigger cooldown
- Second use (teleport): Triggers cooldown
- Beacon is **visible to enemies**
- Beacon **disappears** if Mage dies
- Cannot place beacon on occupied tile
- Cannot teleport if beacon position is now occupied

### Shadow Clone Special Rules
- Clone is marked as **temporary unit**
- Clone has 1 HP, 1 ATK
- Clone **can** move and attack
- Clone **cannot** be healed
- Clone death does **NOT** trigger death choice (no obstacle/BUFF tile)
- Clone removed when duration (2 rounds) expires

### Counter-Attack Rules (Feint, Challenge)
- Counter-attack damage is **normal attack** type
- Counter-attack is **intercepted by Guardian**
- Counter-attack does **NOT consume action**
- Counter-attacks trigger immediately upon condition

### Skill Damage vs ATK Bonus
- Skill damage is **fixed** (not affected by ATK modifiers)
- POWER buff (+3 ATK) does NOT increase skill damage
- WEAKNESS buff (-2 ATK) does NOT decrease skill damage
- Death Mark (+2 damage) DOES increase skill damage (it's a damage modifier, not ATK modifier)

---

## 9. Summary Table

| Question | Decision |
|----------|----------|
| Timeout penalty | Hero HP -1 |
| Round 8+ pressure | All units -1 HP |
| **Simultaneous hero death** | **Active player (attacker) WINS** |
| **Exhaustion rule** | **Opponent takes consecutive turns** |
| **Obstacle HP** | **3** |
| **Obstacle destruction** | **Any unit can attack, POWER = instant** |
| SLOW: Can be attacked? | Yes |
| SLOW: Cancel action? | No |
| SLOW: When executes? | Start of next round, player's turn |
| SLOW: Target moved? | Attack hits original position (misses) |
| SPEED: How it works? | 2 actions before turn ends |
| SPEED: MOVE_AND_ATTACK? | Counts as 1 action |
| Round increment | After both players end turn |
| Random BUFF selection | Use RngProvider.nextInt(6) |
| Skill source | JSON files (extensible) |
| **Guardian: Adjacent** | **Manhattan distance = 1 (4 directions)** |
| **Guardian: Damage transfer** | **100% to TANK** |
| **Guardian: Intercepts skills** | **Yes** |
| **Guardian: Intercepts BLEED** | **No (not an attack)** |
| **Guardian: Intercepts decay** | **No (system damage)** |
| **Guardian: Limit** | **Unlimited per round** |
| **Guardian: Multiple TANKs** | **Lowest unit ID intercepts** |
| AoE vs Invisible | AoE still hits |
| STUN blocks skills | Yes |
| SLOW delays skills | Yes, by 1 round |
| Warp Beacon on Mage death | Beacon disappears |
| Warp Beacon visibility | Visible to enemies |
| Shadow Clone death choice | No (does not trigger) |
| Shadow Clone healing | Cannot be healed |
| Counter-attack type | Normal attack (Guardian intercepts) |
| Counter-attack action cost | Does not consume action |

---

# End of DESIGN_DECISIONS_V3.md
