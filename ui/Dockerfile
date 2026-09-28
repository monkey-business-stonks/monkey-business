# Build stage
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build Angular app
RUN npm run build

# Serve stage
FROM nginx:alpine

WORKDIR /app

# Copy built application from builder stage
# Angular outputs to dist/ui/browser/ (newer Angular versions)
COPY --from=builder /app/dist/ui/browser/ /usr/share/nginx/html/

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Create custom nginx config for Angular SPA routing
RUN printf 'server {\n  listen 4200;\n  location / {\n    root /usr/share/nginx/html;\n    try_files $uri $uri/ /index.html;\n  }\n}' > /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 4200

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4200/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
