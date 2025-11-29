/**
 * 5x5 Tactics - Minimal Web Client
 * Following CLIENT_WIREFRAME_V1 specification
 */

// =============================================================================
// Configuration
// =============================================================================

const WS_URL = "ws://localhost:8080/ws"; // Placeholder URL

// =============================================================================
// Client State
// =============================================================================

const clientState = {
    matchId: "match-1",       // Hard-coded for V1
    playerId: null,           // Server-assigned (P1 or P2)
    connected: false,
    gameState: null,          // Last GameState from server
    selectedUnitId: null,     // String or null
    pendingActionType: null,  // "MOVE", "ATTACK", "MOVE_AND_ATTACK", or null
    lastErrorMessage: null    // String or null
};

// =============================================================================
// WebSocket Manager
// =============================================================================

let ws = null;

/**
 * Initialize WebSocket connection.
 */
function connectWebSocket() {
    updateConnectionStatus("Connecting...");

    ws = new WebSocket(WS_URL);

    ws.onopen = function(event) {
        // TODO: Implement onopen handler
        clientState.connected = true;
        updateConnectionStatus("Connected");
        console.log("WebSocket connected");

        // Send join_match on connect
        sendJoinMatch();
    };

    ws.onclose = function(event) {
        // TODO: Implement onclose handler
        clientState.connected = false;
        updateConnectionStatus("Disconnected");
        console.log("WebSocket disconnected");
    };

    ws.onerror = function(error) {
        console.error("WebSocket error:", error);
        updateConnectionStatus("Error");
    };

    ws.onmessage = function(event) {
        // TODO: Implement onmessage handler
        console.log("Message received:", event.data);
        handleServerMessage(event.data);
    };
}

// =============================================================================
// Outgoing Messages
// =============================================================================

/**
 * Send join_match message to server.
 */
function sendJoinMatch() {
    const message = {
        type: "join_match",
        payload: {
            matchId: clientState.matchId
        }
    };

    sendMessage(message);
}

/**
 * Send action message to server.
 * @param {string} actionType - "MOVE", "ATTACK", "MOVE_AND_ATTACK", or "END_TURN"
 * @param {number|null} targetX - Target X coordinate
 * @param {number|null} targetY - Target Y coordinate
 * @param {string|null} targetUnitId - Target unit ID
 */
function sendAction(actionType, targetX, targetY, targetUnitId) {
    // TODO: Implement sendAction
    const message = {
        type: "action",
        payload: {
            matchId: clientState.matchId,
            playerId: clientState.playerId,
            action: {
                type: actionType,
                targetX: targetX,
                targetY: targetY,
                targetUnitId: targetUnitId
            }
        }
    };

    sendMessage(message);
}

/**
 * Send a message via WebSocket.
 * @param {object} message - Message object to send
 */
function sendMessage(message) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(message));
        logMessage("Sent: " + message.type);
    } else {
        console.error("WebSocket not connected");
    }
}

// =============================================================================
// Incoming Message Handlers
// =============================================================================

/**
 * Handle incoming server message.
 * @param {string} data - Raw JSON string from server
 */
function handleServerMessage(data) {
    try {
        const message = JSON.parse(data);
        const type = message.type;
        const payload = message.payload;

        switch (type) {
            case "match_joined":
                handleMatchJoined(payload);
                break;
            case "state_update":
                handleStateUpdate(payload);
                break;
            case "validation_error":
                handleValidationError(payload);
                break;
            case "game_over":
                handleGameOver(payload);
                break;
            case "game_ready":
                handleGameReady(payload);
                break;
            case "player_disconnected":
                handlePlayerDisconnected(payload);
                break;
            default:
                console.warn("Unknown message type:", type);
        }
    } catch (e) {
        console.error("Failed to parse server message:", e);
    }
}

/**
 * Handle match_joined message.
 * @param {object} payload - { matchId, playerId, state }
 */
function handleMatchJoined(payload) {
    console.log("Match joined:", payload);

    // Store server-assigned playerId
    clientState.playerId = payload.playerId;
    clientState.matchId = payload.matchId;
    clientState.gameState = payload.state;
    clientState.selectedUnitId = null;
    clientState.pendingActionType = null;
    clientState.lastErrorMessage = null;

    logMessage("Joined as " + payload.playerId);

    renderBoard();
    renderControls();
}

