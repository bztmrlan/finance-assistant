#!/bin/bash

echo "🚀 Starting Finance Assistant Frontend..."
echo ""

echo "Starting HTTP server on port 3000..."
echo "Open http://localhost:3000 in your browser"
echo "Frontend will be available at:"
echo "   - Auth Page: http://localhost:3000/index.html"
python -m http.server 3000
