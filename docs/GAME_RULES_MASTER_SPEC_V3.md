# GAME_RULES_MASTER_SPEC_V3.md — 5×5 Tactics Master Specification

## 1. Overview

This document defines the complete logic, rules, and flow for the **5×5 Tactics** game mode within the Mastery System Pixel Dungeon.

**Design Philosophy:**
- **Skill-Based & Deterministic:** No hidden RNG outcomes. All information is public or revealed immediately upon generation.
- **High Pace:** Matches are designed to end within 3-5 minutes via attrition mechanics.
- **RMG Compliant:** Rules are strictly defined to support real-money competitive play.

---

## 2. Core Entities & Definitions

### 2.1 The Board
- **Grid:** 5×5 (Rows 0-4, Columns 0-4).
- **Coordinates:** (x, y) where x is column, y is row.

### 2.2 Team Composition
Each player fields 3 units:
1.  **HERO (1):** Based on the player's RPG character (Stats normalized for PvP).
2.  **MINIONS (2):** Selected during the Draft Phase.

### 2.3 Map Objects
Map objects are entities that occupy a tile but are not units.

| Object Type | Properties |
| :--- | :--- |
| **OBSTACLE** | • **HP:** 3<br>• **Blocking:** Blocks Movement and Line of Sight (LOS).<br>• **Destructible:** Can be attacked by any unit.<br>• **Interaction:** Units with `POWER` buff destroy it in 1 hit. |
| **BUFF TILE** | • **Visibility:** Buff type is revealed immediately on spawn (Icon + Color).<br>• **Trigger:** Applied when a unit moves onto the tile.<br>• **Duration:** Tile lasts 2 rounds; Buff effect lasts 2 rounds.<br>• **Consumption:** Removed after one use. |

### 2.4 Overwrite Rule (Crucial)
If a new Map Object is spawned on a tile that already contains a Map Object (e.g., a corpse turns into an Obstacle on top of an existing Buff Tile):
- **Rule:** **New Overwrites Old.**
- The existing object is immediately destroyed/removed.
- The new object takes its place.

---

## 3. Unit Archetypes

### 3.1 Minions (Draft Pool)
Players draft 2 out of 3 available types.

| Archetype | Key Trait / Ability |
| :--- | :--- |
| **TANK** | **Passive: Guardian Intercept**<br>If an adjacent ally is attacked, the Tank takes the damage instead. |
| **ARCHER** | **Range Restriction**<br>Cannot use `MOVE_AND_ATTACK` at max range. Must be adjacent to target to Move & Attack. |
| **ASSASSIN** | **High Mobility**<br>(Specific stats/skills defined in data tables). |

### 3.2 Hero
- **Source:** Player's SPD Character.
- **Scaling:** Stats are **normalized** for 5×5 balance (e.g., HP capped at 30, Dmg 3-5).
- **Skill:** Player selects 1 active skill during Draft.

---

## 4. Match Lifecycle

### Phase 1: Draft
1.  **Minion Draft:** Both players simultaneously select **2 Minions** from the pool of 3 (Tank, Archer, Assassin). Selections are hidden until Phase 3.
2.  **Skill Select:** Both players select **1 Active Skill** for their Hero.
3.  **Reveal:** A brief animation reveals opposing compositions.

### Phase 2: Setup
Units are placed in fixed starting positions.
- **Player 1 (Bottom):** Row 0 `[Minion] [__] [HERO] [__] [Minion]`
- **Player 2 (Top):** Row 4 `[Minion] [__] [HERO] [__] [Minion]`

### Phase 3: Battle (The Loop)
The game enters the turn-based loop described in Section 5.

---

## 5. Battle Mechanics

### 5.1 Turn Structure
- **Alternating Actions:** P1 Unit 1 → P2 Unit 1 → P1 Unit 2 → ...
- **Exhaustion Rule:**
    - If Player A has moved all their units, but Player B still has unused units:
    - Player B takes **consecutive turns** for their remaining units.
    - *UI Requirement:* Show "Opponent Finishing Turn" indicator.
- **Round End:** A round ends only when **ALL** living units have acted once.

### 5.2 Action Timer
- **Limit:** 10 seconds per action.
- **Timeout Penalty:**
    1.  The acting unit's turn is skipped (`END_TURN`).
    2.  **The Player's HERO loses 1 HP** (Global penalty).

### 5.3 Action Types & Constraints

| Action | Logic & Constraints |
| :--- | :--- |
| **MOVE** | Move up to `moveRange`.<br>Cannot move through Obstacles or Units. |
| **ATTACK** | Attack target within `attackRange`.<br>Cannot attack through Obstacles (unless LoS ignores). |
| **MOVE & ATTACK** | 1. Move up to `moveRange`.<br>2. **Constraint:** Attack is **only** allowed if target is **Adjacent (Distance 1)**.<br>*Note: This prevents ranged kiting.* |
| **USE SKILL** | Use Hero skill (Range/Effect varies). |
| **END TURN** | Pass action. |

