# File purpose: Builds Angular and serves the optimized files with Nginx.
FROM node:22.12-alpine AS build

ENV NODE_OPTIONS="--max-old-space-size=1024" \
    NG_BUILD_MAX_WORKERS=2

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
