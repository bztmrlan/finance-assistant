// Goal Management JavaScript
class GoalManager {
    constructor() {
        this.goals = [];
        this.categories = [];
        this.currentEditingGoal = null;
        this.currentProgressGoal = null;
        
        this.init();
    }

    init() {
        console.log('🎯 GoalManager initializing...');
        
        // Setup event listeners with retry mechanism
        this.setupEventListeners();
        
        // Retry setting up event listeners if forms weren't found
        if (!document.getElementById('progressForm')) {
            console.log('🔍 Progress form not found, retrying in 100ms...');
            setTimeout(() => this.setupEventListeners(), 100);
        }
        
        this.checkAuthStatus();
        this.loadCategories();
        this.loadGoals();
    }

    setupEventListeners() {
        console.log('🔍 Setting up event listeners...');
        
        // Goal creation form
        const goalForm = document.getElementById('goalForm');
        console.log('🔍 Goal form found:', !!goalForm);
        if (goalForm) {
            goalForm.addEventListener('submit', (e) => this.handleCreateGoal(e));
            console.log('✅ Goal form event listener added');
        }

        // Progress update form
        const progressForm = document.getElementById('progressForm');
        console.log('🔍 Progress form found:', !!progressForm);
        if (progressForm) {
            progressForm.addEventListener('submit', (e) => this.handleUpdateProgress(e));
            console.log('✅ Progress form event listener added');
        } else {
            console.error('❌ Progress form not found during setup');
        }

        // Edit goal form
        const editForm = document.getElementById('editForm');
        console.log('🔍 Edit form found:', !!editForm);
        if (editForm) {
            editForm.addEventListener('submit', (e) => this.handleEditGoal(e));
            console.log('✅ Edit form event listener added');
        }

        // Set default target date to 1 year from now
        const targetDateInput = document.getElementById('targetDate');
        if (targetDateInput) {
            const oneYearFromNow = new Date();
            oneYearFromNow.setFullYear(oneYearFromNow.getFullYear() + 1);
            targetDateInput.value = oneYearFromNow.toISOString().split('T')[0];
        }
        
        console.log('🔍 Event listeners setup complete');
    }

    async checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        if (!token) {
            console.log('❌ No auth token found, redirecting to login');
            window.location.href = 'index.html';
            return;
        }

