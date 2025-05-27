# Nested JSON Object Support

This document describes the enhanced functionality that allows your rules engine to accept nested JSON objects as input and access their properties using standard dot notation.

## Overview

The rules engine now supports nested JSON structures in the input data, allowing you to write rules that can access deeply nested properties using standard dot notation syntax (e.g., `user.profile.name`, `address.city`).

## Features

- **Nested Property Access**: Access properties at any depth using dot notation
- **Null Safety**: Graceful handling of missing properties or null objects
- **Mixed Access**: Combine flat and nested property access in the same rule
- **Complex Data Types**: Support for arrays, lists, and mixed data types within nested structures
- **Backward Compatibility**: All existing rules continue to work unchanged

## Examples

### Basic Nested Access

#### Input JSON:

```json
{
	"user": {
		"name": "Alice",
		"age": 30
	}
}
```

#### Rule:

```
user.age >= 18 THEN STRING_UPPERCASE(user.name)
```

#### Output:

```json
{
	"outputVariable": "ALICE"
}
```

### Deep Nested Access

#### Input JSON:

```json
{
	"user": {
		"profile": {
			"firstName": "John",
			"lastName": "Doe",
			"age": 35
		}
	}
}
```

#### Rule:

```
user.profile.age >= 21 THEN STRING_CONCAT(user.profile.firstName, " ", user.profile.lastName)
```

#### Output:

```json
{
	"outputVariable": "John Doe"
}
```

### Multiple Nested Objects

#### Input JSON:

```json
{
	"user": {
		"name": "Bob",
		"age": 25
	},
	"address": {
		"city": "New York",
		"country": "USA"
	}
}
```

#### Rule:

```
user.age >= 18 AND address.country == "USA" THEN STRING_CONCAT(user.name, " from ", address.city)
```

#### Output:

```json
{
	"outputVariable": "Bob from New York"
}
```

### Complex Nested Structures with Arrays

#### Input JSON:

```json
{
	"company": {
		"name": "TechCorp",
		"active": true,
		"employees": ["Alice", "Bob", "Charlie"]
	}
}
```

#### Rule:

```
company.employees.size() > 0 AND company.active == true THEN STRING_CONCAT(company.name, " has ", company.employees.size().toString(), " employees")
```

#### Output:

```json
{
	"outputVariable": "TechCorp has 3 employees"
}
```

### Array Index Access in Nested Structures

#### Input JSON:

```json
{
	"company": {
		"employees": [
			{
				"name": "Alice",
				"profile": {
					"department": "Engineering",
					"active": true
				}
			},
			{
				"name": "Bob",
				"profile": {
					"department": "Sales",
					"active": false
				}
			}
		]
	}
}
```

#### Rule:

```
company.employees.size() > 1 AND company.employees[1].profile.active == false THEN STRING_CONCAT("Inactive employee: ", company.employees[1].name)
```

#### Output:

```json
{
	"outputVariable": "Inactive employee: Bob"
}
```

### Mixed Flat and Nested Properties

#### Input JSON:

```json
{
	"name": "Alice",
	"age": 30,
	"user": {
		"profile": {
			"title": "Senior Developer",
			"verified": true
		}
	}
}
```

#### Rule:

```
age >= 18 AND user.profile.verified == true THEN STRING_CONCAT(name, " - ", user.profile.title)
```

#### Output:

```json
{
	"outputVariable": "Alice - Senior Developer"
}
```

## REST API Usage

### Create Ruleset with Nested Properties

```bash
curl -X POST http://localhost:8080/api/rulesets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "user_validation",
    "rules": [
      {
        "rule": "user.profile.age >= 21 AND user.active == true THEN STRING_CONCAT(\"Welcome, \", user.profile.name)",
        "outputVariable": "welcome_message"
      }
    ]
  }'
```

### Create Ruleset with Array Index Access

```bash
curl -X POST http://localhost:8080/api/rulesets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "employee_validation",
    "rules": [
      {
        "rule": "employees.size() > 0 AND employees[0].age >= 18 THEN STRING_UPPERCASE(employees[0].name)",
        "outputVariable": "first_employee_name"
      }
    ]
  }'
```

