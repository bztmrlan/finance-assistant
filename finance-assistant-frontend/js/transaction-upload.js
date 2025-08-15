// Transaction Upload JavaScript
class TransactionUploadManager {
    constructor() {
        this.selectedFile = null;
        this.uploadInProgress = false;
        this.filePreview = null;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthStatus();
        this.setupDragAndDrop();
    }

    setupEventListeners() {
        // File input change
        const fileInput = document.getElementById('fileInput');
        const fileUploadArea = document.getElementById('fileUploadArea');
        const uploadButton = document.getElementById('uploadButton');

        fileInput.addEventListener('change', (e) => this.handleFileSelect(e));
        fileUploadArea.addEventListener('click', () => fileInput.click());
        uploadButton.addEventListener('click', () => this.handleUpload());

        // Form options change
        document.getElementById('currency').addEventListener('change', () => this.validateForm());
        document.getElementById('dateFormat').addEventListener('change', () => this.validateForm());
        document.getElementById('autoCategorize').addEventListener('change', () => this.validateForm());
        document.getElementById('skipDuplicates').addEventListener('change', () => this.validateForm());

        // Add preview toggle
        const previewToggle = document.getElementById('previewToggle');
        if (previewToggle) {
            previewToggle.addEventListener('click', () => this.togglePreview());
        }
    }

