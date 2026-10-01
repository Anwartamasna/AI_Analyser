#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Deploy AI Resume Analyzer to K3s Kubernetes Cluster
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="${SCRIPT_DIR}/k8s"
NAMESPACE="ai-resume"
KUBECONFIG_PATH="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"

echo "=========================================================="
echo " Deploying AI Resume Analyzer Stack to K3s"
echo " Target Namespace: ${NAMESPACE}"
echo "=========================================================="

KUBECTL_CMD="kubectl"
if ! command -v kubectl &> /dev/null; then
    if command -v k3s &> /dev/null; then
        KUBECTL_CMD="k3s kubectl"
    else
        echo "Error: Neither kubectl nor k3s was found on this host."
        exit 1
    fi
fi

# If a local kubeconfig file is available, use it
KUBECONFIG_ARG=""
if [[ -f "${KUBECONFIG_PATH}" ]]; then
    KUBECONFIG_ARG="--kubeconfig ${KUBECONFIG_PATH}"
fi

echo "1. Applying Namespace..."
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/00-namespace.yaml"

echo "2. Applying Configurations and Secrets..."
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/01-config-secrets.yaml"

echo "3. Applying Persistent Volumes (local-path)..."
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/02-storage.yaml"

echo "4. Deploying Infrastructure Services (Postgres, MinIO, Kafka, Ollama)..."
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/03-postgres.yaml"
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/04-minio.yaml"
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/05-kafka-zookeeper.yaml"
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/06-ollama.yaml"

echo "5. Deploying Application Microservices (Backend, OCR Service, Frontend, Ingress)..."
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/07-backend.yaml"
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/08-ocr-service.yaml"
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/09-frontend.yaml"
${KUBECTL_CMD} ${KUBECONFIG_ARG} apply -f "${K8S_DIR}/10-ingress.yaml"

echo ""
echo "=========================================================="
echo " Checking Deployment Status in namespace '${NAMESPACE}'..."
echo "=========================================================="
${KUBECTL_CMD} ${KUBECONFIG_ARG} get pods,services,ingress,pvc -n "${NAMESPACE}" -o wide
echo ""
echo "Deployment applied successfully!"
