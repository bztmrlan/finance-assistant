// Budget Management JavaScript
class BudgetManager {
    constructor() {
        this.budgets = [];
        this.categories = [];
        this.init();
    }

    init() {
        console.log('🔧 BudgetManager init() called');
        console.log('🔧 window.CONFIG available:', !!window.CONFIG);
        console.log('🔧 window.apiService available:', !!window.apiService);
        
        // Show loading overlay initially
        this.showLoadingOverlay();
        
        this.setupEventListeners();
        this.checkAuthStatus();
        // Don't load categories immediately - wait for auth verification
        // this.loadCategories();
        // this.loadBudgets();
        
        // Add immediate test for debugging
        setTimeout(() => {
            console.log('=== IMMEDIATE TEST AFTER PAGE LOAD ===');
            console.log('BudgetManager initialized:', !!this);
            console.log('Categories loaded:', this.categories?.length || 0);
            console.log('Categories data:', this.categories);
            console.log('Authentication token exists:', !!localStorage.getItem('authToken'));
            console.log('🔧 CONFIG available after timeout:', !!window.CONFIG);
            console.log('🔧 apiService available after timeout:', !!window.apiService);
            
            // Update status display
            BudgetManager.showStatusInUI();
        }, 2000); // Wait 2 seconds for everything to initialize
    }

    setupEventListeners() {
        // Create budget form
        document.getElementById('createBudgetForm').addEventListener('submit', (e) => this.handleCreateBudget(e));
        
        // Edit budget form
        document.getElementById('editBudgetForm').addEventListener('submit', (e) => this.handleEditBudget(e));
        
        // Set default dates
        this.setDefaultDates();
        
        // Initialize button states
        this.updateCategoryButtonStates();
        
        // Show loading message if needed
        this.showLoadingMessage();
        
        // Start readiness checker
        this.startReadinessChecker();
        
        // Show status in UI for debugging
        setTimeout(() => {
            BudgetManager.showStatusInUI();
        }, 1000);
    }

    setDefaultDates() {
        const today = new Date();
        const startDate = document.getElementById('startDate');
        const endDate = document.getElementById('endDate');
        
        if (startDate && endDate) {
            startDate.value = today.toISOString().split('T')[0];
            
            // Set end date to 1 month from start
            const endDateValue = new Date(today);
            endDateValue.setMonth(endDateValue.getMonth() + 1);
            endDate.value = endDateValue.toISOString().split('T')[0];
        }
    }

    checkAuthStatus() {
        const token = localStorage.getItem('authToken'); // Use the same key as auth.js
        const userEmail = localStorage.getItem('userEmail'); // Use the same key as auth.js
        console.log('Checking auth status, token exists:', !!token);
        console.log('User email:', userEmail);
        
        // TEMPORARY DEBUG: Bypass authentication for testing
        if (window.location.search.includes('debug=true')) {
            console.log('DEBUG MODE: Bypassing authentication check');
            this.loadCategories(); // Load categories without auth
            this.loadBudgets(); // Load budgets without auth
            return;
        }
        
        if (!token) {
            this.showMessage('Please log in to manage budgets', 'error');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 2000);
            return;
        }

