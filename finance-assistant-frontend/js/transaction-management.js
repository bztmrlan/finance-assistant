// Transaction Management JavaScript
let currentTransactions = [];
let allCategories = [];
let currentPage = 1;
const pageSize = 20;
let filteredTransactions = [];

// Initialize the page
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== TRANSACTION MANAGEMENT PAGE LOADED ===');
    

    
    console.log('CONFIG object available:', typeof CONFIG !== 'undefined');
    if (typeof CONFIG !== 'undefined') {
        console.log('Backend URL:', CONFIG.getBackendUrl());
        console.log('Endpoints available:', Object.keys(CONFIG.ENDPOINTS));
    }
    
    try {
        checkAuth();
        setupEventListeners();
    } catch (error) {
        console.error('Error during page initialization:', error);
        showMessage('Error initializing page: ' + error.message, 'error');
    }
});

// Check authentication status
function checkAuth() {
    console.log('=== AUTHENTICATION CHECK STARTED ===');
    
    const token = localStorage.getItem('authToken');
    const userEmail = localStorage.getItem('userEmail');
    
    console.log('Token exists:', !!token);
            console.log('Token exists:', !!token);
            console.log('User email exists:', !!userEmail);
    console.log('Current URL:', window.location.href);
    console.log('CONFIG available:', typeof CONFIG !== 'undefined');
    
    if (!token) {
        console.log('No token found, showing error message');
        showMessage('Please log in to view transactions', 'error');
        
        // Add a button to go to login instead of auto-redirecting
        const messageContainer = document.getElementById('messageContainer');
        if (messageContainer) {
            const loginBtn = document.createElement('button');
            loginBtn.textContent = 'Go to Login';
            loginBtn.className = 'btn btn-primary';
            loginBtn.onclick = () => window.location.href = 'index.html';
            messageContainer.appendChild(loginBtn);
        }
        
        // Don't auto-redirect, let user choose
        return;
    }

    // Check if token is expired
    if (isTokenExpired(token)) {
        console.log('Token is expired, showing warning but allowing retry');
        showMessage('Your session has expired. Please log in again or try refreshing.', 'warning');
        // Don't logout immediately, let user try to refresh or retry
        displayNoTransactionsMessage('Session expired. Please log in again.');
        return;
    }

    console.log('Token found and not expired, proceeding to verification');
    console.log('About to call verifyToken...');
    

    
    try {
        // Verify token with backend
        verifyToken(token);
    } catch (error) {
        console.error('Error in verifyToken call:', error);
        showMessage('Error during authentication verification: ' + error.message, 'error');
    }
}

async function verifyToken(token) {
    try {
        console.log('Verifying token...');
        console.log('Token to verify:', !!token);
        
        // Try to verify token with backend
        const response = await fetch(`${CONFIG.getBackendUrl()}/auth/verify`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            console.log('Token verified successfully with backend');
            // Load data only after successful authentication, passing the verified token
            await loadCategories(token);
            await loadTransactions(token);
        } else if (response.status === 404) {
            // If verify endpoint doesn't exist, try to load data directly
            console.log('Verify endpoint not found, trying to load data directly');
            await loadCategories(token);
            await loadTransactions(token);
        } else {
            console.log('Token verification failed, showing warning but allowing retry');
            showMessage('Authentication failed. Please check your login status.', 'warning');
            // Don't logout immediately, let user try to refresh or retry
            displayNoTransactionsMessage('Authentication failed. Please log in again.');
        }
    } catch (error) {
        console.error('Token verification failed:', error);
        console.log('Network error during verification, trying to load data directly');
        // If we can't reach the backend, try to load data anyway
        try {
            await loadCategories(token);
            await loadTransactions(token);
        } catch (loadError) {
            console.error('Failed to load data:', loadError);
            showMessage('Unable to connect to backend. Please check your connection.', 'error');
            displayNoTransactionsMessage('Backend connection failed. Please try again later.');
            
            // Offer to create sample data for testing
            const createSampleBtn = document.createElement('button');
            createSampleBtn.textContent = 'Create Sample Data for Testing';
            createSampleBtn.className = 'btn btn-primary';
            createSampleBtn.onclick = createSampleTransactions;
            
            const messageContainer = document.getElementById('messageContainer');
            if (messageContainer) {
                messageContainer.appendChild(createSampleBtn);
            }
        }
    }
}

