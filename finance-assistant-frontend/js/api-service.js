// API Service for Finance Assistant Frontend
// This file provides methods to interact with all backend controllers

class ApiService {
    constructor() {
        console.log('🔧 ApiService constructor called');
        console.log('🔧 CONFIG available in constructor:', !!window.CONFIG);
        console.log('🔧 CONFIG.ENDPOINTS available in constructor:', !!window.CONFIG?.ENDPOINTS);
        console.log('🔧 BUDGETS_RECALCULATE_SPENDING available in constructor:', !!window.CONFIG?.ENDPOINTS?.BUDGETS_RECALCULATE_SPENDING);
        
        this.baseUrl = window.CONFIG.getBackendUrl();
        console.log('🔧 Base URL set to:', this.baseUrl);
    }

    // Helper method to get auth headers
    getAuthHeaders() {
        const token = localStorage.getItem('authToken'); // Use the same key as auth.js
        console.log('Getting auth headers, token exists:', !!token);
        console.log('Token value:', token ? token.substring(0, 20) + '...' : 'none');
        
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
    }

    // Helper method to handle API responses
    async handleResponse(response) {
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`API Error: ${response.status} - ${errorText}`);
        }
        
        // Check if response has content
        const contentType = response.headers.get('content-type');
        console.log(`🔧 Response content-type:`, contentType);
        console.log(`🔧 Response status:`, response.status);
        console.log(`🔧 Response headers:`, Object.fromEntries(response.headers.entries()));
        
        // Check if response is empty
        const contentLength = response.headers.get('content-length');
        if (contentLength === '0') {
            console.log(`🔧 Response is empty (content-length: 0)`);
            return null;
        }
        
        if (contentType && contentType.includes('application/json')) {
            let responseText;
            try {
                responseText = await response.text();
                console.log(`🔧 Raw response text:`, responseText);
                
                // Check if response is empty or very short (might indicate truncation)
                if (!responseText || responseText.trim().length === 0) {
                    console.warn(`⚠️ Response is empty or contains only whitespace`);
                    return null;
                }
                
                if (responseText.length < 10) {
                    console.warn(`⚠️ Response is very short (${responseText.length} chars): "${responseText}"`);
                }
                
                // Check for common JSON issues
                if (responseText.includes('undefined') || responseText.includes('null')) {
                    console.warn(`⚠️ Response contains undefined or null values`);
                }
                
                // Check for excessive closing braces (indicates malformed JSON)
                const openBraces = (responseText.match(/\{/g) || []).length;
                const closeBraces = (responseText.match(/\}/g) || []).length;
                if (closeBraces > openBraces) {
                    console.warn(`⚠️ Response has more closing braces (${closeBraces}) than opening braces (${openBraces})`);
                }
                
                // Try to parse the JSON
                const parsedResponse = JSON.parse(responseText);
                console.log(`🔧 Parsed response:`, parsedResponse);
                return parsedResponse;
            } catch (jsonError) {
                console.error(`❌ JSON parsing error:`, jsonError);
                console.error(`❌ Raw response that failed to parse:`, responseText || 'Response text not available');
                
                // Provide more detailed error information
                let errorMessage = `JSON parsing error: ${jsonError.message}`;
                if (responseText) {
                    errorMessage += `. Response preview: ${responseText.substring(0, 200)}...`;
                    
                    // Add specific guidance for common JSON issues
                    if (responseText.includes('undefined') || responseText.includes('null')) {
                        errorMessage += ' Response contains undefined or null values.';
                    }
                    
                    const openBraces = (responseText.match(/\{/g) || []).length;
                    const closeBraces = (responseText.match(/\}/g) || []).length;
                    if (closeBraces > openBraces) {
                        errorMessage += ` Response has malformed JSON structure (${closeBraces} closing vs ${openBraces} opening braces).`;
                    }
                } else {
                    errorMessage += '. Response text not available.';
                }
                
                throw new Error(errorMessage);
            }
        } else if (contentType && contentType.includes('text/plain')) {
            const responseText = await response.text();
            console.log(`🔧 Plain text response:`, responseText);
            return responseText;
        } else {
            // Handle cases where content type is not specified or is unexpected
            console.warn(`⚠️ Unexpected content type: ${contentType}`);
            try {
                const responseText = await response.text();
                console.log(`🔧 Raw response (unknown content type):`, responseText);
                
                // Try to parse as JSON anyway (some servers don't set content-type correctly)
                if (responseText && responseText.trim().length > 0) {
                    try {
                        const parsedResponse = JSON.parse(responseText);
                        console.log(`🔧 Successfully parsed as JSON despite unknown content type`);
                        return parsedResponse;
                    } catch (jsonError) {
                        console.log(`🔧 Could not parse as JSON, returning as text`);
                        return responseText;
                    }
                }
                
                return responseText;
            } catch (textError) {
                console.error(`❌ Failed to read response text:`, textError);
                return null;
            }
        }
        
        return null;
    }

    // Helper method to make authenticated requests
    async authenticatedRequest(endpoint, options = {}) {
        // Check if user is authenticated
        if (!this.isAuthenticated()) {
            console.log('🔐 User not authenticated, redirecting to login...');
            this.handleTokenExpired();
            throw new Error('User not authenticated. Please login first.');
        }

        const url = this.baseUrl + endpoint;
        const config = {
            headers: this.getAuthHeaders(),
            ...options
        };

        try {
            console.log(`Making authenticated request to: ${url}`);
            console.log('Request config:', config);
            
            const response = await fetch(url, config);
            console.log(`Response status: ${response.status}`);
            console.log(`Response headers:`, response.headers);
            
            // Handle 403 Forbidden - likely expired token
            if (response.status === 403) {
                console.log('🔐 403 Forbidden - checking token validity...');
                const tokenValid = await this.verifyToken();
                if (!tokenValid) {
                    console.log('🔐 Token is invalid, redirecting to login...');
                    this.handleTokenExpired();
                    throw new Error('Authentication token expired. Please login again.');
                } else {
                    console.log('🔐 Token is valid, retrying request...');
                    // Retry the request with fresh token
                    const retryConfig = {
                        headers: this.getAuthHeaders(),
                        ...options
                    };
                    const retryResponse = await fetch(url, retryConfig);
                    return await this.handleResponse(retryResponse);
                }
            }
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error(`API request failed for ${endpoint}:`, error);
            throw error;
        }
    }

    // Handle expired token
    handleTokenExpired() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userName');
        
        // Redirect to login page
        if (window.location.pathname.includes('html/')) {
            window.location.href = '../html/auth.html';
        } else {
            window.location.href = 'html/auth.html';
        }
    }

    // ========================================
    // AUTHENTICATION ENDPOINTS
    // ========================================

    async verifyToken() {
        try {
            const response = await fetch(this.baseUrl + window.CONFIG.ENDPOINTS.VERIFY, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            
            if (response.ok) {
                console.log('🔐 Token verification successful');
                return true;
            } else {
                console.log('🔐 Token verification failed:', response.status);
                return false;
            }
        } catch (error) {
            console.error('🔐 Token verification error:', error);
            return false;
        }
    }

    async login(credentials) {
        const response = await fetch(this.baseUrl + window.CONFIG.ENDPOINTS.LOGIN, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials)
        });
        return await this.handleResponse(response);
    }

    async register(userData) {
        const response = await fetch(this.baseUrl + window.CONFIG.ENDPOINTS.REGISTER, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData)
        });
        return await this.handleResponse(response);
    }

    // ========================================
    // TRANSACTION UPLOAD ENDPOINTS
    // ========================================

    async uploadTransactions(formData) {
        const token = localStorage.getItem('authToken'); // Use the same key as auth.js
        const response = await fetch(this.baseUrl + window.CONFIG.ENDPOINTS.TRANSACTION_UPLOAD, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });
        return await this.handleResponse(response);
    }

    async getUploadStatus() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.TRANSACTION_UPLOAD_STATUS);
    }

    async getSupportedFormats() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.TRANSACTION_UPLOAD_FORMATS);
    }

    async getUploadTemplate() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.TRANSACTION_UPLOAD_TEMPLATE);
    }

    // ========================================
    // CATEGORY MANAGEMENT ENDPOINTS
    // ========================================

    async createCategory(categoryData) {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.CATEGORIES, {
            method: 'POST',
            body: JSON.stringify(categoryData)
        });
    }

    async getUserCategories() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.CATEGORIES);
    }

    async getCategoryById(categoryId) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.CATEGORY_BY_ID, { categoryId });
        return this.authenticatedRequest(endpoint);
    }

    // ========================================
    // BUDGET MANAGEMENT ENDPOINTS
    // ========================================

    async createBudget(budgetData, categoryLimits) {
        const requestData = {
            budget: budgetData,
            categoryLimits: categoryLimits
        };
        
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.BUDGETS, {
            method: 'POST',
            body: JSON.stringify(requestData)
        });
    }

    async updateBudget(budgetId, budgetData) {
        console.log('🔧 updateBudget called with:', { budgetId, budgetData });
        
        const endpoint = `/api/budgets/${budgetId}`;
        
        return this.authenticatedRequest(endpoint, {
            method: 'PUT',
            body: JSON.stringify(budgetData)
        });
    }

    async getUserBudgets() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.BUDGETS);
    }

    async getActiveUserBudgets() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.BUDGETS_ACTIVE);
    }

    async getBudgetSummary(budgetId) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.BUDGET_SUMMARY, { budgetId });
        return this.authenticatedRequest(endpoint);
    }

    async updateCategoryLimit(budgetId, categoryId, newLimit) {
        console.log('🔧 updateCategoryLimit called with:', { budgetId, categoryId, newLimit });
        console.log('🔧 CONFIG.ENDPOINTS.BUDGET_CATEGORY_LIMIT:', window.CONFIG?.ENDPOINTS?.BUDGET_CATEGORY_LIMIT);
        
        const endpoint = {
            url: window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.BUDGET_CATEGORY_LIMIT, { budgetId, categoryId }),
            method: 'PUT',
            body: { newLimit }
        };
        
        console.log(`🔧 Making request to: ${endpoint.method} ${endpoint.url}`);
        console.log(`🔧 Full URL: ${this.baseUrl}${endpoint.url}`);
        console.log(`🔧 Request body:`, endpoint.body);
        
        try {
            console.log(`🔧 Request body before stringify:`, endpoint.body);
            const requestBody = JSON.stringify(endpoint.body);
            console.log(`🔧 Request body after stringify:`, requestBody);
            
            const result = await this.authenticatedRequest(endpoint.url, {
                method: endpoint.method,
                body: requestBody
            });
            
            console.log(`✅ Success with ${endpoint.method} ${endpoint.url}`);
            console.log(`🔧 Response result:`, result);
            return result;
        } catch (error) {
            console.log(`❌ Request failed: ${endpoint.method} ${endpoint.url}`, error.message);
            console.log(`🔧 Error details:`, {
                message: error.message,
                stack: error.stack,
                name: error.name
            });
            throw error;
        }
    }

    async addCategoryToBudget(budgetId, categoryId, limitAmount) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.BUDGET_ADD_CATEGORY, { budgetId });
        
        const requestBody = { categoryId, limitAmount };
        console.log(`🔧 addCategoryToBudget request body:`, requestBody);
        console.log(`🔧 addCategoryToBudget endpoint:`, endpoint);
        
        return this.authenticatedRequest(endpoint, {
            method: 'POST',
            body: JSON.stringify(requestBody)
        });
    }

    async deleteCategoryFromBudget(budgetId, categoryId) {
        console.log('🗑️ deleteCategoryFromBudget called with:', { budgetId, categoryId });
        
        const endpoint = `/api/budgets/${budgetId}/categories/${categoryId}`;
        console.log('🗑️ DELETE endpoint:', endpoint);
        
        try {
            const result = await this.authenticatedRequest(endpoint, {
                method: 'DELETE'
            });
            
            console.log('🗑️ DELETE request completed, result:', result);
            return result;
        } catch (error) {
            console.error('🗑️ DELETE request failed:', error);
            console.error('🗑️ Error details:', {
                message: error.message,
                name: error.name,
                stack: error.stack
            });
            
            // If it's a 400 error, try to get more details
            if (error.message.includes('400')) {
                console.error('🗑️ 400 error detected - attempting to get response body for more details');
                try {
                    const response = await fetch(`${this.baseUrl}${endpoint}`, {
                        method: 'DELETE',
                        headers: {
                            'Authorization': `Bearer ${this.getAuthToken()}`,
                            'Content-Type': 'application/json'
                        }
                    });
                    
                    const responseText = await response.text();
                    console.error('🗑️ Response body for 400 error:', responseText);
                    
                    if (responseText) {
                        error.message = `API Error: 400 - ${responseText}`;
                    }
                } catch (fetchError) {
                    console.error('🗑️ Failed to get response body:', fetchError);
                }
            }
            
            throw error;
        }
    }

    async evaluateBudget(budgetId) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.BUDGET_EVALUATE, { budgetId });
        return this.authenticatedRequest(endpoint, { method: 'POST' });
    }

    async getBudgetsNeedingAttention() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.BUDGETS_ATTENTION_NEEDED);
    }

    async archiveBudget(budgetId) {
        console.log('🔧 CONFIG object available:', !!window.CONFIG);
        console.log('🔧 CONFIG.ENDPOINTS available:', !!window.CONFIG?.ENDPOINTS);
        console.log('🔧 BUDGET_ARCHIVE endpoint:', window.CONFIG?.ENDPOINTS?.BUDGET_ARCHIVE);
        
        // Fallback to hardcoded endpoint if CONFIG is not available
        let endpoint = `/api/budgets/${budgetId}/archive`;
        
        if (window.CONFIG?.ENDPOINTS?.BUDGET_ARCHIVE) {
            endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.BUDGET_ARCHIVE, { budgetId });
            console.log('🔧 Using CONFIG endpoint:', endpoint);
        } else {
            console.log('🔧 CONFIG not available, using hardcoded endpoint:', endpoint);
        }
        
        console.log('🔧 Final endpoint to call:', endpoint);
        return this.authenticatedRequest(endpoint, { method: 'PUT' });
    }

    async deleteBudget(budgetId) {
        console.log('🗑️ Deleting budget:', budgetId);
        
        // Use the standard DELETE endpoint for budgets
        const endpoint = `/api/budgets/${budgetId}`;
        console.log('🔧 Final endpoint to call:', endpoint);
        
        return this.authenticatedRequest(endpoint, { method: 'DELETE' });
    }

    async recalculateAllBudgetSpending() {
        console.log('🔧 CONFIG object available:', !!window.CONFIG);
        console.log('🔧 CONFIG.ENDPOINTS available:', !!window.CONFIG?.ENDPOINTS);
        console.log('🔧 BUDGETS_RECALCULATE_SPENDING endpoint:', window.CONFIG?.ENDPOINTS?.BUDGETS_RECALCULATE_SPENDING);
        
        // Fallback to hardcoded endpoint if CONFIG is not available
        let endpoint = '/api/budgets/recalculate-spending';
        
        if (window.CONFIG?.ENDPOINTS?.BUDGETS_RECALCULATE_SPENDING) {
            endpoint = window.CONFIG.ENDPOINTS.BUDGETS_RECALCULATE_SPENDING;
            console.log('🔧 Using CONFIG endpoint:', endpoint);
        } else {
            console.log('🔧 CONFIG not available, using hardcoded endpoint:', endpoint);
        }
        
        console.log('🔧 Final endpoint to call:', endpoint);
        
        return this.authenticatedRequest(endpoint, { method: 'POST' });
    }

    async recalculateBudgetSpending(budgetId) {
        console.log('🔄 Recalculating spending for budget:', budgetId);
        
        // Use the same endpoint but with a specific budget ID
        let endpoint = `/api/budgets/${budgetId}/recalculate-spending`;
        
        console.log('🔧 Final endpoint to call:', endpoint);
        
        return this.authenticatedRequest(endpoint, { method: 'POST' });
    }

    async cleanupInvalidBudgetCategories() {
        console.log('🧹 Cleaning up invalid budget categories...');
        
        let endpoint = '/api/budgets/cleanup-invalid-categories';
        
        console.log('🔧 Final endpoint to call:', endpoint);
        
        return this.authenticatedRequest(endpoint, { method: 'POST' });
    }
    


    // ========================================
    // GOAL MANAGEMENT ENDPOINTS
    // ========================================

    async createGoal(goalData) {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.GOALS, {
            method: 'POST',
            body: JSON.stringify(goalData)
        });
    }

    async getUserGoals() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.GOALS);
    }

    async getGoalById(goalId) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.GOAL_BY_ID, { goalId });
        return this.authenticatedRequest(endpoint);
    }

    async updateGoal(goalId, goalData) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.GOAL_BY_ID, { goalId });
        
        return this.authenticatedRequest(endpoint, {
            method: 'PUT',
            body: JSON.stringify(goalData)
        });
    }

    async updateGoalProgress(goalId, amount) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.GOAL_UPDATE_PROGRESS, { goalId });
        
        return this.authenticatedRequest(endpoint, {
            method: 'PUT',
            body: JSON.stringify({ amount })
        });
    }

    async deleteGoal(goalId) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.GOAL_BY_ID, { goalId });
        return this.authenticatedRequest(endpoint, { method: 'DELETE' });
    }

    async evaluateGoals() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.GOALS_EVALUATE, { method: 'POST' });
    }
    
    async calculateGoalProgressFromTransactions() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.GOALS_CALCULATE_PROGRESS, { method: 'POST' });
    }

    // ========================================
    // RULE ENGINE ENDPOINTS
    // ========================================

    async evaluateRules() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.RULE_ENGINE_EVALUATE, { method: 'POST' });
    }

    async evaluateRulesWithEasyRules() {
        return this.authenticatedRequest(window.CONFIG.ENDPOINTS.RULE_ENGINE_EASY_RULES, { method: 'POST' });
    }

    async evaluateRulesForUser(userId) {
        const endpoint = window.CONFIG.replacePathParams(window.CONFIG.ENDPOINTS.RULE_ENGINE_EVALUATE_USER, { userId });
        return this.authenticatedRequest(endpoint, { method: 'POST' });
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    // Test all API endpoints
    async testAllEndpoints() {
        const results = {};
        
        try {
            // Test auth endpoints
            results.auth = {
                verify: await this.verifyToken().then(() => 'OK').catch(e => `Failed: ${e.message}`)
            };
        } catch (error) {
            results.auth = { verify: `Failed: ${error.message}` };
        }

        try {
            // Test transaction upload endpoints
            results.transactionUpload = {
                status: await this.getUploadStatus().then(() => 'OK').catch(e => `Failed: ${e.message}`),
                formats: await this.getSupportedFormats().then(() => 'OK').catch(e => `Failed: ${e.message}`),
                template: await this.getUploadTemplate().then(() => 'OK').catch(e => `Failed: ${e.message}`)
            };
        } catch (error) {
            results.transactionUpload = { error: `Failed: ${error.message}` };
        }

        try {
            // Test budget endpoints
            results.budgets = {
                list: await this.getUserBudgets().then(() => 'OK').catch(e => `Failed: ${e.message}`),
                active: await this.getActiveUserBudgets().then(() => 'OK').catch(e => `Failed: ${e.message}`)
            };
        } catch (error) {
            results.budgets = { error: `Failed: ${error.message}` };
        }

        try {
            // Test goal endpoints
            results.goals = {
                list: await this.getUserGoals().then(() => 'OK').catch(e => `Failed: ${e.message}`)
            };
        } catch (error) {
            results.goals = { error: `Failed: ${error.message}` };
        }

        return results;
    }

    // Check if user is authenticated
    isAuthenticated() {
        const token = localStorage.getItem('authToken');
        return !!token;
    }

    // Get current user info
    getCurrentUser() {
        return {
            email: localStorage.getItem('userEmail'),
            name: localStorage.getItem('userName'),
            token: localStorage.getItem('authToken')
        };
    }

    // Logout user
    logout() {
        localStorage.removeItem('authToken'); // Use the same key as auth.js
        localStorage.removeItem('userEmail'); // Use the same key as auth.js
        localStorage.removeItem('userName'); // Use the same key as auth.js
        window.location.href = 'index.html';
    }
}

// Create global instance
function initializeApiService() {
    if (window.CONFIG && window.CONFIG.ENDPOINTS) {
        console.log('🔧 CONFIG available, initializing ApiService');
        window.apiService = new ApiService();
        console.log('🔧 ApiService initialized successfully');
    } else {
        console.log('🔧 CONFIG not available yet, retrying in 100ms');
        setTimeout(initializeApiService, 100);
    }
}

// Start initialization
initializeApiService();

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ApiService;
} 