        // Verify token with backend
        this.verifyToken(token);
    }

    async verifyToken(token) {
        try {
            console.log('Verifying token...');
            console.log('Token to verify:', token ? token.substring(0, 20) + '...' : 'none');
            console.log('API Service available:', !!window.apiService);
            
            if (!window.apiService) {
                console.error('API Service not available!');
                this.showMessage('API Service not available. Please refresh the page.', 'error');
                return;
            }
            
            const response = await apiService.verifyToken();
            console.log('Token verification response:', response);
            
            if (!response) {
                console.log('Token verification failed, removing token');
                localStorage.removeItem('authToken');
                this.showMessage('Session expired. Please log in again.', 'error');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 2000);
            } else {
                console.log('Token verified successfully');
                await this.loadCategories(); // Load categories only after successful token verification
                await this.loadBudgets(); // Load budgets only after successful token verification
            }
        } catch (error) {
            console.error('Token verification failed:', error);
            console.error('Error details:', {
                message: error.message,
                stack: error.stack,
                name: error.name
            });
            this.showMessage('Unable to verify authentication. Please try again.', 'error');
        }
    }

    // Verify categories are properly loaded
    verifyCategoriesLoaded() {
        console.log('=== VERIFYING CATEGORIES LOADED ===');
        console.log('Categories array exists:', !!this.categories);
        console.log('Categories is array:', Array.isArray(this.categories));
        console.log('Categories length:', this.categories?.length || 0);
        
        if (!this.categories || !Array.isArray(this.categories) || this.categories.length === 0) {
            console.error('Categories not properly loaded!');
            return false;
        }
        
        // Check each category has required fields
        let validCategories = 0;
        this.categories.forEach((cat, index) => {
            if (cat && cat.id && cat.name) {
                validCategories++;
                console.log(`✅ Category ${index + 1}: ${cat.name} (${cat.id})`);
            } else {
                console.error(`❌ Invalid category ${index + 1}:`, cat);
            }
        });
        
        console.log(`Valid categories: ${validCategories}/${this.categories.length}`);
        return validCategories === this.categories.length;
    }

    async loadCategories() {
        try {
            console.log('=== LOADING CATEGORIES ===');
            
            // Check authentication first
            const token = localStorage.getItem('authToken'); // Use the same key as auth.js
            if (!token) {
                throw new Error('No authentication token found');
            }
            
            console.log('Making API call to getUserCategories...');
            console.log('API Service available:', !!window.apiService);
            console.log('API Service methods:', Object.getOwnPropertyNames(window.apiService || {}));
            
            const categories = await apiService.getUserCategories();
            console.log('=== API RESPONSE RECEIVED ===');
            console.log('Raw API response:', categories);
            console.log('Response type:', typeof categories);
            console.log('Response is array:', Array.isArray(categories));
            console.log('Response length:', categories?.length || 0);
            
            if (categories && Array.isArray(categories)) {
                this.categories = categories;
                console.log(`Successfully loaded ${categories.length} categories:`, categories);
                
                // Verify we have all categories
                if (categories.length > 0) {
                    console.log('Category details:');
                    categories.forEach((cat, index) => {
                        console.log(`  ${index + 1}. ID: ${cat.id}, Name: ${cat.name}, Type: ${cat.type}`);
                    });
                }
                
                // Store categories in localStorage for debugging
                localStorage.setItem('debug_categories', JSON.stringify(categories));
                console.log('Categories saved to localStorage for debugging');
                
                this.populateCategorySelects();
            } else {
                console.warn('API returned invalid categories data:', categories);
                this.categories = [];
                this.showMessage('Invalid categories data received from server', 'warning');
            }
        } catch (error) {
            console.error('Error loading categories:', error);
            
            // Hide loading overlay on error
            this.hideLoadingOverlay();
            
            // Update status display to show error
            BudgetManager.showStatusInUI();
            
            // Check if it's an authentication error
            if (error.message && (error.message.includes('401') || error.message.includes('403'))) {
                this.showMessage('Authentication failed. Please log in again.', 'error');
                localStorage.removeItem('authToken');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 2000);
            } else if (error.message && error.message.includes('No authentication token found')) {
                this.showMessage('Please log in to view categories', 'error');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 2000);
            } else {
                this.showMessage(`Failed to load categories: ${error.message}`, 'warning');
            }
        }
    }

    populateCategorySelects() {
        console.log('=== POPULATING CATEGORY SELECTS ===');
        console.log('Categories array:', this.categories);
        console.log('Categories length:', this.categories?.length || 0);
        console.log('Categories type:', Array.isArray(this.categories) ? 'Array' : typeof this.categories);
        console.log('Categories data:', JSON.stringify(this.categories, null, 2));
        
        if (!this.categories || this.categories.length === 0) {
            console.warn('No categories available to populate selects');
            return;
        }
        
        const categorySelects = document.querySelectorAll('.category-select');
        console.log('Found', categorySelects.length, 'category select elements');
        
        if (categorySelects.length === 0) {
            console.warn('No category select elements found in DOM');
            return;
        }
        
        categorySelects.forEach((select, index) => {
            console.log(`\n--- Populating Select ${index} ---`);
            console.log('Select element:', select);
            console.log('Select current options:', select.options.length);
            console.log('Select innerHTML before:', select.innerHTML);
            
            // Clear existing options
            select.innerHTML = '<option value="">Select Category</option>';
            console.log('Cleared select, now has options:', select.options.length);
            console.log('Select innerHTML after clear:', select.innerHTML);
            
            // Add category options
            let addedCount = 0;
            let skippedCount = 0;
            
            this.categories.forEach((category, catIndex) => {
                console.log(`Processing category ${catIndex}:`, category);
                
                if (category && category.id && category.name) {
                    const option = document.createElement('option');
                    option.value = category.id;
                    option.textContent = category.name;
                    select.appendChild(option);
                    addedCount++;
                    console.log(`Added option: ${category.name} (${category.id})`);
                } else {
                    console.warn('Invalid category data:', category);
                    skippedCount++;
                }
            });
            
            console.log(`Select ${index} populated with ${addedCount} options (skipped: ${skippedCount})`);
            console.log('Final options count:', select.options.length);
            console.log('Final options text:', Array.from(select.options).map(opt => opt.textContent));
            console.log('Final innerHTML:', select.innerHTML);
            
            // Verify all categories were added
            if (addedCount !== this.categories.length) {
                console.error(`WARNING: Expected ${this.categories.length} categories but only added ${addedCount}!`);
                console.error('Categories that should have been added:', this.categories);
            }
        });
        
        console.log('=== CATEGORY SELECTS POPULATION COMPLETE ===');
        
        // Update button states after categories are loaded
        this.updateCategoryButtonStates();
        
        // Hide loading overlay when categories are loaded
        this.hideLoadingOverlay();
        
        // Update status display
        BudgetManager.showStatusInUI();
    }
    
    // Update the state of category-related buttons based on availability
    updateCategoryButtonStates() {
        const editAddCategoryBtn = document.getElementById('editAddCategoryBtn');
        if (editAddCategoryBtn) {
            if (this.categories && this.categories.length > 0) {
                editAddCategoryBtn.disabled = false;
                editAddCategoryBtn.title = 'Add a new category limit';
            } else {
                editAddCategoryBtn.disabled = true;
                editAddCategoryBtn.title = 'No categories available';
            }
        }
        
        // Also update any loading indicators
        this.updateLoadingIndicators();
    }
    
    // Update loading indicators on the page
    updateLoadingIndicators() {
        const loadingElements = document.querySelectorAll('.loading-indicator');
        const isReady = this.categories && this.categories.length > 0;
        
        loadingElements.forEach(element => {
            if (isReady) {
                element.style.display = 'none';
            } else {
                element.style.display = 'block';
            }
        });
    }
    
    // Show loading message to user
    showLoadingMessage() {
        if (!this.categories || this.categories.length === 0) {
            this.showMessage('Loading categories and budgets...', 'info');
        }
    }
    
    // Show loading overlay
    showLoadingOverlay() {
        // Create loading overlay if it doesn't exist
        let overlay = document.getElementById('loadingOverlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'loadingOverlay';
            overlay.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 9999;
            `;
            
            overlay.innerHTML = `
                <div style="background: white; padding: 20px; border-radius: 8px; text-align: center;">
                    <div class="fa fa-spinner fa-spin" style="font-size: 24px; margin-bottom: 10px;"></div>
                    <div>Loading Budget Manager...</div>
                    <div style="font-size: 12px; color: #666; margin-top: 10px;">Please wait while categories and budgets are loaded</div>
                </div>
            `;
            
            document.body.appendChild(overlay);
        }
        
        overlay.style.display = 'flex';
    }
    
    // Hide loading overlay
    hideLoadingOverlay() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.style.display = 'none';
        }
    }
    
    // Start readiness checker to periodically update UI
    startReadinessChecker() {
        const checkInterval = setInterval(() => {
            if (this.categories && this.categories.length > 0) {
                console.log('Categories loaded, updating UI...');
                this.updateCategoryButtonStates();
                this.hideLoadingOverlay();
                
                // Update status display
                BudgetManager.showStatusInUI();
                
                clearInterval(checkInterval);
            }
        }, 1000); // Check every second
        
        // Stop checking after 30 seconds to avoid infinite checking
        setTimeout(() => {
            clearInterval(checkInterval);
            // Hide loading overlay if still showing after timeout
            this.hideLoadingOverlay();
        }, 30000);
    }

    async loadBudgets() {
        try {
            const budgets = await apiService.getUserBudgets();
            console.log('📊 Raw budgets data received from API:', budgets);
            
            // Debug: Log spent amounts for each budget category
            if (budgets && budgets.length > 0) {
                budgets.forEach((budget, budgetIndex) => {
                    console.log(`🔍 Budget ${budgetIndex + 1}: "${budget.name}"`);
                    console.log(`  💰 Total Budget Amount: $${budget.amount || 0}`);
                    console.log(`  📊 Budget Amount Type: ${typeof budget.amount}`);
                    console.log(`  📋 Category Count: ${budget.categoryLimits?.length || 0}`);
                    
                    if (budget.categoryLimits && budget.categoryLimits.length > 0) {
                        budget.categoryLimits.forEach((category, catIndex) => {
                            console.log(`  📋 Category ${catIndex + 1}: ${category.categoryName}`);
                            console.log(`    💰 Limit: $${category.limitAmount || 0}`);
                            console.log(`    💸 Spent: $${category.spentAmount || 0}`);
                            console.log(`    📊 Progress: ${category.limitAmount > 0 ? ((category.spentAmount || 0) / category.limitAmount * 100).toFixed(1) : 0}%`);
                        });
                    } else {
                        console.log(`  ⚠️ No categories found for budget "${budget.name}"`);
                    }
                });
            }
            
            this.budgets = budgets || [];
            this.displayBudgets();
            this.updateBudgetStats();
        } catch (error) {
            console.error('Error loading budgets:', error);
            
            // Check if it's an authentication error
            if (error.message && error.message.includes('401') || error.message.includes('403')) {
                this.showMessage('Authentication failed. Please log in again.', 'error');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 2000);
            } else {
                this.showMessage('Failed to load budgets', 'error');
                this.showEmptyState();
            }
        }
    }

    updateBudgetStats() {
        const totalBudgets = this.budgets.length;
        const activeBudgets = this.budgets.filter(b => b.status === 'ACTIVE').length;
        
        // Debug budget amounts
        console.log('🔍 Updating budget stats...');
        this.budgets.forEach((budget, index) => {
            console.log(`  Budget ${index + 1}: "${budget.name}" - Amount: $${budget.amount || 0} (Type: ${typeof budget.amount})`);
        });
        
        const totalBudgetAmount = this.budgets.reduce((sum, b) => {
            const amount = b.amount || 0;
            console.log(`  Adding budget amount: $${amount} to sum: $${sum}`);
            return sum + amount;
        }, 0);
        
        console.log(`  💰 Final total budget amount: $${totalBudgetAmount}`);
        
        const overBudgetCount = this.budgets.filter(b => this.isOverBudget(b)).length;

        document.getElementById('totalBudgets').textContent = totalBudgets;
        document.getElementById('activeBudgets').textContent = activeBudgets;
        document.getElementById('totalBudgetAmount').textContent = `$${totalBudgetAmount.toFixed(2)}`;
        document.getElementById('overBudgetCount').textContent = overBudgetCount;
    }

    isOverBudget(budget) {
        if (!budget.categoryLimits || budget.categoryLimits.length === 0) return false;
        
        return budget.categoryLimits.some(category => {
            const spent = category.spentAmount || 0;
            const limit = category.limitAmount || 0;
            return limit > 0 && spent > limit;
        });
    }

    displayBudgets() {
        const container = document.getElementById('budgetsContainer');
        
        if (this.budgets.length === 0) {
            this.showEmptyState();
            return;
        }

        let html = '<div class="budget-grid">';
        
        this.budgets.forEach(budget => {
            html += this.createBudgetCard(budget);
        });
        
        html += '</div>';
        container.innerHTML = html;
    }

    createBudgetCard(budget) {
        const statusClass = this.getBudgetStatusClass(budget);
        const statusText = this.getBudgetStatusText(budget);
        const isOverBudget = this.isOverBudget(budget);
        
        let categoriesHtml = '';
        let totalSpent = 0;
        let totalLimit = 0;
        
        if (budget.categoryLimits && budget.categoryLimits.length > 0) {
            console.log(`🔍 Processing budget "${budget.name}" with ${budget.categoryLimits.length} categories:`, budget.categoryLimits);
            budget.categoryLimits.forEach(category => {
                console.log(`📋 Category details:`, {
                    id: category.id,
                    categoryId: category.categoryId,
                    categoryName: category.categoryName,
                    limitAmount: category.limitAmount,
                    spentAmount: category.spentAmount
                });
                
                const spent = category.spentAmount || 0;
                const limit = category.limitAmount || 0;
                totalSpent += spent;
                totalLimit += limit;
                
                const progressPercent = limit > 0 ? Math.min((spent / limit) * 100, 100) : 0;
                const progressClass = this.getProgressClass(progressPercent);
                
                categoriesHtml += `
                    <div class="category-item">
                        <div>
                            <span class="category-name">${category.categoryName || 'Unknown Category'}</span>
                            <div class="category-progress">
                                <div class="category-progress-fill ${progressClass}" style="width: ${progressPercent}%"></div>
                            </div>
                        </div>
                        <div class="category-actions">
                            <span class="category-amount">$${spent.toFixed(2)} / $${limit.toFixed(2)}</span>
                            <button class="category-edit-btn" onclick="budgetManager.editCategoryLimit('${budget.id}', '${category.categoryId}', ${limit})" title="Edit category limit">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="category-delete-btn" onclick="budgetManager.deleteCategoryFromBudget('${budget.id}', '${category.categoryId}', '${category.categoryName}')" title="Remove category from budget">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                `;
            });
        }

        // Calculate overall budget progress
        const overallProgress = budget.amount > 0 ? Math.min((totalSpent / budget.amount) * 100, 100) : 0;
        const overallProgressClass = this.getProgressClass(overallProgress);

        // Format dates for display
        const startDate = budget.startDate ? new Date(budget.startDate).toLocaleDateString() : 'N/A';
        const endDate = budget.endDate ? new Date(budget.endDate).toLocaleDateString() : 'N/A';

        return `
            <div class="budget-card">
                <div class="budget-status ${statusClass} ${isOverBudget ? 'over-budget' : ''}">
                    ${isOverBudget ? '⚠️ Over Budget' : statusText}
                </div>
                
                <div class="budget-title">${budget.name || 'Unnamed Budget'}</div>
                ${budget.description ? `<div class="budget-description">${budget.description}</div>` : ''}
                
                <div class="budget-amount">$${budget.amount ? budget.amount.toFixed(2) : '0.00'}</div>
                <div class="budget-period">${startDate} - ${endDate}</div>
                
                <div class="budget-progress">
                    <div class="progress-bar">
                        <div class="progress-fill ${overallProgressClass}" style="width: ${overallProgress}%"></div>
                    </div>
                    <div class="progress-text">
                        <span>Spent: $${totalSpent.toFixed(2)}</span>
                        <span>${overallProgress.toFixed(1)}%</span>
                    </div>
                </div>
                
                ${categoriesHtml ? `<div class="category-list">${categoriesHtml}</div>` : ''}
                
                <div class="add-category-section">
                    <button class="add-category-btn" onclick="budgetManager.addCategoryToBudget('${budget.id}')" title="Add new category to this budget">
                        <i class="fas fa-plus"></i> Add Category
                    </button>
                </div>
                
                <div class="budget-actions">
                    <button class="action-btn primary" onclick="budgetManager.viewBudgetSummary('${budget.id}')">
                        <i class="fas fa-chart-pie"></i> Summary
                    </button>
                    <button class="action-btn" onclick="budgetManager.editBudget('${budget.id}')" title="Edit budget details (name, description, dates)">
                        <i class="fas fa-edit"></i> Edit Budget
                    </button>
                    <button class="action-btn danger" onclick="budgetManager.archiveBudget('${budget.id}')">
                        <i class="fas fa-archive"></i> Archive
                    </button>
                    <button class="action-btn danger" onclick="budgetManager.deleteBudget('${budget.id}')">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        `;
    }

    getProgressClass(percentage) {
        if (percentage >= 90) return 'danger';
        if (percentage >= 75) return 'warning';
        return '';
    }

    getBudgetStatusClass(budget) {
        if (budget.status === 'ACTIVE') return 'active';
        if (budget.status === 'ARCHIVED') return 'archived';
        return 'attention';
    }

    getBudgetStatusText(budget) {
        if (budget.status === 'ACTIVE') return '🟢 Active';
        if (budget.status === 'ARCHIVED') return '🔴 Archived';
        return '🟡 Attention';
    }

    showEmptyState() {
        const container = document.getElementById('budgetsContainer');
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-chart-pie"></i>
                <h3>No Budgets Yet</h3>
                <p>Create your first budget to start tracking your spending</p>
                <button class="create-budget-btn" onclick="openCreateBudgetModal()">
                    <i class="fas fa-plus"></i> Create Your First Budget
                </button>
            </div>
        `;
    }

    async handleCreateBudget(event) {
        event.preventDefault();
        
        try {
            const formData = this.getCreateBudgetFormData();
            if (!formData) return;

            console.log('=== CREATING BUDGET ===');
            console.log('Form data collected:', formData);
            console.log('Budget data:', formData.budget);
            console.log('Category limits:', formData.categoryLimits);
            console.log('Category limits count:', formData.categoryLimits.length);

            const submitBtn = event.target.querySelector('.submit-btn');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Creating...';
            }

            // Create budget first
            console.log('Calling API service to create budget...');
            const budget = await apiService.createBudget(formData.budget, formData.categoryLimits);
            
            if (budget) {
                console.log('Budget created successfully:', budget);
                this.showMessage('Budget created successfully!', 'success');
                this.resetCreateBudgetForm();
                closeCreateBudgetModal();
                await this.loadBudgets(); // Refresh the list
            }
        } catch (error) {
            console.error('Error creating budget:', error);
            console.error('Error details:', {
                message: error.message,
                stack: error.stack,
                name: error.name
            });
            this.showMessage(`Failed to create budget: ${error.message}`, 'error');
        } finally {
            const submitBtn = event.target.querySelector('.submit-btn');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Budget';
            }
        }
    }

    getCreateBudgetFormData() {
        const name = document.getElementById('budgetName').value.trim();
        const description = document.getElementById('budgetDescription').value.trim();
        const amount = parseFloat(document.getElementById('budgetAmount').value);
        const period = document.getElementById('budgetPeriod').value;
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;

        if (!name || !amount || !period || !startDate || !endDate) {
            this.showMessage('Please fill in all required fields', 'error');
            return null;
        }

        if (amount <= 0) {
            this.showMessage('Budget amount must be greater than 0', 'error');
            return null;
        }

        if (new Date(startDate) >= new Date(endDate)) {
            this.showMessage('End date must be after start date', 'error');
            return null;
        }

        // Get category limits
        const categoryLimits = [];
        const categoryRows = document.querySelectorAll('.category-input-row');
        
        categoryRows.forEach(row => {
            const categorySelect = row.querySelector('.category-select');
            const limitInput = row.querySelector('.category-limit');
            
            if (categorySelect.value && limitInput.value) {
                categoryLimits.push({
                    categoryId: categorySelect.value,
                    limitAmount: parseFloat(limitInput.value)
                });
            }
        });

        if (categoryLimits.length === 0) {
            this.showMessage('Please add at least one category with a limit', 'error');
            return null;
        }

        return {
            budget: {
                name: name,
                description: description,
                amount: amount,
                startDate: startDate,
                endDate: endDate,
                status: 'ACTIVE'
            },
            categoryLimits: categoryLimits
        };
    }








    
    // Show helpful message when BudgetManager is not ready
    static showNotReadyMessage() {
        const status = this.getStatus();
        const authStatus = this.checkAuthStatus();
        
        let message = 'Budget Manager is not ready yet.\n\n';
        message += `Current Status: ${status.message}\n`;
        message += `Categories Loaded: ${status.categories}\n`;
        message += `Authentication: ${authStatus.hasToken ? 'OK' : 'Failed'}\n\n`;
        message += 'This usually means:\n';
        message += '• The page is still loading\n';
        message += '• Categories are being fetched from the server\n';
        message += '• There may be a connection issue\n\n';
        message += 'Please wait a moment and try again.';
        
        alert(message);
    }
    
    // Show status in UI
    static showStatusInUI() {
        const status = this.getStatus();
        const authStatus = this.checkAuthStatus();
        
        // Create or update status display
        let statusDisplay = document.getElementById('budgetManagerStatus');
        if (!statusDisplay) {
            statusDisplay = document.createElement('div');
            statusDisplay.id = 'budgetManagerStatus';
            statusDisplay.style.cssText = `
                position: fixed;
                top: 10px;
                right: 10px;
                background: white;
                border: 1px solid #ccc;
                border-radius: 5px;
                padding: 10px;
                font-size: 12px;
                z-index: 1000;
                box-shadow: 0 2px 5px rgba(0,0,0,0.2);
            `;
            document.body.appendChild(statusDisplay);
        }
        
        statusDisplay.innerHTML = `
            <strong>Budget Manager Status</strong><br>
            Status: ${status.message}<br>
            Categories: ${status.categories}<br>
            Budgets: ${status.budgets}<br>
            Auth: ${authStatus.hasToken ? 'OK' : 'Failed'}<br>
            <button onclick="BudgetManager.showStatusInUI()" style="margin-top: 5px;">Refresh</button>
            <button onclick="document.getElementById('budgetManagerStatus').remove()" style="margin-top: 5px;">Close</button>
        `;
    }
    
    // Check if BudgetManager is ready
    static isReady() {
        return window.budgetManager && 
               window.budgetManager.categories && 
               window.budgetManager.categories.length > 0;
    }
    
    // Get detailed status information
    static getStatus() {
        if (!window.budgetManager) {
            return {
                ready: false,
                message: 'Budget Manager not initialized',
                categories: 0,
                budgets: 0
            };
        }
        
        return {
            ready: true,
            message: 'Budget Manager is ready',
            categories: window.budgetManager.categories?.length || 0,
            budgets: window.budgetManager.budgets?.length || 0
        };
    }
    
    // Wait for BudgetManager to be ready
    static async waitForReady(maxWaitTime = 10000) {
        const startTime = Date.now();
        const checkInterval = 100; // Check every 100ms
        
        while (Date.now() - startTime < maxWaitTime) {
            if (this.isReady()) {
                return true;
            }
            
            // Show progress every 500ms
            const elapsed = Date.now() - startTime;
            if (elapsed % 500 === 0) {
                const progress = Math.round((elapsed / maxWaitTime) * 100);
                console.log(`Waiting for BudgetManager... ${progress}%`);
            }
            
            await new Promise(resolve => setTimeout(resolve, checkInterval));
        }
        
        return false;
    }
    
    // Show current status to user
    static showStatus() {
        const status = this.getStatus();
        const message = `Budget Manager Status:\n${status.message}\nCategories: ${status.categories}\nBudgets: ${status.budgets}`;
        alert(message);
    }
    
    // Check authentication status
    static checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        const userEmail = localStorage.getItem('userEmail');
        
        const authStatus = {
            hasToken: !!token,
            hasUserEmail: !!userEmail,
            tokenPreview: token ? token.substring(0, 20) + '...' : 'none',
            userEmail: userEmail || 'none'
        };
        
        console.log('Authentication Status:', authStatus);
        return authStatus;
    }
    
    // Troubleshoot common issues
    static troubleshoot() {
        console.log('=== Troubleshooting Budget Manager ===');
        
        const authStatus = this.checkAuthStatus();
        const status = this.getStatus();
        
        let issues = [];
        let suggestions = [];
        
        if (!authStatus.hasToken) {
            issues.push('No authentication token found');
            suggestions.push('Please log in to the application');
        }
        
        if (!window.budgetManager) {
            issues.push('BudgetManager instance not created');
            suggestions.push('Try refreshing the page or check console for errors');
        }
        
        if (status.ready && status.categories === 0) {
            issues.push('No categories loaded');
            suggestions.push('Categories may still be loading or there may be an API issue');
        }
        
        if (issues.length === 0) {
            console.log('✅ No obvious issues found');
        } else {
            console.log('❌ Issues found:', issues);
            console.log('💡 Suggestions:', suggestions);
        }
        
        return { issues, suggestions };
    }
    
    // Auto-refresh if initialization fails
    static async autoRefreshIfNeeded(maxAttempts = 3) {
        let attempts = 0;
        
        while (attempts < maxAttempts) {
            attempts++;
            console.log(`Attempt ${attempts} to initialize BudgetManager...`);
            
            if (this.isReady()) {
                console.log('BudgetManager is ready!');
                return true;
            }
            
            // Wait before next attempt
            await new Promise(resolve => setTimeout(resolve, 2000));
        }
        
        console.log('BudgetManager failed to initialize after multiple attempts');
        console.log('Suggesting page refresh...');
        
        if (confirm('Budget Manager is having trouble initializing. Would you like to refresh the page?')) {
            window.location.reload();
        }
        
        return false;
    }

    // Remove category row from edit modal
    removeEditCategory(button) {
        const row = button.parentElement;
        const container = document.getElementById('editCategoryInputs');
        
        // Don't remove if it's the last row
        if (container.children.length > 1) {
            row.remove();
        }
    }

    // Make removeEditCategory globally accessible
    static removeEditCategory(button) {
        if (window.budgetManager) {
            window.budgetManager.removeEditCategory(button);
        } else {
            console.error('BudgetManager not initialized');
        }
    }

        async handleEditBudget(event) {
        event.preventDefault();
        
        try {
            const budgetId = document.getElementById('editBudgetId').value;
            const name = document.getElementById('editBudgetName').value.trim();
            const description = document.getElementById('editBudgetDescription').value.trim();
            const amount = parseFloat(document.getElementById('editBudgetAmount').value);
            const period = document.getElementById('editBudgetPeriod').value;
            const startDate = document.getElementById('editBudgetStartDate').value;
            const endDate = document.getElementById('editBudgetEndDate').value;

            if (!name || !amount || !period || !startDate || !endDate) {
                this.showMessage('Please fill in all required fields', 'error');
                return;
            }

            // Validate dates
            if (new Date(startDate) >= new Date(endDate)) {
                this.showMessage('End date must be after start date', 'error');
                return;
            }

            const submitBtn = event.target.querySelector('.submit-btn');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Updating...';

            console.log('🔄 Starting budget details update...');
            console.log('📋 Budget ID:', budgetId);
            console.log('📋 Budget Name:', name);
            console.log('📋 Budget Description:', description);
            console.log('📋 Budget Amount:', amount);
            console.log('📋 Budget Period:', period);
            console.log('📋 Start Date:', startDate);
            console.log('📋 End Date:', endDate);

            // Update the budget details
            const budgetUpdateData = {
                name: name,
                description: description,
                amount: amount,
                period: period,
                startDate: startDate,
                endDate: endDate
            };
            
            console.log('🔄 Sending budget update to backend:', budgetUpdateData);
            
            // Call the backend API to update the budget
            const result = await apiService.updateBudget(budgetId, budgetUpdateData);
            
            if (result) {
                console.log('✅ Budget updated successfully in backend:', result);
                this.showMessage('Budget details updated successfully! Note: Category limits are managed separately.', 'success');
            } else {
                throw new Error('Backend returned no response for budget update');
            }
            
            closeEditBudgetModal();
            
            // Refresh the budget list to show updated data
            try {
                console.log('🔄 Refreshing budget list...');
                await this.loadBudgets();
                console.log('✅ Budget list refreshed successfully');
            } catch (refreshError) {
                console.error('⚠️ Failed to refresh budget list:', refreshError);
                this.showMessage('Budget updated but failed to refresh the list. Please refresh the page manually.', 'warning');
            }
        } catch (error) {
            console.error('Error updating budget details:', error);
            
            // Handle specific error types
            if (error.message.includes('Authentication token expired') || 
                error.message.includes('User not authenticated')) {
                this.showMessage('Your session has expired. Please login again.', 'error');
                // Redirect to login after a short delay
                setTimeout(() => {
                    if (window.location.pathname.includes('html/')) {
                        window.location.href = '../html/auth.html';
                    } else {
                        window.location.href = 'html/auth.html';
                    }
                }, 2000);
            } else if (error.message.includes('403')) {
                this.showMessage('Access denied. You may not have permission to edit this budget.', 'error');
            } else {
                this.showMessage(`Failed to update budget details: ${error.message}`, 'error');
            }
        } finally {
            const submitBtn = event.target.querySelector('.submit-btn');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Update Budget Details';
        }
    }

    async viewBudgetSummary(budgetId) {
        try {
            const summary = await apiService.getBudgetSummary(budgetId);
            if (summary) {
                this.displayBudgetSummary(summary);
                openBudgetSummaryModal();
            }
        } catch (error) {
            console.error('Error loading budget summary:', error);
            this.showMessage('Failed to load budget summary', 'error');
        }
    }

    displayBudgetSummary(summary) {
        const content = document.getElementById('budgetSummaryContent');
        
        // Debug the summary object
        console.log('🔍 Budget Summary received:', summary);
        console.log('💰 Total Budgeted:', summary.totalBudgeted);
        console.log('💸 Total Spent:', summary.totalSpent);
        console.log('📊 Remaining Amount:', summary.remainingAmount);
        console.log('📋 Category Summaries:', summary.categorySummaries);
        
        let html = `
            <div class="budget-summary">
                <h3>${summary.budgetName || 'Budget Summary'}</h3>
                <p>Period: ${summary.startDate} to ${summary.endDate}</p>
                
                <div class="summary-stats">
                    <div class="stat-item">
                        <strong>Total Budget:</strong> $${summary.totalBudgeted?.toFixed(2) || '0.00'}
                    </div>
                    <div class="stat-item">
                        <strong>Total Spent:</strong> $${summary.totalSpent?.toFixed(2) || '0.00'}
                    </div>
                    <div class="stat-item">
                        <strong>Remaining:</strong> $${summary.remainingAmount?.toFixed(2) || '0.00'}
                    </div>
                </div>
                
                <h4>Category Breakdown</h4>
                <div class="category-breakdown">
        `;

        if (summary.categorySummaries && summary.categorySummaries.length > 0) {
            summary.categorySummaries.forEach(category => {
                const spent = category.spentAmount || 0;
                const limit = category.limitAmount || 0;
                const remaining = limit - spent;
                const status = spent > limit ? 'over' : spent > limit * 0.8 ? 'warning' : 'good';
                
                html += `
                    <div class="category-summary ${status}">
                        <div class="category-header">
                            <span class="category-name">${category.categoryName || 'Unknown'}</span>
                            <span class="category-status">${status === 'over' ? '⚠️ Over Budget' : status === 'warning' ? '⚠️ Warning' : '✅ Good'}</span>
                        </div>
                        <div class="category-details">
                            <span>Spent: $${spent.toFixed(2)}</span>
                            <span>Limit: $${limit.toFixed(2)}</span>
                            <span>Remaining: $${remaining.toFixed(2)}</span>
                        </div>
                    </div>
                `;
            });
        } else {
            html += '<p>No categories found for this budget.</p>';
        }

        html += `
                </div>
            </div>
        `;
        
        content.innerHTML = html;
    }

    // Edit individual category limit
    async editCategoryLimit(budgetId, categoryId, currentLimit) {
        try {
            console.log(`🔧 Editing category limit for budget ${budgetId}, category ${categoryId}, current limit: $${currentLimit}`);
            
            // Show prompt for new limit
            const newLimit = prompt(`Enter new limit for this category (current: $${currentLimit}):`, currentLimit);
            
            if (newLimit === null) {
                console.log('User cancelled category limit edit');
                return;
            }
            
            const limitAmount = parseFloat(newLimit);
            if (isNaN(limitAmount) || limitAmount <= 0) {
                this.showMessage('Please enter a valid positive number for the limit', 'error');
                return;
            }
            
            // Update the category limit
            const result = await apiService.updateCategoryLimit(budgetId, categoryId, limitAmount);
            
            if (result) {
                this.showMessage(`Category limit updated successfully to $${limitAmount}`, 'success');
                await this.loadBudgets(); // Refresh the list
            }
        } catch (error) {
            console.error('Error updating category limit:', error);
            
            // Provide specific error messages for different error types
            let errorMessage = `Failed to update category limit: ${error.message}`;
            
            if (error.message.includes('JSON parsing error') || error.message.includes('Unexpected token')) {
                errorMessage = `Failed to update category limit due to a server response error. The server returned malformed data with excessive closing braces (}]}}]}}]}}). This indicates a backend issue that needs to be fixed.`;
                
                // Show detailed error information for developers
                console.error('🔍 JSON parsing error detected - Backend issue details:');
                console.error('   - Response contains excessive closing braces: }]}}]}}]}}');
                console.error('   - This suggests circular references in the backend data model');
                console.error('   - The backend is likely returning objects that reference themselves');
                console.error('   - Full error:', error);
                
                // Provide user-friendly message with action items
                this.showMessage(errorMessage + '\n\nPlease contact support or try again later. The backend team needs to fix the data structure issue.', 'error');
            } else if (error.message.includes('404')) {
                errorMessage = 'Category or budget not found. Please refresh the page and try again.';
                this.showMessage(errorMessage, 'error');
            } else if (error.message.includes('403')) {
                errorMessage = 'Access denied. You may not have permission to modify this budget.';
                this.showMessage(errorMessage, 'error');
            } else if (error.message.includes('400')) {
                errorMessage = 'Invalid data provided. Please check your inputs and try again.';
                this.showMessage(errorMessage, 'error');
            } else if (error.message.includes('500')) {
                errorMessage = 'Server error occurred. Please try again later.';
                this.showMessage(errorMessage, 'error');
            } else {
                this.showMessage(errorMessage, 'error');
            }
        }
    }

    // Delete category from budget
    async deleteCategoryFromBudget(budgetId, categoryId, categoryName) {
        try {
            console.log(`🗑️ Deleting category ${categoryName} (${categoryId}) from budget ${budgetId}`);
            
            // Show confirmation dialog
            const confirmed = confirm(`Are you sure you want to remove "${categoryName}" from this budget?\n\nThis will:\n• Remove the category limit\n• Delete all spending data for this category in this budget\n• This action cannot be undone`);
            
            if (!confirmed) {
                console.log('User cancelled category deletion');
                return;
            }
            
            // Call the backend API to delete the category
            console.log(`🔄 Calling API to delete category ${categoryId} from budget ${budgetId}`);
            const result = await apiService.deleteCategoryFromBudget(budgetId, categoryId);
            
            console.log(`✅ API call completed, result:`, result);
            
            if (result) {
                this.showMessage(`Category "${categoryName}" removed successfully from budget`, 'success');
                console.log(`🔄 Refreshing budget list to show changes...`);
                await this.loadBudgets(); // Refresh the list
                console.log(`✅ Budget list refreshed after deletion`);
                
                // Additional verification - check if the category is still in the budget
                const updatedBudget = this.budgets.find(b => b.id === budgetId);
                if (updatedBudget && updatedBudget.categoryLimits) {
                    const categoryStillExists = updatedBudget.categoryLimits.find(cat => cat.categoryId === categoryId);
                    if (categoryStillExists) {
                        console.error(`❌ Category still exists after refresh! Category:`, categoryStillExists);
                        this.showMessage(`Warning: Category may not have been fully removed. Please refresh the page.`, 'warning');
                    } else {
                        console.log(`✅ Category successfully removed - no longer found in budget data`);
                    }
                }
            } else {
                console.warn(`⚠️ API returned no result for deletion`);
                this.showMessage(`Category "${categoryName}" removal completed, but no confirmation received`, 'warning');
                await this.loadBudgets(); // Refresh anyway to show current state
            }
        } catch (error) {
            console.error('Error deleting category from budget:', error);
            
            // Provide specific error messages for different error types
            let errorMessage = `Failed to remove category: ${error.message}`;
            
            if (error.message.includes('400')) {
                console.error('🔍 400 Bad Request detected - checking for specific error details');
                console.error('   - Full error object:', error);
                console.error('   - Error message:', error.message);
                console.error('   - Error stack:', error.stack);
                
                // Try to extract more details from the error
                if (error.message.includes('API Error: 400')) {
                    errorMessage = 'Server returned a 400 error. This usually means there was a problem processing your request. Please check the console for more details and contact support if the issue persists.';
                } else {
                    errorMessage = 'Invalid request data. Please check your inputs and try again.';
                }
                
                this.showMessage(errorMessage, 'error');
            } else if (error.message.includes('JSON parsing error') || error.message.includes('Unexpected token')) {
                errorMessage = `Failed to remove category due to a server response error. The server returned malformed data. This indicates a backend issue that needs to be fixed.`;
                
                // Show detailed error information for developers
                console.error('🔍 JSON parsing error detected - Backend issue details:');
                console.error('   - Response contains malformed JSON structure');
                console.error('   - This suggests circular references in the backend data model');
                console.error('   - Full error:', error);
                
                // Provide user-friendly message with action items
                this.showMessage(errorMessage + '\n\nPlease contact support or try again later. The backend team needs to fix the data structure issue.', 'error');
            } else if (error.message.includes('404')) {
                errorMessage = 'Budget or category not found. Please refresh the page and try again.';
                this.showMessage(errorMessage, 'error');
            } else if (error.message.includes('403')) {
                errorMessage = 'Access denied. You may not have permission to modify this budget.';
                this.showMessage(errorMessage, 'error');
            } else if (error.message.includes('500')) {
                errorMessage = 'Server error occurred. Please try again later.';
                this.showMessage(errorMessage, 'error');
            } else {
                this.showMessage(errorMessage, 'error');
            }
        }
    }

    // Add new category to existing budget
    async addCategoryToBudget(budgetId) {
        try {
            console.log(`🔧 Adding new category to budget ${budgetId}`);
            
            // Show prompt for category selection and limit
            const categorySelect = document.createElement('select');
            categorySelect.innerHTML = '<option value="">Select Category</option>';
            
            if (this.categories && this.categories.length > 0) {
                this.categories.forEach(cat => {
                    categorySelect.innerHTML += `<option value="${cat.id}">${cat.name}</option>`;
                });
            }
            
            const limitInput = document.createElement('input');
            limitInput.type = 'number';
            limitInput.step = '0.01';
            limitInput.min = '0';
            limitInput.placeholder = 'Enter limit amount';
            
            const dialog = document.createElement('div');
            dialog.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 10000;
            `;
            
            const dialogContent = document.createElement('div');
            dialogContent.style.cssText = `
                background: white;
                padding: 20px;
                border-radius: 8px;
                min-width: 300px;
            `;
            
            dialogContent.innerHTML = `
                <h3>Add Category to Budget</h3>
                <div style="margin: 15px 0;">
                    <label>Category:</label><br>
                    <div style="margin-top: 5px;">${categorySelect.outerHTML}</div>
                </div>
                <div style="margin: 15px 0;">
                    <label>Limit Amount:</label><br>
                    <div style="margin-top: 5px;">${limitInput.outerHTML}</div>
                </div>
                <div style="text-align: right; margin-top: 20px;">
                    <button onclick="this.closest('.add-category-dialog').remove()" style="margin-right: 10px;">Cancel</button>
                    <button onclick="this.closest('.add-category-dialog').confirmAdd()" class="primary">Add Category</button>
                </div>
            `;
            
            dialog.className = 'add-category-dialog';
            dialog.appendChild(dialogContent);
            document.body.appendChild(dialog);
            
            // Get the actual elements from the DOM
            const actualCategorySelect = dialog.querySelector('select');
            const actualLimitInput = dialog.querySelector('input');
            
            // Add event listener for confirmation
            dialog.querySelector('button.primary').onclick = async () => {
                const categoryId = actualCategorySelect.value;
                const limitAmount = parseFloat(actualLimitInput.value);
                
                if (!categoryId) {
                    this.showMessage('Please select a category', 'error');
                    return;
                }
                
                if (isNaN(limitAmount) || limitAmount <= 0) {
                    this.showMessage('Please enter a valid positive number for the limit', 'error');
                    return;
                }
                
                try {
                    const result = await apiService.addCategoryToBudget(budgetId, categoryId, limitAmount);
                    
                    if (result) {
                        this.showMessage(`Category added successfully with limit $${limitAmount}`, 'success');
                        dialog.remove();
                        await this.loadBudgets(); // Refresh the list
                    }
                } catch (error) {
                    console.error('Error adding category to budget:', error);
                    
                    // Provide specific error messages for different error types
                    let errorMessage = `Failed to add category: ${error.message}`;
                    
                    if (error.message.includes('JSON parsing error') || error.message.includes('Unexpected token')) {
                        errorMessage = `Failed to add category due to a server response error. The server returned malformed data. This indicates a backend issue that needs to be fixed.`;
                        
                        // Show detailed error information for developers
                        console.error('🔍 JSON parsing error detected - Backend issue details:');
                        console.error('   - Response contains malformed JSON structure');
                        console.error('   - This suggests circular references in the backend data model');
                        console.error('   - Full error:', error);
                        
                        // Provide user-friendly message with action items
                        this.showMessage(errorMessage + '\n\nPlease contact support or try again later. The backend team needs to fix the data structure issue.', 'error');
                    } else if (error.message.includes('404')) {
                        errorMessage = 'Budget not found. Please refresh the page and try again.';
                        this.showMessage(errorMessage, 'error');
                    } else if (error.message.includes('403')) {
                        errorMessage = 'Access denied. You may not have permission to modify this budget.';
                        this.showMessage(errorMessage, 'error');
                    } else if (error.message.includes('400')) {
                        errorMessage = 'Invalid data provided. Please check your inputs and try again.';
                        this.showMessage(errorMessage, 'error');
                    } else if (error.message.includes('500')) {
                        errorMessage = 'Server error occurred. Please try again later.';
                        this.showMessage(errorMessage, 'error');
                    } else {
                        this.showMessage(errorMessage, 'error');
                    }
                }
            };
            
        } catch (error) {
            console.error('Error in addCategoryToBudget:', error);
            this.showMessage(`Failed to add category: ${error.message}`, 'error');
        }
    }

    async editBudget(budgetId) {
        try {
            const budget = this.budgets.find(b => b.id === budgetId);
            if (!budget) {
                this.showMessage('Budget not found', 'error');
                return;
            }

            // Populate form fields
            document.getElementById('editBudgetId').value = budget.id;
            document.getElementById('editBudgetName').value = budget.name || '';
            document.getElementById('editBudgetDescription').value = budget.description || '';
            document.getElementById('editBudgetAmount').value = budget.amount || '';
            document.getElementById('editBudgetPeriod').value = budget.period || 'MONTHLY';
            
            // Populate date fields
            if (budget.startDate) {
                document.getElementById('editBudgetStartDate').value = budget.startDate;
            }
            if (budget.endDate) {
                document.getElementById('editBudgetEndDate').value = budget.endDate;
            }
            
            openEditBudgetModal();
        } catch (error) {
            console.error('Error opening edit modal:', error);
            this.showMessage('Failed to open edit modal', 'error');
        }
    }

    async evaluateBudget(budgetId) {
        try {
            const result = await apiService.evaluateBudget(budgetId);
            if (result) {
                this.showMessage('Budget evaluation completed successfully!', 'success');
                await this.loadBudgets(); // Refresh to show updated status
            }
        } catch (error) {
            console.error('Error evaluating budget:', error);
            this.showMessage(`Failed to evaluate budget: ${error.message}`, 'error');
        }
    }

    async archiveBudget(budgetId) {
        if (!confirm('Are you sure you want to archive this budget? This action cannot be undone.')) {
            return;
        }

        try {
            const result = await apiService.archiveBudget(budgetId);
            if (result) {
                this.showMessage('Budget archived successfully!', 'success');
                await this.loadBudgets(); // Refresh the list
            }
        } catch (error) {
            console.error('Error archiving budget:', error);
            this.showMessage(`Failed to archive budget: ${error.message}`, 'error');
        }
    }

    async deleteBudget(budgetId) {
        if (!confirm('⚠️ WARNING: Are you sure you want to DELETE this budget? This action is PERMANENT and cannot be undone. All budget data will be lost forever.')) {
            return;
        }

        // Double confirmation for deletion
        if (!confirm('🚨 FINAL WARNING: This will permanently delete the budget and all its data. Are you absolutely sure?')) {
            return;
        }

        try {
            const result = await apiService.deleteBudget(budgetId);
            if (result) {
                this.showMessage('Budget deleted successfully!', 'success');
                await this.loadBudgets(); // Refresh the list
            }
        } catch (error) {
            console.error('Error deleting budget:', error);
            this.showMessage(`Failed to delete budget: ${error.message}`, 'error');
        }
    }

    async refreshBudgetSpending(budgetId) {
        try {
            console.log('🔄 Refreshing spending for budget:', budgetId);
            this.showMessage('Refreshing budget spending...', 'info');
            
            // Call the backend to recalculate spending for this specific budget
            await apiService.recalculateBudgetSpending(budgetId);
            
            // Reload the budgets to get updated data
            await this.loadBudgets();
            
            this.showMessage('Budget spending refreshed successfully!', 'success');
        } catch (error) {
            console.error('Error refreshing budget spending:', error);
            this.showMessage('Error refreshing budget spending: ' + error.message, 'error');
        }
    }

    async debugBudgetData(budgetId) {
        try {
            console.log('🔍 Debugging budget data for budget:', budgetId);
            
            // Get the budget from our local data
            const budget = this.budgets.find(b => b.id === budgetId);
            if (!budget) {
                console.error('Budget not found in local data:', budgetId);
                return;
            }
            
            console.log('📊 Local budget data:', budget);
            console.log('📋 Category limits:', budget.categoryLimits);
            
            if (budget.categoryLimits && budget.categoryLimits.length > 0) {
                budget.categoryLimits.forEach((category, index) => {
                    console.log(`📋 Category ${index + 1}:`, {
                        id: category.id,
                        categoryId: category.categoryId,
                        categoryName: category.categoryName,
                        limitAmount: category.limitAmount,
                        spentAmount: category.spentAmount,
                        spentAmountType: typeof category.spentAmount,
                        limitAmountType: typeof category.limitAmount
                    });
                });
            } else {
                console.log('⚠️ No category limits found in budget');
            }
            
            // Also try to get fresh data from the API
            console.log('🔄 Fetching fresh data from API...');
            const freshBudgets = await apiService.getUserBudgets();
            const freshBudget = freshBudgets.find(b => b.id === budgetId);
            
            if (freshBudget) {
                console.log('📊 Fresh budget data from API:', freshBudget);
                console.log('📋 Fresh category limits:', freshBudget.categoryLimits);
            } else {
                console.log('⚠️ Fresh budget not found in API response');
            }
            
            // Check for cross-user categories
            console.log('🔍 Checking for cross-user categories...');
            const currentUserEmail = localStorage.getItem('userEmail');
            console.log('Current user email:', currentUserEmail);
            
            if (budget.categoryLimits && budget.categoryLimits.length > 0) {
                const suspiciousCategories = budget.categoryLimits.filter(cat => 
                    cat.categoryName && (
                        cat.categoryName.toLowerCase().includes('food') ||
                        cat.categoryName.toLowerCase().includes('dining') ||
                        cat.categoryName.toLowerCase().includes('restaurant')
                    )
                );
                
                if (suspiciousCategories.length > 0) {
                    console.log('⚠️ POTENTIAL CROSS-USER CATEGORIES FOUND:', suspiciousCategories);
                    console.log('These categories might belong to other users!');
                } else {
                    console.log('✅ No suspicious cross-user categories detected');
                }
            }
            
        } catch (error) {
            console.error('Error debugging budget data:', error);
        }
    }

    async createTestCategories() {
        try {
            console.log('Creating test categories...');
            
            // Check if categories already exist
            if (this.categories && this.categories.length > 0) {
                console.log('Categories already exist, skipping creation');
                return;
            }
            
            // Define test categories
            const testCategories = [
                { name: 'Food & Dining', type: 'EXPENSE' },
                { name: 'Shopping', type: 'EXPENSE' },
                { name: 'Transportation', type: 'EXPENSE' },
                { name: 'Entertainment', type: 'EXPENSE' },
                { name: 'Utilities', type: 'EXPENSE' },
                { name: 'Healthcare', type: 'EXPENSE' },
                { name: 'Salary', type: 'INCOME' },
                { name: 'Freelance', type: 'INCOME' },
                { name: 'ATM Withdrawal', type: 'TRANSFER' }
            ];
            
            const createdCategories = [];
            
            for (const categoryData of testCategories) {
                try {
                    const category = await apiService.createCategory(categoryData);
                    createdCategories.push(category);
                    console.log(`Created category: ${category.name}`);
                } catch (error) {
                    console.error(`Failed to create category ${categoryData.name}:`, error);
                }
            }
            
            if (createdCategories.length > 0) {
                this.categories = createdCategories;
                this.populateCategorySelects();
                console.log(`Successfully created ${createdCategories.length} test categories`);
                this.showMessage(`Created ${createdCategories.length} test categories`, 'success');
            } else {
                console.warn('No test categories were created');
                this.showMessage('No test categories were created', 'warning');
            }
            
        } catch (error) {
            console.error('Error creating test categories:', error);
            this.showMessage(`Failed to create test categories: ${error.message}`, 'error');
            throw error;
        }
    }

    resetCreateBudgetForm() {
        document.getElementById('createBudgetForm').reset();
        this.setDefaultDates();
        
        // Reset category inputs to single row
        const categoryInputs = document.getElementById('categoryInputs');
        categoryInputs.innerHTML = `
            <div class="category-input-row">
                <select class="category-select" required>
                    <option value="">Select Category</option>
                </select>
                <input type="number" step="0.01" min="0" placeholder="Limit amount" class="category-limit" required>
                <button type="button" class="remove-category-btn" onclick="removeCategory(this)" style="display: none;">×</button>
            </div>
        `;
        
        // Populate category selects after DOM manipulation
        // Use setTimeout to ensure DOM is ready
        setTimeout(() => {
            this.populateCategorySelects();
        }, 0);
    }

    showMessage(message, type = 'info') {
        // Create message element
        const messageDiv = document.createElement('div');
        messageDiv.className = `alert ${type}`;
        messageDiv.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : type === 'warning' ? 'exclamation-triangle' : 'info-circle'}"></i>
            ${message}
        `;

        // Insert at top of container
        const container = document.querySelector('.budget-container');
        container.insertBefore(messageDiv, container.firstChild);

        // Auto-remove after timeout
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.parentNode.removeChild(messageDiv);
            }
        }, CONFIG.UI.MESSAGE_TIMEOUT);
    }
}

