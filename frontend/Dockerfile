# ── Build stage ───────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package definition files first to leverage Docker layer caching
COPY package*.json ./
RUN npm ci

# Copy application source code
COPY . .

# Compile Angular for production
RUN npm run build -- --configuration=production

# ── Runtime stage ─────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Copy the build output to Nginx web root directory
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

# Replace default configuration to support SPA routing (avoid 404 on refresh)
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

# Copy entrypoint script and configure runtime permissions
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
