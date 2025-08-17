// Insight Service JavaScript
console.log('🧠 INSIGHT SERVICE JS LOADED');

let currentInsights = [];

// Initialize the page
document.addEventListener('DOMContentLoaded', function() {
    console.log('🔍 INSIGHT SERVICE PAGE LOADED');
    
    // Show page status
    const pageStatus = document.getElementById('pageStatus');
    if (pageStatus) {
        pageStatus.style.display = 'block';
        pageStatus.textContent = 'Page loaded successfully. Checking authentication...';
        console.log('Updated page status: Page loaded successfully. Checking authentication...');
    } else {
        console.warn('Page status element not found during page loading');
    }
    
    // Wait a bit for CONFIG to be fully loaded
    setTimeout(function() {
        initializePage();
    }, 100);
});

function initializePage() {

    
    // Check if CONFIG is available
    console.log('CONFIG available:', typeof CONFIG !== 'undefined');
    if (typeof CONFIG !== 'undefined') {
        console.log('Backend URL:', CONFIG.getBackendUrl());
        console.log('Full CONFIG object:', CONFIG);
    } else {
        console.error('❌ CONFIG object not available!');
        const pageStatus = document.getElementById('pageStatus');
        if (pageStatus) {
            pageStatus.style.background = '#f8d7da';
            pageStatus.style.color = '#721c24';
            pageStatus.textContent = 'Configuration not loaded. Please refresh the page.';
            console.log('Updated page status: Configuration not loaded. Please refresh the page.');
        } else {
            console.warn('Page status element not found during config failure');
        }
        return;
    }
    
    try {
        checkAuth();
        setupEventListeners();
        loadExistingInsights();
        
        const pageStatus = document.getElementById('pageStatus');
        if (pageStatus) {
            pageStatus.textContent = 'Page initialized successfully. Ready to use!';
            console.log('Updated page status: Page initialized successfully. Ready to use!');
            setTimeout(() => {
                pageStatus.style.display = 'none';
                console.log('Hiding page status after 3 seconds');
            }, 3000);
        } else {
            console.warn('Page status element not found during initialization');
        }
    } catch (error) {
        console.error('❌ Error during page initialization:', error);
        const pageStatus = document.getElementById('pageStatus');
        if (pageStatus) {
            pageStatus.style.background = '#f8d7da';
            pageStatus.style.color = '#721c24';
            pageStatus.textContent = 'Error initializing page: ' + error.message;
            console.log('Updated page status with error:', 'Error initializing page: ' + error.message);
        } else {
            console.warn('Page status element not found during error handling');
        }
        alert('Error initializing page: ' + error.message);
    }
}



// Check authentication status
function checkAuth() {
    // Update page status
    const pageStatus = document.getElementById('pageStatus');
    if (pageStatus) {
        pageStatus.textContent = 'Checking authentication...';
    }
    
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        if (pageStatus) {
            pageStatus.style.background = '#f8d7da';
            pageStatus.style.color = '#721c24';
            pageStatus.textContent = 'No authentication token found. Redirecting to login...';
        }
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2000);
        return;
    }
    
    if (pageStatus) {
        pageStatus.textContent = 'Authentication successful. Loading insights...';
    }
}

// Setup event listeners
function setupEventListeners() {
    const form = document.getElementById('insightForm');
    
    if (form) {
        form.addEventListener('submit', handleInsightRequest);
    }
}

