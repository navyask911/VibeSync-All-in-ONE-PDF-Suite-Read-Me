#!/usr/bin/env bash
# ==============================================================================
# VibeSync WebSocket Debugger with wscat
# ==============================================================================
# Usage:
#   npm install -g wscat
#   bash tests/websocket/wscat_debug.sh [ws_url]
# ==============================================================================

WS_TARGET="${1:-wss://ais-dev-ysij5gfggpt2akxlk7vm3b-58584043488.asia-east1.run.app/ws/chat}"

echo "============================================================"
echo "⚡ Starting VibeSync WebSocket Diagnostic with wscat"
echo "Target Endpoint: $WS_TARGET"
echo "============================================================"

# Check if wscat is available
if ! command -v wscat &> /dev/null; then
    echo "⚠️  wscat is not installed globally."
    echo "👉 Install via npm: npm install -g wscat"
    echo "👉 Or test via npx: npx wscat -c $WS_TARGET"
    echo ""
    echo "Sample Chat Handshake Payload:"
    echo '{"type": "PING", "timestamp": '$(date +%s%3N)', "clientId": "debug_wscat_01"}'
    echo ""
    echo "Sample Message Send Payload:"
    echo '{"type": "CHAT_MESSAGE", "senderId": "current_user", "matchId": "m_1", "text": "Hello from wscat 💬", "timestamp": '$(date +%s%3N)'}'
    exit 0
fi

# Execute wscat with connection timeout and protocol handshake
echo "Connecting to $WS_TARGET..."
wscat -c "$WS_TARGET" \
    --header "User-Agent: VibeSync-WSCat-Debug/1.0" \
    --slash \
    --execute '{"type": "PING", "client": "wscat-inspector"}'
