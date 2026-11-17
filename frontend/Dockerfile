FROM node:22-bookworm AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_API_URL=/api
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /app/dist /srv
COPY frontend/Caddyfile /etc/caddy/Caddyfile
EXPOSE 80 443
