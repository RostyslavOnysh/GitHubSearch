#!/usr/bin/env bash

sh stopK.sh

DIRECTORY=target
if [ -d "$DIRECTORY" ]; then
    echo "The project is already built, skipping docker build"
else
    echo "Building docker images for Kubernetes..."
    sh buildK.sh
fi

echo "Creating ConfigMap for application.properties..."
kubectl create configmap app-config --from-file=application.properties=/Users/rostyslav/IdeaProjects/GitHubSearch/src/main/resources/application.properties

# Розгортання додатку у Kubernetes
echo "Deploying application to Kubernetes..."
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/redis-pv-pvc.yaml
kubectl apply -f k8s/redis-secret.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-pv-pvc.yaml
kubectl apply -f k8s/postgres-secret.yaml

# Перевірка стану сервісів
echo "Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app=github-search-app --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis --timeout=120s

# Налаштування port-forward для доступу до додатку
echo "Setting up port forwarding to localhost:8080..."
kubectl port-forward service/github-search-app 8080:8080 &

echo "Application has been deployed to Kubernetes and is accessible at http://localhost:8080"