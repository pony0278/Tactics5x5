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
    playerId: "P1",           // Hard-coded for V1 (can be toggled to "P2")
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
    // TODO: Implement sendJoinMatch
    const message = {
        type: "join_match",
        payload: {
            matchId: clientState.matchId,
            playerId: clientState.playerId
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
    // TODO: Implement handleMatchJoined
    console.log("Match joined:", payload);
    clientState.gameState = payload.state;
    clientState.selectedUnitId = null;
    clientState.pendingActionType = null;
    clientState.lastErrorMessage = null;

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
    // TODO: Implement handleGameOver
    console.log("Game over:", payload);
    clientState.gameState = payload.state;

    renderBoard();
    renderControls();
    showGameOver(payload.winner);
}

// =============================================================================
// Rendering Functions
// =============================================================================

/**
 * Render the 5x5 board based on current gameState.
 */
function renderBoard() {
    // TODO: Implement renderBoard
    console.log("renderBoard called");

    // Clear all cells
    const cells = document.querySelectorAll(".cell");
    cells.forEach(cell => {
        cell.innerHTML = "";
        cell.className = "cell";
    });

    // Render units if gameState exists
    if (clientState.gameState && clientState.gameState.units) {
        // TODO: Render units in cells
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
    // TODO: Implement cell click logic
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