// Global functions for modal management
async function openCreateBudgetModal() {
    console.log('Opening create budget modal...');
    
    // Check authentication first
    const token = localStorage.getItem('authToken'); // Use the same key as auth.js
    if (!token) {
        console.log('No authentication token found');
        budgetManager.showMessage('Please log in to create budgets', 'error');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2000);
        return;
    }
    
    // Verify token before proceeding
    try {
        console.log('Verifying authentication token...');
        const isAuthenticated = await apiService.verifyToken();
        if (!isAuthenticated) {
            console.log('Token verification failed');
            localStorage.removeItem(CONFIG.JWT.STORAGE_KEY);
            budgetManager.showMessage('Session expired. Please log in again.', 'error');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 2000);
            return;
        }
        console.log('Token verified successfully');
    } catch (error) {
        console.error('Authentication verification failed:', error);
        budgetManager.showMessage('Authentication failed. Please log in again.', 'error');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2000);
        return;
    }
    
    // Show the modal first
    document.getElementById('createBudgetModal').style.display = 'block';
    
    // Load categories if not already loaded
    if (!budgetManager.categories || budgetManager.categories.length === 0) {
        console.log('No categories loaded, loading categories...');
        try {
            await budgetManager.loadCategories();
            console.log('Categories loaded successfully:', budgetManager.categories.length);
        } catch (error) {
            console.error('Failed to load categories:', error);
            budgetManager.showMessage('Failed to load categories. Please try again.', 'error');
            // Don't close modal, just show error
            return;
        }
    }
    
    // Verify categories are properly loaded
    if (!budgetManager.verifyCategoriesLoaded()) {
        console.error('Categories verification failed, attempting to reload...');
        try {
            await budgetManager.loadCategories();
            if (!budgetManager.verifyCategoriesLoaded()) {
                budgetManager.showMessage('Failed to load valid categories. Please try again.', 'error');
                return;
            }
        } catch (error) {
            console.error('Failed to reload categories:', error);
            budgetManager.showMessage('Failed to load categories. Please try again.', 'error');
            return;
        }
    }
    
    // Populate category selects
    budgetManager.populateCategorySelects();
    
    // Reset form
    document.getElementById('createBudgetForm').reset();
    budgetManager.setDefaultDates();
    
    // Clear any existing category inputs (keep only the first one)
    const categoryInputs = document.getElementById('categoryInputs');
    categoryInputs.innerHTML = `
        <div class="category-input-row">
            <select class="category-select" required>
                <option value="">Select Category</option>
            </select>
            <input type="number" step="0.01" min="0" placeholder="Limit amount" class="category-limit" required>
            <button type="button" class="remove-category-btn" onclick="removeCategory(this)">×</button>
        </div>
    `;
    
    // Populate the category select after DOM manipulation
    // Use setTimeout to ensure DOM is ready
    setTimeout(() => {
        budgetManager.populateCategorySelects();
    }, 0);
}