function setupEventListeners() {
    // Add event listeners for filters
    document.getElementById('searchDescription').addEventListener('input', debounce(applyFilters, 300));
    document.getElementById('categoryFilter').addEventListener('change', applyFilters);
    document.getElementById('typeFilter').addEventListener('change', applyFilters);
    document.getElementById('dateFrom').addEventListener('change', applyFilters);
    document.getElementById('dateTo').addEventListener('change', applyFilters);
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

async function loadCategories(token) {
    try {
        console.log('=== LOADING CATEGORIES ===');
        console.log('Using token for categories:', !!token);
        
        if (!token) {
            console.error('No auth token provided');
            return;
        }

        const response = await fetch(`${CONFIG.getBackendUrl()}/api/categories`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            allCategories = await response.json();
            console.log('Categories loaded successfully:', allCategories.length, 'categories');
            populateCategoryFilter();
        } else if (response.status === 401) {
            console.error('Authentication failed (401) - token may be expired');
            showMessage('Session expired. Please log in again.', 'error');
            // Clear expired token and redirect to login
            localStorage.removeItem('authToken');
            localStorage.removeItem('userEmail');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 2000);
        } else {
            console.error('Failed to load categories:', response.statusText);
        }
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

// Create sample transactions for demonstration
function createSampleTransactions() {
    // Create some sample categories if none exist
    if (allCategories.length === 0) {
        allCategories = [
            { id: 'cat-1', name: 'Groceries', type: 'EXPENSE' },
            { id: 'cat-2', name: 'Transportation', type: 'EXPENSE' },
            { id: 'cat-3', name: 'Salary', type: 'INCOME' },
            { id: 'cat-4', name: 'Freelance', type: 'INCOME' },
            { id: 'cat-5', name: 'Entertainment', type: 'EXPENSE' },
            { id: 'cat-6', name: 'Utilities', type: 'EXPENSE' }
        ];
    }
    
    const sampleTransactions = [
        {
            id: 'sample-1',
            date: '2025-08-13',
            description: 'Grocery Shopping at Walmart',
            amount: -85.50,
            currency: 'USD',
            type: 'EXPENSE',
            categoryId: allCategories.find(cat => cat.name === 'Groceries')?.id || null,
            categoryName: allCategories.find(cat => cat.name === 'Groceries')?.name || null,
            categoryType: allCategories.find(cat => cat.name === 'Groceries')?.type || null
        },
        {
            id: 'sample-2',
            date: '2025-08-12',
            description: 'Monthly Salary Deposit',
            amount: 2500.00,
            currency: 'USD',
            type: 'INCOME',
            categoryId: allCategories.find(cat => cat.name === 'Salary')?.id || null,
            categoryName: allCategories.find(cat => cat.name === 'Salary')?.name || null,
            categoryType: allCategories.find(cat => cat.name === 'Salary')?.type || null
        },
        {
            id: 'sample-3',
            date: '2025-08-11',
            description: 'Gas Station - Shell',
            amount: -45.00,
            currency: 'USD',
            type: 'EXPENSE',
            categoryId: allCategories.find(cat => cat.name === 'Transportation')?.id || null,
            categoryName: allCategories.find(cat => cat.name === 'Transportation')?.name || null,
            categoryType: allCategories.find(cat => cat.name === 'Transportation')?.type || null
        },
        {
            id: 'sample-4',
            date: '2025-08-10',
            description: 'Freelance Web Design Project',
            amount: 500.00,
            currency: 'USD',
            type: 'INCOME',
            categoryId: allCategories.find(cat => cat.name === 'Freelance')?.id || null,
            categoryName: allCategories.find(cat => cat.name === 'Freelance')?.name || null,
            categoryType: allCategories.find(cat => cat.name === 'Freelance')?.type || null
        },
        {
            id: 'sample-5',
            date: '2025-08-09',
            description: 'Netflix Subscription',
            amount: -15.99,
            currency: 'USD',
            type: 'EXPENSE',
            categoryId: allCategories.find(cat => cat.name === 'Entertainment')?.id || null,
            categoryName: allCategories.find(cat => cat.name === 'Entertainment')?.name || null,
            categoryType: allCategories.find(cat => cat.name === 'Entertainment')?.type || null
        },
        {
            id: 'sample-6',
            date: '2025-08-08',
            description: 'Electricity Bill',
            amount: -120.00,
            currency: 'USD',
            type: 'EXPENSE',
            categoryId: allCategories.find(cat => cat.name === 'Utilities')?.id || null,
            categoryName: allCategories.find(cat => cat.name === 'Utilities')?.name || null,
            categoryType: allCategories.find(cat => cat.name === 'Utilities')?.type || null
        }
    ];
    
    currentTransactions = sampleTransactions;
    filteredTransactions = [...sampleTransactions];
    
    showMessage('Sample transactions created successfully! You can now edit categories.', 'success');
    updateStatistics();
    displayTransactions();
    updatePagination();
    
    // Show/hide bulk update button based on filtered results
    const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
    if (bulkUpdateBtn) {
        if (filteredTransactions.length > 1) {
            bulkUpdateBtn.style.display = 'inline-block';
        } else {
            bulkUpdateBtn.style.display = 'none';
        }
    }
}



// Retry loading data
function retryLoadData() {
    const token = localStorage.getItem('authToken');
    if (token && !isTokenExpired(token)) {
        showMessage('Retrying to load data...', 'info');
        loadCategories(token);
        loadTransactions(token);
    } else {
        showMessage('Please log in again to access transactions', 'error');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2000);
    }
}

// Logout function
function logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userEmail');
    window.location.href = 'index.html';
}