/**
 * Handle state_update message.
 * @param {object} payload - { state }
 */
function handleStateUpdate(payload) {
    // TODO: Implement handleStateUpdate
    console.log("State update:", payload);
    clientState.gameState = payload.state;
    clientState.pendingActionType = null;
    clientState.lastErrorMessage = null;

    renderBoard();
    renderControls();
}

/**
 * Handle validation_error message.
 * @param {object} payload - { message, action }
 */
function handleValidationError(payload) {
    // TODO: Implement handleValidationError
    console.log("Validation error:", payload);
    clientState.lastErrorMessage = payload.message;

    renderError(payload.message);
}

/**
 * Handle game_over message.
 * @param {object} payload - { winner, state }
 */
function handleGameOver(payload) {
    console.log("Game over:", payload);
    clientState.gameState = payload.state;

    renderBoard();
    renderControls();
    showGameOver(payload.winner);
}

/**
 * Handle game_ready message (both players connected).
 * @param {object} payload - { message }
 */
function handleGameReady(payload) {
    console.log("Game ready:", payload);
    logMessage(payload.message || "Both players connected!");
}

/**
 * Handle player_disconnected message.
 * @param {object} payload - { playerId }
 */
function handlePlayerDisconnected(payload) {
    console.log("Player disconnected:", payload);
    logMessage("Player " + payload.playerId + " disconnected");
    renderError("Opponent disconnected");
}

// =============================================================================
// Rendering Functions
// =============================================================================

/**
 * Render the 5x5 board based on current gameState.
 */
function renderBoard() {
    console.log("renderBoard called");

    // Clear all cells
    const cells = document.querySelectorAll(".cell");
    cells.forEach(cell => {
        cell.innerHTML = "";
        cell.className = "cell";
    });

    // Render units if gameState exists
    if (clientState.gameState && clientState.gameState.units) {
        clientState.gameState.units.forEach(unit => {
            if (!unit.alive) return; // Don't render dead units

            const x = unit.position.x;
            const y = unit.position.y;
            const cell = document.querySelector(`.cell[data-x="${x}"][data-y="${y}"]`);

            if (cell) {
                // Create unit element
                const unitEl = document.createElement("div");
                unitEl.className = "unit";
                unitEl.dataset.unitId = unit.id;

                // Add owner class for styling
                unitEl.classList.add(unit.owner === "P1" ? "player1" : "player2");

                // Add selected class if this unit is selected
                if (clientState.selectedUnitId === unit.id) {
                    unitEl.classList.add("selected");
                    cell.classList.add("selected");
                }

                // Show unit info
                unitEl.innerHTML = `
                    <div class="unit-id">${unit.id}</div>
                    <div class="unit-stats">HP:${unit.hp} ATK:${unit.attack}</div>
                `;

                cell.appendChild(unitEl);

                // Mark cell as occupied
                cell.classList.add("occupied");
                cell.classList.add(unit.owner === "P1" ? "p1-unit" : "p2-unit");
            }
        });
    }

    // Highlight valid move targets if MOVE is pending
    if (clientState.pendingActionType === "MOVE" && clientState.selectedUnitId) {
        highlightValidMoves();
    }
}

/**
 * Render the controls panel (status, player info, etc.).
 */
function renderControls() {
    // TODO: Implement renderControls
    console.log("renderControls called");

    // Update local player display
    const localPlayerEl = document.getElementById("local-player-id");
    if (localPlayerEl) {
        localPlayerEl.textContent = clientState.playerId;
    }

    // Update current turn display
    const currentPlayerEl = document.getElementById("current-player");
    if (currentPlayerEl && clientState.gameState) {
        currentPlayerEl.textContent = clientState.gameState.currentPlayer || "--";
    }

    // Update match ID display
    const matchIdEl = document.getElementById("match-id");
    if (matchIdEl) {
        matchIdEl.textContent = clientState.matchId;
    }

    // Update selected unit display
    const selectedUnitEl = document.getElementById("selected-unit");
    if (selectedUnitEl) {
        selectedUnitEl.textContent = clientState.selectedUnitId || "None";
    }

    // Update pending action display
    const pendingActionEl = document.getElementById("pending-action");
    if (pendingActionEl) {
        pendingActionEl.textContent = clientState.pendingActionType || "None";
    }
}