// Handle insight form submission
async function handleInsightRequest(e) {
    e.preventDefault();
    
    const token = localStorage.getItem('authToken');
    console.log('Auth token found:', !!token);
    if (!token) {
        alert('Please log in to use the insight service');
        return;
    }
    
    const question = document.getElementById('question').value.trim();
    const timePeriod = document.getElementById('timePeriod').value;
    const categoryFilter = document.getElementById('categoryFilter').value;
    const analysisDepth = document.getElementById('analysisDepth').value;
    
    console.log('Form values:');
    console.log('  Question:', question);
    console.log('  Time Period:', timePeriod);
    console.log('  Category Filter:', categoryFilter);
    console.log('  Analysis Depth:', analysisDepth);
    
    if (question.length < 10) {
        alert('Please enter a question with at least 10 characters');
        return;
    }
    
    // Show loading state
    showLoading(true);
    
    try {
        // Extract time period from question if not explicitly provided
        let extractedTimePeriod = timePeriod;
        console.log('Original time period:', timePeriod);
        if (!extractedTimePeriod) {
            console.log('No time period provided, extracting from question...');
            extractedTimePeriod = extractTimePeriodFromQuestion(question);
            console.log('Extracted time period from question:', extractedTimePeriod);
        } else {
            console.log('Using provided time period:', extractedTimePeriod);
        }
        
        // If still no time period, set a default
        if (!extractedTimePeriod) {
            extractedTimePeriod = 'LAST_30_DAYS';
            console.log('No time period found, using default:', extractedTimePeriod);
        }
        
        const request = {
            question: question,
            timePeriod: extractedTimePeriod,
            categoryFilter: categoryFilter || null,
            includeCharts: true,
            analysisDepth: analysisDepth
        };
        
        console.log('Extracted time period:', extractedTimePeriod);
        console.log('Question:', question);
        console.log('Sending insight request:', request);
        
        const backendUrl = `${CONFIG.getBackendUrl()}/api/insights`;
        console.log('Making request to:', backendUrl);
        const headers = {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
        console.log('Request headers:', headers);
        console.log('Request body:', request);
        
        const response = await fetch(backendUrl, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(request)
        });
        
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);
        
        if (response.ok) {
            const insight = await response.json();
            console.log('Generated insight:', insight);
            console.log('Insight processed successfully');
            console.log('Insight message:', insight.message);
            console.log('Insight type:', insight.type);
            
                    console.log('Successfully generated insight using Hugging Face AI, adding to current insights...');
        console.log('Insight source:', insight.dataSource);
        console.log('Analysis type:', insight.analysisType);
        console.log('Data points analyzed:', insight.dataPointsAnalyzed);
        
        // Add to current insights and display
        currentInsights.unshift(insight);
        displayInsights();
        
        // Clear form
        document.getElementById('insightForm').reset();
        
        // Show success message
        showMessage('AI-powered insight generated successfully using Hugging Face!', 'success');
        console.log('Insight generation completed successfully');
            
        } else if (response.status === 401) {
            console.error('Authentication failed - 401 status');
            alert('Authentication failed. Please log in again.');
            window.location.href = 'index.html';
        } else {
            console.error('Non-OK response status:', response.status);
            const errorData = await response.json();
            console.error('Error response data:', errorData);
            console.error('Response status:', response.status);
            console.error('Response headers:', response.headers);
            throw new Error(errorData.message || 'Failed to generate insight');
        }
        
    } catch (error) {
        console.error('Error generating insight:', error);
        console.error('Error stack:', error.stack);
        showMessage('Error generating insight: ' + error.message, 'error');
    } finally {
        console.log('Cleaning up - hiding loading state');
        showLoading(false);
    }
}

