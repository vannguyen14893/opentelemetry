#!/bin/bash

# Exit on error
set -e

echo "Building Docker images and starting services..."

# Build Docker images for Service-A and Service-B
echo "Building Service-A Docker image..."
docker build -t service-a:latest ./Service-A

echo "Building Service-B Docker image..."
docker build -t service-b:latest ./Service-B

# Run docker-compose
echo "Starting all services with docker-compose..."
docker-compose up -d

echo "All services are now running!"
echo "You can check the status with: docker-compose ps"
echo "To view logs: docker-compose logs -f"
echo "To stop all services: docker-compose down"