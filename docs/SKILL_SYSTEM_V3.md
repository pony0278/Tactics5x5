# SKILL_SYSTEM_V3.md — Hero Skill System (Version 3)

## Overview

V3 introduces a modular, extensible skill system for Heroes based on SPD (Shattered Pixel Dungeon) character classes. Each hero has access to a skill tree with 3 skills, and players select 1 skill to bring into battle.

---

## 1. Core Concepts

### 1.1 Skill Properties
| Property | Description |
|----------|-------------|
| `skillId` | Unique identifier (e.g., `warrior_heroic_leap`) |
| `name` | Display name |
| `description` | What the skill does |
| `heroClass` | Which hero class can use this skill |
| `cooldown` | Rounds before skill can be used again (default: 2) |
| `targetType` | How the skill selects targets |
| `range` | Maximum distance for targeted skills |
| `effects` | List of effects when activated |

### 1.2 Target Types
| Type | Description |
|------|-------------|
| `SELF` | Affects only the caster |
| `SINGLE_ENEMY` | Target one enemy unit |
| `SINGLE_ALLY` | Target one friendly unit |
| `SINGLE_TILE` | Target an empty tile |
| `AREA_AROUND_SELF` | Affects tiles around caster |
| `AREA_AROUND_TARGET` | Affects tiles around target |
| `LINE` | Affects tiles in a straight line |
| `ALL_ENEMIES` | Affects all enemy units |
| `ALL_ALLIES` | Affects all friendly units |

### 1.3 Effect Types
| Effect | Parameters | Description |
|--------|------------|-------------|
| `DAMAGE` | `amount` | Deal damage to target |
| `HEAL` | `amount` | Restore HP |
| `MOVE_SELF` | `targetTile` | Teleport/leap to position |
| `MOVE_TARGET` | `direction`, `distance` | Push/pull target |
| `APPLY_BUFF` | `buffType`, `duration` | Apply BUFF to target |
| `REMOVE_BUFF` | `buffType` | Remove BUFF from target |
| `SPAWN_UNIT` | `unitType`, `position` | Create temporary unit |
| `SPAWN_OBSTACLE` | `position` | Create obstacle |
| `STUN` | `duration` | Prevent target from acting |
| `MARK` | `duration` | Mark target for bonus damage |

---

## 2. Skill Selection Flow

### 2.1 Pre-Match Draft
```
1. Player's SPD character class determines available skills
2. System presents 3 skills from that class
3. Player selects 1 skill to equip
4. Selected skill stored in Hero.selectedSkillId
```

### 2.2 In-Match Usage
```
1. Player selects USE_SKILL action
2. If skill requires target: player selects target
3. Validation checks:
   - Hero is alive
   - Skill is not on cooldown
   - Target is valid (if required)
4. Skill activates, effects applied
5. Cooldown set to skill.cooldown value
```

---

## 3. Cooldown System

### 3.1 Cooldown Rules
- Default cooldown: **2 rounds**
- Cooldown decrements at **end of each round**
- Skill usable when `cooldown == 0`
- Using skill sets `currentCooldown = skill.cooldown`

### 3.2 Cooldown Tracking
```java
class Hero {
    String selectedSkillId;
    int skillCooldown;  // 0 = ready, >0 = rounds remaining
}
```

### 3.3 Round End Processing
```java
// In round-end processing
for (Hero hero : getAllHeroes()) {
    if (hero.skillCooldown > 0) {
        hero.skillCooldown--;
    }
}
```

---

## 4. Hero Classes

V3 supports 6 hero classes from SPD:

| Class | Role | Skill Theme |
|-------|------|-------------|
| WARRIOR | Tank/Bruiser | Mobility, AoE damage, survivability |
| MAGE | Ranged/Control | Teleport, elemental damage, utility |
| ROGUE | Assassin | Stealth, burst damage, debuffs |
| HUNTRESS | Ranged/Summoner | Ranged attacks, summons, nature magic |
| DUELIST | Duelist | Single-target, counter-attacks, challenges |
| CLERIC | Support | Healing, buffs, protection |

---

## 5. Skill Definitions

### 5.1 WARRIOR Skills

#### Heroic Leap
| Property | Value |
|----------|-------|
| ID | `warrior_heroic_leap` |
| Target | `SINGLE_TILE` |
| Range | 3 |
| Cooldown | 2 |

**Effect:**
- Hero leaps to target tile
- Deals 2 damage to all adjacent enemies upon landing
- Cannot leap to occupied tiles

```
Use case: Gap close, escape, or AoE initiation
```

#### Shockwave
| Property | Value |
|----------|-------|
| ID | `warrior_shockwave` |
| Target | `AREA_AROUND_SELF` |
| Range | 1 (adjacent) |
| Cooldown | 2 |

**Effect:**
- Deals 1 damage to all adjacent enemies
- Pushes all adjacent enemies 1 tile away
- If enemy cannot be pushed (blocked), deal +1 damage instead

