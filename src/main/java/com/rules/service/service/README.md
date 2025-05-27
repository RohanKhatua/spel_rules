# Rule Execution Service - Refactored Architecture

This package contains the refactored rule execution service, which has been broken down into smaller, more focused classes following SOLID principles.

## Architecture Overview

### Core Components

#### 1. `RuleExecutionService.java`

- **Purpose**: Main service for executing rule sets against input data
- **Responsibilities**:
  - Orchestrates rule execution flow
  - Handles transaction management
  - Manages logging and error handling
- **Dependencies**: `RuleRepository`, `SpelContextConfigurationService`

#### 2. `PropertyAccessWrapper.java`

- **Purpose**: Wrapper class that makes Map properties accessible as object properties in SpEL
- **Responsibilities**:
  - Provides dynamic property access with dot notation
  - Supports nested object navigation
  - Handles array/list index access (e.g., `users[0].name`)
  - Stores and manages output variables from previous rules
  - Delegates to `SpelFunctionUtils` for string functions

#### 3. `SpelContextConfigurationService.java`

- **Purpose**: Service responsible for configuring SpEL evaluation contexts
- **Responsibilities**:
  - Creates and configures `StandardEvaluationContext`
  - Registers custom property accessors
  - Registers SpEL functions
  - Manages context variables and output variables

#### 4. `PropertyAccessWrapperAccessor.java`

- **Purpose**: Custom PropertyAccessor for `PropertyAccessWrapper` objects
- **Responsibilities**:
  - Enables dynamic property access on `PropertyAccessWrapper` instances
  - Integrates with Spring's SpEL evaluation context

#### 5. `NestedMapPropertyAccessor.java`

- **Purpose**: Custom PropertyAccessor for nested Map structures
- **Responsibilities**:
  - Allows SpEL to navigate nested Map structures using dot notation
  - Provides direct Map property access

#### 6. `SpelFunctionUtils.java`

- **Purpose**: Utility class containing SpEL functions for rule evaluation
- **Responsibilities**:
  - String manipulation functions (uppercase, lowercase, substring, etc.)
  - String validation functions (contains, startsWith, endsWith)
  - Utility functions for common operations

## Key Improvements

### 1. **Single Responsibility Principle**

- Each class now has a single, well-defined responsibility
- Separation of concerns between rule execution, SpEL configuration, and property access

### 2. **Improved Maintainability**

- Smaller, focused classes are easier to understand and modify
- Clear separation between different aspects of functionality

### 3. **Better Testability**

- Individual components can be unit tested in isolation
- Dependencies are injected, making mocking easier

### 4. **Enhanced Extensibility**

- New SpEL functions can be easily added to `SpelFunctionUtils`
- New property accessors can be added without modifying existing code
- Rule execution logic can be extended without affecting other components

### 5. **Cleaner Code Organization**

- Related functionality is grouped together
- Reduced code duplication
- Better naming and documentation

## Usage Example

```java
@Autowired
private RuleExecutionService ruleExecutionService;

// Execute a ruleset
Map<String, Object> inputData = Map.of(
    "age", 25,
    "name", "John Doe",
    "user", Map.of("profile", Map.of("status", "active"))
);

Map<String, Object> results = ruleExecutionService.executeRuleset("user-validation", inputData);
```

## Property Access Patterns

The refactored system supports multiple property access patterns:

### 1. Direct Property Access

```spel
age > 18
name != null
```

### 2. Variable Access (Legacy)

```spel
#age > 18
#name != null
```

### 3. Nested Property Access

```spel
user.profile.status == 'active'
```

### 4. Array/List Access

```spel
users[0].name
company.employees[1].profile.title
```

### 5. Function Calls

```spel
STRING_UPPERCASE(name)
STRING_CONTAINS(email, '@')
```

## Configuration

The `SpelContextConfigurationService` automatically:

- Registers all custom property accessors
- Registers all SpEL functions from `SpelFunctionUtils`
- Sets up the evaluation context with input data
- Manages output variables from rule execution

## Error Handling

The service supports null-safe evaluation, which can be configured per ruleset execution:

- Null property access in conditions defaults to `false`
- Null property access in transformations returns `null`
- Detailed logging for debugging and monitoring
