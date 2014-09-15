from BaseHTTPServer import BaseHTTPRequestHandler
import httplib


class EmptyUrlListHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        status_code = httplib.UNAUTHORIZED
        auth_uri = '{"auth_uri":[]}'
        self.send_response(status_code)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(auth_uri)

pass