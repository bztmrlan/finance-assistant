#!/bin/bash

echo "🚀 Starting Finance Assistant Frontend..."
echo ""

# Check if Python 3 is available
if command -v python3 &> /dev/null; then
    echo "✅ Python 3 found. Starting HTTP server on port 3000..."
    echo "🌐 Open http://localhost:3000 in your browser"
    echo "📱 Frontend will be available at:"
    echo "   - Auth Page: http://localhost:3000/index.html"
    echo "   - Dashboard: http://localhost:3000/dashboard.html"
    echo "   - Test Page: http://localhost:3000/test.html"
    echo "   - Config Test: http://localhost:3000/test-config.html"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    python3 -m http.server 3000
elif command -v python &> /dev/null; then
    echo "✅ Python found. Starting HTTP server on port 3000..."
    echo "🌐 Open http://localhost:3000 in your browser"
    echo "📱 Frontend will be available at:"
    echo "   - Auth Page: http://localhost:3000/index.html"
    echo "   - Dashboard: http://localhost:3000/dashboard.html"
    echo "   - Test Page: http://localhost:3000/test.html"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    python -m http.server 3000
elif command -v php &> /dev/null; then
    echo "✅ PHP found. Starting HTTP server on port 3000..."
    echo "🌐 Open http://localhost:3000 in your browser"
    echo "📱 Frontend will be available at:"
    echo "   - Auth Page: http://localhost:3000/index.html"
    echo "   - Dashboard: http://localhost:3000/dashboard.html"
    echo "   - Test Page: http://localhost:3000/test.html"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    php -S localhost:3000
elif command -v node &> /dev/null; then
    echo "✅ Node.js found. Installing http-server..."
    echo "🌐 Starting HTTP server on port 3000..."
    echo "📱 Frontend will be available at:"
    echo "   - Auth Page: http://localhost:3000/index.html"
    echo "   - Dashboard: http://localhost:3000/dashboard.html"
    echo "   - Test Page: http://localhost:3000/test.html"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    npx http-server -p 3000
else
    echo "❌ No suitable HTTP server found."
    echo ""
    echo "Please install one of the following:"
    echo "  - Python 3: sudo apt-get install python3 (Ubuntu/Debian)"
    echo "  - Python: sudo apt-get install python (Ubuntu/Debian)"
    echo "  - PHP: sudo apt-get install php (Ubuntu/Debian)"
    echo "  - Node.js: https://nodejs.org/"
    echo ""
    echo "Or manually open index.html in your browser (may have CORS issues)"
    echo ""
    echo "Alternative: Use any web server like nginx, Apache, or live-server"
fi 