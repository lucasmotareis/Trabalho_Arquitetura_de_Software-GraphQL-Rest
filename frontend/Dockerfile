FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_REST_API_URL=http://localhost:8081/api
ARG VITE_GRAPHQL_API_URL=http://localhost:8082/graphql

ENV VITE_REST_API_URL=$VITE_REST_API_URL
ENV VITE_GRAPHQL_API_URL=$VITE_GRAPHQL_API_URL

COPY frontend/package.json frontend/pnpm-lock.yaml ./

RUN corepack enable && pnpm install --frozen-lockfile

COPY frontend ./

RUN pnpm build

FROM nginx:1.27-alpine

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
