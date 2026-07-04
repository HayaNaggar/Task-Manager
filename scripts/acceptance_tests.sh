#!/usr/bin/env bash
# Simple curl-based acceptance test script for Task Flow
BASE=http://localhost:8081

echo "Login as admin"
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"email":"admin@example.com","password":"adminpass"}' | jq -r '.token')

echo "Create user"
curl -s -X POST "$BASE/api/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"fullName":"John Doe","email":"john.doe@example.com","password":"pass1234"}' | jq .

echo "Create project"
curl -s -X POST "$BASE/api/projects" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Web Application","key":"WEB","description":"Main web application project"}' | jq .

echo "Create task"
curl -s -X POST "$BASE/api/projects/1/tasks" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Setup authentication system","description":"Implement JWT authentication","priority":"HIGH","reporterId":1,"assigneeId":2,"dueDate":"2026-08-01"}' | jq .

echo "Change status"
curl -s -X PATCH "$BASE/api/tasks/1/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"IN_PROGRESS"}' | jq .

echo "Search tasks"
curl -s "$BASE/api/tasks?status=IN_PROGRESS&priority=HIGH&page=0&size=20&sort=dueDate,asc" \
  -H "Authorization: Bearer $TOKEN" | jq .
