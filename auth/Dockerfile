# Use Node.js LTS image for building
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install ALL dependencies (including devDependencies for building)
RUN npm install

# Copy source code
COPY tsconfig*.json ./
COPY src/ ./src/

# Build the NestJS application (compiles TS to JS in /dist)
RUN npm run build

# --- Production Image ---
FROM node:20-alpine AS runner

WORKDIR /app

# Copy package files and install only production dependencies
COPY package*.json ./
RUN npm install --production

# Copy the built output from the builder stage
COPY --from=builder /app/dist ./dist

# Expose port
EXPOSE 3000

# Health check (NestJS doesn't have a /health route by default unless you add one, 
# but you can add a health controller or point it to your root/auth route)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/auth/login || exit 1

# Start the compiled NestJS application
CMD ["node", "dist/main"]