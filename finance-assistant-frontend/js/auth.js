// Configuration - Now loaded from config.js
let API_BASE_URL;
let API_ENDPOINTS;

// Initialize configuration when CONFIG is available
function initializeConfig() {
    if (typeof CONFIG !== 'undefined') {
        API_BASE_URL = CONFIG.getBackendUrl();
        API_ENDPOINTS = CONFIG.ENDPOINTS;
        console.log('🔧 Auth.js: Configuration initialized');
    } else {
        console.error('❌ Auth.js: CONFIG not available');
    }
}

// Wait for CONFIG to be available
if (typeof CONFIG !== 'undefined') {
    initializeConfig();
} else {
    // Wait for CONFIG to load
    document.addEventListener('DOMContentLoaded', function() {
        if (typeof CONFIG !== 'undefined') {
            initializeConfig();
        } else {
            console.error('❌ Auth.js: CONFIG still not available after DOM load');
        }
    });
}

// DOM Elements
const tabBtns = document.querySelectorAll('.tab-btn');
const authForms = document.querySelectorAll('.auth-form');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const loadingOverlay = document.getElementById('loadingOverlay');
const messageContainer = document.getElementById('messageContainer');

// Tab switching functionality - only add if tab buttons exist
if (tabBtns.length > 0) {
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.dataset.tab;
            
            // Update active tab button
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            // Update active form
            authForms.forEach(form => {
                form.classList.remove('active');
                if (form.id === `${targetTab}-form`) {
                    form.classList.add('active');
                }
            });
            
            // Clear forms when switching tabs
            clearForms();
            clearMessages();
        });
    });
}

// Password visibility toggle - only add if elements exist
const togglePasswordBtns = document.querySelectorAll('.toggle-password');
if (togglePasswordBtns.length > 0) {
    togglePasswordBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const input = e.target.closest('.password-input').querySelector('input');
            const icon = e.target;
            
            if (input.type === 'password') {
                input.type = 'text';
                icon.className = 'fas fa-eye-slash';
            } else {
                input.type = 'password';
                icon.className = 'fas fa-eye';
            }
        });
    });
}

// Form submission handlers - only add if forms exist
if (loginForm) {
    loginForm.addEventListener('submit', handleLogin);
}
if (registerForm) {
    registerForm.addEventListener('submit', handleRegister);
}

// Login form handler
async function handleLogin(e) {
    e.preventDefault();
    
    // Check if configuration is available
    if (!API_BASE_URL || !API_ENDPOINTS) {
        showMessage('Configuration not loaded. Please refresh the page.', 'error');
        return;
    }
    
    const formData = new FormData(loginForm);
    const email = formData.get('email');
    const password = formData.get('password');
    
    // Basic validation
    if (!validateEmail(email)) {
        showMessage('Please enter a valid email address', 'error');
        return;
    }
    
    if (password.length < 6) {
        showMessage('Password must be at least 6 characters long', 'error');
        return;
    }
    
    try {
        showLoading(true);
        
        const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.LOGIN}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password })
        });
        
        if (response.ok) {
            const data = await response.json();
            
            // Debug: Log the response data
            console.log('🔐 Login response data:', data);
            console.log('🔐 Available fields:', Object.keys(data));
            
            // Store JWT token and user data
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('userEmail', email);
            localStorage.setItem('userName', data.name || data.userName || 'User'); // Store name from response
            
            console.log('🔐 Stored userName:', data.name || data.userName || 'User');
            
            showMessage('Login successful! Redirecting...', 'success');
            
            // Redirect to dashboard or main page
            setTimeout(() => {
                window.location.href = 'dashboard.html'; // Use relative path
            }, 1500);
            
        } else {
            showMessage(data.message || 'Login failed. Please check your credentials.', 'error');
        }
        
    } catch (error) {
        console.error('Login error:', error);
        showMessage('Network error. Please try again.', 'error');
    } finally {
        showLoading(false);
    }
}

// Register form handler
async function handleRegister(e) {
    e.preventDefault();
    
    // Check if configuration is available
    if (!API_BASE_URL || !API_ENDPOINTS) {
        showMessage('Configuration not loaded. Please refresh the page.', 'error');
        return;
    }
    
    const formData = new FormData(registerForm);
    const name = formData.get('name');
    const email = formData.get('email');
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    // Validation
    if (!name.trim()) {
        showMessage('Please enter your full name', 'error');
        return;
    }
    
    if (!validateEmail(email)) {
        showMessage('Please enter a valid email address', 'error');
        return;
    }
    
    if (password.length < 6) {
        showMessage('Password must be at least 6 characters long', 'error');
        return;
    }
    
    if (password !== confirmPassword) {
        showMessage('Passwords do not match', 'error');
        return;
    }
    
    try {
        showLoading(true);
        
        const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.REGISTER}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ name, email, password })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Store JWT token
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('userEmail', email);
            localStorage.setItem('userName', name);
            
            showMessage('Registration successful! Redirecting...', 'success');
            
            // Redirect to dashboard or main page
            setTimeout(() => {
                window.location.href = '../html/dashboard.html'; // Use relative path
            }, 1500);
            
        } else {
            showMessage(data.message || 'Registration failed. Please try again.', 'error');
        }
        
    } catch (error) {
        console.error('Registration error:', error);
        showMessage('Network error. Please try again.', 'error');
    } finally {
        showLoading(false);
    }
}

