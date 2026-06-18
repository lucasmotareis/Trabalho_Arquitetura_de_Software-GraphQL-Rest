import { createReadStream, existsSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize, resolve } from "node:path";

const port = Number(process.env.PORT ?? 5173);
const distDir = resolve("dist");

const contentTypes = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".ico": "image/x-icon",
};

createServer((request, response) => {
  const urlPath = decodeURIComponent(new URL(request.url ?? "/", `http://localhost:${port}`).pathname);
  const requestedPath = normalize(urlPath === "/" ? "/index.html" : urlPath);
  let filePath = resolve(join(distDir, requestedPath));

  if (!filePath.startsWith(distDir) || !existsSync(filePath)) {
    filePath = join(distDir, "index.html");
  }

  response.setHeader("Content-Type", contentTypes[extname(filePath)] ?? "application/octet-stream");
  createReadStream(filePath).pipe(response);
}).listen(port, "0.0.0.0");