function closeCreateBudgetModal() {
    document.getElementById('createBudgetModal').style.display = 'none';
}

function openEditBudgetModal() {
    document.getElementById('editBudgetModal').style.display = 'block';
}

function closeEditBudgetModal() {
    document.getElementById('editBudgetModal').style.display = 'none';
}

function openBudgetSummaryModal() {
    document.getElementById('budgetSummaryModal').style.display = 'block';
}

function closeBudgetSummaryModal() {
    document.getElementById('budgetSummaryModal').style.display = 'none';
}

function addCategory() {
    console.log('Adding new category row...');
    
    // Ensure categories are loaded
    if (!budgetManager.categories || budgetManager.categories.length === 0) {
        console.log('No categories available, cannot add new row');
        budgetManager.showMessage('No categories available. Please create some categories first.', 'warning');
        return;
    }
    
    const categoryInputs = document.getElementById('categoryInputs');
    const newRow = document.createElement('div');
    newRow.className = 'category-input-row';
    newRow.innerHTML = `
        <select class="category-select" required>
            <option value="">Select Category</option>
        </select>
        <input type="number" step="0.01" min="0" placeholder="Limit amount" class="category-limit" required>
        <button type="button" class="remove-category-btn" onclick="removeCategory(this)">×</button>
    `;
    
    categoryInputs.appendChild(newRow);
    console.log('Added new category row, populating selects...');
    
    // Populate the new select with categories
    const newSelect = newRow.querySelector('.category-select');
    budgetManager.categories.forEach(category => {
        const option = document.createElement('option');
        option.value = category.id;
        option.textContent = category.name;
        newSelect.appendChild(option);
    });
    
    console.log(`New row populated with ${budgetManager.categories.length} categories`);
}