// Extract time period from question text
function extractTimePeriodFromQuestion(question) {
    console.log('Extracting time period from question:', question);
    const lowerQuestion = question.toLowerCase();
    console.log('Lowercase question:', lowerQuestion);
    
    // Check for specific months
    if (lowerQuestion.includes('august')) {
        console.log('Found August, returning AUGUST_2025');
        return 'AUGUST_2025';
    } else if (lowerQuestion.includes('july')) {
        console.log('Found July, returning JULY_2025');
        return 'JULY_2025';
    } else if (lowerQuestion.includes('september')) {
        console.log('Found September, returning SEPTEMBER_2025');
        return 'SEPTEMBER_2025';
    } else if (lowerQuestion.includes('june')) {
        console.log('Found June, returning JUNE_2025');
        return 'JUNE_2025';
    } else if (lowerQuestion.includes('may')) {
        console.log('Found May, returning MAY_2025');
        return 'MAY_2025';
    } else if (lowerQuestion.includes('april')) {
        console.log('Found April, returning APRIL_2025');
        return 'APRIL_2025';
    } else if (lowerQuestion.includes('march')) {
        console.log('Found March, returning MARCH_2025');
        return 'MARCH_2025';
    } else if (lowerQuestion.includes('february')) {
        console.log('Found February, returning FEBRUARY_2025');
        return 'FEBRUARY_2025';
    } else if (lowerQuestion.includes('january')) {
        console.log('Found January, returning JANUARY_2025');
        return 'JANUARY_2025';
    } else if (lowerQuestion.includes('december')) {
        console.log('Found December, returning DECEMBER_2025');
        return 'DECEMBER_2025';
    } else if (lowerQuestion.includes('november')) {
        console.log('Found November, returning NOVEMBER_2025');
        return 'NOVEMBER_2025';
    } else if (lowerQuestion.includes('october')) {
        console.log('Found October, returning OCTOBER_2025');
        return 'OCTOBER_2025';
    }
    
    // Check for other time patterns
    if (lowerQuestion.includes('last') && lowerQuestion.includes('days')) {
        // Extract number of days
        const match = question.match(/last\s+(\d+)\s+days/i);
        if (match) {
            console.log('Found last N days pattern, returning LAST_' + match[1] + '_DAYS');
            return `LAST_${match[1]}_DAYS`;
        }
    }
    
    if (lowerQuestion.includes('this month')) {
        console.log('Found this month, returning CURRENT_MONTH');
        return 'CURRENT_MONTH';
    }
    
    if (lowerQuestion.includes('this year')) {
        console.log('Found this year, returning CURRENT_YEAR');
        return 'CURRENT_YEAR';
    }
    
    // Default to null if no time period found
    console.log('No time period pattern found, returning null');
    return null;
}

// Show/hide loading state
function showLoading(show) {
    console.log('Setting loading state to:', show);
    const loading = document.getElementById('loading');
    const submitBtn = document.getElementById('submitBtn');
    
    if (loading && submitBtn) {
        if (show) {
            loading.style.display = 'block';
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating...';
            console.log('Loading state enabled');
        } else {
            loading.style.display = 'none';
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-lightbulb"></i> Generate Insight';
            console.log('Loading state disabled');
        }
    } else {
        console.warn('Loading or submit button elements not found');
    }
}