    setupDragAndDrop() {
        const fileUploadArea = document.getElementById('fileUploadArea');
        const fileInput = document.getElementById('fileInput');

        // Prevent default drag behaviors
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            fileUploadArea.addEventListener(eventName, this.preventDefaults, false);
            document.body.addEventListener(eventName, this.preventDefaults, false);
        });

        // Highlight drop area when item is dragged over it
        ['dragenter', 'dragover'].forEach(eventName => {
            fileUploadArea.addEventListener(eventName, () => {
                fileUploadArea.classList.add('dragover');
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            fileUploadArea.addEventListener(eventName, () => {
                fileUploadArea.classList.remove('dragover');
            }, false);
        });

        // Handle dropped files
        fileUploadArea.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;
            
            if (files.length > 0) {
                fileInput.files = files;
                this.handleFileSelect({ target: fileInput });
            }
        }, false);
    }

    preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    checkAuthStatus() {
        const token = localStorage.getItem(CONFIG.JWT.STORAGE_KEY);
        if (!token) {
            this.showMessage('Please log in to upload transactions', 'error');
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
            const response = await apiService.verifyToken();
            if (!response) {
                localStorage.removeItem(CONFIG.JWT.STORAGE_KEY);
                this.showMessage('Session expired. Please log in again.', 'error');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 2000);
            }
        } catch (error) {
            console.error('Token verification failed:', error);
            this.showMessage('Unable to verify authentication. Please try again.', 'error');
        }
    }

    async handleFileSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        // Validate file type
        const allowedTypes = ['.csv', '.xlsx', '.xls'];
        const fileExtension = '.' + file.name.split('.').pop().toLowerCase();
        
        if (!allowedTypes.includes(fileExtension)) {
            this.showMessage('Invalid file type. Please select a CSV or Excel file.', 'error');
            return;
        }

        // Validate file size (max 10MB)
        const maxSize = 10 * 1024 * 1024; // 10MB
        if (file.size > maxSize) {
            this.showMessage('File too large. Maximum size is 10MB.', 'error');
            return;
        }

        this.selectedFile = file;
        this.displayFileInfo(file);
        this.validateForm();
        
        // Generate file preview
        await this.generateFilePreview(file);
        
        // Auto-detect file format if possible
        this.autoDetectFileFormat(file);
    }

    async generateFilePreview(file) {
        try {
            if (file.type === 'text/csv' || file.name.toLowerCase().endsWith('.csv')) {
                const text = await this.readFileAsText(file);
                this.filePreview = this.parseCSVPreview(text);
                this.displayFilePreview();
            } else {
                // For Excel files, we'll show a message that preview is not available
                this.filePreview = null;
                this.hideFilePreview();
            }
        } catch (error) {
            console.error('Error generating file preview:', error);
            this.showMessage('Unable to generate file preview', 'warning');
        }
    }

    readFileAsText(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => resolve(e.target.result);
            reader.onerror = reject;
            reader.readAsText(file);
        });
    }

    parseCSVPreview(text) {
        const lines = text.split('\n').filter(line => line.trim());
        const preview = lines.slice(0, 6); // Show first 6 lines including header
        
        // Parse CSV (simple parsing for preview)
        return preview.map(line => {
            return line.split(',').map(cell => cell.trim().replace(/^"|"$/g, ''));
        });
    }

    displayFilePreview() {
        const previewContainer = document.getElementById('filePreviewContainer');
        if (!previewContainer) return;

        if (!this.filePreview || this.filePreview.length === 0) {
            previewContainer.style.display = 'none';
            return;
        }

        let previewHTML = '<h4>📋 File Preview (First 5 rows)</h4>';
        previewHTML += '<div class="preview-table-container">';
        previewHTML += '<table class="preview-table">';
        
        // Header row
        previewHTML += '<thead><tr>';
        this.filePreview[0].forEach(header => {
            previewHTML += `<th>${header}</th>`;
        });
        previewHTML += '</tr></thead>';
        
        // Data rows
        previewHTML += '<tbody>';
        for (let i = 1; i < this.filePreview.length; i++) {
            previewHTML += '<tr>';
            this.filePreview[i].forEach(cell => {
                previewHTML += `<td>${cell}</td>`;
            });
            previewHTML += '</tr>';
        }
        previewHTML += '</tbody>';
        previewHTML += '</table></div>';

        previewContainer.innerHTML = previewHTML;
        previewContainer.style.display = 'block';
    }

    hideFilePreview() {
        const previewContainer = document.getElementById('filePreviewContainer');
        if (previewContainer) {
            previewContainer.style.display = 'none';
        }
    }

    togglePreview() {
        const previewContainer = document.getElementById('filePreviewContainer');
        if (previewContainer) {
            if (previewContainer.style.display === 'none') {
                this.displayFilePreview();
            } else {
                previewContainer.style.display = 'none';
            }
        }
    }

    autoDetectFileFormat(file) {
        if (this.filePreview && this.filePreview.length > 1) {
            // Try to auto-detect date format from first data row
            const firstDataRow = this.filePreview[1];
            if (firstDataRow.length > 0) {
                const dateValue = firstDataRow[0];
                const detectedFormat = this.detectDateFormat(dateValue);
                if (detectedFormat) {
                    const dateFormatSelect = document.getElementById('dateFormat');
                    if (dateFormatSelect) {
                        dateFormatSelect.value = detectedFormat;
                        this.showMessage(`Auto-detected date format: ${detectedFormat}`, 'info');
                    }
                }
            }

            // Try to auto-detect currency from amount values
            if (firstDataRow.length > 1) {
                const amountValue = firstDataRow[1];
                const detectedCurrency = this.detectCurrency(amountValue);
                if (detectedCurrency) {
                    const currencySelect = document.getElementById('currency');
                    if (currencySelect) {
                        currencySelect.value = detectedCurrency;
                        this.showMessage(`Auto-detected currency: ${detectedCurrency}`, 'info');
                    }
                }
            }
        }
    }

    detectDateFormat(dateString) {
        // Common date format patterns
        const patterns = [
            { pattern: /^\d{4}-\d{2}-\d{2}$/, format: 'yyyy-MM-dd' },
            { pattern: /^\d{2}\/\d{2}\/\d{4}$/, format: 'MM/dd/yyyy' },
            { pattern: /^\d{2}\/\d{2}\/\d{4}$/, format: 'dd/MM/yyyy' },
            { pattern: /^\d{4}\/\d{2}\/\d{2}$/, format: 'yyyy/MM/dd' }
        ];

        for (const { pattern, format } of patterns) {
            if (pattern.test(dateString)) {
                return format;
            }
        }
        return null;
    }

    detectCurrency(amountString) {
        // Common currency symbols
        const currencyMap = {
            '$': 'USD',
            '€': 'EUR',
            '£': 'GBP',
            '¥': 'JPY',
            'C$': 'CAD',
            'A$': 'AUD'
        };

        for (const [symbol, currency] of Object.entries(currencyMap)) {
            if (amountString.includes(symbol)) {
                return currency;
            }
        }
        return null;
    }

    displayFileInfo(file) {
        const fileInfo = document.getElementById('fileInfo');
        const fileName = document.getElementById('fileName');
        const fileSize = document.getElementById('fileSize');

        fileName.textContent = file.name;
        fileSize.textContent = this.formatFileSize(file.size);
        fileInfo.classList.add('show');
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    validateForm() {
        const uploadButton = document.getElementById('uploadButton');
        uploadButton.disabled = !this.selectedFile || this.uploadInProgress;
    }

    async handleUpload() {
        if (!this.selectedFile || this.uploadInProgress) return;

        this.uploadInProgress = true;
        this.showProgress();
        this.validateForm();

        try {
            const formData = new FormData();
            formData.append('file', this.selectedFile);
            formData.append('currency', document.getElementById('currency').value);
            formData.append('autoCategorize', document.getElementById('autoCategorize').checked);
            formData.append('skipDuplicates', document.getElementById('skipDuplicates').checked);
            formData.append('dateFormat', document.getElementById('dateFormat').value);

            const token = localStorage.getItem(CONFIG.JWT.STORAGE_KEY);
            if (!token) {
                throw new Error('No authentication token found');
            }

            // Simulate progress
            this.simulateProgress();

            const result = await apiService.uploadTransactions(formData);
            this.showResults(result);

        } catch (error) {
            console.error('Upload error:', error);
            this.showMessage(`Upload failed: ${error.message}`, 'error');
            this.hideProgress();
        } finally {
            this.uploadInProgress = false;
            this.validateForm();
        }
    }

    simulateProgress() {
        let progress = 0;
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');
        
        const interval = setInterval(() => {
            progress += Math.random() * 15;
            if (progress >= 90) {
                progress = 90;
                clearInterval(interval);
            }
            
            progressFill.style.width = progress + '%';
            progressText.textContent = `Uploading... ${Math.round(progress)}%`;
        }, 200);

        this.progressInterval = interval;
    }

    showProgress() {
        const progressContainer = document.getElementById('progressContainer');
        progressContainer.style.display = 'block';
        
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');
        
        progressFill.style.width = '0%';
        progressText.textContent = 'Preparing upload...';
    }

    hideProgress() {
        const progressContainer = document.getElementById('progressContainer');
        progressContainer.style.display = 'none';
        
        if (this.progressInterval) {
            clearInterval(this.progressInterval);
        }
    }

    showResults(response) {
        this.hideProgress();
        
        const resultsContainer = document.getElementById('resultsContainer');
        const resultsContent = document.getElementById('resultsContent');
        
        // Complete progress to 100%
        const progressFill = document.getElementById('progressFill');
        progressFill.style.width = '100%';
        
        // Build results HTML
        let resultsHTML = '';
        
        // Determine result type
        let resultClass = 'success';
        if (this.hasErrors(response)) {
            resultClass = 'error';
        } else if (response.warnings && response.warnings.length > 0) {
            resultClass = 'warning';
        }

        resultsHTML += `
            <div class="result-card ${resultClass}">
                <h3>${this.getResultTitle(response)}</h3>
                
                <div class="result-stats">
                    <div class="stat-item">
                        <div class="stat-number">${response.totalRows || 0}</div>
                        <div class="stat-label">Total Rows</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-number">${response.successfulTransactions || 0}</div>
                        <div class="stat-label">Successful</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-number">${response.failedTransactions || 0}</div>
                        <div class="stat-label">Failed</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-number">${response.skippedDuplicates || 0}</div>
                        <div class="stat-label">Skipped</div>
                    </div>
                </div>
                
                <p><strong>Processing Time:</strong> ${response.processingTime || 'N/A'}</p>
        `;

        // Show errors if any
        if (this.hasErrors(response)) {
            resultsHTML += `
                <div class="error-list">
                    <h4>❌ Errors (${response.errors ? response.errors.length : 0})</h4>
                    <ul>
                        ${(response.errors || []).map(error => `<li>${error}</li>`).join('')}
                    </ul>
                </div>
            `;
        }

        // Show warnings if any
        if (this.hasWarnings(response)) {
            resultsHTML += `
                <div class="warning-list">
                    <h4>⚠️ Warnings (${response.warnings ? response.warnings.length : 0})</h4>
                    <ul>
                        ${(response.warnings || []).map(warning => `<li>${warning}</li>`).join('')}
                    </ul>
                </div>
            `;
        }

        resultsHTML += '</div>';

        // Add action buttons
        if (response.successfulTransactions > 0) {
            resultsHTML += `
                <div class="result-card success">
                    <h4>🎉 Upload Successful!</h4>
                    <p>${response.successfulTransactions} transactions have been uploaded to your account.</p>
                    <div class="navigation-buttons">
                        <a href="dashboard.html" class="nav-button primary">View Dashboard</a>
                        <button class="nav-button" onclick="location.reload()">Upload Another File</button>
                    </div>
                </div>
            `;
        }

        resultsContent.innerHTML = resultsHTML;
        resultsContainer.style.display = 'block';

        // Scroll to results
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }

    getResultTitle(response) {
        if (this.hasErrors(response)) {
            return '❌ Upload Failed';
        } else if (response.successfulTransactions > 0) {
            return '✅ Upload Completed';
        } else {
            return '⚠️ Upload Completed with Issues';
        }
    }

    hasErrors(response) {
        return response.errors && response.errors.length > 0;
    }

    hasWarnings(response) {
        return response.warnings && response.warnings.length > 0;
    }

    showMessage(message, type = 'info') {
        // Create message element
        const messageDiv = document.createElement('div');
        messageDiv.className = `message message-${type}`;
        messageDiv.textContent = message;
        
        // Add to page
        document.body.appendChild(messageDiv);
        
        // Show message
        setTimeout(() => messageDiv.classList.add('show'), 100);
        
        // Remove message after timeout
        setTimeout(() => {
            messageDiv.classList.remove('show');
            setTimeout(() => messageDiv.remove(), 300);
        }, CONFIG.UI.MESSAGE_TIMEOUT);
    }

    // Utility method to test API endpoints
    async testApiEndpoints() {
        console.log('Testing API endpoints...');
        
        try {
            const results = await apiService.testAllEndpoints();
            console.log('API Test Results:', results);
            
            // Log specific transaction upload results
            if (results.transactionUpload) {
                console.log('Transaction Upload Endpoints:', results.transactionUpload);
            }
        } catch (error) {
            console.error('API testing failed:', error);
        }
    }

    // Load upload history
    async loadUploadHistory() {
        try {
            // This would typically call an API endpoint to get upload history
            // For now, we'll show a placeholder
            const historyContainer = document.getElementById('uploadHistory');
            if (historyContainer) {
                // In a real implementation, you would fetch this from the backend
                // const history = await apiService.getUploadHistory();
                // this.displayUploadHistory(history);
            }
        } catch (error) {
            console.error('Error loading upload history:', error);
        }
    }

    // Display upload history
    displayUploadHistory(history) {
        const historyContainer = document.getElementById('uploadHistory');
        if (!historyContainer) return;

        if (!history || history.length === 0) {
            historyContainer.innerHTML = `
                <div class="history-placeholder">
                    <p>No recent uploads found. Upload your first file to see history here.</p>
                </div>
            `;
            return;
        }

        let historyHTML = '';
        history.forEach(upload => {
            const statusClass = upload.status === 'success' ? 'success' : 
                              upload.status === 'error' ? 'error' : 'warning';
            
            historyHTML += `
                <div class="history-item ${statusClass}">
                    <div class="history-header">
                        <span class="history-date">${upload.uploadDate}</span>
                        <span class="history-status">${upload.status}</span>
                    </div>
                    <div class="history-details">
                        <strong>${upload.fileName}</strong> - ${upload.totalRows} rows
                    </div>
                    <div class="history-stats">
                        <span class="stat">✅ ${upload.successfulTransactions}</span>
                        <span class="stat">❌ ${upload.failedTransactions}</span>
                        <span class="stat">⏭️ ${upload.skippedDuplicates}</span>
                    </div>
                </div>
            `;
        });

        historyContainer.innerHTML = historyHTML;
    }
}

