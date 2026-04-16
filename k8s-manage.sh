#!/bin/bash

NAMESPACE="ecommerce"

show_usage() {
    cat << EOF
eCommerce Kubernetes Management Tool

Usage: ./k8s-manage.sh [command] [options]

Commands:
    status              Show deployment status
    logs [service]      Show logs for a service (or all if not specified)
    restart [service]   Restart a service deployment
    scale [service] [n] Scale a service to n replicas
    delete              Delete all deployments
    port-forward [port] Port-forward frontend to local port (default: 8080)
    shell [service]     Open shell in a running pod
    describe [service]  Describe a deployment
    events              Show recent events in namespace
    yaml [service]      Export service YAML

Examples:
    ./k8s-manage.sh status
    ./k8s-manage.sh logs cart-service
    ./k8s-manage.sh scale cart-service 3
    ./k8s-manage.sh port-forward 9000
    ./k8s-manage.sh shell catalogue-service

EOF
}

if [ "$#" -eq 0 ]; then
    show_usage
    exit 0
fi

COMMAND=$1
SERVICE=$2
VALUE=$3

case $COMMAND in
    status)
        kubectl get deployments -n $NAMESPACE
        kubectl get pods -n $NAMESPACE
        kubectl get services -n $NAMESPACE
        ;;
    
    logs)
        if [ -z "$SERVICE" ]; then
            kubectl logs -n $NAMESPACE -l app --all-containers=true --tail=100 -f
        else
            kubectl logs -n $NAMESPACE -l app=$SERVICE --all-containers=true --tail=100 -f
        fi
        ;;
    
    restart)
        if [ -z "$SERVICE" ]; then
            echo "Error: Service name required"
            show_usage
            exit 1
        fi
        kubectl rollout restart deployment/$SERVICE -n $NAMESPACE
        kubectl rollout status deployment/$SERVICE -n $NAMESPACE --timeout=5m
        ;;
    
    scale)
        if [ -z "$SERVICE" ] || [ -z "$VALUE" ]; then
            echo "Error: Service name and replica count required"
            show_usage
            exit 1
        fi
        kubectl scale deployment/$SERVICE --replicas=$VALUE -n $NAMESPACE
        ;;
    
    delete)
        echo "Deleting all resources in namespace $NAMESPACE..."
        read -p "Are you sure? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            kubectl delete namespace $NAMESPACE
        fi
        ;;
    
    port-forward)
        PORT=${SERVICE:-8080}
        kubectl port-forward -n $NAMESPACE svc/frontend-service $PORT:80
        ;;
    
    shell)
        if [ -z "$SERVICE" ]; then
            echo "Error: Service name required"
            show_usage
            exit 1
        fi
        POD=$(kubectl get pods -n $NAMESPACE -l app=$SERVICE -o jsonpath='{.items[0].metadata.name}')
        if [ -z "$POD" ]; then
            echo "No pod found for service: $SERVICE"
            exit 1
        fi
        echo "Opening shell in pod: $POD"
        kubectl exec -it -n $NAMESPACE $POD -- /bin/bash
        ;;
    
    describe)
        if [ -z "$SERVICE" ]; then
            echo "Error: Service name required"
            show_usage
            exit 1
        fi
        kubectl describe deployment/$SERVICE -n $NAMESPACE
        ;;
    
    events)
        kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp'
        ;;
    
    yaml)
        if [ -z "$SERVICE" ]; then
            echo "Error: Service name required"
            show_usage
            exit 1
        fi
        kubectl get deployment/$SERVICE -n $NAMESPACE -o yaml
        ;;
    
    *)
        echo "Unknown command: $COMMAND"
        show_usage
        exit 1
        ;;
esac