### Execute Ruleset with Nested JSON Input

```bash
curl -X POST http://localhost:8080/api/rulesets/execute \
  -H "Content-Type: application/json" \
  -d '{
    "rulesetName": "user_validation",
    "inputData": {
      "user": {
        "active": true,
        "profile": {
          "name": "Alice",
          "age": 25
        }
      }
    }
  }'
```

### Execute Ruleset with Array Input

```bash
curl -X POST http://localhost:8080/api/rulesets/execute \
  -H "Content-Type: application/json" \
  -d '{
    "rulesetName": "employee_validation",
    "inputData": {
      "employees": [
        {
          "name": "alice",
          "age": 25
        },
        {
          "name": "bob",
          "age": 30
        }
      ]
    }
  }'
```

### Response

```json
{
	"outputVariables": {
		"welcome_message": "Welcome, Alice"
	},
	"stats": {
		"totalRules": 1,
		"executedRules": 1
	}
}
```

## Null Safety

The engine gracefully handles missing properties:

#### Input JSON:

```json
{
	"user": {
		"name": "alice"
	}
}
```

#### Rule:

```
user.profile.age >= 18 THEN STRING_UPPERCASE(user.profile.name)
```

#### Behavior:

- The condition `user.profile.age >= 18` evaluates to `false` (not an error)
- The rule is skipped gracefully
- No output variable is created

## Best Practices

1. **Use Descriptive Property Names**: Choose clear, descriptive names for nested properties
2. **Consider Null Safety**: Design rules that handle missing properties gracefully
3. **Keep Nesting Reasonable**: While deep nesting is supported, consider readability
4. **Test Complex Structures**: Thoroughly test rules with complex nested input
5. **Document Your Schema**: Maintain documentation of your expected input structure

## Technical Implementation

The nested JSON support is implemented using:

- **PropertyAccessWrapper**: Enhanced to handle nested property paths with dot notation
- **NestedMapPropertyAccessor**: Custom Spring EL property accessor for Map objects
- **Null-Safe Navigation**: Built-in protection against null pointer exceptions

## Migration Guide

### From Flat Properties

Before:

```json
{
	"userName": "Alice",
	"userAge": 30
}
```

After:

```json
{
	"user": {
		"name": "Alice",
		"age": 30
	}
}
```

### Rule Updates

Before:

```
userAge >= 18 THEN STRING_UPPERCASE(userName)
```

After:

```
user.age >= 18 THEN STRING_UPPERCASE(user.name)
```

## Limitations

1. **Dynamic Property Names**: Property names must be known at rule definition time
2. **Circular References**: Avoid circular references in nested objects
3. **Array Bounds**: Always use size checks to prevent out-of-bounds errors

## Working with Arrays

Array index access is fully supported in dot notation paths. You can access array elements using standard bracket notation within your property paths.

### Array Index Access Examples

#### Input JSON:

```json
{
	"users": [
		{
			"name": "Alice",
			"age": 25
		},
		{
			"name": "Bob",
			"age": 30
		}
	]
}
```

#### Supported Array Operations:

- `users[0].age >= 18` - Access first user's age
- `users[1].name` - Access second user's name
- `users.size() > 0` - Check array size for bounds safety
- `users.contains("Alice")` - Check if array contains value

#### Best Practice - Safe Array Access:

```
users.size() > 0 AND users[0].age >= 18 THEN STRING_UPPERCASE(users[0].name)
```

This ensures the array has elements before accessing them.

## Troubleshooting

### Common Issues

1. **Property Not Found**: Ensure property names match exactly (case-sensitive)
2. **Null Pointer**: Use null-safe navigation or check for null explicitly
3. **Type Mismatch**: Ensure property types match expected types in conditions

### Debug Tips

1. **Check Input Structure**: Verify your JSON structure matches expectations
2. **Test Simple Cases First**: Start with basic nested access before complex rules
3. **Use Logging**: Enable debug logging to see property access attempts

## Version History

- **v1.2.0**: Added array index access support in dot notation paths (e.g., `users[0].name`, `company.employees[1].profile.title`)
- **v1.1.0**: Added basic nested JSON object support with dot notation
- **v1.0.0**: Initial release with flat property support only
