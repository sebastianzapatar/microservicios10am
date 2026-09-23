# Etapa 1: compila la SPA de React con Vite.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: nginx sirve los estáticos y hace de proxy hacia la red de Compose.
FROM nginx:1.29-alpine
ENV ARQUITECTURA=rest
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=5s --timeout=3s --retries=10 \
  CMD wget -qO- http://127.0.0.1/config.json >/dev/null || exit 1
