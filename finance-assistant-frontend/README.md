# Finance Assistant Frontend

A modern, responsive web application for AI-powered financial management with Google Gemini integration.

## 🏗️ Directory Structure

The frontend has been organized into a logical directory hierarchy for better maintainability and organization:

```
finance-assistant-frontend/
├── index.html                 # Main entry point and welcome page
├── pages/                     # Core application pages
│   ├── index.html            # Authentication/login page
│   ├── dashboard.html        # Main dashboard
│   ├── budget-management.html # Budget management interface
│   ├── goal-management.html  # Financial goals management
│   ├── transaction-management.html # Transaction viewing/editing
│   ├── transaction-upload.html # File upload interface
│   └── insight-service.html  # AI insights and analysis
├── css/                       # Stylesheets
│   └── styles.css            # Global CSS styles
├── js/                        # JavaScript files
│   ├── config.js             # Configuration and API endpoints
│   ├── auth.js               # Authentication logic
│   ├── api-service.js        # API communication service
│   ├── budget-management.js  # Budget management functionality
│   ├── goal-management.js    # Goal management functionality
│   ├── transaction-management.js # Transaction management logic
│   ├── transaction-upload.js # File upload handling
│   └── insight-service.js    # AI insights service
├── docs/                      # Documentation
│   ├── README.md             # This file
│   ├── AUTHENTICATION_FIX_README.md
│   ├── BUDGET_README.md
│   ├── CATEGORY_FIX_README.md
│   ├── GOAL_README.md
│   └── INSIGHT_SERVICE_README.md
├── tests/                     # Testing and debugging
│   ├── api-test.html         # API testing interface
│   ├── auth-debug.html       # Authentication debugging
│   ├── budget-debug.html     # Budget system debugging
│   ├── budget-test.html      # Budget functionality testing
│   ├── category-debug.html   # Category system debugging
│   ├── debug-403.html        # Access control debugging
│   ├── debug-categories.html # Category debugging interface
│   ├── gemini-test.html      # Gemini AI testing
│   ├── goal-test.html        # Goal functionality testing
│   ├── test-budget-access.html # Budget access testing
│   ├── test-config.html      # Configuration testing
│   ├── test-cors.html        # CORS testing
│   ├── test.html             # General testing interface
│   └── transaction-upload-test.html # Upload testing
└── assets/                    # Additional resources
    ├── rule-engine.html      # Rule engine interface
    ├── sample-transactions.csv # Sample data file
    └── start.sh              # Startup script
```

## 🚀 Getting Started

### 1. Entry Point
- **Main Welcome Page**: `index.html` - Overview and navigation to all sections
- **Authentication**: `pages/index.html` - Login and registration
- **Dashboard**: `pages/dashboard.html` - Main application interface

### 2. Core Features
- **Budget Management**: `pages/budget-management.html` - Create and manage budgets
- **Goal Management**: `pages/goal-management.html` - Set and track financial goals
- **Transaction Management**: `pages/transaction-management.html` - View and edit transactions
- **Transaction Upload**: `pages/transaction-upload.html` - Upload CSV/Excel files
- **AI Insights**: `pages/insight-service.html` - AI-powered financial analysis

## 🎨 Styling

- **Global Styles**: `css/styles.css` - Consistent design across all pages
- **Responsive Design**: Mobile-first approach with CSS Grid and Flexbox
- **Theme**: Modern, clean interface with consistent color scheme

## ⚙️ JavaScript Architecture

- **Configuration**: `js/config.js` - Centralized configuration management
- **Authentication**: `js/auth.js` - User authentication and session management
- **API Service**: `js/api-service.js` - Backend communication layer
- **Feature Modules**: Separate JS files for each major feature area

## 📚 Documentation

- **Technical Docs**: `docs/` - Detailed documentation for each component
- **API References**: Configuration and endpoint documentation
- **Troubleshooting**: Common issues and solutions

## 🧪 Testing & Development

- **Test Pages**: `tests/` - Isolated testing interfaces for each feature
- **Debug Tools**: Development and troubleshooting utilities
- **Sample Data**: Test files and example data for development

## 🔧 Development Workflow

### File Organization
1. **HTML Pages**: Core application interfaces in `pages/`
2. **JavaScript Logic**: Feature-specific logic in `js/`
3. **Styling**: Global styles in `css/`
4. **Documentation**: Technical docs in `docs/`
5. **Testing**: Debug and test tools in `tests/`
6. **Assets**: Additional resources in `assets/`

### Path References
- All HTML files reference CSS and JS files using relative paths (`../css/`, `../js/`)
- Navigation links between pages use relative paths within the `pages/` directory
- External resources (CDN links) remain unchanged

## 🌐 Browser Compatibility

- **Modern Browsers**: Chrome, Firefox, Safari, Edge (latest versions)
- **Mobile Support**: Responsive design for tablets and smartphones
- **Progressive Enhancement**: Core functionality works without JavaScript

## 📱 Responsive Design

- **Mobile First**: Designed for mobile devices first
- **Breakpoints**: Responsive breakpoints for different screen sizes
- **Touch Friendly**: Optimized for touch interfaces

## 🔐 Security Features

- **Authentication**: JWT-based authentication system
- **Session Management**: Secure session handling
- **Input Validation**: Client-side and server-side validation
- **CORS Support**: Proper cross-origin resource sharing

## 🚀 Performance

- **Optimized Assets**: Minified CSS and JavaScript
- **Lazy Loading**: On-demand resource loading
- **Caching**: Browser caching strategies
- **CDN Integration**: External resources from CDN

## 📋 File Naming Conventions

- **HTML Files**: `feature-name.html` (kebab-case)
- **JavaScript Files**: `feature-name.js` (kebab-case)
- **CSS Files**: `styles.css` (descriptive names)
- **Documentation**: `FEATURE_README.md` (UPPER_CASE with underscores)

## 🔄 Maintenance

### Adding New Features
1. Create HTML page in `pages/`
2. Add JavaScript logic in `js/`
3. Update styles in `css/styles.css`
4. Add documentation in `docs/`
5. Create test page in `tests/`

### Updating Existing Features
1. Modify the relevant files in their respective directories
2. Update documentation if needed
3. Test changes using appropriate test pages
4. Ensure responsive design is maintained

## 📞 Support

For technical support or questions about the directory structure:
- Check the documentation in `docs/`
- Review test pages in `tests/`
- Examine the main entry point at `index.html`

---

**Note**: This directory structure provides a clean separation of concerns while maintaining easy navigation and development workflow. All paths have been updated to reflect the new organization. 