// Utility functions
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function showLoading(show) {
    if (show) {
        loadingOverlay.classList.add('active');
    } else {
        loadingOverlay.classList.remove('active');
    }
}

function showMessage(message, type = 'info') {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${type}`;
    messageElement.textContent = message;
    
    messageContainer.appendChild(messageElement);
    
    // Auto-remove message after 5 seconds
    setTimeout(() => {
        messageElement.remove();
    }, 5000);
}

function clearMessages() {
    messageContainer.innerHTML = '';
}

function clearForms() {
    loginForm.reset();
    registerForm.reset();
    
    // Clear validation states
    document.querySelectorAll('.form-group input').forEach(input => {
        input.classList.remove('error', 'success');
    });
}

// Check if user is already authenticated
function checkAuthStatus() {
    const token = localStorage.getItem('authToken');
    if (token) {
        // Only verify token if we're on the login/register page
        // Don't auto-redirect from authenticated pages
        if (window.location.pathname.includes('index.html') || 
            window.location.pathname.includes('login') || 
            window.location.pathname.includes('register')) {
            verifyToken(token);
        }
    }
}

// Verify JWT token with backend
async function verifyToken(token) {
    // Check if configuration is available
    if (!API_BASE_URL) {
        console.error('Configuration not available for token verification');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/verify`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            }
        });
        
        if (response.ok) {
            // Token is valid, only redirect to dashboard if we're on login/register page
            if (window.location.pathname.includes('index.html') || 
                window.location.pathname.includes('login') || 
                window.location.pathname.includes('register')) {
                window.location.href = '../html/dashboard.html';
            }
        } else {
            // Token is invalid, remove it
            localStorage.removeItem('authToken');
            localStorage.removeItem('userEmail');
            localStorage.removeItem('userName');
        }
    } catch (error) {
        console.error('Token verification error:', error);
        // Remove token on error
        localStorage.removeItem('authToken');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userName');
    }
}

// API health check
async function checkApiHealth() {
    // Check if configuration is available
    if (!API_BASE_URL || !API_ENDPOINTS) {
        console.error('Configuration not available for API health check');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.HEALTH}`);
        if (!response.ok) {
            showMessage('Backend service is not available. Please try again later.', 'error');
        }
    } catch (error) {
        console.error('API health check failed:', error);
        showMessage('Cannot connect to backend service. Please check your connection.', 'error');
    }
}

// Form validation with real-time feedback
function setupFormValidation() {
    const inputs = document.querySelectorAll('input');
    
    inputs.forEach(input => {
        input.addEventListener('blur', () => {
            validateField(input);
        });
        
        input.addEventListener('input', () => {
            // Clear error state on input
            input.classList.remove('error');
        });
    });
}

function validateField(input) {
    const value = input.value.trim();
    
    if (input.type === 'email' && value) {
        if (!validateEmail(value)) {
            input.classList.add('error');
            return false;
        }
    }
    
    if (input.required && !value) {
        input.classList.add('error');
        return false;
    }
    
    if (input.type === 'password' && value && value.length < 6) {
        input.classList.add('error');
        return false;
    }
    
    input.classList.remove('error');
    input.classList.add('success');
    return true;
}

// Initialize the application
function init() {
    setupFormValidation();
    checkAuthStatus();
    checkApiHealth();
    
    // Add some visual feedback for form interactions
    document.querySelectorAll('input').forEach(input => {
        input.addEventListener('focus', () => {
            input.parentElement.classList.add('focused');
        });
        
        input.addEventListener('blur', () => {
            input.parentElement.classList.remove('focused');
        });
    });
}

// Start the application when DOM is loaded
document.addEventListener('DOMContentLoaded', init);

// Export functions for potential use in other scripts
window.AuthManager = {
    login: handleLogin,
    register: handleRegister,
    logout: () => {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userName');
        window.location.href = '../index.html';
    },
    isAuthenticated: () => {
        return !!localStorage.getItem('authToken');
    },
    getToken: () => {
        return localStorage.getItem('authToken');
    },
    getUserInfo: () => {
        return {
            email: localStorage.getItem('userEmail'),
            name: localStorage.getItem('userName')
        };
    }
}; 