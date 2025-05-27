# SPEL Rules Engine

A powerful, flexible Spring Boot-based rules engine that uses Spring Expression Language (SpEL) for dynamic rule evaluation. This engine allows you to create, manage, and execute complex business rules with support for nested JSON objects, output variable chaining, and comprehensive API management.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Rule Syntax](#rule-syntax)
- [Advanced Features](#advanced-features)
- [Configuration](#configuration)
- [Development](#development)
- [Testing](#testing)
- [Examples](#examples)

## Features

### Core Functionality

- **Dynamic Rule Creation**: Create and manage rules dynamically through REST APIs
- **SpEL-based Evaluation**: Leverage the power of Spring Expression Language for complex expressions
- **Ruleset Management**: Organize rules into named rulesets for better organization
- **Persistent Storage**: Rules are stored in H2 database with JPA/Hibernate
- **RESTful API**: Complete REST API for rule and ruleset management

### Advanced Capabilities

- **Nested JSON Support**: Access deeply nested properties using dot notation (`user.profile.name`)
- **Output Variable Chaining**: Use output from previous rules as input for subsequent rules
- **Null-Safe Evaluation**: Graceful handling of missing properties and null values
- **Array and Collection Support**: Work with arrays, lists, and complex data structures
- **Custom Functions**: Built-in string manipulation and utility functions
- **Comprehensive Logging**: Detailed execution logging for debugging and monitoring

### Developer Experience

- **OpenAPI Documentation**: Auto-generated API documentation with Swagger UI
- **H2 Console**: Built-in database console for development and debugging
- **Docker Support**: Easy containerization and deployment
- **IDE Integration**: Full IDE support with proper error handling and validation

## Technology Stack

- **Java 17**: Modern Java features and performance
- **Spring Boot 3.2.3**: Latest Spring Boot framework
- **Spring Data JPA**: Database access and entity management
- **H2 Database**: Embedded database for development and testing
- **Spring Expression Language (SpEL)**: Rule evaluation engine
- **SpringDoc OpenAPI**: API documentation generation
- **SLF4J + Logback**: Comprehensive logging
- **Gradle**: Build automation and dependency management

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 7.0+ (or use included wrapper)

### Installation

1. **Clone the repository**:

   ```bash
   git clone https://github.com/your-username/spel_rules.git
   cd spel_rules
   ```

2. **Build the application**:

   ```bash
   ./gradlew build
   ```

3. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`.

### Quick Start

1. **Access the API Documentation**:
   Open `http://localhost:8080/swagger-ui.html` to explore the API

2. **Access H2 Console** (Development):
   Open `http://localhost:8080/h2-console`

   - JDBC URL: `jdbc:h2:file:./rulesdb`
   - Username: `sa`
   - Password: `password`

3. **Create your first ruleset**:
   ```bash
   curl -X POST http://localhost:8080/api/rulesets \
     -H "Content-Type: application/json" \
     -d '{
       "name": "user-validation",
       "rules": [
         {
           "rule": "age >= 18 THEN STRING_UPPERCASE(name)",
           "outputVariable": "adult_name"
         }
       ]
     }'
   ```

## API Documentation

### Endpoints

#### Ruleset Management

- **`POST /api/rulesets`** - Create a new ruleset
- **`GET /api/rulesets`** - List all rulesets
- **`GET /api/rulesets/{name}`** - Get a specific ruleset
- **`POST /api/rulesets/{name}/rules`** - Add a rule to an existing ruleset
- **`POST /api/rulesets/execute`** - Execute a ruleset

### Request/Response Examples

#### Creating a Ruleset

```http
POST /api/rulesets
Content-Type: application/json

{
  "name": "user-processing",
  "rules": [
    {
      "rule": "age >= 18 THEN STRING_UPPERCASE(name)",
      "outputVariable": "adult_name"
    },
    {
      "rule": "adult_name != null THEN STRING_CONCAT('Hello, ', adult_name)",
      "outputVariable": "greeting"
    }
  ]
}
```

#### Executing a Ruleset

```http
POST /api/rulesets/execute
Content-Type: application/json

{
  "rulesetName": "user-processing",
  "inputData": {
    "name": "john",
    "age": 25
  }
}
```

**Response**:

```json
{
	"outputVariables": {
		"adult_name": "JOHN",
		"greeting": "Hello, JOHN"
	},
	"stats": {
		"totalRules": 2,
		"outputVariablesGenerated": 2
	}
}
```

## Rule Syntax

### Basic Structure

Rules follow the pattern: `CONDITION THEN TRANSFORMATION`

```
age >= 18 THEN STRING_UPPERCASE(name)
```

### Supported Operators

- **Comparison**: `==`, `!=`, `<`, `<=`, `>`, `>=`
- **Logical**: `AND`, `OR`, `NOT`
- **Arithmetic**: `+`, `-`, `*`, `/`, `%`
- **String**: `matches` (regex), `contains`, `startsWith`, `endsWith`

### Built-in Functions

#### String Functions

- `STRING_UPPERCASE(str)` - Convert to uppercase
- `STRING_LOWERCASE(str)` - Convert to lowercase
- `STRING_CONCAT(str1, str2, ...)` - Concatenate strings
- `STRING_SUBSTRING(str, start, end)` - Extract substring
- `STRING_LENGTH(str)` - Get string length
- `STRING_REPLACE(str, old, new)` - Replace text

#### Utility Functions

- `MATH_MAX(a, b)` - Maximum of two numbers
- `MATH_MIN(a, b)` - Minimum of two numbers
- `MATH_ABS(number)` - Absolute value

### Example Rules

```java
// Simple condition
"age >= 18 THEN 'adult'"

// String manipulation
"name != null THEN STRING_UPPERCASE(name)"

// Complex conditions
"age >= 21 AND country == 'USA' THEN 'eligible_voter'"

// Using output variables
"adult_status == 'adult' THEN STRING_CONCAT('Welcome, ', name)"

// Nested property access
"user.profile.age >= 18 THEN user.profile.name"

// Array operations
"scores.size() > 0 THEN scores[0]"
```

## Advanced Features

### Nested JSON Support

The engine supports complex nested JSON structures with dot notation access:

```json
{
	"user": {
		"profile": {
			"name": "Alice",
			"age": 30,
			"address": {
				"city": "New York",
				"country": "USA"
			}
		},
		"preferences": {
			"notifications": true,
			"theme": "dark"
		}
	}
}
```

**Rules can access nested properties**:

```java
// Access nested properties
"user.profile.age >= 18 THEN user.profile.name"

// Complex nested conditions
"user.profile.address.country == 'USA' AND user.preferences.notifications == true THEN 'send_notification'"

// Array access in nested structures
"user.profile.skills[0] == 'Java' THEN 'java_developer'"
```

### Output Variable Chaining

Rules can use outputs from previous rules as inputs:

```java
// Rule 1: Process name
"age >= 18 THEN STRING_UPPERCASE(name)" → adult_name

// Rule 2: Use previous output (direct access)
"adult_name != null THEN STRING_CONCAT('Mr. ', adult_name)" → formal_greeting

// Rule 3: Use previous outputs (variable access)
"age >= 21 THEN STRING_CONCAT('Greeting: ', #formal_greeting)" → description
```

### Null-Safe Evaluation

The engine gracefully handles missing properties and null values:

```java
// These won't crash if properties are missing
"user.profile.age >= 18 THEN 'adult'"           // Returns false if user.profile is null
"address?.city == 'NYC' THEN 'new_yorker'"     // Safe navigation operator
```

### Custom Property Access

The engine provides flexible property access through custom wrappers:

- **Direct Access**: `property.nested.value`
- **Variable Access**: `#outputVariable`
- **Mixed Access**: Combine both in the same expression

## Configuration

### Application Properties

```properties
# Application
spring.application.name=spel_rules

# Database
spring.datasource.url=jdbc:h2:file:./rulesdb
spring.datasource.username=sa
spring.datasource.password=password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 Console (Development only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Logging
logging.level.com.rules.service=INFO
logging.level.org.hibernate.SQL=DEBUG
```

### Production Configuration

For production deployment, consider:

1. **Database**: Replace H2 with PostgreSQL/MySQL
2. **Security**: Add authentication and authorization
3. **Monitoring**: Configure metrics and health checks
4. **Logging**: Use structured logging with log aggregation

```yaml
spring:
  profiles:
    active: production
  datasource:
    url: jdbc:postgresql://localhost:5432/rulesdb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  h2:
    console:
      enabled: false
```

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/rules/service/
│   │   ├── controller/          # REST Controllers
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── model/               # JPA Entities
│   │   ├── repository/          # Data Repositories
│   │   ├── service/             # Business Logic
│   │   └── RulesServiceApplication.java
│   └── resources/
│       ├── application.properties
│       ├── logback-spring.xml
│       └── static/
└── test/                        # Test files
```

### Key Components

#### 1. Rule Entity (`model/Rule.java`)

- Stores rule definition with condition, transformation, and output variable
- Uses UUID as primary key
- Organized by ruleset name

#### 2. Rule Execution Service (`service/RuleExecutionService.java`)

- Core engine for rule evaluation
- Handles SpEL expression parsing and execution
- Manages rule chaining and output variables
- Provides null-safe evaluation

#### 3. Rule Parser Service (`service/RuleParserService.java`)

- Parses rule strings into condition and transformation parts
- Validates rule syntax
- Separates concerns between parsing and execution

#### 4. Property Access Wrapper (`service/PropertyAccessWrapper.java`)

- Enables flexible property access in SpEL expressions
- Supports both input data and output variables
- Handles nested object navigation

#### 5. SpEL Context Configuration (`service/SpelContextConfigurationService.java`)

- Configures SpEL evaluation context
- Registers custom functions
- Manages variable scoping

### Building and Testing

```bash
# Run tests
./gradlew test

# Build JAR
./gradlew bootJar

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Generate test report
./gradlew test jacocoTestReport
```

### Adding Custom Functions

To add custom functions to the rule engine:

1. **Create function in `SpelFunctionUtils.java`**:

   ```java
   public static String customFunction(String input) {
       return "processed_" + input;
   }
   ```

2. **Register in `SpelContextConfigurationService.java`**:

   ```java
   context.registerFunction("CUSTOM_FUNC",
       SpelFunctionUtils.class.getDeclaredMethod("customFunction", String.class));
   ```

3. **Use in rules**:
   ```java
   "name != null THEN CUSTOM_FUNC(name)"
   ```

## Testing

The project includes comprehensive tests:

### Unit Tests

- Rule parsing and validation
- SpEL expression evaluation
- Property access mechanisms
- Custom function behavior

### Integration Tests

- Full ruleset execution
- API endpoint functionality
- Database operations
- Error handling scenarios

### Example Test Cases

```java
@Test
void testNestedPropertyAccess() {
    Map<String, Object> input = Map.of(
        "user", Map.of(
            "profile", Map.of("age", 25)
        )
    );

    String rule = "user.profile.age >= 18 THEN 'adult'";
    // Test execution...
}

@Test
void testOutputVariableChaining() {
    // Test multiple rules using each other's outputs
}
```

## Examples

### Complete Use Cases

#### 1. User Eligibility System

```java
// Ruleset: user-eligibility
[
  {
    "rule": "age >= 18 THEN 'adult'",
    "outputVariable": "status"
  },
  {
    "rule": "status == 'adult' AND country == 'USA' THEN 'eligible'",
    "outputVariable": "voting_eligibility"
  },
  {
    "rule": "voting_eligibility == 'eligible' THEN STRING_CONCAT('Welcome voter: ', name)",
    "outputVariable": "welcome_message"
  }
]
```

#### 2. E-commerce Pricing System

```java
// Ruleset: pricing-rules
[
  {
    "rule": "customer.type == 'premium' THEN price * 0.9",
    "outputVariable": "discounted_price"
  },
  {
    "rule": "order.quantity >= 10 THEN discounted_price * 0.95",
    "outputVariable": "bulk_discount_price"
  },
  {
    "rule": "customer.loyaltyPoints >= 1000 THEN bulk_discount_price - 50",
    "outputVariable": "final_price"
  }
]
```

#### 3. Complex Data Processing

```java
// Input:
{
  "company": {
    "employees": [
      {
        "name": "Alice",
        "department": "Engineering",
        "salary": 100000,
        "performance": {
          "rating": 4.5,
          "projects": ["ProjectA", "ProjectB"]
        }
      }
    ]
  }
}

// Rules:
[
  {
    "rule": "company.employees[0].performance.rating >= 4.0 THEN company.employees[0].salary * 1.1",
    "outputVariable": "bonus_salary"
  },
  {
    "rule": "company.employees[0].performance.projects.size() >= 2 THEN 'high_performer'",
    "outputVariable": "performance_category"
  }
]
```

---

**Built with ❤️ using Spring Boot and SpEL**