// Check if current token is expired
function isTokenExpired(token) {
    try {
        // Decode JWT payload (second part)
        const payload = token.split('.')[1];
        const decodedPayload = JSON.parse(atob(payload));
        
        // Check if token is expired
        const currentTime = Math.floor(Date.now() / 1000);
        const expirationTime = decodedPayload.exp;
        
        console.log('Token expiration check:', {
            currentTime,
            expirationTime,
            isExpired: currentTime >= expirationTime,
            timeUntilExpiry: expirationTime - currentTime
        });
        
        return currentTime >= expirationTime;
    } catch (error) {
        console.error('Error checking token expiration:', error);
        return true; // Assume expired if we can't decode
    }
}

// Show message to user
function showMessage(message, type = 'info') {
    const messageContainer = document.getElementById('messageContainer');
    if (messageContainer) {
        messageContainer.textContent = message;
        messageContainer.className = `message ${type}`;
        messageContainer.style.display = 'block';
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            messageContainer.style.display = 'none';
        }, 5000);
    } else {
        // Fallback to console if no message container
        console.log(`${type.toUpperCase()}: ${message}`);
    }
}

function populateCategoryFilter() {
    const categoryFilter = document.getElementById('categoryFilter');
    categoryFilter.innerHTML = '<option value="">All Categories</option>';
    
    allCategories.forEach(category => {
        const option = document.createElement('option');
        option.value = category.id;
        option.textContent = category.name;
        categoryFilter.appendChild(option);
    });
    
    // Also populate type filter if it exists
    const typeFilter = document.getElementById('typeFilter');
    if (typeFilter) {
        typeFilter.innerHTML = '<option value="">All Types</option>';
        
        // Get unique types from categories
        const uniqueTypes = [...new Set(allCategories.map(cat => cat.type))];
        uniqueTypes.forEach(type => {
            if (type) {
                const option = document.createElement('option');
                option.value = type;
                option.textContent = type;
                typeFilter.appendChild(option);
            }
        });
    }
}

