FROM node:20-slim AS builder
WORKDIR /app

# Install build dependencies for native modules
RUN apt-get update && apt-get install -y \
    python3 \
    make \
    g++ \
    && rm -rf /var/lib/apt/lists/*

ENV NEXT_TELEMETRY_DISABLED=1

# Public API base URL is inlined into the client bundle at BUILD time
# (Next.js NEXT_PUBLIC_* are compile-time). Override per environment with
# --build-arg NEXT_PUBLIC_API_URL=... (docker-compose / Copilot pass this).
# A shell ENV takes precedence over .env files, so this wins over any .env.local.
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

# Install dependencies directly in builder to ensure correct platform binaries
COPY package.json ./
RUN npm install

# Copy source code
COPY . .

RUN npm run build

# Prune dev dependencies for production
RUN npm prune --omit=dev

FROM node:20-slim AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json

EXPOSE 3000

CMD ["npm", "run", "start"]
