#!/bin/bash

echo "🚀 Comprehensive Rule Engine Testing"
echo "======================================"
echo ""

BASE_URL="http://localhost:8080/api"
CONTENT_TYPE="Content-Type: application/json"

# Function to run test
run_test() {
    local test_name="$1"
    local ruleset_name="$2"
    local rule="$3"
    local output_var="$4"
    local input_data="$5"
    local expected="$6"
    
    echo "🧪 Test: $test_name"
    echo "Rule: $rule"
    echo "Input: $input_data"
    
    # Create ruleset
    curl -s -X POST "$BASE_URL/rulesets" \
        -H "$CONTENT_TYPE" \
        -d "{
            \"name\": \"$ruleset_name\",
            \"rules\": [
                {
                    \"rule\": \"$rule\",
                    \"outputVariable\": \"$output_var\"
                }
            ]
        }" > /dev/null
    
    # Execute rule
    result=$(curl -s -X POST "$BASE_URL/rulesets/execute" \
        -H "$CONTENT_TYPE" \
        -d "{
            \"rulesetName\": \"$ruleset_name\",
            \"inputData\": $input_data
        }")
    
    echo "Result: $result"
    
    # Extract output variable value
    actual=$(echo "$result" | jq -r ".outputVariables.\"$output_var\"")
    
    if [ "$actual" = "$expected" ]; then
        echo "✅ PASS"
    else
        echo "❌ FAIL - Expected: $expected, Got: $actual"
    fi
    echo ""
}

echo "📝 BASIC STRING TRANSFORMATIONS"
echo "================================"

run_test "STRING_UPPERCASE" "test1" \
    "age >= 18 THEN STRING_UPPERCASE(name)" "name_upper" \
    '{"name": "alice", "age": 25}' \
    "ALICE"

run_test "STRING_LOWERCASE" "test2" \
    "age >= 18 THEN STRING_LOWERCASE(name)" "name_lower" \
    '{"name": "JOHN", "age": 30}' \
    "john"

run_test "STRING_CONCAT" "test3" \
    "age >= 18 THEN STRING_CONCAT(\"Hello, \", name)" "greeting" \
    '{"name": "world", "age": 25}' \
    "Hello, world"

run_test "STRING_SUBSTRING" "test4" \
    "name.length() > 5 THEN STRING_SUBSTRING(name, 0, 3)" "name_short" \
    '{"name": "Alexander", "age": 25}' \
    "Ale"

echo "🔗 NESTED TRANSFORMATIONS"
echo "=========================="

run_test "Nested UPPERCASE(CONCAT)" "test5" \
    "age >= 18 THEN STRING_UPPERCASE(STRING_CONCAT(\"Mr. \", name))" "formal_name" \
    '{"name": "smith", "age": 30}' \
    "MR. SMITH"

run_test "Complex Nested - Proper Case" "test6" \
    "name.length() > 3 THEN STRING_CONCAT(STRING_UPPERCASE(STRING_SUBSTRING(name, 0, 1)), STRING_LOWERCASE(STRING_SUBSTRING(name, 1, name.length())))" "proper_case" \
    '{"name": "jOHN", "age": 25}' \
    "John"

run_test "Triple Nested" "test7" \
    "age >= 21 THEN STRING_UPPERCASE(STRING_CONCAT(\"DR. \", STRING_LOWERCASE(name)))" "doctor_title" \
    '{"name": "WATSON", "age": 35}' \
    "DR. watson"

run_test "Quad Nested - Professor Title" "test8" \
    "age >= 30 THEN STRING_UPPERCASE(STRING_CONCAT(STRING_CONCAT(\"Prof. \", STRING_LOWERCASE(name)), STRING_CONCAT(\" - Age: \", age.toString())))" "professor_title" \
    '{"name": "EINSTEIN", "age": 42}' \
    "PROF. EINSTEIN - AGE: 42"

echo "🎯 COMPLEX CONDITIONS"
echo "===================="

run_test "Multiple AND Conditions" "test9" \
    "age >= 18 AND name.length() > 3 AND age < 65 THEN STRING_CONCAT(name, \" - Valid Adult\")" "status_message" \
    '{"name": "Alice", "age": 25}' \
    "Alice - Valid Adult"

run_test "OR Conditions - Senior" "test10" \
    "age >= 65 OR age <= 12 THEN STRING_CONCAT(name, \" - Special Rate\")" "rate_category" \
    '{"name": "George", "age": 70}' \
    "George - Special Rate"

run_test "OR Conditions - Child" "test11" \
    "age >= 65 OR age <= 12 THEN STRING_CONCAT(name, \" - Special Rate\")" "rate_category2" \
    '{"name": "Emma", "age": 8}' \
    "Emma - Special Rate"