async function loadTransactions(token) {
    try {
        console.log('=== LOADING TRANSACTIONS ===');
        console.log('Using token for transactions:', !!token);
        
        if (!token) {
            console.error('No auth token provided');
            return;
        }

        console.log('Making request to:', `${CONFIG.getBackendUrl()}/api/transactions`);
        const response = await fetch(`${CONFIG.getBackendUrl()}/api/transactions`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            currentTransactions = await response.json();
            filteredTransactions = [...currentTransactions];
            console.log('Transactions loaded successfully:', currentTransactions.length, 'transactions');
            updateStatistics();
            displayTransactions();
            updatePagination();
            
            // Show/hide bulk update button based on filtered results
            const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
            if (bulkUpdateBtn) {
                if (filteredTransactions.length > 1) {
                    bulkUpdateBtn.style.display = 'inline-block';
                } else {
                    bulkUpdateBtn.style.display = 'none';
                }
            }
        } else if (response.status === 401) {
            console.error('Authentication failed (401) - token may be expired');
            showMessage('Authentication failed. Please check your login status.', 'error');
            // Don't logout immediately, just show error
            displayNoTransactionsMessage('Authentication required to view transactions');
        } else if (response.status === 503 || response.status === 500) {
            console.error('Backend service unavailable:', response.statusText);
            showMessage('Backend service is currently unavailable. You can work with sample data.', 'warning');
            displayNoTransactionsMessage('Backend service unavailable. Use sample data to test functionality.');
        } else if (response.status === 404) {
            console.error('Transactions endpoint not found:', response.statusText);
            showMessage('Transactions endpoint not found. You can work with sample data.', 'warning');
            displayNoTransactionsMessage('Transactions endpoint not found. Use sample data to test functionality.');
        } else {
            console.error('Failed to load transactions:', response.statusText);
            showMessage('Failed to load transactions', 'error');
            displayNoTransactionsMessage('Unable to load transactions');
        }
    } catch (error) {
        console.error('Error loading transactions:', error);
        showMessage('Error loading transactions', 'error');
    }
}

function updateStatistics() {
    const totalTransactions = currentTransactions.length;
    const totalIncome = currentTransactions
        .filter(t => t.amount > 0)
        .reduce((sum, t) => sum + Math.abs(t.amount), 0);
    const totalExpenses = currentTransactions
        .filter(t => t.amount < 0)
        .reduce((sum, t) => sum + Math.abs(t.amount), 0);
    const netAmount = currentTransactions.reduce((sum, t) => sum + t.amount, 0);

    document.getElementById('totalTransactions').textContent = totalTransactions;
    document.getElementById('totalIncome').textContent = formatCurrency(totalIncome);
    document.getElementById('totalIncome').className = 'stat-value amount-positive';
    document.getElementById('totalExpenses').textContent = formatCurrency(totalExpenses);
    document.getElementById('totalExpenses').className = 'stat-value amount-negative';
    document.getElementById('netAmount').textContent = formatCurrency(netAmount);
    document.getElementById('netAmount').className = netAmount >= 0 ? 'stat-value amount-positive' : 'stat-value amount-negative';
}

function displayTransactions() {
    const container = document.getElementById('transactionsContainer');
    
    if (filteredTransactions.length === 0) {
        displayNoTransactionsMessage('No transactions found');
        return;
    }

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const pageTransactions = filteredTransactions.slice(startIndex, endIndex);

    const table = `
        <table class="transaction-table">
            <thead>
                <tr>
                    <th>Date</th>
                    <th>Description</th>
                    <th>Amount</th>
                    <th>Category</th>
                    <th>Type</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${pageTransactions.map(transaction => createTransactionRow(transaction)).join('')}
            </tbody>
        </table>
    `;

    container.innerHTML = table;
}

