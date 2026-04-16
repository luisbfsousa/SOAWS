# SOAWS - eCommerce Microservices

### Docker
```bash
docker-compose up -d
```

### Kubernetes
```bash
minikube start
eval $(minikube docker-env)

./build-images.sh
./deploy-k8s.sh

kubectl port-forward -n ecommerce svc/frontend-service 8080:80
```

### Frontend
```
http://localhost:8080
```
