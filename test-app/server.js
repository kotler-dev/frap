const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { URL } = require('url');

const PORT = process.env.PORT || 3000;
const WS_MAGIC = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11';

const MIME_TYPES = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
};

function sendJson(res, status, body) {
    res.writeHead(status, {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache',
        'Access-Control-Allow-Origin': '*',
    });
    res.end(JSON.stringify(body));
}

function handleApi(req, res, urlPath, searchParams) {
    if (urlPath === '/api/payment-intent' && req.method === 'POST') {
        const mode = searchParams.get('mode') || 'normal';
        const delayMs = mode === 'slow' ? 8000 : 80;
        setTimeout(() => {
            if (mode === 'slow') {
                sendJson(res, 504, { error: 'gateway_timeout', message: 'Payment intent timed out' });
            } else {
                sendJson(res, 200, { id: 'pi_demo', status: 'ready' });
            }
        }, delayMs);
        return true;
    }

    if (urlPath === '/api/cart' && req.method === 'GET') {
        const delayParam = searchParams.get('delay');
        const delayMs = delayParam ? parseInt(delayParam, 10) : 120;
        setTimeout(() => {
            sendJson(res, 200, {
                items: [{ id: 'item-1', name: 'Demo ticket' }],
                latency_ms: delayMs,
            });
        }, Number.isFinite(delayMs) ? delayMs : 120);
        return true;
    }

    return false;
}

function serveStatic(req, res, urlPath) {
    let filePath = '.' + urlPath;
    if (filePath === './') {
        filePath = './index.html';
    }
    if (filePath.endsWith('/')) {
        filePath += 'index.html';
    }

    const extname = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[extname] || 'application/octet-stream';

    fs.readFile(filePath, (error, content) => {
        if (error) {
            if (error.code === 'ENOENT') {
                fs.readFile('./404.html', (err, content404) => {
                    if (err) {
                        res.writeHead(404, { 'Content-Type': 'text/plain' });
                        res.end('404 Not Found', 'utf-8');
                    } else {
                        res.writeHead(404, { 'Content-Type': 'text/html' });
                        res.end(content404, 'utf-8');
                    }
                });
            } else {
                res.writeHead(500);
                res.end('Server Error: ' + error.code + ' ..\n');
            }
        } else {
            res.writeHead(200, {
                'Content-Type': contentType,
                'Cache-Control': 'no-cache',
            });
            res.end(content, 'utf-8');
        }
    });
}

function createWsTextFrame(text) {
    const payload = Buffer.from(text, 'utf-8');
    const len = payload.length;
  if (len < 126) {
        const frame = Buffer.alloc(2 + len);
        frame[0] = 0x81;
        frame[1] = len;
        payload.copy(frame, 2);
        return frame;
    }
    const frame = Buffer.alloc(4 + len);
    frame[0] = 0x81;
    frame[1] = 126;
    frame.writeUInt16BE(len, 2);
    payload.copy(frame, 4);
    return frame;
}

function decodeWsTextFrame(buffer) {
    if (buffer.length < 2) {
        return null;
    }
    const masked = (buffer[1] & 0x80) !== 0;
    let payloadLen = buffer[1] & 0x7f;
    let offset = 2;
    if (payloadLen === 126) {
        if (buffer.length < 4) {
            return null;
        }
        payloadLen = buffer.readUInt16BE(2);
        offset = 4;
    }
    let mask;
    if (masked) {
        mask = buffer.slice(offset, offset + 4);
        offset += 4;
    }
    const payload = buffer.slice(offset, offset + payloadLen);
    if (masked && mask) {
        for (let i = 0; i < payload.length; i++) {
            payload[i] ^= mask[i % 4];
        }
    }
    return payload.toString('utf-8');
}

function handleWebSocketUpgrade(req, socket) {
    const key = req.headers['sec-websocket-key'];
    if (!key) {
        socket.destroy();
        return;
    }
    const accept = crypto.createHash('sha1').update(key + WS_MAGIC).digest('base64');
    socket.write(
        'HTTP/1.1 101 Switching Protocols\r\n' +
            'Upgrade: websocket\r\n' +
            'Connection: Upgrade\r\n' +
            `Sec-WebSocket-Accept: ${accept}\r\n` +
            '\r\n'
    );

    socket.on('error', () => {
        socket.destroy();
    });

    let buffer = Buffer.alloc(0);
    socket.on('data', (chunk) => {
        buffer = Buffer.concat([buffer, chunk]);
        const text = decodeWsTextFrame(buffer);
        if (text === null) {
            return;
        }
        buffer = Buffer.alloc(0);
        try {
            const parsed = JSON.parse(text);
            const reply = JSON.stringify({
                ok: true,
                action: parsed.action || 'sync',
                items: [{ id: 'ws-item-1', name: 'WebSocket ticket' }],
            });
            socket.write(createWsTextFrame(reply));
        } catch {
            socket.write(createWsTextFrame(JSON.stringify({ ok: false, error: 'invalid_json' })));
        }
    });
}

const server = http.createServer((req, res) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);

    const parsed = new URL(req.url, `http://localhost:${PORT}`);
    const urlPath = parsed.pathname;
    const searchParams = parsed.searchParams;

    if (handleApi(req, res, urlPath, searchParams)) {
        return;
    }

    serveStatic(req, res, urlPath);
});

server.on('upgrade', (req, socket) => {
    const parsed = new URL(req.url, `http://localhost:${PORT}`);
    if (parsed.pathname === '/ws/cart') {
        handleWebSocketUpgrade(req, socket);
        return;
    }
    socket.destroy();
});

server.listen(PORT, () => {
    console.log(`
========================================
  fletta Test Server running
========================================
  Port: ${PORT}

  Conference demo (FixtureConf):
  - http://localhost:${PORT}/conference/index.html

  Context layer demos (C002/C003/C004):
  - http://localhost:${PORT}/context/checkout.html?mode=slow
  - http://localhost:${PORT}/context/cart.html?delay=600
  - http://localhost:${PORT}/context/ws-cart.html

  Press Ctrl+C to stop
========================================
    `);
});

process.on('SIGINT', () => {
    console.log('\nShutting down server...');
    server.close(() => {
        console.log('Server stopped');
        process.exit(0);
    });
});