function removeCategory(button) {
    const row = button.parentElement;
    const categoryInputs = document.getElementById('categoryInputs');
    const rows = categoryInputs.querySelectorAll('.category-input-row');
    
    if (rows.length > 1) {
        row.remove();
        
        // Hide remove button if only one row remains
        if (rows.length === 2) {
            const remainingRemoveButton = categoryInputs.querySelector('.remove-category-btn');
            if (remainingRemoveButton) {
                remainingRemoveButton.style.display = 'none';
            }
        }
    }
}

// Close modals when clicking outside
window.onclick = function(event) {
    const createModal = document.getElementById('createBudgetModal');
    const editModal = document.getElementById('editBudgetModal');
    const budgetSummaryModal = document.getElementById('budgetSummaryModal');
    
    if (event.target === createModal) {
        closeCreateBudgetModal();
    }
    if (event.target === editModal) {
        closeEditBudgetModal();
    }
    if (event.target === budgetSummaryModal) {
        closeBudgetSummaryModal();
    }
}

// Initialize the budget manager when the page loads
let budgetManager;
document.addEventListener('DOMContentLoaded', () => {
    budgetManager = new BudgetManager();
    // Make it globally accessible for static methods
    window.budgetManager = budgetManager;
    
    // Show initial status after a short delay
    setTimeout(() => {
        if (BudgetManager.showStatusInUI) {
            BudgetManager.showStatusInUI();
        }
    }, 500);
    
    // Show status again after BudgetManager is fully initialized
    setTimeout(() => {
        if (BudgetManager.showStatusInUI) {
            BudgetManager.showStatusInUI();
        }
    }, 3000);
    
    // Show status every 5 seconds for debugging
    setInterval(() => {
        if (BudgetManager.showStatusInUI) {
            BudgetManager.showStatusInUI();
        }
    }, 5000);
    
    // Log initialization complete
    console.log('✅ BudgetManager initialization complete');
    console.log('🔧 Available debug functions:');
    console.log('  • checkBudgetManagerStatus()');
    console.log('  • BudgetManager.troubleshoot()');
    console.log('  • BudgetManager.showStatusInUI()');
    console.log('  • helpWithBudgetManagerError()');
});



