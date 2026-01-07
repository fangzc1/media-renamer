# Multi-stage build for Media Renamer

# Stage 1: Build Backend
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app/backend

# Configure Maven to use Aliyun Mirror (Accelerate builds in China)
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"><mirrors><mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><name>阿里云公共仓库</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# Cache dependencies
COPY backend/pom.xml .
# Download dependencies to cache them
RUN mvn dependency:go-offline -B

# Build application
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Build Frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend

# Configure NPM to use Taobao/Aliyun Mirror (Accelerate builds in China)
RUN npm config set registry https://registry.npmmirror.com

# Cache dependencies
COPY frontend/package.json frontend/package-lock.json* ./

# Use 'npm install' instead of 'npm ci' to handle potential cross-platform lockfile issues
# (e.g. macOS generated lockfile missing Linux binaries for esbuild/vite)
RUN npm install

# Build application
COPY frontend/ . 
RUN npm run build

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl/wget for healthcheck if needed (Alpine usually has wget)
# RUN apk add --no-cache curl

# Copy Backend JAR
COPY --from=backend-build /app/backend/target/*.jar app.jar

# Copy Frontend Static Files
COPY --from=frontend-build /app/frontend/dist /app/static

# Create Media Directory & Logs
RUN mkdir -p /media /app/logs

# Expose Port
EXPOSE 8080

# Environment Variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV TMDB_API_KEY=""
ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/static/,classpath:/static/

# Entrypoint
ENTRYPOINT ["java", "-jar", "/app/app.jar"]