### 5.4 Action Execution Flow (with Guardian Intercept)
When a player submits an `ATTACK` command:

1.  **Validation:** Check range, cooldowns, and line of sight.
2.  **Guardian Check (The Tank Passive):**
    - Is the target adjacent to an enemy **TANK**?
    - Is the Tank alive?
    - **IF YES:** The attack target is redirected to the **TANK**.
3.  **Damage Calculation:** Calculate damage (Base + Buffs - Defense).
4.  **Apply Damage:** Reduce HP.
5.  **Trigger On-Hit Effects:** Bleed application, etc.
6.  **Death Check:** If HP ≤ 0, proceed to **Section 7 (Death Logic)**.

---

## 6. System Mechanics (Round End)

At the end of every round, the system processes effects in this **strict order**:

1.  **BLEED Damage:** Apply damage to units with Bleed status.
2.  **MINION DECAY (Attrition A):**
    - **Condition:** Round ≥ 3.
    - **Effect:** All Minions lose 1 HP.
3.  **LATE GAME PRESSURE (Attrition B):**
    - **Condition:** Round ≥ 8.
    - **Effect:** **ALL Units** (Heroes included) lose 1 HP.
    - *Note: Stacks with Decay (Minions lose 2 HP total).*
4.  **SYSTEM DEATH CHECK:**
    - Identify units that died from Step 1, 2, or 3.
    - Resolve spawning via **System Death Logic** (See Section 7.2).
5.  **VICTORY CHECK:** If Hero is dead, end game.
6.  **DURATION TICK:** Decrement duration of Buffs and Map Objects. Remove expired.
7.  **INCREMENT ROUND:** Round N becomes N+1.

---

## 7. Death & Spawning Logic

### 7.1 Hero Death
- **Effect:** Immediate Game Over.
- **Simultaneous Death Rule:** If an action (e.g., Reflect Dmg, AoE) kills BOTH heroes instantly, the **Active Player** (the one who initiated the action) **WINS**.

### 7.2 Minion Death & Spawning
When a minion dies, it **always** spawns a Map Object. The type depends on the **Cause of Death**.

#### Scenario A: Combat Death (PvP)
- **Cause:** Killed by Attack, Skill, or Reflection.
- **Logic:** **Active Player Choice.**
- The killer chooses immediately:
    1.  Spawn **OBSTACLE**
    2.  Spawn **BUFF TILE** (Visible type)

#### Scenario B: System Death (Environment)
- **Cause:** Killed by Decay, Bleed, or Pressure.
- **Logic:** **Deterministic Odd/Even Rule.**
    - **Odd Rounds (3, 5, 7...):** Spawns **OBSTACLE**.
    - **Even Rounds (4, 6, 8...):** Spawns **BUFF TILE**.

### 7.3 Spawning Constraints
- **Overwrite Rule:** If the death tile already has a Buff Tile, the new spawn (Obstacle or new Buff) **destroys and replaces** the old one.
- **Buff Determinism:** The type of Buff Tile spawned is determined by the match seed. It is **never** random selection at the moment of spawn.

---

## 8. Implementation Checklist for Developers

- [ ] **Data Structure:** Ensure `Unit` class has `isHero`, `minionType` (enum), and `stats`.
- [ ] **Tank Passive:** Implement logic to redirect damage if neighbor is Tank.
- [ ] **Anti-Kiting:** Implement `Move & Attack` range check (Target distance must be 1).
- [ ] **Turn Logic:** Implement Exhaustion state (consecutive turns).
- [ ] **Death Handler:**
    - Separate logic for `CombatDeath` (User Input required) vs `SystemDeath` (Auto).
    - Implement `Overwrite` logic for tile spawning.
- [ ] **Round End Loop:** Strictly follow the order in Section 6.
- [ ] **Timer:** Implement 10s timer with `-1 HP to Hero` penalty.

---

### 文檔整合說明
1.  **整合了 Tank Passive:** 在 `3.1 Minion Archetypes` 定義，並在 `5.4 Action Execution Flow` 中詳述了觸發時機。
2.  **整合了 Overwrite Rule:** 在 `2.4` 和 `7.3` 明確定義了新物件覆蓋舊物件的規則。
3.  **整合了 Attrition:** 將 Rules 的規則與 Flow 的結算順序合併在 `Section 6`。
4.  **結構化:** 將內容重新編排為實作導向（實體 -> 流程 -> 邏輯 -> 檢核表）。