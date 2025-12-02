# 5x5 Tactics – Documentation Index

This directory contains the core specification files for the 5x5 Tactics Project.
All AI coding must strictly follow these documents and must not invent new classes,
fields, rules, or protocols beyond what is defined here.

---

## 1. TECH_ARCH.md
**System architecture overview.**  
Defines module boundaries (engine / server / client), core principles (determinism,
no cross-layer dependency), and high-level concepts such as GameState, Unit,
Action, RuleEngine, and replayability.

---

## 2. ENGINE_SKELETON_V1.md
**Engine-level Class & Package Skeleton (Mid-level Specification).**  
Defines the package layout, class responsibilities, allowed fields, required
interfaces, and method signatures.  
No implementations are included.  
This is the main reference for vibecoding the engine.

---

## 3. GAME_RULES_V1.md
**Formal game rule specification.**  
Defines unit stats, movement rules, attack rules, turn structure, win conditions,
and all logic constraints needed by RuleEngine.  
No protocol / networking details here.

---

## 4. WS_PROTOCOL_V1.md
**WebSocket communication protocol.**  
Defines request/response message formats, error codes, action encoding,
and server-authoritative update flow.

---

### Reading Order (for AI agents)
1. TECH_ARCH.md  
2. ENGINE_SKELETON_V1.md  
3. GAME_RULES_V1.md  
4. WS_PROTOCOL_V1.md  

The AI must respect this order and must not generate any code or specification
that violates or extends these documents.
