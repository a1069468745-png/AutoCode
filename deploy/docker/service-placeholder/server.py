import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


SERVICE_NAME = os.getenv("SERVICE_NAME", "placeholder-service")
SERVICE_ROLE = os.getenv("SERVICE_ROLE", "backend")
SERVICE_PORT = int(os.getenv("SERVICE_PORT", "18080"))
SERVICE_DESCRIPTION = os.getenv("SERVICE_DESCRIPTION", "Service placeholder")
SPRING_PROFILES_ACTIVE = os.getenv("SPRING_PROFILES_ACTIVE", "local")
API_BASE_PATH = os.getenv("API_BASE_PATH", "/api")


def build_payload():
    return {
        "service": SERVICE_NAME,
        "role": SERVICE_ROLE,
        "port": SERVICE_PORT,
        "description": SERVICE_DESCRIPTION,
        "profile": SPRING_PROFILES_ACTIVE,
        "status": "placeholder-ready",
    }


def build_frontend_html():
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AutoCode Web Console Placeholder</title>
    <style>
      body {{
        margin: 0;
        font-family: "Microsoft YaHei", sans-serif;
        background: linear-gradient(135deg, #f4f7fb, #dfe8ff);
        color: #1d2a44;
      }}
      main {{
        max-width: 920px;
        margin: 64px auto;
        padding: 40px;
        background: rgba(255, 255, 255, 0.94);
        border-radius: 24px;
        box-shadow: 0 24px 60px rgba(29, 42, 68, 0.12);
      }}
      h1 {{
        margin: 0 0 12px;
        font-size: 34px;
      }}
      p {{
        line-height: 1.7;
      }}
      code {{
        padding: 2px 8px;
        border-radius: 8px;
        background: #eef3ff;
      }}
      .grid {{
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 16px;
        margin-top: 28px;
      }}
      .card {{
        padding: 20px;
        border-radius: 18px;
        border: 1px solid #d6e1ff;
        background: #fff;
      }}
      .muted {{
        color: #4d5f85;
      }}
    </style>
  </head>
  <body data-page-marker="autocode-compose-skeleton-ready">
    <main>
      <h1>AutoCode 部署骨架已就绪</h1>
      <p class="muted">
        当前页面由 <code>{SERVICE_NAME}</code> 占位服务提供，用于验证
        INF-05 已将前端、网关与基础组件纳入统一 Compose 骨架。
      </p>
      <div class="grid">
        <section class="card">
          <strong>入口层</strong>
          <p>Nginx 负责统一入口，根路径转发到前端占位服务，<code>{API_BASE_PATH}</code> 转发到网关占位服务。</p>
        </section>
        <section class="card">
          <strong>后端骨架</strong>
          <p>8 个后端服务已预留容器名、端口、依赖顺序与健康检查。</p>
        </section>
        <section class="card">
          <strong>基础组件</strong>
          <p>PostgreSQL、Redis、Qdrant 继续作为后续数据与检索任务的部署底座。</p>
        </section>
      </div>
    </main>
  </body>
</html>"""


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, payload, status=200):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_text(self, text, status=200):
        body = text.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_html(self, html, status=200):
        body = html.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/healthz":
            self._send_text("ok")
            return

        if self.path in ("/", "/index.html"):
            if SERVICE_ROLE == "frontend":
                self._send_html(build_frontend_html())
            else:
                self._send_json(build_payload())
            return

        if self.path == "/info":
            self._send_json(build_payload())
            return

        self._send_json(
            {
                **build_payload(),
                "path": self.path,
                "message": "Placeholder endpoint is ready for real service integration.",
            }
        )

    def log_message(self, fmt, *args):
        print(f"{self.address_string()} - {fmt % args}")


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", SERVICE_PORT), Handler)
    print(f"Starting {SERVICE_NAME} on port {SERVICE_PORT}")
    server.serve_forever()
