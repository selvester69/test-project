#!/bin/bash

# --- Configuration ---
IMAGE_NAME="user-service-app"
CONTAINER_NAME="user-service-container"
LOCAL_PORT="8081"
CONTAINER_PORT="8081"
# Directory where your Dockerfile (user-service/Dockerfile) is located
// DOCKERFILE_PATH="./user-service" 
DOCKERFILE_PATH="./" 

# 1. STOP & REMOVE any existing container with the same name
# We suppress output (2> /dev/null) so it doesn't print errors if the container doesn't exist
echo "Attempting to stop and remove any container named '$CONTAINER_NAME'..."
docker stop $CONTAINER_NAME 2> /dev/null
docker rm $CONTAINER_NAME 2> /dev/null

# 2. CHECK if the Docker image exists locally
# 'docker image inspect' checks for the image; if it fails (exit code > 0), the image is missing.
if docker image inspect $IMAGE_NAME > /dev/null 2>&1; then
    echo "✅ Image '$IMAGE_NAME' found locally. Skipping build step."
    NEEDS_BUILD=false
else
    echo "❌ Image '$IMAGE_NAME' not found locally. Starting build process..."
    NEEDS_BUILD=true
fi

# 3. CONDITIONAL BUILD
if [ "$NEEDS_BUILD" = true ]; then
    # Build the image, tagging it with the specified name
    docker build -t $IMAGE_NAME $DOCKERFILE_PATH
    
    # Check the exit status of the build command
    if [ $? -ne 0 ]; then
        echo "💥 Error: Docker image build failed. Aborting."
        exit 1
    fi
    echo "✅ Build successful."
fi

# 4. RUN the container with port mapping
echo "🚀 Running container '$CONTAINER_NAME' and mapping host port $LOCAL_PORT to container port $CONTAINER_PORT..."

docker run -d \
    -p $LOCAL_PORT:$CONTAINER_PORT \
    --name $CONTAINER_NAME \
    $IMAGE_NAME

# Final status check
if [ $? -eq 0 ]; then
    echo ""
    echo "*****************************************************"
    echo "Container started successfully in detached mode (-d)."
    echo "Application available at http://localhost:$LOCAL_PORT"
    echo "To view logs: docker logs $CONTAINER_NAME"
    echo "*****************************************************"
else
    echo "💥 Error: Docker container failed to start."
    exit 1
fi
