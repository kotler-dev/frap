const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;

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

const server = http.createServer((req, res) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
    
    const urlPath = req.url.split('?')[0].split('#')[0];
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
                // Page not found
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
                // Server error
                res.writeHead(500);
                res.end('Server Error: ' + error.code + ' ..\n');
            }
        } else {
            // Success
            res.writeHead(200, { 
                'Content-Type': contentType,
                'Cache-Control': 'no-cache',
            });
            res.end(content, 'utf-8');
        }
    });
});

server.listen(PORT, () => {
    console.log(`
========================================
  fletta Test Server running
========================================
  Port: ${PORT}
  
  Conference demo (FixtureConf):
  - http://localhost:${PORT}/conference/index.html
  - http://localhost:${PORT}/conference/schedule-v1.html
  - http://localhost:${PORT}/conference/schedule-v2.html
  - http://localhost:${PORT}/conference/register.html
  - http://localhost:${PORT}/conference/cfp.html
  - http://localhost:${PORT}/conference/speakers.html
  - http://localhost:${PORT}/conference/speaker.html?id=alexey
  - http://localhost:${PORT}/conference/schedule-heal.html
  - http://localhost:${PORT}/conference/talk.html?id=opening
  
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
