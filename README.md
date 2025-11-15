# Bước 1
Run file run.sh
# Bước 2
1. Test traceId http
http://localhost:8089/users
2. Test traceId grpc
http://localhost:8081/send-grpc
3. Test traceId kafka
http://localhost:8089/send-message
4. Test traceId khi call external api
http://localhost:8089/call-external?url=http://service-b:8081/service-b