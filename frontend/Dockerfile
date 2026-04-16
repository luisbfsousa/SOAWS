FROM nginx:alpine

COPY frontend/index.html /usr/share/nginx/html/
COPY frontend/store.css /usr/share/nginx/html/

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