function displayNoTransactionsMessage(message) {
    const container = document.getElementById('transactionsContainer');
    
    if (message === 'No transactions found') {
        container.innerHTML = `
            <div class="no-transactions">
                <h3>No transactions found</h3>
                <p>You don't have any transactions yet. You can:</p>
                <ul>
                    <li>Upload transactions using the Upload page</li>
                    <li>Create transactions manually</li>
                    <li>Import from your bank statements</li>
                </ul>
                <button onclick="createSampleTransactions()" class="btn btn-primary">Create Sample Data</button>
                <button onclick="retryLoadData()" class="btn btn-secondary">Refresh</button>
            </div>
        `;
    } else {
        container.innerHTML = `
            <div class="no-transactions">
                <h3>${message}</h3>
                <p>Please check your authentication status or try refreshing the page.</p>
                <button onclick="retryLoadData()" class="btn btn-primary">Retry</button>
            </div>
        `;
    }
}

function createTransactionRow(transaction) {
    // Check if categories are loaded
    if (!allCategories || allCategories.length === 0) {
        console.error('No categories loaded! Cannot create transaction row properly.');
        return `
            <tr data-transaction-id="${transaction.id}">
                <td>${formatDate(transaction.date)}</td>
                <td>${escapeHtml(transaction.description || '')}</td>
                <td class="${transaction.amount >= 0 ? 'amount-positive' : 'amount-negative'}">
                    ${formatCurrency(transaction.amount)}
                </td>
                <td>
                    <div class="category-cell">
                        <select class="category-select" disabled>
                            <option value="">Categories not loaded</option>
                        </select>
                    </div>
                </td>
                <td>${transaction.type || 'Unknown'}</td>
                <td>
                    <button class="save-btn" disabled>Save</button>
                </td>
            </tr>
        `;
    }
    
    // Determine transaction type based on amount if not explicitly set
    let transactionType = transaction.type;
    if (!transactionType && transaction.amount !== undefined) {
        transactionType = transaction.amount >= 0 ? 'INCOME' : 'EXPENSE';
    }
    
    // Also check if we can get type from categoryType
    if (!transactionType && transaction.categoryType) {
        transactionType = transaction.categoryType;
    }
    
    // Get all categories for this transaction type, or all categories if type is not specified
    const relevantCategories = transactionType ? 
        allCategories.filter(cat => cat.type === transactionType) : 
        allCategories;
    
    // If no categories are available, show a message
    let categoryOptions;
    if (relevantCategories.length === 0) {
        // Fallback to all categories if type filtering fails
        categoryOptions = '<option value="">Uncategorized</option>';
        categoryOptions += allCategories
            .map(cat => {
                const isSelected = cat.id === transaction.categoryId;
                return `<option value="${cat.id}" ${isSelected ? 'selected' : ''}>${cat.name}</option>`;
            })
            .join('');
    } else {
        // Add "Uncategorized" option first
        categoryOptions = '<option value="">Uncategorized</option>';
        
        // Add relevant categories
        categoryOptions += relevantCategories
            .map(cat => {
                const isSelected = cat.id === transaction.categoryId;
                return `<option value="${cat.id}" ${isSelected ? 'selected' : ''}>${cat.name}</option>`;
            })
            .join('');
    }

    // Get current category info from backend data
    const currentCategoryId = transaction.categoryId;
    const currentCategoryName = transaction.categoryName || 'Uncategorized';
    const currentCategoryType = transaction.categoryType || '';

    return `
        <tr data-transaction-id="${transaction.id}">
            <td>${formatDate(transaction.date)}</td>
            <td>${escapeHtml(transaction.description || '')}</td>
            <td class="${transaction.amount >= 0 ? 'amount-positive' : 'amount-negative'}">
                ${formatCurrency(transaction.amount)}
            </td>
            <td>
                <div class="category-cell">
                    <select class="category-select" onchange="updateTransactionCategory('${transaction.id}', this.value)" 
                            data-original-category="${currentCategoryId || ''}">
                        ${categoryOptions}
                    </select>
                    ${currentCategoryType ? `<span class="category-type-badge ${currentCategoryType.toLowerCase()}">${currentCategoryType}</span>` : ''}
                </div>
            </td>
            <td>${transactionType || 'Unknown'}</td>
            <td>
                <button class="save-btn" onclick="saveTransactionChanges('${transaction.id}')" id="save-${transaction.id}" disabled>
                    <i class="fas fa-save"></i> Save
                </button>
            </td>
        </tr>
    `;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString();
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

function updateTransactionCategory(transactionId, categoryId) {
    const row = document.querySelector(`tr[data-transaction-id="${transactionId}"]`);
    const categorySelect = row.querySelector('.category-select');
    const originalCategory = categorySelect.getAttribute('data-original-category');
    
    // Check if the category has actually changed
    if (categoryId === originalCategory) {
        // No change, disable save button and reset styling
        const saveBtn = document.getElementById(`save-${transactionId}`);
        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.innerHTML = '<i class="fas fa-save"></i> Save';
            saveBtn.style.background = '';
            saveBtn.style.color = '';
        }
        
        // Reset row background
        row.style.backgroundColor = '';
        return;
    }
    
    // Enable the save button for this transaction
    const saveBtn = document.getElementById(`save-${transactionId}`);
    if (saveBtn) {
        saveBtn.disabled = false;
        saveBtn.innerHTML = '<i class="fas fa-save"></i> Save Changes';
        saveBtn.style.background = '#ffc107';
        saveBtn.style.color = '#212529';
    }
    
    // Show a small indicator that changes are pending
    if (row) {
        row.style.backgroundColor = '#fff3cd';
    }
    
    // Update the category type badge if category is selected
    const categoryCell = row.querySelector('.category-cell');
    if (categoryId) {
        const selectedCategory = allCategories.find(cat => cat.id === categoryId);
        if (selectedCategory) {
            // Remove existing badge if any
            const existingBadge = categoryCell.querySelector('.category-type-badge');
            if (existingBadge) {
                existingBadge.remove();
            }
            
            // Add new badge
            const badge = document.createElement('span');
            badge.className = `category-type-badge ${selectedCategory.type.toLowerCase()}`;
            badge.textContent = selectedCategory.type;
            categoryCell.appendChild(badge);
        }
    } else {
        // Remove badge if uncategorized
        const existingBadge = categoryCell.querySelector('.category-type-badge');
        if (existingBadge) {
            existingBadge.remove();
        }
    }
}