// Console helper functions for debugging
console.log('=== Budget Manager Debug Functions Available ===');
console.log('• checkBudgetManagerStatus() - Check current status');
console.log('• BudgetManager.troubleshoot() - Run troubleshooting');
console.log('• BudgetManager.showStatus() - Show status to user');
console.log('• BudgetManager.showStatusInUI() - Show status in UI');
console.log('• BudgetManager.checkAuthStatus() - Check authentication');
console.log('• reloadCategories() - Manually reload categories');
console.log('• helpWithBudgetManagerError() - Get help with errors');
console.log('• BudgetManager.autoRefreshIfNeeded() - Attempt auto-recovery');
console.log('• debugBackendJsonError() - Debug JSON parsing errors from backend');

// Debug function to check BudgetManager status (can be called from console)
function checkBudgetManagerStatus() {
    console.log('=== BudgetManager Status Check ===');
    console.log('BudgetManager instance:', window.budgetManager);
    console.log('Static methods available:', {
        isReady: BudgetManager.isReady(),
        getStatus: BudgetManager.getStatus()
    });
    
    if (window.budgetManager) {
        console.log('Instance properties:', {
            categories: window.budgetManager.categories?.length || 0,
            budgets: window.budgetManager.budgets?.length || 0,
            categoriesData: window.budgetManager.categories
        });
    }
    
    // Run troubleshooting
    BudgetManager.troubleshoot();
}