echo "🔧 BUILT-IN STRING METHODS"
echo "=========================="

run_test "Native String Methods" "test12" \
    "name.length() > 5 THEN name.toUpperCase()" "long_name_upper" \
    '{"name": "alexander", "age": 25}' \
    "ALEXANDER"

run_test "Mixed Native + Custom" "test13" \
    "name.length() > 3 THEN STRING_CONCAT(name.substring(0, 1).toUpperCase(), name.substring(1).toLowerCase())" "title_case" \
    '{"name": "mARY", "age": 28}' \
    "Mary"

echo "📊 DATA TYPES"
echo "============="

run_test "Boolean Values" "test14" \
    "isActive == true AND age >= 18 THEN STRING_CONCAT(name, \" - Active Adult\")" "status" \
    '{"name": "Bob", "age": 30, "isActive": true}' \
    "Bob - Active Adult"

run_test "Numeric Values" "test15" \
    "age >= 18 THEN STRING_CONCAT(name, \" is \", age.toString(), \" years old\")" "age_description" \
    '{"name": "Alice", "age": 25}' \
    "Alice is 25 years old"

echo "🛡️ EDGE CASES"
echo "============="

run_test "Empty String" "test16" \
    "age >= 18 THEN STRING_CONCAT(\"Hello, \", name)" "greeting_empty" \
    '{"name": "", "age": 25}' \
    "Hello, "

run_test "False Condition" "test17" \
    "age >= 21 THEN STRING_UPPERCASE(name)" "name_upper_false" \
    '{"name": "john", "age": 18}' \
    "null"

echo "⚡ PERFORMANCE TEST"
echo "=================="

run_test "Super Complex Nested" "test18" \
    "age >= 18 THEN STRING_UPPERCASE(STRING_CONCAT(STRING_CONCAT(STRING_CONCAT(\"Dr. \", STRING_LOWERCASE(name)), \" - \"), STRING_CONCAT(\"Age: \", age.toString())))" "super_complex" \
    '{"name": "FEYNMAN", "age": 45}' \
    "DR. FEYNMAN - AGE: 45"

echo "🎉 MULTIPLE RULES TEST"
echo "======================"

echo "🧪 Testing Multiple Rules in Single Ruleset"
curl -s -X POST "$BASE_URL/rulesets" \
    -H "$CONTENT_TYPE" \
    -d '{
        "name": "multi_rules",
        "rules": [
            {
                "rule": "age >= 18 THEN \"ADULT\"",
                "outputVariable": "age_category"
            },
            {
                "rule": "age >= 18 THEN STRING_UPPERCASE(name)",
                "outputVariable": "name_upper"
            },
            {
                "rule": "age >= 21 THEN STRING_CONCAT(name, \" can drink\")",
                "outputVariable": "drink_status"
            },
            {
                "rule": "age >= 18 THEN STRING_CONCAT(\"Welcome, \", STRING_UPPERCASE(name))",
                "outputVariable": "welcome_message"
            }
        ]
    }' > /dev/null

result=$(curl -s -X POST "$BASE_URL/rulesets/execute" \
    -H "$CONTENT_TYPE" \
    -d '{
        "rulesetName": "multi_rules",
        "inputData": {
            "name": "john",
            "age": 25
        }
    }')

echo "Input: {\"name\": \"john\", \"age\": 25}"
echo "Result: $result"

age_category=$(echo "$result" | jq -r ".outputVariables.age_category")
name_upper=$(echo "$result" | jq -r ".outputVariables.name_upper")
drink_status=$(echo "$result" | jq -r ".outputVariables.drink_status")
welcome_message=$(echo "$result" | jq -r ".outputVariables.welcome_message")

echo ""
echo "✅ Multiple Rules Results:"
echo "   age_category: $age_category"
echo "   name_upper: $name_upper"
echo "   drink_status: $drink_status"
echo "   welcome_message: $welcome_message"

echo ""
echo "🏁 TESTING COMPLETE!"
echo "====================="
echo "✨ All transformation types tested:"
echo "   • STRING_UPPERCASE(name)"
echo "   • STRING_LOWERCASE(name)"
echo "   • STRING_CONCAT(\"text\", name)"
echo "   • STRING_SUBSTRING(name, 0, 3)"
echo "   • Nested: STRING_UPPERCASE(STRING_CONCAT(\"Mr. \", name))"
echo "   • Complex conditions with AND/OR"
echo "   • Native string methods: name.toUpperCase()"
echo "   • Mixed native + custom functions"
echo "   • Multiple data types (boolean, numeric)"
echo "   • Edge cases (empty strings, false conditions)"
echo "   • Multiple rules in single ruleset"
echo ""
echo "🎯 All without using # prefix - direct property access!" 