async function saveTransactionChanges(transactionId) {
    try {
        const token = localStorage.getItem('authToken');
        if (!token) {
            showMessage('No auth token found', 'error');
            return;
        }

        const row = document.querySelector(`tr[data-transaction-id="${transactionId}"]`);
        const categorySelect = row.querySelector('.category-select');
        const newCategoryId = categorySelect.value;

        // Allow setting to uncategorized (empty string)
        // if (!newCategoryId) {
        //     showMessage('Please select a category', 'error');
        //     return;
        // }

        const response = await fetch(`${CONFIG.getBackendUrl()}/api/transactions/${transactionId}/category`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                categoryId: newCategoryId
            })
        });

        if (response.ok) {
            // Update the transaction in our local data
            const transaction = currentTransactions.find(t => t.id === transactionId);
            if (transaction) {
                transaction.categoryId = newCategoryId;
                if (newCategoryId) {
                    const selectedCategory = allCategories.find(cat => cat.id === newCategoryId);
                    if (selectedCategory) {
                        transaction.categoryName = selectedCategory.name;
                        transaction.categoryType = selectedCategory.type;
                    }
                } else {
                    transaction.categoryName = null;
                    transaction.categoryType = null;
                }
            }
            
            // Update the transaction in filtered data
            const filteredTransaction = filteredTransactions.find(t => t.id === transactionId);
            if (filteredTransaction) {
                filteredTransaction.categoryId = newCategoryId;
                if (newCategoryId) {
                    const selectedCategory = allCategories.find(cat => cat.id === newCategoryId);
                    if (selectedCategory) {
                        filteredTransaction.categoryName = selectedCategory.name;
                        filteredTransaction.categoryType = selectedCategory.type;
                    }
                } else {
                    filteredTransaction.categoryName = null;
                    filteredTransaction.categoryType = null;
                }
            }

            // Reset the save button and row styling
            const saveBtn = document.getElementById(`save-${transactionId}`);
            if (saveBtn) {
                saveBtn.disabled = true;
                saveBtn.textContent = 'Save';
                saveBtn.style.background = '';
                saveBtn.style.color = '';
            }
            
            // Reset row background
            const row = document.querySelector(`tr[data-transaction-id="${transactionId}"]`);
            if (row) {
                row.style.backgroundColor = '';
            }

            showMessage('Category updated successfully', 'success');
            
            // Refresh the display to show the new category name
            displayTransactions();
        } else {
            const errorData = await response.json();
            showMessage(errorData.message || 'Failed to update category', 'error');
        }
    } catch (error) {
        console.error('Error updating transaction category:', error);
        showMessage('Error updating category', 'error');
    }
}

