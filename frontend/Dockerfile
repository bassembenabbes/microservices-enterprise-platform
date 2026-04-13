# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Copier package.json
COPY react-app/package*.json ./

# Afficher la version de npm
RUN npm --version

# Installer les dépendances avec plus de logs
RUN npm install --verbose --legacy-peer-deps

# Copier tout le code source
COPY react-app/ ./

# Afficher les fichiers avant build
RUN ls -la

# Build l'application
RUN npm run build

# Afficher le résultat du build
RUN ls -la build/

# Production stage
FROM nginx:alpine
RUN apk add --no-cache curl
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Vérifier les fichiers copiés
RUN ls -la /usr/share/nginx/html/

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

