#!/bin/bash
set -e

REGISTRY="ecommerce"
VERSION="1.0.0"

docker build -f catalogue-service/Dockerfile -t ${REGISTRY}/catalogue-service:${VERSION} .
docker build -f authentication-service/Dockerfile -t ${REGISTRY}/authentication-service:${VERSION} .
docker build -f payment-service/Dockerfile -t ${REGISTRY}/payment-service:${VERSION} .
docker build -f warehouse-service/Dockerfile -t ${REGISTRY}/warehouse-service:${VERSION} .
docker build -f cart-service/Dockerfile -t ${REGISTRY}/cart-service:${VERSION} .
docker build -f frontend/Dockerfile -t ${REGISTRY}/frontend:${VERSION} .
