# V3_QUICK_REFERENCE.md — Implementation Quick Reference

## File Structure
```
docs/
├── GAME_RULES_MASTER_SPEC_V3.md  # Master spec (rules + flow)
├── UNIT_TYPES_V3.md              # Hero & Minion definitions
├── BUFF_SYSTEM_V3.md             # BUFF mechanics
└── V3_QUICK_REFERENCE.md         # This file
```

---

## Key Numbers (V3)

| Constant | Value | Notes |
|----------|-------|-------|
| Board Size | 5×5 | Fixed |
| Team Size | 3 | 1 Hero + 2 Minions |
| Draft Pool | 3 | Pick 2 minions |
| Turn Timer | 10 sec | Timeout = Hero -1 HP |
| BUFF Duration | 2 rounds | All BUFFs |
| BUFF Tile Duration | 2 rounds | On map |
| **Obstacle HP** | **3** | **Any unit can attack** |
| Pressure Start | Round 8 | All units -1 HP/round |
| Skill Cooldown | 2 rounds | After use |

---

## Unit Quick Stats

| Unit | HP | ATK | Move | Range | Special |
|------|----|----|------|-------|---------|
| Hero | 5 | 1 | 1 | 1 | 1 Skill |
| Tank | 5 | 1 | 1 | 1 | Guardian |
| Archer | 3 | 1 | 1 | 3 | — |
| Assassin | 2 | 2 | 4 | 1 | — |

---

## BUFF Quick Stats

| BUFF | ATK | HP | Special |
|------|-----|----|---------|
| POWER | +3 | +1 | No move+attack, **1-hit obstacle destroy** |
| LIFE | — | +3 | — |
| SPEED | -1 | — | Double action |
| WEAKNESS | -2 | -1 | — |
| BLEED | — | -1/round | DoT |
| SLOW | — | — | Actions delayed |

---

## Victory Condition
**Kill enemy Hero = Win**

---

## Turn Order
```
Player A Unit 1 → Player B Unit 1 → 
Player A Unit 2 → Player B Unit 2 → 
Player A Unit 3 → Player B Unit 3 → 
Round End → Next Round
```

**Exhaustion Rule**: If one player runs out of units, opponent takes consecutive turns for remaining units.

---

## Round End Processing Order
1. BLEED damage (all units with BLEED)
2. Minion decay (-1 HP to all minions)
3. BUFF duration -1, remove expired
4. BUFF tile duration -1, remove expired
5. Round 8+ pressure check

---

## Death Mechanics
| Unit Type | On Death |
|-----------|----------|
| Hero | Game Over (opponent wins) |
| Minion | Owner chooses: Obstacle OR BUFF Tile |

---

## Confirmed Design Decisions

| Question | Answer |
|----------|--------|
| Timeout penalty | **Hero** loses 1 HP |
| Round 8+ pressure | **All units** lose 1 HP/round |
| Simultaneous death | **Active player (attacker) WINS** |
| Exhaustion rule | **Opponent takes consecutive turns** |
| Obstacle HP | **3** (any unit can attack) |
| POWER vs Obstacle | **Instant destroy (1 hit)** |
| SLOW: attackable? | **Yes**, can be attacked while preparing |
| SLOW: cancel? | **No**, cannot cancel declared action |
| Skills source | **JSON files** (extensible architecture) |

See `DESIGN_DECISIONS_V3.md` for full details.

---

## Claude CLI Usage Examples

### Implement a specific feature:
```bash
claude "根據 docs/UNIT_TYPES_V3.md 實作 TANK 的 Guardian 被動技能"
```

### Fix a specific test:
```bash
claude "讓 BuffTileTest 的 testPowerBuffBlocksMoveAndAttack 通過"
```

### General implementation:
```bash
claude "實作 V3 的回合結束處理邏輯，包括 BLEED 傷害和小兵衰減"
```

---

# End of V3_QUICK_REFERENCE.md