function applyFilters() {
    const dateFrom = document.getElementById('dateFrom').value;
    const dateTo = document.getElementById('dateTo').value;
    const categoryFilter = document.getElementById('categoryFilter').value;
    const typeFilter = document.getElementById('typeFilter').value;
    const searchDescription = document.getElementById('searchDescription').value.toLowerCase();

    filteredTransactions = currentTransactions.filter(transaction => {
        // Date filter
        if (dateFrom && new Date(transaction.date) < new Date(dateFrom)) {
            return false;
        }
        if (dateTo && new Date(transaction.date) > new Date(dateTo)) {
            return false;
        }
        
        // Category filter - check both categoryId and categoryName
        if (categoryFilter) {
            const hasMatchingCategory = transaction.categoryId === categoryFilter || 
                                      transaction.categoryName === categoryFilter;
            if (!hasMatchingCategory) {
                return false;
            }
        }
        
        // Type filter - check both type and categoryType
        if (typeFilter) {
            const hasMatchingType = transaction.type === typeFilter || 
                                   transaction.categoryType === typeFilter;
            if (!hasMatchingType) {
                return false;
            }
        }
        
        // Description search
        if (searchDescription && (!transaction.description || !transaction.description.toLowerCase().includes(searchDescription))) {
            return false;
        }
        
        return true;
    });

    currentPage = 1;
    displayTransactions();
    updatePagination();
    updateStatistics();
    
    // Show/hide bulk update button based on filtered results
    const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
    if (bulkUpdateBtn) {
        if (filteredTransactions.length > 1) {
            bulkUpdateBtn.style.display = 'inline-block';
        } else {
            bulkUpdateBtn.style.display = 'none';
        }
    }
}

function clearFilters() {
    document.getElementById('dateFrom').value = '';
    document.getElementById('dateTo').value = '';
    document.getElementById('categoryFilter').value = '';
    document.getElementById('typeFilter').value = '';
    document.getElementById('searchDescription').value = '';
    
    filteredTransactions = [...currentTransactions];
    currentPage = 1;
    displayTransactions();
    updatePagination();
    updateStatistics();
    
    // Show/hide bulk update button based on filtered results
    const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
    if (bulkUpdateBtn) {
        if (filteredTransactions.length > 1) {
            bulkUpdateBtn.style.display = 'inline-block';
        } else {
            bulkUpdateBtn.style.display = 'none';
        }
    }
}

function updatePagination() {
    const totalPages = Math.ceil(filteredTransactions.length / pageSize);
    const pagination = document.getElementById('pagination');
    
    if (totalPages <= 1) {
        pagination.style.display = 'none';
        return;
    }
    
    pagination.style.display = 'flex';
    
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const pageInfo = document.getElementById('pageInfo');
    
    prevBtn.disabled = currentPage === 1;
    nextBtn.disabled = currentPage === totalPages;
    
    pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
}

function changePage(direction) {
    const totalPages = Math.ceil(filteredTransactions.length / pageSize);
    
    if (direction === 'prev' && currentPage > 1) {
        currentPage--;
    } else if (direction === 'next' && currentPage < totalPages) {
        currentPage++;
    }
    
    displayTransactions();
    updatePagination();
}

