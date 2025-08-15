// Configuration file for Finance Assistant Frontend
const CONFIG = {
    // Frontend port (different from backend to avoid port conflicts)
    FRONTEND_PORT: 3000,
    
    // Backend API configuration
    BACKEND: {
        HOST: 'localhost',
        PORT: 8080,
        PROTOCOL: 'http'
    },
    
    // API endpoints
    ENDPOINTS: {
        LOGIN: '/auth/login',
        REGISTER: '/auth/register',
        HEALTH: '/auth/health',
        VERIFY: '/auth/verify',
        
        // Transaction Upload
        TRANSACTION_UPLOAD: '/api/transactions/upload',
        TRANSACTION_UPLOAD_STATUS: '/api/transactions/upload/status',
        TRANSACTION_UPLOAD_FORMATS: '/api/transactions/upload/formats',
        TRANSACTION_UPLOAD_TEMPLATE: '/api/transactions/upload/template',
        
        // Category Management
        CATEGORIES: '/api/categories',
        CATEGORY_BY_ID: '/api/categories/{categoryId}',
        
        // Budget Management
        BUDGETS: '/api/budgets',
        BUDGETS_ACTIVE: '/api/budgets/active',
        BUDGET_SUMMARY: '/api/budgets/{budgetId}/summary',
        BUDGET_CATEGORY_LIMIT: '/api/budgets/{budgetId}/categories/{categoryId}/limit',
        BUDGET_ADD_CATEGORY: '/api/budgets/{budgetId}/categories',
        BUDGET_EVALUATE: '/api/budgets/{budgetId}/evaluate',
        BUDGETS_ATTENTION_NEEDED: '/api/budgets/attention-needed',
        BUDGET_ARCHIVE: '/api/budgets/{budgetId}/archive',
        BUDGETS_RECALCULATE_SPENDING: '/api/budgets/recalculate-spending',
        
        // Goal Management
        GOALS: '/api/goals',
        GOAL_BY_ID: '/api/goals/{goalId}',
        GOAL_UPDATE_PROGRESS: '/api/goals/{goalId}/progress',
        GOALS_EVALUATE: '/api/goals/evaluate',
        GOALS_CALCULATE_PROGRESS: '/api/goals/calculate-progress',
        
        // Rule Engine
        RULE_ENGINE_EVALUATE: '/api/rule-engine/evaluate',
        RULE_ENGINE_EASY_RULES: '/api/rule-engine/evaluate/easy-rules',
        RULE_ENGINE_EVALUATE_USER: '/api/rule-engine/evaluate/{userId}'
    },
    
    // JWT configuration
    JWT: {
        STORAGE_KEY: 'authToken',
        USER_EMAIL_KEY: 'userEmail',
        USER_NAME_KEY: 'userName'
    },
    
    // UI configuration
    UI: {
        MESSAGE_TIMEOUT: 5000, // 5 seconds
        REDIRECT_DELAY: 1500,  // 1.5 seconds
        PASSWORD_MIN_LENGTH: 6
    }
};

// Helper function to get full backend URL
CONFIG.getBackendUrl = function() {
    return `${this.BACKEND.PROTOCOL}://${this.BACKEND.HOST}:${this.BACKEND.PORT}`;
};

// Helper function to get full frontend URL
CONFIG.getFrontendUrl = function() {
    return `${this.BACKEND.PROTOCOL}://${this.BACKEND.HOST}:${this.FRONTEND_PORT}`;
};

// Helper function to replace path parameters
CONFIG.replacePathParams = function(endpoint, params) {
    let result = endpoint;
    for (const [key, value] of Object.entries(params)) {
        result = result.replace(`{${key}}`, value);
    }
    return result;
};

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CONFIG;
} else {
    window.CONFIG = CONFIG;
    console.log('🔧 CONFIG loaded successfully:', {
        hasEndpoints: !!CONFIG.ENDPOINTS,
        hasBudgetEndpoints: !!CONFIG.ENDPOINTS.BUDGETS_RECALCULATE_SPENDING,
        budgetEndpoint: CONFIG.ENDPOINTS.BUDGETS_RECALCULATE_SPENDING
    });
} 