```
Use case: Create space, combo with terrain
```

#### Endure
| Property | Value |
|----------|-------|
| ID | `warrior_endure` |
| Target | `SELF` |
| Cooldown | 2 |

**Effect:**
- Hero gains 3 temporary HP (shield)
- Shield lasts 2 rounds
- Removes BLEED debuff if present

```
Use case: Survive burst damage, tank for minions
```

---

### 5.2 MAGE Skills

#### Elemental Blast
| Property | Value |
|----------|-------|
| ID | `mage_elemental_blast` |
| Target | `SINGLE_ENEMY` |
| Range | 3 |
| Cooldown | 2 |

**Effect:**
- Deals 3 damage to target
- 50% chance to apply random debuff (WEAKNESS, BLEED, or SLOW)

```
Use case: Long-range burst damage
```

#### Warp Beacon
| Property | Value |
|----------|-------|
| ID | `mage_warp_beacon` |
| Target | `SINGLE_TILE` |
| Range | 4 |
| Cooldown | 2 |

**Effect:**
- First use: Place beacon on target tile
- Second use: Teleport to beacon position
- Beacon persists until used or Mage dies

```
Use case: Strategic repositioning, escape route
```

#### Wild Magic
| Property | Value |
|----------|-------|
| ID | `mage_wild_magic` |
| Target | `ALL_ENEMIES` |
| Cooldown | 2 |

**Effect:**
- Deals 1 damage to ALL enemy units
- Each enemy has 33% chance to receive random debuff

```
Use case: Chip damage, debuff spread
```

---

### 5.3 ROGUE Skills

#### Smoke Bomb
| Property | Value |
|----------|-------|
| ID | `rogue_smoke_bomb` |
| Target | `SINGLE_TILE` |
| Range | 3 |
| Cooldown | 2 |

**Effect:**
- Teleport to target tile
- Become invisible for 1 round (enemies cannot target you)
- Adjacent enemies at original position are BLINDED (cannot attack) for 1 round

```
Use case: Escape, reposition for backstab
```

#### Death Mark
| Property | Value |
|----------|-------|
| ID | `rogue_death_mark` |
| Target | `SINGLE_ENEMY` |
| Range | 2 |
| Cooldown | 2 |

**Effect:**
- Mark target for 2 rounds
- Marked targets take +2 damage from all sources
- If marked target dies, Rogue heals 2 HP

```
Use case: Focus fire, sustain in fights
```

#### Shadow Clone
| Property | Value |
|----------|-------|
| ID | `rogue_shadow_clone` |
| Target | `SINGLE_TILE` (adjacent) |
| Range | 1 |
| Cooldown | 2 |

**Effect:**
- Spawn a Shadow Clone on adjacent tile
- Clone has 1 HP, 1 ATK, can move and attack
- Clone lasts 2 rounds then disappears
- Clone does not trigger death mechanics (no obstacle/BUFF tile)

```
Use case: Extra damage, body block, distraction
```

---

### 5.4 HUNTRESS Skills

#### Spirit Hawk
| Property | Value |
|----------|-------|
| ID | `huntress_spirit_hawk` |
| Target | `SINGLE_ENEMY` |
| Range | 4 |
| Cooldown | 2 |

**Effect:**
- Summon hawk to attack target from any range
- Deals 2 damage
- Reveals target's position (if fog of war implemented)

```
Use case: Long-range poke, finish low HP targets
```

#### Spectral Blades
| Property | Value |
|----------|-------|
| ID | `huntress_spectral_blades` |
| Target | `LINE` |
| Range | 3 |
| Cooldown | 2 |

**Effect:**
- Fire blades in a line
- Deals 1 damage to all enemies in the line
- Pierces through enemies (hits all in line)

```
Use case: Multi-target damage, zone control
```

#### Nature's Power
| Property | Value |
|----------|-------|
| ID | `huntress_natures_power` |
| Target | `SELF` |
| Cooldown | 2 |

**Effect:**
- Next 2 attacks deal +2 damage
- Applies LIFE buff to self (+3 HP instant)

```
Use case: Burst damage setup, self-sustain
```

---

### 5.5 DUELIST Skills

#### Challenge
| Property | Value |
|----------|-------|
| ID | `duelist_challenge` |
| Target | `SINGLE_ENEMY` |
| Range | 2 |
| Cooldown | 2 |

**Effect:**
- Mark target as "Challenged" for 2 rounds
- Challenged enemy deals 50% damage to non-Duelist targets
- If Challenged enemy attacks Duelist, Duelist counter-attacks for 2 damage

```
Use case: Protect allies, bait attacks
```

#### Elemental Strike
| Property | Value |
|----------|-------|
| ID | `duelist_elemental_strike` |
| Target | `SINGLE_ENEMY` |
| Range | 1 |
| Cooldown | 2 |

**Effect:**
- Deals 3 damage to adjacent enemy
- Applies one of: BLEED, SLOW, or WEAKNESS (player chooses)

```
Use case: Burst + guaranteed debuff
```

