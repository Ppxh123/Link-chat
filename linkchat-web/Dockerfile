# ===================================
# LinkChat Web Dockerfile
# 阶段1: Node构建
# 阶段2: Nginx部署
# ===================================

FROM node:20-alpine AS builder
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npx vite build

FROM nginx:1.27-alpine
# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html
# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Nginx前台运行
CMD ["nginx", "-g", "daemon off;"]