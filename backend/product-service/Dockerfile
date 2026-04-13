FROM node:20-alpine

WORKDIR /app

# Installer les dépendances système
RUN apk add --no-cache python3 make g++

COPY package*.json ./
RUN npm ci --only=production

COPY . .

EXPOSE 8002

CMD ["node", "src/index.js"]