        try {
            await window.apiService.verifyToken();
            console.log('✅ Authentication verified');
        } catch (error) {
            console.error('❌ Authentication failed:', error);
            localStorage.removeItem('authToken');
            window.location.href = 'index.html';
        }
    }

    async loadCategories() {
        try {
            console.log('📂 Loading categories...');
            this.categories = await window.apiService.getUserCategories();
            console.log('✅ Categories loaded:', this.categories.length);
            this.populateCategorySelects();
        } catch (error) {
            console.error('❌ Error loading categories:', error);
            this.showMessage('Error loading categories: ' + error.message, 'error');
        }
    }

    populateCategorySelects() {
        // Populate create goal form category select
        const categorySelect = document.getElementById('categoryId');
        const editCategorySelect = document.getElementById('editCategoryId');
        
        if (categorySelect) {
            categorySelect.innerHTML = '<option value="">Select a category</option>';
            this.categories.forEach(category => {
                const option = document.createElement('option');
                option.value = category.id;
                option.textContent = category.name;
                categorySelect.appendChild(option);
            });
        }

        if (editCategorySelect) {
            editCategorySelect.innerHTML = '<option value="">Select a category</option>';
            this.categories.forEach(category => {
                const option = document.createElement('option');
                option.value = category.id;
                option.textContent = category.name;
                editCategorySelect.appendChild(option);
            });
        }
    }

    async loadGoals() {
        try {
            console.log('🎯 Loading goals...');
            this.goals = await window.apiService.getUserGoals();
            console.log('✅ Goals loaded:', this.goals.length);
            this.updateStats();
            this.displayGoals();
        } catch (error) {
            console.error('❌ Error loading goals:', error);
            this.showMessage('Error loading goals: ' + error.message, 'error');
            this.showEmptyState();
        }
    }

    updateStats() {
        const totalGoals = this.goals.length;
        const completedGoals = this.goals.filter(goal => goal.completed).length;
        const activeGoals = this.goals.filter(goal => !goal.completed).length;
        const overdueGoals = this.goals.filter(goal => {
            if (goal.completed) return false;
            const targetDate = new Date(goal.targetDate);
            const today = new Date();
            return targetDate < today;
        }).length;

        document.getElementById('totalGoals').textContent = totalGoals;
        document.getElementById('completedGoals').textContent = completedGoals;
        document.getElementById('activeGoals').textContent = activeGoals;
        document.getElementById('overdueGoals').textContent = overdueGoals;
    }

    displayGoals() {
        const goalsList = document.getElementById('goalsList');
        
        if (!goalsList) return;

        if (this.goals.length === 0) {
            this.showEmptyState();
            return;
        }

        goalsList.innerHTML = '';
        
        // Sort goals: completed last, overdue first
        const sortedGoals = [...this.goals].sort((a, b) => {
            if (a.completed && !b.completed) return 1;
            if (!a.completed && b.completed) return -1;
            if (a.completed && b.completed) return 0;
            
            // For active goals, sort by deadline (earliest first)
            const aDate = new Date(a.targetDate);
            const bDate = new Date(b.targetDate);
            return aDate - bDate;
        });

        sortedGoals.forEach(goal => {
            const goalCard = this.createGoalCard(goal);
            goalsList.appendChild(goalCard);
        });
    }

    createGoalCard(goal) {
        const card = document.createElement('div');
        card.className = 'goal-card';
        
        // Add special classes for completed and overdue goals
        if (goal.completed) {
            card.classList.add('completed-goal');
        } else {
            const targetDate = new Date(goal.targetDate);
            const today = new Date();
            if (targetDate < today) {
                card.classList.add('overdue-goal');
            }
        }

        const progressPercentage = goal.progressPercentage || 0;
        const progressClass = this.getProgressClass(progressPercentage);
        
        const daysRemaining = this.calculateDaysRemaining(goal.targetDate);
        const daysText = daysRemaining > 0 ? `${daysRemaining} days left` : 
                        daysRemaining === 0 ? 'Due today!' : 
                        `${Math.abs(daysRemaining)} days overdue`;

        card.innerHTML = `
            <div class="goal-header">
                <h3 class="goal-title">${goal.name}</h3>
                <span class="goal-category">${goal.categoryName || 'Uncategorized'}</span>
            </div>
            
            <div class="goal-progress">
                <div class="progress-bar">
                    <div class="progress-fill ${progressClass}" style="width: ${Math.min(progressPercentage, 100)}%"></div>
                </div>
                <div class="progress-text">
                    <span>${goal.currentAmount || 0} / ${goal.targetAmount} ${goal.currency}</span>
                    <span>${Math.round(progressPercentage)}%</span>
                </div>
            </div>
            
            <div class="goal-details">
                <div class="detail-item">
                    <div class="detail-label">Target Date</div>
                    <div class="detail-value">${this.formatDate(goal.targetDate)}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">Days Remaining</div>
                    <div class="detail-value">${daysText}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">Status</div>
                    <div class="detail-value">${goal.completed ? 'Completed' : 'Active'}</div>
                </div>
            </div>
            
            <div class="goal-actions">
                <button onclick="goalManager.openProgressModal('${goal.id}')" class="action-btn success">
                    <i class="fas fa-plus"></i> Add Progress
                </button>
                <button onclick="goalManager.openEditModal('${goal.id}')" class="action-btn primary">
                    <i class="fas fa-edit"></i> Edit
                </button>
                <button onclick="goalManager.deleteGoal('${goal.id}')" class="action-btn danger">
                    <i class="fas fa-trash"></i> Delete
                </button>
            </div>
        `;

        return card;
    }

    getProgressClass(percentage) {
        if (percentage >= 80) return 'success';
        if (percentage >= 50) return 'warning';
        return 'danger';
    }

    calculateDaysRemaining(targetDate) {
        const target = new Date(targetDate);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        target.setHours(0, 0, 0, 0);
        
        const diffTime = target.getTime() - today.getTime();
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        return diffDays;
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    showEmptyState() {
        const goalsList = document.getElementById('goalsList');
        if (goalsList) {
            goalsList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-bullseye"></i>
                    <h3>No goals yet</h3>
                    <p>Create your first financial goal to get started on your journey to financial success!</p>
                </div>
            `;
        }
    }

    async handleCreateGoal(event) {
        event.preventDefault();
        
        const formData = new FormData(event.target);
        const goalData = {
            name: formData.get('name'),
            targetAmount: parseFloat(formData.get('targetAmount')),
            targetDate: formData.get('targetDate'),
            categoryId: formData.get('categoryId'),
            currency: formData.get('currency')
        };

        try {
            console.log('🎯 Creating goal:', goalData);
            const newGoal = await window.apiService.createGoal(goalData);
            console.log('✅ Goal created:', newGoal);
            
            this.showMessage('Goal created successfully!', 'success');
            this.resetForm();
            await this.loadGoals();
        } catch (error) {
            console.error('❌ Error creating goal:', error);
            this.showMessage('Error creating goal: ' + error.message, 'error');
        }
    }

    resetForm() {
        const form = document.getElementById('goalForm');
        if (form) {
            form.reset();
            // Reset target date to 1 year from now
            const targetDateInput = document.getElementById('targetDate');
            if (targetDateInput) {
                const oneYearFromNow = new Date();
                oneYearFromNow.setFullYear(oneYearFromNow.getFullYear() + 1);
                targetDateInput.value = oneYearFromNow.toISOString().split('T')[0];
            }
        }
    }

    openProgressModal(goalId) {
        console.log('🔍 openProgressModal called with goalId:', goalId);
        console.log('🔍 Current progress goal before:', this.currentProgressGoal);
        
        this.currentProgressGoal = goalId;
        console.log('🔍 Current progress goal after:', this.currentProgressGoal);
        
        const modal = document.getElementById('progressModal');
        console.log('🔍 Progress modal element:', modal);
        
        if (modal) {
            modal.style.display = 'block';
            console.log('🔍 Modal displayed');
            
            const amountInput = document.getElementById('progressAmount');
            console.log('🔍 Amount input element:', amountInput);
            if (amountInput) {
                amountInput.focus();
                console.log('🔍 Amount input focused');
            } else {
                console.error('❌ Amount input not found');
            }
        } else {
            console.error('❌ Progress modal not found');
        }
    }

    closeProgressModal() {
        this.currentProgressGoal = null;
        const modal = document.getElementById('progressModal');
        if (modal) {
            modal.style.display = 'none';
            document.getElementById('progressForm').reset();
        }
    }

    async handleUpdateProgress(event) {
        event.preventDefault();
        
        console.log('🔍 handleUpdateProgress called');
        console.log('🔍 currentProgressGoal:', this.currentProgressGoal);
        
        if (!this.currentProgressGoal) {
            console.error('❌ No current progress goal set');
            this.showMessage('Error: No goal selected for progress update', 'error');
            return;
        }
        
        const formData = new FormData(event.target);
        const amount = parseFloat(formData.get('amount'));
        
        console.log('🔍 Form data amount:', amount);
        console.log('🔍 Amount type:', typeof amount);
        console.log('🔍 Amount is NaN:', isNaN(amount));
        
        if (isNaN(amount) || amount <= 0) {
            console.error('❌ Invalid amount:', amount);
            this.showMessage('Error: Please enter a valid positive amount', 'error');
            return;
        }
        
        try {
            console.log('📈 Updating progress for goal:', this.currentProgressGoal, 'Amount:', amount);
            console.log('🔍 API Service available:', !!window.apiService);
            console.log('🔍 API Service methods:', Object.keys(window.apiService || {}));
            
            const result = await window.apiService.updateGoalProgress(this.currentProgressGoal, amount);
            console.log('✅ Progress updated successfully, result:', result);
            
            this.showMessage('Progress updated successfully!', 'success');
            this.closeProgressModal();
            await this.loadGoals();
        } catch (error) {
            console.error('❌ Error updating progress:', error);
            console.error('❌ Error details:', {
                message: error.message,
                stack: error.stack,
                name: error.name
            });
            this.showMessage('Error updating progress: ' + error.message, 'error');
        }
    }

    openEditModal(goalId) {
        const goal = this.goals.find(g => g.id === goalId);
        if (!goal) return;
        
        this.currentEditingGoal = goal;
        
        // Populate edit form
        document.getElementById('editGoalName').value = goal.name;
        document.getElementById('editTargetAmount').value = goal.targetAmount;
        document.getElementById('editTargetDate').value = goal.targetDate;
        document.getElementById('editCategoryId').value = goal.categoryId || '';
        document.getElementById('editCurrency').value = goal.currency;
        
        const modal = document.getElementById('editModal');
        if (modal) {
            modal.style.display = 'block';
        }
    }

    closeEditModal() {
        this.currentEditingGoal = null;
        const modal = document.getElementById('editModal');
        if (modal) {
            modal.style.display = 'none';
            document.getElementById('editForm').reset();
        }
    }

    async handleEditGoal(event) {
        event.preventDefault();
        
        if (!this.currentEditingGoal) return;
        
        const formData = new FormData(event.target);
        const goalData = {
            name: formData.get('name'),
            targetAmount: parseFloat(formData.get('targetAmount')),
            targetDate: formData.get('targetDate'),
            categoryId: formData.get('categoryId'),
            currency: formData.get('currency')
        };

        try {
            console.log('✏️ Updating goal:', this.currentEditingGoal.id, goalData);
            await window.apiService.updateGoal(this.currentEditingGoal.id, goalData);
            console.log('✅ Goal updated successfully');
            
            this.showMessage('Goal updated successfully!', 'success');
            this.closeEditModal();
            await this.loadGoals();
        } catch (error) {
            console.error('❌ Error updating goal:', error);
            this.showMessage('Error updating goal: ' + error.message, 'error');
        }
    }

    async deleteGoal(goalId) {
        if (!confirm('Are you sure you want to delete this goal? This action cannot be undone.')) {
            return;
        }

        try {
            console.log('🗑️ Deleting goal:', goalId);
            await window.apiService.deleteGoal(goalId);
            console.log('✅ Goal deleted successfully');
            
            this.showMessage('Goal deleted successfully!', 'success');
            await this.loadGoals();
        } catch (error) {
            console.error('❌ Error deleting goal:', error);
            this.showMessage('Error deleting goal: ' + error.message, 'error');
        }
    }

    async evaluateAllGoals() {
        try {
            console.log('🔄 Evaluating all goals...');
            await window.apiService.evaluateGoals();
            console.log('✅ Goals evaluated successfully');
            
            this.showMessage('All goals have been evaluated!', 'success');
            await this.loadGoals();
        } catch (error) {
            console.error('❌ Error evaluating goals:', error);
            this.showMessage('Error evaluating goals: ' + error.message, 'error');
        }
    }
    
    async calculateGoalProgressFromTransactions() {
        try {
            console.log('💰 Calculating goal progress from transactions...');
            await window.apiService.calculateGoalProgressFromTransactions();
            console.log('✅ Goal progress calculated successfully');
            
            this.showMessage('Goal progress has been calculated from transactions!', 'success');
            await this.loadGoals();
        } catch (error) {
            console.error('❌ Error calculating goal progress:', error);
            this.showMessage('Error calculating goal progress: ' + error.message, 'error');
        }
    }

    showMessage(message, type = 'info') {
        // Create a simple toast message
        const toast = document.createElement('div');
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 6px;
            color: white;
            font-weight: 500;
            z-index: 10000;
            max-width: 300px;
            word-wrap: break-word;
        `;
        
        switch (type) {
            case 'success':
                toast.style.background = '#38a169';
                break;
            case 'error':
                toast.style.background = '#e53e3e';
                break;
            case 'warning':
                toast.style.background = '#ed8936';
                break;
            default:
                toast.style.background = '#3182ce';
        }
        
        toast.textContent = message;
        document.body.appendChild(toast);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 5000);
    }
}

