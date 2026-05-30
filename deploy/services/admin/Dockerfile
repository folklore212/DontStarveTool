FROM node:20-alpine AS build
WORKDIR /build
COPY src/frontend/admin/package.json src/frontend/admin/package-lock.json ./
RUN npm install --legacy-peer-deps
COPY src/frontend/admin/ .
RUN npm run build

FROM nginx:alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /build/dist/ /usr/share/nginx/html/
COPY deploy/services/admin/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