#### Feint
| Property | Value |
|----------|-------|
| ID | `duelist_feint` |
| Target | `SELF` |
| Cooldown | 2 |

**Effect:**
- Next attack against Duelist misses automatically
- If attack misses, Duelist can immediately counter-attack for 2 damage
- Lasts until triggered or 2 rounds

```
Use case: Defensive play, punish attackers
```

---

### 5.6 CLERIC Skills

#### Trinity
| Property | Value |
|----------|-------|
| ID | `cleric_trinity` |
| Target | `SINGLE_ALLY` (or SELF) |
| Range | 2 |
| Cooldown | 2 |

**Effect:**
- Heal target for 3 HP
- Remove one random debuff
- Grant LIFE buff (+3 HP instant)

```
Use case: Primary heal, save low HP allies
```

#### Power of Many
| Property | Value |
|----------|-------|
| ID | `cleric_power_of_many` |
| Target | `ALL_ALLIES` |
| Cooldown | 2 |

**Effect:**
- Heal ALL friendly units for 1 HP
- Grant all allies +1 ATK for 1 round

```
Use case: Team-wide sustain, push advantage
```

#### Ascended Form
| Property | Value |
|----------|-------|
| ID | `cleric_ascended_form` |
| Target | `SELF` |
| Cooldown | 2 |

**Effect:**
- Become invulnerable for 1 round (cannot take damage)
- During invulnerability, healing effects are doubled
- Cannot attack while invulnerable

```
Use case: Survive focus, setup big heal
```

---

## 6. Validation Rules

### 6.1 USE_SKILL Validation
```
Invalid if:
- Unit is not a Hero
- Hero has no selectedSkillId
- skillCooldown > 0
- Hero is dead
- Hero is stunned (STUN debuff)
- Target is invalid for skill's targetType
- Target is out of range
- Target tile is blocked (for SINGLE_TILE skills)
```

### 6.2 Target Validation by Type
| Target Type | Valid Targets |
|-------------|---------------|
| SELF | Always valid |
| SINGLE_ENEMY | Alive enemy unit in range |
| SINGLE_ALLY | Alive friendly unit in range |
| SINGLE_TILE | Empty, passable tile in range |
| AREA_* | At least one valid target in area |
| LINE | Valid direction with at least one target |
| ALL_* | At least one valid target exists |

---

## 7. Data Structures

### 7.1 Skill Definition
```java
class SkillDefinition {
    String id;
    String name;
    String description;
    HeroClass heroClass;
    
    TargetType targetType;
    int range;
    int cooldown;
    
    List<SkillEffect> effects;
}
```

### 7.2 Skill Effect
```java
class SkillEffect {
    EffectType type;
    int amount;           // For DAMAGE, HEAL
    BuffType buffType;    // For APPLY_BUFF, REMOVE_BUFF
    int duration;         // For buffs, marks
    String spawnType;     // For SPAWN_UNIT
}
```

### 7.3 Hero Skill State
```java
class Hero {
    // ... other fields
    String selectedSkillId;
    int skillCooldown;
    
    // For skills with state (e.g., Warp Beacon)
    Map<String, Object> skillState;
}
```

---

## 8. Extensibility

### 8.1 Adding New Skills
To add a new skill:
1. Create skill definition in `skills/[heroclass]/[SkillName].json`
2. Implement any custom effect handlers
3. Add to hero class skill list
4. No engine changes needed for standard effects

### 8.2 Skill Definition JSON Format
```json
{
  "id": "warrior_new_skill",
  "name": "New Skill",
  "description": "Description here",
  "heroClass": "WARRIOR",
  "targetType": "SINGLE_ENEMY",
  "range": 2,
  "cooldown": 2,
  "effects": [
    {
      "type": "DAMAGE",
      "amount": 3
    },
    {
      "type": "APPLY_BUFF",
      "buffType": "SLOW",
      "duration": 2
    }
  ]
}
```

### 8.3 Custom Effect Handlers
For complex skills that can't be expressed with standard effects:
```java
interface CustomSkillHandler {
    void activate(GameState state, Hero hero, Target target);
    boolean validate(GameState state, Hero hero, Target target);
}

// Register custom handler
skillRegistry.registerHandler("mage_warp_beacon", new WarpBeaconHandler());
```

---

## 9. Implementation Checklist

- [ ] SkillDefinition data structure
- [ ] SkillEffect enum and handlers
- [ ] HeroClass enum with skill lists
- [ ] Skill selection during draft phase
- [ ] USE_SKILL action type
- [ ] Skill validation in RuleEngine
- [ ] Skill execution in RuleEngine
- [ ] Cooldown tracking and decrement
- [ ] Standard effect implementations (DAMAGE, HEAL, etc.)
- [ ] Custom handlers for complex skills (Warp Beacon, etc.)
- [ ] JSON skill definitions for all 18 skills
- [ ] Unit tests for each skill

---

# End of SKILL_SYSTEM_V3.md
