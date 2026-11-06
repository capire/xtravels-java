#!/usr/bin/env bash
# A2A protocol conformance test script for the xtravels agent.
# Usage: ./a2a-test.sh [BASE_URL] [USER] [PASS]
#
# Defaults to http://localhost:8080, admin, empty password.
# Requires: curl, python3
set -euo pipefail

BASE="${1:-http://localhost:8080}"
USER="${2:-admin}"
PASS="${3:-}"
A2A="$BASE/a2a/travel"
CREDS="$USER:$PASS"

PASS_COUNT=0
FAIL_COUNT=0

# ── helpers ────────────────────────────────────────────────────────────────────

ok()   { echo "  ✅  $1"; PASS_COUNT=$((PASS_COUNT + 1)); }
fail() { echo "  ❌  $1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

rpc() {
  curl -s -u "$CREDS" -X POST "$A2A" \
    -H "Content-Type: application/json" \
    --data "@$2" -o "$1"
}

rpc_stream() {
  curl -s -u "$CREDS" -X POST "$A2A" \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream" \
    --max-time 15 \
    --data "@$2" -o "$1"
}

make_send() {
  # make_send <file> <text> [contextId]
  python3 - "$1" "$2" "${3:-}" <<'PYEOF'
import json, uuid, sys
out, text, ctx = sys.argv[1], sys.argv[2], sys.argv[3]
msg = {"role": "user", "parts": [{"kind": "text", "text": text}],
       "messageId": str(uuid.uuid4()), "kind": "message"}
if ctx: msg["contextId"] = ctx
with open(out, "w") as f:
    json.dump({"jsonrpc": "2.0", "id": str(uuid.uuid4()),
               "method": "message/send", "params": {"message": msg}}, f)
PYEOF
}

make_stream() {
  # make_stream <file> <text> [contextId]
  python3 - "$1" "$2" "${3:-}" <<'PYEOF'
import json, uuid, sys
out, text, ctx = sys.argv[1], sys.argv[2], sys.argv[3]
msg = {"role": "user", "parts": [{"kind": "text", "text": text}],
       "messageId": str(uuid.uuid4()), "kind": "message"}
if ctx: msg["contextId"] = ctx
with open(out, "w") as f:
    json.dump({"jsonrpc": "2.0", "id": str(uuid.uuid4()),
               "method": "message/stream", "params": {"message": msg}}, f)
PYEOF
}

make_get_task() {
  python3 -c "
import json, uuid, sys
with open('$1', 'w') as f:
    json.dump({'jsonrpc':'2.0','id':str(uuid.uuid4()),'method':'tasks/get','params':{'id':'$2'}}, f)
"
}

make_cancel_task() {
  python3 -c "
import json, uuid, sys
with open('$1', 'w') as f:
    json.dump({'jsonrpc':'2.0','id':str(uuid.uuid4()),'method':'tasks/cancel','params':{'id':'$2'}}, f)
"
}

extract() {
  python3 - "$1" "$2" <<'PYEOF'
import json, sys
data = json.load(open(sys.argv[1]))
for k in sys.argv[2].split('.'):
    data = data[k]
print(data)
PYEOF
}

sse_events() {
  grep '^data:' "$1" | sed 's/^data: //'
}

assert_py() {
  # assert_py <label> <python-expr> [file-for-context]
  local label="$1" expr="$2" ctx="${3:-}"
  if python3 -c "$expr" 2>/dev/null; then
    ok "$label"
  else
    fail "$label"
    if [ -n "$ctx" ]; then
      echo "      $(python3 -m json.tool "$ctx" 2>/dev/null | head -15)"
    fi
  fi
}

TMPDIR_TEST=$(mktemp -d)
trap 'rm -rf "$TMPDIR_TEST"' EXIT

# ── TEST 1: Agent card ─────────────────────────────────────────────────────────
echo ""
echo "=== TEST 1: Agent Card ==="
curl -s -u "$CREDS" "$A2A/.well-known/agent-card.json" -o "$TMPDIR_TEST/card.json"
CARD=$(cat "$TMPDIR_TEST/card.json")
assert_py "has 'name' field" "
import json; d=json.loads('''$CARD'''.replace(\"'\",\"'\"))
" # fallback: use file
python3 -c "import json; d=json.load(open('$TMPDIR_TEST/card.json')); assert 'name' in d" 2>/dev/null \
  && ok "agent card present with 'name'" || fail "agent card missing 'name'"
python3 -c "import json; d=json.load(open('$TMPDIR_TEST/card.json')); assert d.get('capabilities',{}).get('streaming') == True" 2>/dev/null \
  && ok "streaming=true" || fail "streaming not true"
AGENT_NAME=$(python3 -c "import json; print(json.load(open('$TMPDIR_TEST/card.json'))['name'])" 2>/dev/null || echo "?")
echo "  ℹ️   agent name: $AGENT_NAME"

# ── TEST 2: message/send — new conversation ────────────────────────────────────
echo ""
echo "=== TEST 2: message/send — new conversation ==="
make_send "$TMPDIR_TEST/req2.json" "Show me all current travels"
rpc "$TMPDIR_TEST/resp2.json" "$TMPDIR_TEST/req2.json"
python3 -c "import json; json.load(open('$TMPDIR_TEST/resp2.json'))" 2>/dev/null \
  && ok "response is valid JSON" || fail "response is not valid JSON"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp2.json'))
assert 'result' in d and 'error' not in d
assert d['result']['kind'] == 'task'
assert d['result']['status']['state'] == 'completed'
" 2>/dev/null && ok "result is a completed task" || { fail "result is not a completed task"; python3 -m json.tool "$TMPDIR_TEST/resp2.json" 2>/dev/null | head -15; }
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp2.json'))
arts = d['result'].get('artifacts', [])
assert any(p.get('text') for a in arts for p in a.get('parts',[]) if p.get('kind')=='text')
" 2>/dev/null && ok "artifact with text part present" || fail "no text artifact"

TASK1_ID=$(extract "$TMPDIR_TEST/resp2.json" result.id)
CTX1_ID=$(extract  "$TMPDIR_TEST/resp2.json" result.contextId)
echo "  ℹ️   taskId=$TASK1_ID"
echo "  ℹ️   contextId=$CTX1_ID"

# ── TEST 3: tasks/get — retrieve persisted task ────────────────────────────────
echo ""
echo "=== TEST 3: tasks/get — retrieve persisted task ==="
make_get_task "$TMPDIR_TEST/req3.json" "$TASK1_ID"
rpc "$TMPDIR_TEST/resp3.json" "$TMPDIR_TEST/req3.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp3.json'))
assert 'result' in d and 'error' not in d, f'error: {d.get(\"error\")}'
assert d['result']['kind'] == 'task'
assert d['result']['id'] == '$TASK1_ID'
assert d['result']['status']['state'] == 'completed'
" 2>/dev/null \
  && ok "tasks/get returned the persisted task" \
  || { fail "tasks/get failed"; python3 -m json.tool "$TMPDIR_TEST/resp3.json" 2>/dev/null | head -15; }

# ── TEST 4: message/send — continue conversation (same contextId) ──────────────
echo ""
echo "=== TEST 4: message/send — continue in same context ==="
make_send "$TMPDIR_TEST/req4.json" \
  "Add a flight booking from FRA to JFK on 2026-12-01 to travel 1" "$CTX1_ID"
rpc "$TMPDIR_TEST/resp4.json" "$TMPDIR_TEST/req4.json"
CTX2_ID=$(extract "$TMPDIR_TEST/resp4.json" result.contextId)
TASK2_ID=$(extract "$TMPDIR_TEST/resp4.json" result.id)
[ "$CTX2_ID" = "$CTX1_ID" ] \
  && ok "contextId preserved across turns" \
  || fail "contextId changed: expected=$CTX1_ID got=$CTX2_ID"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp4.json'))
assert d['result']['status']['state'] == 'completed'
" 2>/dev/null && ok "second turn completed" || fail "second turn did not complete"
echo "  ℹ️   taskId=$TASK2_ID"

# ── TEST 5: tasks/get — second task also persisted ────────────────────────────
echo ""
echo "=== TEST 5: tasks/get — second task persisted ==="
make_get_task "$TMPDIR_TEST/req5.json" "$TASK2_ID"
rpc "$TMPDIR_TEST/resp5.json" "$TMPDIR_TEST/req5.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp5.json'))
assert 'result' in d and 'error' not in d
assert d['result']['id'] == '$TASK2_ID'
assert d['result']['contextId'] == '$CTX1_ID'
" 2>/dev/null \
  && ok "second task persisted with correct contextId" \
  || { fail "second task not retrievable"; python3 -m json.tool "$TMPDIR_TEST/resp5.json" 2>/dev/null | head -15; }

# ── TEST 6: message/stream — SSE streaming ────────────────────────────────────
echo ""
echo "=== TEST 6: message/stream — SSE streaming ==="
make_stream "$TMPDIR_TEST/req6.json" "What flights are available from FRA?" "$CTX1_ID"
rpc_stream "$TMPDIR_TEST/resp6.raw" "$TMPDIR_TEST/req6.json"
EVENT_COUNT=$(sse_events "$TMPDIR_TEST/resp6.raw" | wc -l | tr -d ' ')
echo "  ℹ️   received $EVENT_COUNT SSE events"
[ "$EVENT_COUNT" -ge 3 ] \
  && ok "at least 3 SSE events (submitted → working → artifact/completed)" \
  || fail "fewer than 3 SSE events"
LAST_EVENT=$(sse_events "$TMPDIR_TEST/resp6.raw" | tail -1)
echo "$LAST_EVENT" | python3 -c "
import json,sys; d=json.load(sys.stdin)
assert d.get('result',{}).get('final') == True
" 2>/dev/null \
  && ok "last SSE event has final=true" \
  || { fail "last SSE event does not have final=true"; echo "      $LAST_EVENT"; }

STREAM_TASK_ID=$(sse_events "$TMPDIR_TEST/resp6.raw" | head -1 \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['result']['taskId'])" 2>/dev/null || echo "")
echo "  ℹ️   stream taskId=$STREAM_TASK_ID"
if [ -n "$STREAM_TASK_ID" ]; then
  make_get_task "$TMPDIR_TEST/req6b.json" "$STREAM_TASK_ID"
  rpc "$TMPDIR_TEST/resp6b.json" "$TMPDIR_TEST/req6b.json"
  python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp6b.json'))
assert 'result' in d and 'error' not in d
assert d['result']['id'] == '$STREAM_TASK_ID'
" 2>/dev/null \
    && ok "streamed task retrievable via tasks/get" \
    || { fail "streamed task not retrievable"; python3 -m json.tool "$TMPDIR_TEST/resp6b.json" 2>/dev/null | head -10; }
fi

# ── TEST 7: tasks/cancel — completed task ─────────────────────────────────────
echo ""
echo "=== TEST 7: tasks/cancel ==="
make_cancel_task "$TMPDIR_TEST/req7.json" "$TASK1_ID"
rpc "$TMPDIR_TEST/resp7.json" "$TMPDIR_TEST/req7.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp7.json'))
if 'error' in d:
    # -32002 = task not cancellable in current state, -32004/-32005 also acceptable
    assert d['error']['code'] in [-32002, -32004, -32005], f'unexpected error: {d[\"error\"]}'
else:
    assert 'result' in d
" 2>/dev/null \
  && ok "tasks/cancel responded appropriately" \
  || { fail "tasks/cancel unexpected response"; python3 -m json.tool "$TMPDIR_TEST/resp7.json" 2>/dev/null | head -10; }

# ── TEST 8: tasks/get — unknown task ID ───────────────────────────────────────
echo ""
echo "=== TEST 8: tasks/get — unknown task ID ==="
make_get_task "$TMPDIR_TEST/req8.json" "00000000-0000-0000-0000-000000000000"
rpc "$TMPDIR_TEST/resp8.json" "$TMPDIR_TEST/req8.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp8.json'))
assert 'error' in d
assert d['error']['code'] == -32001, f'expected -32001, got {d[\"error\"][\"code\"]}'
" 2>/dev/null \
  && ok "tasks/get returns -32001 for unknown task" \
  || { fail "tasks/get did not return -32001"; python3 -m json.tool "$TMPDIR_TEST/resp8.json" 2>/dev/null | head -10; }

# ── TEST 9: invalid JSON body ──────────────────────────────────────────────────
echo ""
echo "=== TEST 9: Invalid JSON body ==="
INVALID_RESP=$(echo 'not json at all' | curl -s -u "$CREDS" -X POST "$A2A" \
  -H "Content-Type: application/json" --data @-)
echo "$INVALID_RESP" | python3 -c "
import json,sys; d=json.load(sys.stdin)
assert 'error' in d
assert d['error']['code'] == -32700, f'expected -32700, got {d[\"error\"][\"code\"]}'
" 2>/dev/null \
  && ok "invalid JSON returns -32700 (Parse error)" \
  || { fail "invalid JSON did not return -32700"; echo "      $INVALID_RESP"; }

# ── TEST 10: unknown method ────────────────────────────────────────────────────
echo ""
echo "=== TEST 10: Unknown JSON-RPC method ==="
echo '{"jsonrpc":"2.0","id":"x","method":"no/such/method","params":{}}' \
  | curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" --data @- \
  > "$TMPDIR_TEST/resp10.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp10.json'))
assert 'error' in d
assert d['error']['code'] == -32601, f'expected -32601, got {d[\"error\"][\"code\"]}'
" 2>/dev/null \
  && ok "unknown method returns -32601 (Method not found)" \
  || { fail "unknown method did not return -32601"; python3 -m json.tool "$TMPDIR_TEST/resp10.json" 2>/dev/null | head -10; }

# ── TEST 11: listTasks — default page (no params) ─────────────────────────────
echo ""
echo "=== TEST 11: listTasks — default page ==="
echo '{"jsonrpc":"2.0","id":"lt1","method":"ListTasks","params":{}}' \
  | curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" -H "A2A-Version: 1.0" --data @- \
  > "$TMPDIR_TEST/resp11.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp11.json'))
assert 'result' in d and 'error' not in d, f'error: {d.get(\"error\")}'
r = d['result']
tasks = r.get('tasks', [])
assert len(tasks) == 50, f'expected 50 (default pageSize), got {len(tasks)}'
total = r.get('totalSize')
assert total >= 100, f'totalSize={total} expected >=106'
assert r.get('nextPageToken') is not None, 'expected nextPageToken'
" 2>/dev/null \
  && ok "default page: 50 tasks, totalSize>=100, nextPageToken present" \
  || { fail "default page unexpected response"; python3 -m json.tool "$TMPDIR_TEST/resp11.json" 2>/dev/null | head -20; }

TOTAL=$(python3 -c "import json; print(json.load(open('$TMPDIR_TEST/resp11.json'))['result']['totalSize'])")
echo "  ℹ️   totalSize=$TOTAL"

# ── TEST 12: listTasks — explicit pageSize=10 ──────────────────────────────────
echo ""
echo "=== TEST 12: listTasks — pageSize=10 ==="
echo '{"jsonrpc":"2.0","id":"lt2","method":"ListTasks","params":{"pageSize":10}}' \
  | curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" -H "A2A-Version: 1.0" --data @- \
  > "$TMPDIR_TEST/resp12.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp12.json'))
r = d['result']
tasks = r.get('tasks', [])
assert len(tasks) == 10, f'expected 10, got {len(tasks)}'
assert r.get('nextPageToken') is not None
# tasks ordered by createdAt asc — check IDs are distinct
ids = [t['id'] for t in tasks]
assert len(ids) == len(set(ids)), 'duplicate task IDs on page'
" 2>/dev/null \
  && ok "pageSize=10: 10 tasks, nextPageToken present, no duplicates" \
  || { fail "pageSize=10 unexpected response"; python3 -m json.tool "$TMPDIR_TEST/resp12.json" 2>/dev/null | head -20; }

PAGE1_TOKEN=$(python3 -c "import json; print(json.load(open('$TMPDIR_TEST/resp12.json'))['result']['nextPageToken'])")
PAGE1_IDS=$(python3 -c "import json; r=json.load(open('$TMPDIR_TEST/resp12.json'))['result']; print(','.join(t['id'] for t in r['tasks']))")
echo "  ℹ️   page 1 nextPageToken=$PAGE1_TOKEN"

# ── TEST 13: listTasks — page 2 via nextPageToken ─────────────────────────────
echo ""
echo "=== TEST 13: listTasks — page 2 via nextPageToken ==="
python3 -c "
import json
with open('$TMPDIR_TEST/req13.json','w') as f:
    json.dump({'jsonrpc':'2.0','id':'lt3','method':'ListTasks',
               'params':{'pageSize':10,'pageToken':'$PAGE1_TOKEN'}}, f)
"
curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" -H "A2A-Version: 1.0" \
  --data "@$TMPDIR_TEST/req13.json" > "$TMPDIR_TEST/resp13.json"
python3 -c "
import json
d = json.load(open('$TMPDIR_TEST/resp13.json'))
r = d['result']
tasks = r.get('tasks', [])
assert len(tasks) == 10, f'expected 10, got {len(tasks)}'
# totalSize must be consistent with page 1
total = r.get('totalSize')
assert total >= 100, f'totalSize={total}'
# No overlap with page 1
page1_ids = set('$PAGE1_IDS'.split(','))
page2_ids = {t['id'] for t in tasks}
overlap = page1_ids & page2_ids
assert not overlap, f'overlap between pages: {overlap}'
" 2>/dev/null \
  && ok "page 2: 10 tasks, no overlap with page 1, consistent totalSize" \
  || { fail "page 2 unexpected response"; python3 -m json.tool "$TMPDIR_TEST/resp13.json" 2>/dev/null | head -20; }

PAGE2_TOKEN=$(python3 -c "import json; print(json.load(open('$TMPDIR_TEST/resp13.json'))['result'].get('nextPageToken',''))")
echo "  ℹ️   page 2 nextPageToken=$PAGE2_TOKEN"

# ── TEST 14: listTasks — walk all pages, collect all IDs ──────────────────────
echo ""
echo "=== TEST 14: listTasks — walk all pages (pageSize=20) ==="
python3 - <<'PYEOF'
import json, subprocess, sys, uuid

base  = "http://localhost:8080/a2a/travel"
creds = "admin:"

all_ids   = []
page_num  = 0
page_token = None
total_size = None

while True:
    page_num += 1
    params = {"pageSize": 20}
    if page_token:
        params["pageToken"] = page_token
    payload = json.dumps({"jsonrpc": "2.0", "id": str(uuid.uuid4()),
                          "method": "ListTasks", "params": params})
    out = subprocess.run(
        ["curl", "-s", "-u", creds, "-X", "POST", base,
         "-H", "Content-Type: application/json",
         "-H", "A2A-Version: 1.0", "--data", payload],
        capture_output=True, text=True
    ).stdout
    d = json.loads(out)
    if "error" in d:
        print(f"  ERROR on page {page_num}: {d['error']}")
        sys.exit(1)
    r = d["result"]
    tasks = r.get("tasks", [])
    if total_size is None:
        total_size = r["totalSize"]
    else:
        if r["totalSize"] != total_size:
            print(f"  WARN: totalSize changed page {page_num}: {r['totalSize']} != {total_size}")
    all_ids.extend(t["id"] for t in tasks)
    page_token = r.get("nextPageToken")
    print(f"  page {page_num}: {len(tasks)} tasks  nextPageToken={'...' if page_token else 'none'}")
    if not page_token:
        break

print(f"  total pages: {page_num}")
print(f"  total tasks fetched: {len(all_ids)}")
print(f"  reported totalSize:  {total_size}")
duplicates = len(all_ids) - len(set(all_ids))
if duplicates:
    print(f"  FAIL: {duplicates} duplicate task IDs across pages")
    sys.exit(1)
if len(all_ids) != total_size:
    print(f"  FAIL: fetched {len(all_ids)} but totalSize={total_size}")
    sys.exit(1)
print(f"  OK: all {len(all_ids)} tasks fetched, no duplicates, matches totalSize")
PYEOF
[ $? -eq 0 ] && ok "all pages walked: no duplicates, count matches totalSize" \
             || fail "paging walk failed"

# ── TEST 15: listTasks — filter by contextId ──────────────────────────────────
echo ""
echo "=== TEST 15: listTasks — filter by contextId ==="
# Use the contextId from test 2 which has exactly 3 tasks (tests 2,4,6)
python3 -c "
import json
with open('$TMPDIR_TEST/req15.json','w') as f:
    json.dump({'jsonrpc':'2.0','id':'lt5','method':'ListTasks',
               'params':{'contextId':'$CTX1_ID'}}, f)
"
curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" -H "A2A-Version: 1.0" \
  --data "@$TMPDIR_TEST/req15.json" > "$TMPDIR_TEST/resp15.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp15.json'))
r = d['result']
tasks = r['tasks']
# All tasks must belong to our contextId
assert all(t['contextId'] == '$CTX1_ID' for t in tasks), 'wrong contextId in results'
assert r['totalSize'] == len(tasks), f'totalSize mismatch: {r[\"totalSize\"]} vs {len(tasks)}'
# small context — enough tasks to fit on one page (default pageSize=50)
assert len(tasks) <= 50
assert not r.get('nextPageToken'), 'unexpected nextPageToken (context has >50 tasks?)'
print(f'  found {len(tasks)} tasks in context')
" 2>/dev/null \
  && ok "contextId filter returns only matching tasks, no nextPageToken" \
  || { fail "contextId filter unexpected response"; python3 -m json.tool "$TMPDIR_TEST/resp15.json" 2>/dev/null | head -20; }

# ── TEST 16: listTasks — last page has no nextPageToken ───────────────────────
echo ""
echo "=== TEST 16: listTasks — last page has no nextPageToken ==="
# Request a page larger than total — should return all tasks and no token
python3 -c "
import json
with open('$TMPDIR_TEST/req16.json','w') as f:
    json.dump({'jsonrpc':'2.0','id':'lt6','method':'ListTasks',
               'params':{'pageSize':100}}, f)
"
curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" -H "A2A-Version: 1.0" \
  --data "@$TMPDIR_TEST/req16.json" > "$TMPDIR_TEST/resp16.json"
python3 -c "
import json; d=json.load(open('$TMPDIR_TEST/resp16.json'))
r = d['result']
total = r['totalSize']
tasks = r['tasks']
assert total > 100, f'need >100 tasks for this test, got totalSize={total}'
assert len(tasks) == 100, f'expected 100, got {len(tasks)}'
assert r.get('nextPageToken') is not None, 'expected nextPageToken (>100 tasks)'
" 2>/dev/null \
  && ok "pageSize=100 with >100 tasks: 100 returned, nextPageToken present" \
  || { fail "unexpected result for pageSize=100"; python3 -m json.tool "$TMPDIR_TEST/resp16.json" 2>/dev/null | head -10; }

LAST_TOKEN=$(python3 -c "import json; print(json.load(open('$TMPDIR_TEST/resp16.json'))['result']['nextPageToken'])")
FIRST_PAGE_TOTAL=$(python3 -c "import json; print(json.load(open('$TMPDIR_TEST/resp16.json'))['result']['totalSize'])")
python3 -c "
import json
with open('$TMPDIR_TEST/req16b.json','w') as f:
    json.dump({'jsonrpc':'2.0','id':'lt6b','method':'ListTasks',
               'params':{'pageSize':100,'pageToken':'$LAST_TOKEN'}}, f)
"
curl -s -u "$CREDS" -X POST "$A2A" -H "Content-Type: application/json" -H "A2A-Version: 1.0" \
  --data "@$TMPDIR_TEST/req16b.json" > "$TMPDIR_TEST/resp16b.json"
LAST_PAGE_COUNT=$(python3 -c "import json; print(len(json.load(open('$TMPDIR_TEST/resp16b.json'))['result']['tasks']))")
python3 -c "
import json
d = json.load(open('$TMPDIR_TEST/resp16b.json'))
r = d['result']
tasks = r['tasks']
expected_remaining = $FIRST_PAGE_TOTAL - 100
assert len(tasks) == expected_remaining, f'expected {expected_remaining} remaining, got {len(tasks)}'
assert not r.get('nextPageToken'), 'unexpected nextPageToken on last page'
" 2>/dev/null \
  && ok "last page: $LAST_PAGE_COUNT remaining tasks, no nextPageToken" \
  || { fail "last page unexpected"; python3 -m json.tool "$TMPDIR_TEST/resp16b.json" 2>/dev/null | head -10; }

# ── Summary ────────────────────────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════"
echo "  Results: $PASS_COUNT passed, $FAIL_COUNT failed"
echo "══════════════════════════════════════"
[ "$FAIL_COUNT" -eq 0 ] && exit 0 || exit 1
