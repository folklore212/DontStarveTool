FROM node:20-alpine AS build
WORKDIR /build
COPY src/frontend/customer/package.json src/frontend/customer/package-lock.json ./
RUN npm install --legacy-peer-deps
COPY src/frontend/customer/ .
RUN npm run build

FROM nginx:alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /build/dist/ /usr/share/nginx/html/
COPY deploy/services/customer/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
