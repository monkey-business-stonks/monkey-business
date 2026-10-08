#!/bin/bash

# Kafka Topics Initialization Script
# This script creates all required Kafka topics for the Monkey Business platform
# Usage: ./create-kafka-topics.sh

set -e

echo "=== Kafka Topics Initialization ==="

# Kafka broker addresses (Docker internal)
KAFKA_BROKERS="${KAFKA_BROKERS:-kafka-1:9092,kafka-2:9092,kafka-3:9092}"

echo "Kafka Brokers: $KAFKA_BROKERS"

# Function to create topic
create_topic() {
    local topic_name=$1
    local partitions=$2
    local replication_factor=$3
    
    echo "Creating topic: $topic_name (partitions: $partitions, replication: $replication_factor)"
    
    kafka-topics --bootstrap-server "$KAFKA_BROKERS" \
        --create \
        --topic "$topic_name" \
        --partitions "$partitions" \
        --replication-factor "$replication_factor" \
        --config retention.ms=604800000 \
        --if-not-exists 2>/dev/null || echo "Topic $topic_name already exists"
}

# Create order processing topics
echo ""
echo "--- Creating Order Processing Topics ---"
create_topic "orders.submitted" 10 3
create_topic "orders.validated" 10 3
create_topic "orders.filled" 10 3
create_topic "orders.rejected" 10 3
create_topic "orders.dlq" 3 3

# Create market data topics
echo ""
echo "--- Creating Market Data Topics ---"
create_topic "prices.market-update" 5 3

# Verify topics were created
echo ""
echo "--- Verifying Topics ---"
kafka-topics --bootstrap-server "$KAFKA_BROKERS" --list

echo ""
echo "=== Kafka Topics Initialization Complete ==="
