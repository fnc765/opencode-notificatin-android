#!/usr/bin/env python3
"""Mock OpenCode server that sends test SSE events for notification testing."""

import json
import time
import sys
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

class ThreadingServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True

HOST = "0.0.0.0"
PORT = 4196

def sse_event(event_type, props):
    envelope = json.dumps({"directory": "/test", "payload": {"type": event_type, "properties": props}})
    return f"data: {envelope}\n\n"


class MockOpenCodeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/event" or self.path == "/global/event":
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.end_headers()

            # server.connected
            self.wfile.write(sse_event("server.connected", {}).encode())
            self.wfile.flush()
            time.sleep(1)

            # session.idle (completion notification)
            self.wfile.write(sse_event("session.idle", {"sessionID": "test-session-1"}).encode())
            self.wfile.flush()
            time.sleep(2)

            # session.error
            self.wfile.write(sse_event("session.error", {"sessionID": "test-session-2"}).encode())
            self.wfile.flush()
            time.sleep(2)

            # permission.updated (approval notification)
            self.wfile.write(sse_event("permission.updated", {
                "id": "perm-001",
                "type": "bash",
                "sessionID": "test-session-1",
                "messageID": "msg-001",
                "title": "rm -rf /tmp/cache",
                "metadata": {}
            }).encode())
            self.wfile.flush()
            time.sleep(3)

            # question (ask question notification)
            self.wfile.write(sse_event("permission.updated", {
                "id": "perm-002",
                "type": "other",
                "sessionID": "test-session-1",
                "messageID": "msg-002",
                "title": "Which HTTP library should I use for this project?",
                "metadata": {}
            }).encode())
            self.wfile.flush()
            time.sleep(5)

            # keep connection alive, send idle every 30s
            while True:
                self.wfile.write(sse_event("session.idle", {"sessionID": "test-session-keepalive"}).encode())
                self.wfile.flush()
                time.sleep(30)

        elif self.path.startswith("/session"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"id": "test-session-1", "title": "Test"}).encode())

        elif self.path == "/doc":
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            self.end_headers()
            self.wfile.write(b"<h1>Mock OpenCode Server</h1>")

        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        if "permissions" in self.path:
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len) if content_len else b"{}"
            print(f"\n[PERMISSION RESPONSE] {self.path} -> {body.decode()}")
        self.wfile.write(b"true")

    def log_message(self, format, *args):
        # suppress default logging
        sys.stderr.write(f"[SERVER] {args[0]}\n")


if __name__ == "__main__":
    server = ThreadingServer((HOST, PORT), MockOpenCodeHandler)
    print(f"Mock OpenCode server running on http://{HOST}:{PORT}")
    print()
    print("Events sequence:")
    print("  1s -> server.connected")
    print("  2s -> session.idle     (completion notification)")
    print("  4s -> session.error    (error notification)")
    print("  6s -> permission.updated (approval notification)")
    print("  30s loop -> session.idle (keepalive)")
    print()
    print("Point the OpenCode Notifier app to this server:")
    print(f"  Server URL: http://<this-machine-ip>:{PORT}")
    print("  Username/password: leave empty (no auth)")
    print()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped.")