// Global functions for HTML onclick handlers
function resetForm() {
    if (window.goalManager) {
        window.goalManager.resetForm();
    }
}

function openProgressModal(goalId) {
    console.log('🔍 Global openProgressModal called with goalId:', goalId);
    console.log('🔍 window.goalManager available:', !!window.goalManager);
    
    if (window.goalManager) {
        console.log('🔍 Calling goalManager.openProgressModal');
        window.goalManager.openProgressModal(goalId);
    } else {
        console.error('❌ GoalManager not available');
    }
}

function closeProgressModal() {
    if (window.goalManager) {
        window.goalManager.closeProgressModal();
    }
}

function openEditModal(goalId) {
    if (window.goalManager) {
        window.goalManager.openEditModal(goalId);
    }
}

function closeEditModal() {
    if (window.goalManager) {
        window.goalManager.closeEditModal();
    }
}

function deleteGoal(goalId) {
    if (window.goalManager) {
        window.goalManager.deleteGoal(goalId);
    }
}

function evaluateAllGoals() {
    if (window.goalManager) {
        window.goalManager.evaluateAllGoals();
    }
}

function calculateGoalProgressFromTransactions() {
    if (window.goalManager) {
        window.goalManager.calculateGoalProgressFromTransactions();
    }
}

function logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
    window.location.href = 'index.html';
}

// Close modals when clicking outside
window.onclick = function(event) {
    const progressModal = document.getElementById('progressModal');
    const editModal = document.getElementById('editModal');
    
    if (event.target === progressModal) {
        closeProgressModal();
    }
    if (event.target === editModal) {
        closeEditModal();
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('🎯 DOM loaded, initializing GoalManager...');
    
    // Wait for API service to be available
    const initGoalManager = () => {
        if (window.apiService) {
            console.log('✅ API Service available, creating GoalManager');
            window.goalManager = new GoalManager();
        } else {
            console.log('⏳ API Service not ready yet, retrying...');
            setTimeout(initGoalManager, 100);
        }
    };
    
    initGoalManager();
}); 