// Global functions for modal
function showTemplateHelp() {
    const modal = document.getElementById('templateHelpModal');
    if (modal) {
        modal.style.display = 'block';
    }
}

function closeTemplateHelp() {
    const modal = document.getElementById('templateHelpModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// Close modal when clicking outside of it
window.onclick = function(event) {
    const modal = document.getElementById('templateHelpModal');
    if (event.target === modal) {
        modal.style.display = 'none';
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.transactionUploadManager = new TransactionUploadManager();
    
    // Load upload history
    window.transactionUploadManager.loadUploadHistory();
    
    // Test API endpoints in development
    if (window.location.hostname === 'localhost') {
        setTimeout(() => {
            window.transactionUploadManager.testApiEndpoints();
        }, 1000);
    }
});

// Add message styles dynamically
const messageStyles = `
    .message {
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 6px;
        color: white;
        font-weight: 600;
        z-index: 1000;
        transform: translateX(100%);
        transition: transform 0.3s ease;
        max-width: 300px;
    }
    
    .message.show {
        transform: translateX(0);
    }
    
    .message-info {
        background: #17a2b8;
    }
    
    .message-success {
        background: #28a745;
    }
    
    .message-error {
        background: #dc3545;
    }
    
    .message-warning {
        background: #ffc107;
        color: #212529;
    }
`;

const styleSheet = document.createElement('style');
styleSheet.textContent = messageStyles;
document.head.appendChild(styleSheet); 