function showSuccess(message) {
    // Create a simple success notification
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #28a745;
        color: white;
        padding: 15px 20px;
        border-radius: 4px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        z-index: 1000;
        font-weight: 500;
    `;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

function showError(message) {
    // Create a simple error notification
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #dc3545;
        color: white;
        padding: 15px 20px;
        border-radius: 4px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        z-index: 1000;
        font-weight: 500;
    `;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 5000);
}

// Bulk update categories for filtered transactions
function bulkUpdateCategories() {
    const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
    
    if (bulkUpdateBtn.textContent.includes('Cancel')) {
        // Cancel bulk update mode
        bulkUpdateBtn.innerHTML = '<i class="fas fa-edit"></i> Bulk Update Categories';
        bulkUpdateBtn.className = 'btn btn-success';
        
        // Remove bulk update UI
        const existingBulkUI = document.querySelector('.bulk-update-ui');
        if (existingBulkUI) {
            existingBulkUI.remove();
        }
        
        // Reset all category selects to original values
        document.querySelectorAll('.category-select').forEach(select => {
            const originalCategory = select.getAttribute('data-original-category');
            select.value = originalCategory || '';
            updateTransactionCategory(select.closest('tr').getAttribute('data-transaction-id'), originalCategory || '');
        });
        
        return;
    }
    
    // Enter bulk update mode
    bulkUpdateBtn.innerHTML = '<i class="fas fa-times"></i> Cancel';
    bulkUpdateBtn.className = 'btn btn-secondary';
    
    // Create bulk update UI
    const bulkUpdateUI = document.createElement('div');
    bulkUpdateUI.className = 'bulk-update-ui';
    bulkUpdateUI.innerHTML = `
        <div class="filters" style="margin-top: 20px;">
            <div class="filter-row">
                <div class="filter-group">
                    <label>Bulk Update Category:</label>
                    <select id="bulkCategorySelect" class="category-select">
                        <option value="">Uncategorized</option>
                        ${allCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('')}
                    </select>
                </div>
                <div class="filter-actions">
                    <button class="btn btn-primary" onclick="applyBulkCategoryUpdate()">
                        <i class="fas fa-save"></i> Apply to ${filteredTransactions.length} Transactions
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Insert after the filters
    const filtersDiv = document.querySelector('.filters');
    filtersDiv.parentNode.insertBefore(bulkUpdateUI, filtersDiv.nextSibling);
}

// Apply bulk category update
async function applyBulkCategoryUpdate() {
    const bulkCategoryId = document.getElementById('bulkCategorySelect').value;
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        showMessage('No auth token found', 'error');
        return;
    }
    
    if (filteredTransactions.length === 0) {
        showMessage('No transactions to update', 'error');
        return;
    }
    
    // Show loading state
    const applyBtn = document.querySelector('.bulk-update-ui .btn-primary');
    const originalText = applyBtn.innerHTML;
    applyBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Updating...';
    applyBtn.disabled = true;
    
    try {
        let successCount = 0;
        let errorCount = 0;
        
        // Update each transaction
        for (const transaction of filteredTransactions) {
            try {
                const response = await fetch(`${CONFIG.getBackendUrl()}/api/transactions/${transaction.id}/category`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        categoryId: bulkCategoryId
                    })
                });
                
                if (response.ok) {
                    // Update local data
                    transaction.category = allCategories.find(cat => cat.id === bulkCategoryId);
                    successCount++;
                } else {
                    errorCount++;
                }
            } catch (error) {
                console.error(`Error updating transaction ${transaction.id}:`, error);
                errorCount++;
            }
        }
        
        // Show results
        if (errorCount === 0) {
            showMessage(`Successfully updated ${successCount} transactions`, 'success');
        } else {
            showMessage(`Updated ${successCount} transactions, ${errorCount} failed`, 'warning');
        }
        
        // Refresh display
        displayTransactions();
        updateStatistics();
        
        // Exit bulk update mode
        bulkUpdateCategories();
        
    } catch (error) {
        console.error('Error in bulk update:', error);
        showMessage('Error during bulk update', 'error');
    } finally {
        // Reset button
        applyBtn.innerHTML = originalText;
        applyBtn.disabled = false;
    }
}