/**
 * Update connection status display.
 * @param {string} status - Connection status text
 */
function updateConnectionStatus(status) {
    const statusEl = document.getElementById("connection-status");
    if (statusEl) {
        statusEl.textContent = status;
    }
}

/**
 * Render an error message.
 * @param {string} message - Error message text
 */
function renderError(message) {
    const errorEl = document.getElementById("error");
    if (errorEl) {
        errorEl.textContent = message;
    }
}

/**
 * Clear the error display.
 */
function clearError() {
    const errorEl = document.getElementById("error");
    if (errorEl) {
        errorEl.textContent = "";
    }
}

/**
 * Show game over banner.
 * @param {string|null} winner - Winner player ID or null
 */
function showGameOver(winner) {
    const gameOverEl = document.getElementById("game-over");
    const winnerEl = document.getElementById("winner-display");

    if (gameOverEl) {
        gameOverEl.classList.remove("hidden");
    }

    if (winnerEl) {
        if (winner) {
            winnerEl.textContent = "Winner: " + winner;
        } else {
            winnerEl.textContent = "No winner (draw)";
        }
    }

    // Disable action buttons
    disableActionButtons();
}

/**
 * Disable all action buttons.
 */
function disableActionButtons() {
    const buttons = document.querySelectorAll("#action-buttons button");
    buttons.forEach(btn => {
        btn.disabled = true;
    });
}

/**
 * Log a message to the log panel.
 * @param {string} message - Message to log
 */
function logMessage(message) {
    const logEl = document.getElementById("log");
    if (logEl) {
        const entry = document.createElement("div");
        entry.textContent = message;
        logEl.appendChild(entry);
        logEl.scrollTop = logEl.scrollHeight;
    }
}

// =============================================================================
// Event Handlers
// =============================================================================

/**
 * Handle cell click event.
 * @param {Event} event - Click event
 */
function handleCellClick(event) {
    const cell = event.target.closest(".cell");
    if (!cell) return;

    const x = parseInt(cell.dataset.x, 10);
    const y = parseInt(cell.dataset.y, 10);

    console.log("Cell clicked:", x, y);
    clearError();

    // Find unit at clicked position
    const unitAtCell = findUnitAt(x, y);

    // If no pending action, clicking a friendly unit selects it
    if (!clientState.pendingActionType) {
        if (unitAtCell && unitAtCell.owner === clientState.playerId && unitAtCell.alive) {
            clientState.selectedUnitId = unitAtCell.id;
            renderBoard();
            renderControls();
            logMessage("Selected unit: " + unitAtCell.id);
        }
        return;
    }

    // Handle pending MOVE action
    if (clientState.pendingActionType === "MOVE") {
        if (!unitAtCell) {
            // Empty cell - send move action
            sendAction("MOVE", x, y, null);
            clientState.selectedUnitId = null;
            clientState.pendingActionType = null;
            renderControls();
        } else {
            renderError("Cannot move to occupied cell");
        }
        return;
    }

    // Handle pending ATTACK action
    if (clientState.pendingActionType === "ATTACK") {
        if (unitAtCell && unitAtCell.owner !== clientState.playerId && unitAtCell.alive) {
            sendAction("ATTACK", x, y, unitAtCell.id);
            clientState.selectedUnitId = null;
            clientState.pendingActionType = null;
            renderControls();
        } else {
            renderError("Select an enemy unit to attack");
        }
        return;
    }

    // Handle pending MOVE_AND_ATTACK action
    if (clientState.pendingActionType === "MOVE_AND_ATTACK") {
        // First click selects move destination (empty cell)
        // For simplicity, we'll require selecting an empty cell, then prompting for target
        // In a more complete implementation, this would be a two-step process
        if (!unitAtCell) {
            // Store the move target temporarily
            clientState.moveTarget = { x, y };
            clientState.pendingActionType = "MOVE_AND_ATTACK_SELECT_TARGET";
            renderError("Now click an enemy unit to attack");
            renderControls();
        } else if (unitAtCell.owner !== clientState.playerId && unitAtCell.alive) {
            // Direct attack without move (fallback)
            sendAction("MOVE_AND_ATTACK", x, y, unitAtCell.id);
            clientState.selectedUnitId = null;
            clientState.pendingActionType = null;
            renderControls();
        }
        return;
    }

    // Handle MOVE_AND_ATTACK target selection
    if (clientState.pendingActionType === "MOVE_AND_ATTACK_SELECT_TARGET") {
        if (unitAtCell && unitAtCell.owner !== clientState.playerId && unitAtCell.alive) {
            sendAction("MOVE_AND_ATTACK", clientState.moveTarget.x, clientState.moveTarget.y, unitAtCell.id);
            clientState.selectedUnitId = null;
            clientState.pendingActionType = null;
            clientState.moveTarget = null;
            renderControls();
        } else {
            renderError("Select an enemy unit to attack");
        }
        return;
    }
}