// Function to manually reload categories (can be called from console)
async function reloadCategories() {
    if (!window.budgetManager) {
        console.error('BudgetManager not available');
        return;
    }
    
    try {
        console.log('Reloading categories...');
        await window.budgetManager.loadCategories();
        console.log('Categories reloaded successfully');
    } catch (error) {
        console.error('Failed to reload categories:', error);
    }
}

// Function to debug backend JSON parsing errors
function debugBackendJsonError() {
    console.log('=== Backend JSON Error Debugging ===');
    console.log('🔍 The error you\'re seeing indicates a backend issue:');
    console.log('');
    console.log('❌ PROBLEM: Backend is returning malformed JSON');
    console.log('   - Excessive closing braces: }]}}]}}]}}');
    console.log('   - This suggests circular references in the data model');
    console.log('   - Objects are referencing themselves, causing infinite loops');
    console.log('');
    console.log('🔧 LIKELY CAUSES:');
    console.log('   1. Circular references in JPA entities');
    console.log('   2. Bidirectional relationships not properly managed');
    console.log('   3. Missing @JsonManagedReference/@JsonBackReference annotations');
    console.log('   4. Infinite recursion in toString() or getter methods');
    console.log('');
    console.log('📋 BACKEND FIXES NEEDED:');
    console.log('   1. Check Budget and Category entity relationships');
    console.log('   2. Add @JsonIgnore or @JsonManagedReference annotations');
    console.log('   3. Review toString() methods for circular calls');
    console.log('   4. Check for bidirectional relationships');
    console.log('');
    console.log('🚀 FRONTEND WORKAROUNDS:');
    console.log('   1. Try refreshing the page');
    console.log('   2. Wait a few minutes and try again');
    console.log('   3. Contact backend team with this error');
    console.log('   4. Check browser console for full error details');
    console.log('');
    console.log('📞 NEXT STEPS:');
    console.log('   1. Copy the full error message from console');
    console.log('   2. Send to backend development team');
    console.log('   3. Include the response preview shown in the error');
    console.log('   4. Mention the excessive closing braces issue');
}

// Help function for users encountering the "BudgetManager not initialized" error
function helpWithBudgetManagerError() {
    console.log('=== Help: BudgetManager not initialized error ===');
    console.log('This error typically occurs when:');
    console.log('1. The page is still loading');
    console.log('2. There are JavaScript errors preventing initialization');
    console.log('3. Authentication has failed');
    console.log('4. The API service is not available');
    console.log('');
    console.log('Try these steps:');
    console.log('1. Wait a few seconds and try again');
    console.log('2. Refresh the page');
    console.log('3. Check the console for other errors');
    console.log('4. Make sure you are logged in');
    console.log('5. Run checkBudgetManagerStatus() to see current state');
    console.log('6. Run BudgetManager.troubleshoot() for detailed analysis');
    console.log('7. Run BudgetManager.autoRefreshIfNeeded() to attempt auto-recovery');
    console.log('');
    console.log('If the problem persists, check the browser console for additional error messages.');
}

// Test function for debugging category loading
function testCategoryLoading() {
    console.log('=== Testing Category Loading ===');
    console.log('BudgetManager instance:', budgetManager);
    console.log('Categories loaded:', budgetManager?.categories);
    console.log('Categories length:', budgetManager?.categories?.length);
    
    if (budgetManager) {
        console.log('Testing manual category load...');
        budgetManager.loadCategories().then(() => {
            console.log('Categories loaded successfully:', budgetManager.categories);
            budgetManager.populateCategorySelects();
        }).catch(error => {
            console.error('Failed to load categories:', error);
        });
    } else {
        console.error('BudgetManager not initialized');
    }
}

// Comprehensive debugging function
function debugBudgetSystem() {
    console.log('=== DEBUGGING BUDGET SYSTEM ===');
    
    // Check authentication
    const token = localStorage.getItem(CONFIG.JWT.STORAGE_KEY);
    console.log('JWT Token exists:', !!token);
    console.log('JWT Token value:', token ? token.substring(0, 20) + '...' : 'none');
    
    // Check BudgetManager instance
    console.log('BudgetManager instance:', budgetManager);
    if (budgetManager) {
        console.log('Categories loaded:', budgetManager.categories?.length || 0);
        console.log('Categories data:', budgetManager.categories);
        console.log('Budgets loaded:', budgetManager.budgets?.length || 0);
    }
    
    // Test API service
    console.log('API Service base URL:', apiService.baseUrl);
    
    // Test category loading manually
    if (token) {
        console.log('Testing manual category load...');
        apiService.getUserCategories().then(categories => {
            console.log('Manual API call successful:', categories);
        }).catch(error => {
            console.error('Manual API call failed:', error);
        });
    } else {
        console.log('No token available for API test');
    }
    
    // Check DOM elements
    const categorySelects = document.querySelectorAll('.category-select');
    console.log('Category select elements found:', categorySelects.length);
    categorySelects.forEach((select, index) => {
        console.log(`Select ${index} has ${select.options.length} options:`, 
            Array.from(select.options).map(opt => opt.textContent));
    });
} 

// Simple test function for browser console
function testAuthAndCategories() {
    console.log('=== TESTING AUTHENTICATION AND CATEGORIES ===');
    
    // Check if user is logged in
    const token = localStorage.getItem('authToken'); // Use the same key as auth.js
    const userEmail = localStorage.getItem('userEmail'); // Use the same key as auth.js
    
    console.log('1. Authentication Check:');
    console.log('   - Token exists:', !!token);
    console.log('   - User email:', userEmail);
    
    if (!token) {
        console.log('   ❌ No authentication token found');
        console.log('   💡 Please log in first at index.html');
        return;
    }
    
    console.log('   ✅ Authentication token found');
    
    // Test API call
    console.log('\n2. Testing API Call:');
    const apiUrl = 'http://localhost:8080/api/categories';
    
    fetch(apiUrl, {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        console.log('   - Response status:', response.status);
        console.log('   - Response ok:', response.ok);
        return response.json();
    })
    .then(data => {
        console.log('   ✅ API call successful');
        console.log('   - Categories returned:', data.length);
        console.log('   - Categories data:', data);
        
        // Update the BudgetManager if it exists
        if (window.budgetManager) {
            window.budgetManager.categories = data;
            window.budgetManager.populateCategorySelects();
            console.log('   ✅ Categories loaded into BudgetManager');
        } else {
            console.log('   ⚠️ BudgetManager not found');
        }
    })
    .catch(error => {
        console.error('   ❌ API call failed:', error);
    });
}

// Enhanced test function for debugging
function testCategoryLoadingEnhanced() {
    console.log('=== ENHANCED CATEGORY LOADING TEST ===');
    
    // Check authentication
    const token = localStorage.getItem('authToken'); // Use the same key as auth.js
    console.log('1. Authentication Check:');
    console.log('   - Token exists:', !!token);
    console.log('   - Token value:', token ? token.substring(0, 20) + '...' : 'none');
    
    if (!token) {
        console.log('   ❌ No authentication token found');
        console.log('   💡 Please log in first');
        return;
    }
    
    console.log('   ✅ Authentication token found');
    
    // Check BudgetManager instance
    console.log('\n2. BudgetManager Check:');
    if (window.budgetManager) {
        console.log('   ✅ BudgetManager instance found');
        console.log('   - Categories loaded:', window.budgetManager.categories?.length || 0);
        console.log('   - Categories data:', window.budgetManager.categories);
    } else {
        console.log('   ❌ BudgetManager instance not found');
        return;
    }
    
    // Test API service directly
    console.log('\n3. API Service Test:');
    if (window.apiService) {
        console.log('   ✅ API Service found');
        console.log('   - Base URL:', window.apiService.baseUrl);
        
        // Test getUserCategories
        console.log('   - Testing getUserCategories...');
        window.apiService.getUserCategories()
            .then(categories => {
                console.log('   ✅ getUserCategories successful');
                console.log('   - Categories returned:', categories?.length || 0);
                console.log('   - Categories data:', categories);
                
                // Update BudgetManager
                if (window.budgetManager) {
                    window.budgetManager.categories = categories || [];
                    window.budgetManager.populateCategorySelects();
                    console.log('   ✅ Categories loaded into BudgetManager');
                }
            })
            .catch(error => {
                console.error('   ❌ getUserCategories failed:', error);
            });
    } else {
        console.log('   ❌ API Service not found');
    }
} 

// Comprehensive test function for categories
function testAllCategories() {
    console.log('=== TESTING ALL CATEGORIES ===');
    
    if (!budgetManager) {
        console.error('❌ BudgetManager not available');
        return;
    }
    
    // Test 1: Check if categories are loaded
    console.log('\n1. Checking categories data...');
    const categories = budgetManager.categories;
    console.log('Categories loaded:', !!categories);
    console.log('Categories is array:', Array.isArray(categories));
    console.log('Categories count:', categories?.length || 0);
    
    if (categories && categories.length > 0) {
        console.log('Categories data:', categories);
        
        // Test 2: Verify each category has required fields
        console.log('\n2. Verifying category structure...');
        let validCategories = 0;
        categories.forEach((cat, index) => {
            if (cat && cat.id && cat.name) {
                validCategories++;
                console.log(`✅ Category ${index + 1}: ${cat.name} (ID: ${cat.id})`);
            } else {
                console.error(`❌ Invalid category ${index + 1}:`, cat);
            }
        });
        console.log(`Valid categories: ${validCategories}/${categories.length}`);
        
        // Test 3: Check DOM elements
        console.log('\n3. Checking DOM elements...');
        const categorySelects = document.querySelectorAll('.category-select');
        console.log('Category select elements found:', categorySelects.length);
        
        categorySelects.forEach((select, index) => {
            console.log(`\nSelect ${index}:`);
            console.log('  - Element:', select);
            console.log('  - Options count:', select.options.length);
            console.log('  - Options text:', Array.from(select.options).map(opt => opt.textContent));
            
            // Check if all categories are present
            const expectedOptions = categories.length + 1; // +1 for "Select Category"
            if (select.options.length === expectedOptions) {
                console.log(`  ✅ Select ${index} has correct number of options`);
            } else {
                console.error(`  ❌ Select ${index} has ${select.options.length} options, expected ${expectedOptions}`);
            }
        });
        
        // Test 4: Manual population test
        console.log('\n4. Testing manual population...');
        budgetManager.populateCategorySelects();
        
        // Test 5: Final verification
        console.log('\n5. Final verification...');
        const finalSelects = document.querySelectorAll('.category-select');
        finalSelects.forEach((select, index) => {
            const finalOptions = Array.from(select.options).map(opt => opt.textContent);
            console.log(`Select ${index} final options:`, finalOptions);
            
            if (finalOptions.length === categories.length + 1) {
                console.log(`✅ Select ${index} correctly populated with all categories`);
            } else {
                console.error(`❌ Select ${index} missing categories. Expected ${categories.length + 1}, got ${finalOptions.length}`);
            }
        });
        
    } else {
        console.error('❌ No categories available for testing');
    }
    
    console.log('\n=== CATEGORIES TEST COMPLETE ===');
}

