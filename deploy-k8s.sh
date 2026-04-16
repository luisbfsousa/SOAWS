#!/bin/bash
set -e

NAMESPACE="ecommerce"

command -v kubectl &> /dev/null || { echo "Error: kubectl not installed"; exit 1; }
kubectl cluster-info &>/dev/null || { echo "Error: No Kubernetes cluster accessible"; exit 1; }

kubectl apply -f k8s/

kubectl rollout status deployment/catalogue-service -n ${NAMESPACE} --timeout=5m || true
kubectl rollout status deployment/authentication-service -n ${NAMESPACE} --timeout=5m || true
kubectl rollout status deployment/payment-service -n ${NAMESPACE} --timeout=5m || true
kubectl rollout status deployment/warehouse-service -n ${NAMESPACE} --timeout=5m || true
kubectl rollout status deployment/cart-service -n ${NAMESPACE} --timeout=5m || true
kubectl rollout status deployment/frontend -n ${NAMESPACE} --timeout=5m || true
kubectl rollout status deployment/activemq -n ${NAMESPACE} --timeout=5m || true

kubectl get services -n ${NAMESPACE}
kubectl get pods -n ${NAMESPACE}