/**
 * Find a unit at the given position.
 * @param {number} x - X coordinate
 * @param {number} y - Y coordinate
 * @returns {object|null} Unit at position or null
 */
function findUnitAt(x, y) {
    if (!clientState.gameState || !clientState.gameState.units) return null;

    return clientState.gameState.units.find(unit =>
        unit.position.x === x && unit.position.y === y && unit.alive
    ) || null;
}

/**
 * Highlight valid move positions for the selected unit.
 */
function highlightValidMoves() {
    if (!clientState.selectedUnitId || !clientState.gameState) return;

    const selectedUnit = clientState.gameState.units.find(u => u.id === clientState.selectedUnitId);
    if (!selectedUnit) return;

    const { x, y } = selectedUnit.position;

    // Adjacent positions (orthogonal only)
    const adjacent = [
        { x: x - 1, y: y },
        { x: x + 1, y: y },
        { x: x, y: y - 1 },
        { x: x, y: y + 1 }
    ];

    adjacent.forEach(pos => {
        // Check bounds
        if (pos.x < 0 || pos.x >= 5 || pos.y < 0 || pos.y >= 5) return;

        // Check if occupied
        if (findUnitAt(pos.x, pos.y)) return;

        // Highlight cell
        const cell = document.querySelector(`.cell[data-x="${pos.x}"][data-y="${pos.y}"]`);
        if (cell) {
            cell.classList.add("valid-move");
        }
    });
}

/**
 * Handle Move button click.
 */
function handleMoveClick() {
    clientState.pendingActionType = "MOVE";
    renderControls();
    console.log("Move action selected");
}

/**
 * Handle Attack button click.
 */
function handleAttackClick() {
    clientState.pendingActionType = "ATTACK";
    renderControls();
    console.log("Attack action selected");
}

/**
 * Handle Move+Attack button click.
 */
function handleMoveAttackClick() {
    clientState.pendingActionType = "MOVE_AND_ATTACK";
    renderControls();
    console.log("Move+Attack action selected");
}

/**
 * Handle End Turn button click.
 */
function handleEndTurnClick() {
    sendAction("END_TURN", null, null, null);
    clientState.pendingActionType = null;
    renderControls();
    console.log("End Turn sent");
}

// =============================================================================
// Initialization
// =============================================================================

/**
 * Initialize the client.
 */
function init() {
    console.log("Initializing 5x5 Tactics client...");

    // Set up cell click handlers
    const cells = document.querySelectorAll(".cell");
    cells.forEach(cell => {
        cell.addEventListener("click", handleCellClick);
    });

    // Set up button click handlers
    document.getElementById("btn-move")?.addEventListener("click", handleMoveClick);
    document.getElementById("btn-attack")?.addEventListener("click", handleAttackClick);
    document.getElementById("btn-move-attack")?.addEventListener("click", handleMoveAttackClick);
    document.getElementById("btn-end-turn")?.addEventListener("click", handleEndTurnClick);

    // Initial render
    renderBoard();
    renderControls();

    // Connect to WebSocket
    connectWebSocket();
}

// Run initialization when DOM is ready
document.addEventListener("DOMContentLoaded", init);
