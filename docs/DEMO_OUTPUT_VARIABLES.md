# Output Variables Access in Subsequent Rules - Demo

This document demonstrates the enhanced functionality that allows output variables created by a rule execution to be accessed in subsequent rules.

## Key Features

1. **Direct Property Access**: Output variables can be accessed directly by name (e.g., `name_upper`)
2. **Variable Access**: Output variables can also be accessed using the `#` prefix (e.g., `#name_upper`)
3. **Seamless Integration**: Both access methods work seamlessly in the same ruleset

## Example Usage

### Scenario: User Profile Processing

Consider a ruleset that processes user data through multiple rules:

```java
// Rule 1: Convert name to uppercase
Rule rule1 = new Rule();
rule1.setCondition("age >= 18");
rule1.setTransformation("STRING_UPPERCASE(name)");
rule1.setOutputVariable("name_upper");

// Rule 2: Create formal greeting using direct access
Rule rule2 = new Rule();
rule2.setCondition("age >= 21");
rule2.setTransformation("STRING_CONCAT(\"Mr. \", name_upper)");
rule2.setOutputVariable("formal_greeting");

// Rule 3: Create description using variable access
Rule rule3 = new Rule();
rule3.setCondition("age >= 21");
rule3.setTransformation("STRING_CONCAT(\"Greeting: \", #formal_greeting)");
rule3.setOutputVariable("description");
```

### Input Data

```java
Map<String, Object> inputData = Map.of(
    "name", "john",
    "age", 25
);
```

### Expected Output

```java
{
    "name_upper": "JOHN",
    "formal_greeting": "Mr. JOHN",
    "description": "Greeting: Mr. JOHN"
}
```

## Complex Example: Multi-Variable Chain

```java
// Rule 1: Process name
Rule rule1 = new Rule();
rule1.setCondition("age >= 18");
rule1.setTransformation("STRING_UPPERCASE(name)");
rule1.setOutputVariable("name_upper");

// Rule 2: Calculate future age
Rule rule2 = new Rule();
rule2.setCondition("age >= 21");
rule2.setTransformation("age + 10");
rule2.setOutputVariable("future_age");

// Rule 3: Create prediction using both output variables
Rule rule3 = new Rule();
rule3.setCondition("name_upper != null");
rule3.setTransformation("STRING_CONCAT(name_upper, \" will be \", future_age.toString(), \" in 10 years\")");
rule3.setOutputVariable("prediction");
```

### Input Data

```java
Map<String, Object> inputData = Map.of(
    "name", "bob",
    "age", 30
);
```

### Expected Output

```java
{
    "name_upper": "BOB",
    "future_age": 40,
    "prediction": "BOB will be 40 in 10 years"
}
```

## Technical Implementation

The enhancement includes:

1. **PropertyAccessWrapper**: A mutable wrapper that stores both input data and output variables
2. **Custom PropertyAccessor**: Enables SpEL to dynamically access properties from the wrapper
3. **Dual Access Support**: Variables are stored in both the SpEL context (for `#` access) and the wrapper (for direct access)

## Benefits

- **Flexibility**: Choose the access method that fits your expression style
- **Backward Compatibility**: Existing rules using `#variable` syntax continue to work
- **Improved Readability**: Direct access can make expressions more readable
- **Seamless Integration**: No changes needed to existing rule execution logic

## Testing

The functionality is thoroughly tested with:

- Basic output variable access
- Complex rule chains
- Mixed access methods (direct and variable)
- Error handling and edge cases

All tests pass, ensuring reliable functionality for production use.
