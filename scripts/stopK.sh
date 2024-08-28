#!/usr/bin/env bash

echo "Stopping Kubernetes resources..."
kubectl delete -f k8s/app-deployment.yaml
kubectl delete -f k8s/redis-deployment.yaml
kubectl delete -f k8s/redis-pv-pvc.yaml
kubectl delete -f k8s/redis-secret.yaml

echo "The Kubernetes application has been stopped."