// Load existing insights
async function loadExistingInsights() {
    console.log('📊 Loading existing insights...');
    const token = localStorage.getItem('authToken');
    if (!token) {
        console.log('❌ No token, skipping insight loading');
        return;
    }
    
    console.log('✅ Token found, proceeding to load insights');
    
    try {
        console.log('🌐 Calling insights API...');
        const response = await fetch(`${CONFIG.getBackendUrl()}/api/insights`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        console.log('📡 API response status:', response.status);
        
        if (response.ok) {
            currentInsights = await response.json();
            console.log('✅ Insights loaded:', currentInsights.length);
            console.log('Insights data:', currentInsights);
            displayInsights();
        } else {
            console.error('❌ Failed to load insights:', response.status);
        }
        
    } catch (error) {
        console.error('❌ Error loading insights:', error);
        console.error('Error stack:', error.stack);
    }
}

// Display insights in the UI
function displayInsights() {
    console.log('Displaying insights, count:', currentInsights.length);
    const container = document.getElementById('insightsContainer');
    if (!container) {
        console.warn('Insights container not found');
        return;
    }
    
    if (currentInsights.length === 0) {
        console.log('No insights to display, showing empty state');
        container.innerHTML = `
            <div class="no-insights">
                <i class="fas fa-lightbulb"></i>
                <h3>No insights yet</h3>
                <p>Ask your first question above to get started with AI-powered financial insights!</p>
            </div>
        `;
        return;
    }
    
    const insightsHTML = currentInsights.map(insight => createInsightCard(insight)).join('');
    console.log('Generated insights HTML, length:', insightsHTML.length);
    container.innerHTML = `
        <h2>Your Insights (${currentInsights.length})</h2>
        <div class="insights-grid">
            ${insightsHTML}
        </div>
    `;
    console.log('Insights displayed successfully');
}

// Create insight card HTML
function createInsightCard(insight) {
            console.log('Creating insight card');
    const date = new Date(insight.generatedAt).toLocaleDateString();
    const time = new Date(insight.generatedAt).toLocaleTimeString();
    const confidencePercent = Math.round((insight.confidenceScore || 0) * 100);
    
    return `
        <div class="insight-card" data-insight-id="${insight.id}">
            <div class="insight-header-card">
                <span class="insight-type">${insight.type || 'INSIGHT'}</span>
                <span class="insight-date">${date} at ${time}</span>
            </div>
            
            <div class="insight-question">
                "${insight.userQuestion || 'Question not available'}"
            </div>
            
            <div class="insight-message">
                ${insight.message || 'No insight message available'}
            </div>
            
            <div class="insight-meta">
                <div class="confidence-score">
                    <span>Confidence:</span>
                    <div class="confidence-bar">
                        <div class="confidence-fill" style="width: ${confidencePercent}%"></div>
                    </div>
                    <span>${confidencePercent}%</span>
                </div>
                
                <div class="insight-actions">
                    <button class="action-btn" onclick="markAsViewed('${insight.id}')" ${insight.viewed ? 'disabled' : ''}>
                        ${insight.viewed ? 'Viewed' : 'Mark Viewed'}
                    </button>
                    <button class="action-btn delete" onclick="deleteInsight('${insight.id}')">
                        Delete
                    </button>
                </div>
            </div>
        </div>
    `;
}

// Mark insight as viewed
async function markAsViewed(insightId) {
            console.log('Marking insight as viewed');
    const token = localStorage.getItem('authToken');
    if (!token) {
        console.warn('No auth token found for marking insight as viewed');
        return;
    }
    
    try {
        const response = await fetch(`${CONFIG.getBackendUrl()}/api/insights/${insightId}/view`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            console.log('Successfully marked insight as viewed');
            // Update local state
            const insight = currentInsights.find(i => i.id === insightId);
            if (insight) {
                insight.viewed = true;
                displayInsights();
            }
        } else {
            console.error('Failed to mark insight as viewed, status:', response.status);
        }
        
    } catch (error) {
        console.error('Error marking insight as viewed:', error);
    }
}

// Delete insight
async function deleteInsight(insightId) {
            console.log('Deleting insight');
    if (!confirm('Are you sure you want to delete this insight?')) {
        console.log('User cancelled insight deletion');
        return;
    }
    
    const token = localStorage.getItem('authToken');
    if (!token) {
        console.warn('No auth token found for deleting insight');
        return;
    }
    
    try {
        const response = await fetch(`${CONFIG.getBackendUrl()}/api/insights/${insightId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            console.log('Successfully deleted insight');
            // Remove from local state
            currentInsights = currentInsights.filter(i => i.id !== insightId);
            displayInsights();
            showMessage('Insight deleted successfully', 'success');
        } else {
            console.error('Failed to delete insight, status:', response.status);
        }
        
    } catch (error) {
        console.error('Error deleting insight:', error);
        showMessage('Error deleting insight', 'error');
    }
}

// Fill question with example
function fillQuestion(question) {
    console.log('Filling question with example:', question);
    const questionField = document.getElementById('question');
    if (questionField) {
        questionField.value = question;
        questionField.focus();
        console.log('Question field filled and focused');
    } else {
        console.warn('Question field not found');
    }
}

// Show message to user
function showMessage(message, type = 'info') {
    console.log('Showing message:', message, 'Type:', type);
    // Create message element
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;
    messageDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        color: white;
        font-weight: 500;
        z-index: 1000;
        max-width: 300px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    
    // Set background color based on type
    switch (type) {
        case 'success':
            messageDiv.style.background = '#48bb78';
            break;
        case 'error':
            messageDiv.style.background = '#f56565';
            break;
        case 'warning':
            messageDiv.style.background = '#ed8936';
            break;
        default:
            messageDiv.style.background = '#4299e1';
    }
    
    // Add to page
    document.body.appendChild(messageDiv);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (messageDiv.parentNode) {
            messageDiv.parentNode.removeChild(messageDiv);
            console.log('Message removed:', message);
        }
    }, 5000);
}

// Logout function
function logout() {
    console.log('Logging out user');
    localStorage.removeItem('authToken');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
    console.log('Cleared local storage, redirecting to login');
    window.location.href = 'index.html';
} 