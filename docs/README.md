# Tactics 5×5 — Documentation Index

## READ THIS FIRST

This index describes all V3 documentation files. Start with `V3_QUICK_REFERENCE.md` for a high-level overview, then dive into specific docs as needed.

---

## File Map

```
docs/
├── README.md                       ← This file (START HERE)
├── V3_QUICK_REFERENCE.md           ← Quick stats & numbers
├── GAME_RULES_MASTER_SPEC_V3.md    ← Master spec (rules + flow combined)
├── UNIT_TYPES_V3.md                ← Hero & Minion definitions
├── BUFF_SYSTEM_V3.md               ← BUFF mechanics (6 types)
├── SKILL_SYSTEM_V3.md              ← Hero skill architecture
├── HERO_SKILLS_REFERENCE.md        ← Quick skill reference (中英對照)
└── DESIGN_DECISIONS_V3.md          ← Confirmed design decisions
```

---

## Reading Order by Task

### Implementing Core Engine
1. `GAME_RULES_MASTER_SPEC_V3.md` — Master spec (rules + flow + implementation checklist)
2. `UNIT_TYPES_V3.md` — Unit stats, passive abilities
3. `DESIGN_DECISIONS_V3.md` — Edge cases resolved

### Implementing BUFF System
1. `BUFF_SYSTEM_V3.md` — Full BUFF mechanics
2. `V3_QUICK_REFERENCE.md` — Quick stat reference

### Implementing Skill System
1. `SKILL_SYSTEM_V3.md` — Architecture & data structures
2. `HERO_SKILLS_REFERENCE.md` — All 18 skills defined
3. `DESIGN_DECISIONS_V3.md` — Guardian vs Skills, etc.

### Quick Lookup
- `V3_QUICK_REFERENCE.md` — Numbers, stats, constants
- `HERO_SKILLS_REFERENCE.md` — Skill effects at a glance

---

## Key Constants (V3)

| Constant | Value |
|----------|-------|
| Board | 5×5 |
| Team | 1 Hero + 2 Minions |
| Turn Timer | 10 seconds |
| BUFF Duration | 2 rounds |
| Skill Cooldown | 2 rounds |
| Pressure Start | Round 8 |

---

## Version History

| Version | Focus |
|---------|-------|
| V1 | Basic movement, attack, turn structure |
| V2 | Ranged attacks, moveRange/attackRange |
| V3 | Hero system, skills, BUFFs, death mechanics |

---

## Quick Start for Claude CLI

```bash
# Implement a feature
claude "根據 docs/ 實作 TANK 的 Guardian 被動"

# Fix tests  
claude "讓 BuffSystemTest 的所有測試通過"

# Add new skill
claude "新增一個 Warrior 技能：Whirlwind，對周圍 2 格敵人造成 1 傷害"
```

---

## SPD Reference Files

Original SPD ability implementations are in:
```
abilities/
├── warrior/   (HeroicLeap, Shockwave, Endure)
├── mage/      (ElementalBlast, WarpBeacon, WildMagic)
├── rogue/     (SmokeBomb, DeathMark, ShadowClone)
├── huntress/  (SpiritHawk, SpectralBlades, NaturesPower)
├── duelist/   (Challenge, ElementalStrike, Feint)
└── cleric/    (Trinity, PowerOfMany, AscendedForm)
```

These are reference implementations. V3 skills are simplified adaptations for 5×5 tactics.

---

# End of README.md
