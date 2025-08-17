# Finance Assistant Integration Tests

This directory contains comprehensive integration tests for the Finance Assistant application. These tests verify that all components work together correctly, from the database layer through to the REST API endpoints.

## Test Structure

### 1. FinanceAssistantIntegrationTest.java
**Purpose**: Tests the complete application stack including controllers, services, repositories, and database interactions.

**Key Features**:
- Complete user workflow testing (registration → authentication → budget management)
- End-to-end transaction processing
- Budget management with multiple categories
- Goal progress tracking
- Error handling and validation across layers

**Test Scenarios**:
- `testCompleteUserWorkflow()`: Full user journey from registration to budget management
- `testBudgetManagementIntegration()`: Budget creation, category management, and spending tracking
- `testGoalManagementIntegration()`: Goal creation and progress calculation from transactions
- `testTransactionCategorizationIntegration()`: Transaction processing and categorization
- `testErrorHandlingIntegration()`: Validation and error handling across the stack

### 2. ApiIntegrationTest.java
**Purpose**: Focused testing of REST API endpoints with proper authentication and data flow.

**Key Features**:
- API endpoint testing with MockMvc
- JWT authentication testing
- Request/response validation
- Data consistency verification

**Test Scenarios**:
- `testUserAuthenticationFlow()`: User registration and login
- `testCategoryManagementEndpoints()`: CRUD operations for categories
- `testBudgetManagementEndpoints()`: Budget creation and management
- `testGoalManagementEndpoints()`: Goal creation and progress updates
- `testTransactionManagementEndpoints()`: Transaction upload and retrieval
- `testInsightServiceEndpoints()`: Insight generation and retrieval
- `testRuleEngineEndpoints()`: Rule creation and evaluation
- `testErrorHandlingAndValidation()`: Input validation and error responses
- `testDataConsistencyAcrossEndpoints()`: Data integrity verification

### 3. ServiceLayerIntegrationTest.java
**Purpose**: Tests business logic and service interactions without the web layer.

**Key Features**:
- Service layer integration testing
- Business rule validation
- Data processing workflows
- Transaction management

**Test Scenarios**:
- `testCompleteTransactionProcessingWorkflow()`: End-to-end transaction processing
- `testBudgetManagementWithSpendingLimits()`: Budget limit enforcement
- `testGoalProgressCalculationFromTransactions()`: Goal progress tracking
- `testRuleEngineEvaluationAndAlertGeneration()`: Rule evaluation and alerting
- `testInsightGenerationFromTransactionData()`: Insight generation
- `testCategoryManagementAndTransactionCategorization()`: Category and transaction management
- `testBudgetEvaluationAndStatusUpdates()`: Budget status management
- `testDataConsistencyAndTransactionRollback()`: Data integrity and rollback scenarios

### 4. PerformanceIntegrationTest.java
**Purpose**: Performance and stress testing under various load conditions.

**Key Features**:
- Bulk transaction processing performance
- Concurrent transaction processing
- Database query performance
- Memory usage monitoring
- Scalability testing

**Test Scenarios**:
- `testBulkTransactionUploadPerformance()`: 1000 transaction upload performance
- `testConcurrentTransactionProcessing()`: 10 concurrent threads processing 100 transactions each
- `testBudgetEvaluationPerformance()`: Budget evaluation with 50 categories
- `testRuleEngineEvaluationPerformance()`: Rule evaluation with 100 rules
- `testInsightGenerationPerformance()`: Insight generation with 2000 transactions
- `testDatabaseQueryPerformance()`: Query performance under load
- `testMemoryUsageAndGarbageCollection()`: Memory usage monitoring

## Running the Tests

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- H2 in-memory database (configured in test profile)

### Run All Integration Tests
```bash
mvn test -Dtest="*IntegrationTest"
```

### Run Specific Integration Test Classes
```bash
# Run main integration tests
mvn test -Dtest=FinanceAssistantIntegrationTest

# Run API integration tests
mvn test -Dtest=ApiIntegrationTest

# Run service layer integration tests
mvn test -Dtest=ServiceLayerIntegrationTest

# Run performance integration tests
mvn test -Dtest=PerformanceIntegrationTest
```

### Run Specific Test Methods
```bash
# Run a specific test method
mvn test -Dtest=FinanceAssistantIntegrationTest#testCompleteUserWorkflow

# Run multiple specific test methods
mvn test -Dtest=ApiIntegrationTest#testUserAuthenticationFlow,ApiIntegrationTest#testCategoryManagementEndpoints
```

### Run Tests with Profile
```bash
# Run with test profile (default)
mvn test -Dspring.profiles.active=test

# Run with specific test class and profile
mvn test -Dtest=FinanceAssistantIntegrationTest -Dspring.profiles.active=test
```

## Test Configuration

### Test Profile (application-test.properties)
- Uses H2 in-memory database
- Disables security for testing
- Enables debug logging
- Sets testing-specific configurations

### Test Annotations
- `@SpringBootTest`: Full application context
- `@AutoConfigureWebMvc`: Web layer configuration
- `@ActiveProfiles("test")`: Test profile activation
- `@Transactional`: Transaction management for tests
- `@DisplayName`: Descriptive test names

## Test Data Management

### Setup and Teardown
- `@BeforeEach`: Creates test data before each test
- `@Transactional`: Automatically rolls back changes after each test
- Helper methods for creating test entities

### Test Data Isolation
- Each test runs in its own transaction
- Test data is automatically cleaned up
- No cross-test data contamination

## Performance Benchmarks

### Expected Performance Metrics
- **Transaction Processing**: 1000 transactions in < 10 seconds
- **Budget Evaluation**: 50 categories in < 5 seconds
- **Rule Evaluation**: 100 rules in < 10 seconds
- **Insight Generation**: 2000 transactions in < 15 seconds
- **Database Queries**: User transactions < 1 second, categories < 2 seconds

### Memory Usage
- Expected memory usage: < 100MB for typical test scenarios
- Garbage collection monitoring
- Memory leak detection

## Troubleshooting

### Common Issues
1. **Test Timeout**: Increase timeout values for performance tests
2. **Memory Issues**: Adjust JVM memory settings if needed
3. **Database Connection**: Ensure H2 database is properly configured
4. **Authentication**: Check JWT token generation and validation

### Debug Mode
```bash
# Run with debug logging
mvn test -Dtest=*IntegrationTest -Dlogging.level.com.github.bztmrlan.financeassistant=DEBUG

# Run with verbose Maven output
mvn test -Dtest=*IntegrationTest -X
```

### Test Reports
Test reports are generated in:
- `target/surefire-reports/` - Test execution reports
- `target/test-classes/` - Compiled test classes

## Best Practices

### Test Design
- Each test should be independent and isolated
- Use descriptive test names with `@DisplayName`
- Test both happy path and error scenarios
- Verify data consistency across layers

### Performance Testing
- Set realistic performance thresholds
- Monitor resource usage during tests
- Use appropriate data sizes for testing
- Consider CI/CD environment constraints

### Maintenance
- Keep test data creation methods updated
- Review and update performance benchmarks regularly
- Maintain test coverage for new features
- Document any test-specific configurations

## Integration with CI/CD

These integration tests are designed to run in CI/CD pipelines:

```yaml
# Example GitHub Actions step
- name: Run Integration Tests
  run: mvn test -Dtest="*IntegrationTest" -Dspring.profiles.active=test
```

The tests use in-memory databases and are designed to run quickly while providing comprehensive coverage of the application's integration points. 