// Simple authentication check function
function checkAuthStatus() {
    console.log('=== AUTHENTICATION STATUS CHECK ===');
    
    const token = localStorage.getItem('authToken'); // Use the same key as auth.js
    const userEmail = localStorage.getItem('userEmail'); // Use the same key as auth.js
    const userName = localStorage.getItem('userName'); // Use the same key as auth.js
    
    console.log('1. Storage Keys:');
    console.log('   - CONFIG.JWT.STORAGE_KEY:', CONFIG.JWT.STORAGE_KEY);
    console.log('   - CONFIG.JWT.USER_EMAIL_KEY:', CONFIG.JWT.USER_EMAIL_KEY);
    console.log('   - CONFIG.JWT.USER_NAME_KEY:', CONFIG.JWT.USER_NAME_KEY);
    
    console.log('\n2. Stored Values:');
    console.log('   - Token exists:', !!token);
    console.log('   - Token value:', token ? token.substring(0, 20) + '...' : 'none');
    console.log('   - User email:', userEmail);
    console.log('   - User name:', userName);
    
    console.log('\n3. BudgetManager Status:');
    if (window.budgetManager) {
        console.log('   - Instance exists:', !!window.budgetManager);
        console.log('   - Categories loaded:', window.budgetManager.categories?.length || 0);
        console.log('   - Categories data:', window.budgetManager.categories);
    } else {
        console.log('   - BudgetManager not found');
    }
    
    console.log('\n4. API Service Status:');
    if (window.apiService) {
        console.log('   - Instance exists:', !!window.apiService);
        console.log('   - Base URL:', window.apiService.baseUrl);
    } else {
        console.log('   - API Service not found');
    }
    
    return {
        hasToken: !!token,
        token: token,
        userEmail: userEmail,
        userName: userName,
        budgetManagerExists: !!window.budgetManager,
        apiServiceExists: !!window.apiService
    };
}

// Test API call directly
async function testCategoriesAPI() {
    console.log('=== TESTING CATEGORIES API DIRECTLY ===');
    
    try {
        // Check if API service is available
        if (!window.apiService) {
            console.error('❌ API Service not available');
            return;
        }
        
        console.log('✅ API Service available');
        console.log('API Service methods:', Object.getOwnPropertyNames(window.apiService));
        
        // Check authentication
        const token = localStorage.getItem('authToken');
        console.log('Auth token exists:', !!token);
        console.log('Token value:', token ? token.substring(0, 20) + '...' : 'none');
        
        if (!token) {
            console.error('❌ No auth token found');
            return;
        }
        
        // Make the API call
        console.log('Making direct API call to getUserCategories...');
        const categories = await apiService.getUserCategories();
        
        console.log('=== DIRECT API RESPONSE ===');
        console.log('Raw response:', categories);
        console.log('Response type:', typeof categories);
        console.log('Response is array:', Array.isArray(categories));
        console.log('Response length:', categories?.length || 0);
        
        if (categories && Array.isArray(categories)) {
            console.log('✅ Valid categories array received');
            console.log('Categories:', categories);
            
            // Check each category
            categories.forEach((cat, index) => {
                console.log(`Category ${index + 1}:`, cat);
                if (cat && cat.id && cat.name) {
                    console.log(`  ✅ Valid: ${cat.name} (${cat.id})`);
                } else {
                    console.error(`  ❌ Invalid:`, cat);
                }
            });
            
            // Store for comparison
            localStorage.setItem('direct_api_categories', JSON.stringify(categories));
            console.log('Categories saved to localStorage as "direct_api_categories"');
            
        } else {
            console.error('❌ Invalid response format:', categories);
        }
        
    } catch (error) {
        console.error('❌ API call failed:', error);
        console.error('Error details:', {
            message: error.message,
            stack: error.stack,
            name: error.name
        });
    }
    
    console.log('=== DIRECT API TEST COMPLETE ===');
}

// Compare localStorage data with BudgetManager data
function compareCategoriesData() {
    console.log('=== COMPARING CATEGORIES DATA ===');
    
    // Get data from localStorage
    const debugCategories = localStorage.getItem('debug_categories');
    const directAPICategories = localStorage.getItem('direct_api_categories');
    
    console.log('1. Categories from localStorage (debug_categories):');
    if (debugCategories) {
        try {
            const parsed = JSON.parse(debugCategories);
            console.log('  - Data:', parsed);
            console.log('  - Count:', parsed?.length || 0);
            console.log('  - Is array:', Array.isArray(parsed));
        } catch (e) {
            console.log('  - Invalid JSON:', debugCategories);
        }
    } else {
        console.log('  - No data found');
    }
    
    console.log('\n2. Categories from localStorage (direct_api_categories):');
    if (directAPICategories) {
        try {
            const parsed = JSON.parse(directAPICategories);
            console.log('  - Data:', parsed);
            console.log('  - Count:', parsed?.length || 0);
            console.log('  - Is array:', Array.isArray(parsed));
        } catch (e) {
            console.log('  - Invalid JSON:', directAPICategories);
        }
    } else {
        console.log('  - No data found');
    }
    
    console.log('\n3. Categories from BudgetManager:');
    if (window.budgetManager) {
        const bmCategories = budgetManager.categories;
        console.log('  - Data:', bmCategories);
        console.log('  - Count:', bmCategories?.length || 0);
        console.log('  - Is array:', Array.isArray(bmCategories));
        
        if (bmCategories && bmCategories.length > 0) {
            console.log('  - Categories:');
            bmCategories.forEach((cat, index) => {
                console.log(`    ${index + 1}. ${cat.name} (${cat.id})`);
            });
        }
    } else {
        console.log('  - BudgetManager not available');
    }
    
    console.log('\n4. Analysis:');
    if (debugCategories && directAPICategories) {
        try {
            const debug = JSON.parse(debugCategories);
            const direct = JSON.parse(directAPICategories);
            
            if (debug.length === direct.length) {
                console.log('  ✅ localStorage data matches direct API data');
            } else {
                console.log(`  ❌ localStorage data count (${debug.length}) != direct API count (${direct.length})`);
            }
        } catch (e) {
            console.log('  ❌ Error comparing localStorage data');
        }
    }
    
    if (window.budgetManager && window.budgetManager.categories) {
        const bmCount = budgetManager.categories.length;
        const directCount = directAPICategories ? JSON.parse(directAPICategories).length : 0;
        
        if (bmCount === directCount) {
            console.log('  ✅ BudgetManager data matches direct API data');
        } else {
            console.log(`  ❌ BudgetManager count (${bmCount}) != direct API count (${directCount})`);
        }
    }
    
    console.log('=== COMPARISON COMPLETE ===');
}

// Recalculate all budget spending for the authenticated user
async function recalculateAllBudgetSpending() {
    try {
        console.log('🔄 Starting budget spending recalculation...');
        
        // Check if API service is available
        if (!window.apiService) {
            console.error('❌ API Service not available!');
            alert('API Service not available. Please refresh the page.');
            return;
        }
        
        // Check authentication
        const token = localStorage.getItem('authToken');
        if (!token) {
            console.error('❌ No authentication token found');
            alert('Please log in to recalculate budget spending.');
            return;
        }
        
        // Show loading message
        const button = document.querySelector('button[onclick="recalculateAllBudgetSpending()"]');
        const originalText = button.innerHTML;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Recalculating...';
        button.disabled = true;
        
        // Call the backend endpoint using API service
        const result = await window.apiService.recalculateAllBudgetSpending();
        
        console.log('✅ Budget spending recalculation successful:', result);
        alert('Budget spending recalculation completed successfully! The spending amounts should now be accurate.');
        
        // Refresh the budgets to show updated spending amounts
        if (window.budgetManager) {
            await window.budgetManager.loadBudgets();
        }
        
    } catch (error) {
        console.error('❌ Error during budget spending recalculation:', error);
        alert(`Error during budget spending recalculation: ${error.message}`);
    } finally {
        // Restore button state
        const button = document.querySelector('button[onclick="recalculateAllBudgetSpending()"]');
        if (button) {
            button.innerHTML = originalText;
            button.disabled = false;
        }
    }
}

// Global debug function for budget data
async function debugBudgetData(budgetId) {
    if (window.budgetManager) {
        await window.budgetManager.debugBudgetData(budgetId);
    } else {
        console.error('BudgetManager not available');
    }
}

// Cleanup invalid budget categories
async function cleanupInvalidCategories() {
    try {
        console.log('🧹 Starting cleanup of invalid budget categories...');
        
        if (!window.apiService) {
            console.error('❌ API Service not available!');
            alert('API Service not available. Please refresh the page.');
            return;
        }
        
        // Check authentication
        const token = localStorage.getItem('authToken');
        if (!token) {
            console.error('❌ No authentication token found');
            alert('Please log in to cleanup invalid categories.');
            return;
        }
        
        // Show confirmation
        if (!confirm('This will remove any budget categories that don\'t belong to your account. This action cannot be undone. Continue?')) {
            return;
        }
        
        // Show loading message
        const button = document.querySelector('button[onclick="cleanupInvalidCategories()"]');
        const originalText = button.innerHTML;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cleaning...';
        button.disabled = true;
        
        // Call the backend endpoint
        const result = await window.apiService.cleanupInvalidBudgetCategories();
        
        console.log('✅ Invalid categories cleanup successful:', result);
        alert('Invalid budget categories cleanup completed successfully! Cross-user categories have been removed.');
        
        // Refresh the budgets to show updated data
        if (window.budgetManager) {
            await window.budgetManager.loadBudgets();
        }
        
    } catch (error) {
        console.error('❌ Error during invalid categories cleanup:', error);
        alert(`Error during invalid categories cleanup: ${error.message}`);
    } finally {
        // Restore button state
        const button = document.querySelector('button[onclick="cleanupInvalidCategories()"]');
        if (button) {
            button.innerHTML = originalText;
            button.disabled = false;
        }
    }
}

