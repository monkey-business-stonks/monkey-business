# Phase 1: Kafka Infrastructure Setup - Getting Started

This guide walks through bringing up all the infrastructure for Phase 1.

## Prerequisites

- Docker and Docker Compose installed
- Git repository cloned
- About 5-10 minutes

## Step 1: Start All Services

From the project root directory, start the Kafka, Zookeeper, and existing services:

```bash
docker-compose up -d
```

This will start:
- ✅ PostgreSQL (5432)
- ✅ PgAdmin (5050)
- ✅ Auth Service (3000)
- ✅ Kafka Brokers (9092, 9093, 9094)
- ✅ Zookeeper nodes (2181, 2182, 2183)
- ✅ Backend Spring Boot (8080)
- ✅ Frontend Angular (4200)

Wait for all services to be healthy:

```bash
# Check service health
docker-compose ps

# All services should show "Up" and pass health checks
# This takes about 30-60 seconds
```

## Step 2: Verify Kafka Broker Health

Check that Kafka brokers are ready:

```bash
# Connect to kafka-1 container
docker exec monkey_business_kafka1 kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Expected output: Shows broker version and supported API versions
```

## Step 3: Create Kafka Topics

Navigate to the scripts directory and run the topic creation script:

```bash
# Option A: Run from host (requires Kafka CLI installed locally)
cd scripts
./create-kafka-topics.sh

# Option B: Run inside docker container
docker exec monkey_business_kafka1 bash -c "
kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --create --topic orders.submitted \
  --partitions 10 --replication-factor 3 --if-not-exists

kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --create --topic orders.validated \
  --partitions 10 --replication-factor 3 --if-not-exists

kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --create --topic orders.filled \
  --partitions 10 --replication-factor 3 --if-not-exists

kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --create --topic orders.rejected \
  --partitions 10 --replication-factor 3 --if-not-exists

kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --create --topic orders.dlq \
  --partitions 3 --replication-factor 3 --if-not-exists

kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --create --topic prices.market-update \
  --partitions 5 --replication-factor 3 --if-not-exists
"
```

## Step 4: Verify Topics Created

List all topics:

```bash
docker exec monkey_business_kafka1 kafka-topics \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --list

# Expected output:
# orders.dlq
# orders.filled
# orders.rejected
# orders.submitted
# orders.validated
# prices.market-update
```

Describe a specific topic:

```bash
docker exec monkey_business_kafka1 kafka-topics \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --describe --topic orders.submitted

# Expected output shows:
# Topic: orders.submitted
# 10 partitions, replication factor 3, assigned replicas in ISR
```

## Step 5: Test Message Publishing

Publish a test message:

```bash
docker exec monkey_business_kafka1 bash -c '
  echo "{\"test\": \"message\"}" | \
  kafka-console-producer \
    --broker-list kafka-1:9092,kafka-2:9092,kafka-3:9092 \
    --topic orders.submitted
'
```

Consume the test message:

```bash
docker exec monkey_business_kafka1 kafka-console-consumer \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --topic orders.submitted \
  --from-beginning \
  --max-messages 1
```

## Step 6: Build Maven Project

Update Maven dependencies:

```bash
cd server
mvn clean install

# Expected: Build SUCCESS
# This downloads Kafka, Spring Cloud Stream, and Caffeine dependencies
```

## Verify Installation

### Check Docker Compose Services

```bash
docker-compose ps
```

All services should show:
- `Up` state
- Passing health checks (✓)

### Check Kafka Cluster Status

```bash
docker exec monkey_business_kafka1 \
  kafka-cluster.sh --bootstrap-server localhost:9092
```

### Check Spring Boot Logs

```bash
docker-compose logs backend | tail -20
```

Should show:
- `Kafka brokers: [kafka-1:9092, kafka-2:9092, kafka-3:9092]`
- `Caffeine cache configuration: expireAfterWrite=5m`
- No errors or exceptions

## Troubleshooting

### Kafka brokers not starting
- Check Zookeeper health first: `docker-compose logs zk-1`
- Wait 30-60 seconds for ensemble to form
- Restart Kafka: `docker-compose restart kafka-1 kafka-2 kafka-3`

### Topic creation fails
- Verify brokers are healthy: `docker exec monkey_business_kafka1 jps`
- Should show QuorumPeerMain and Kafka processes
- Check broker logs: `docker-compose logs kafka-1`

### Spring Boot can't connect to Kafka
- Verify broker addresses in application.yml: should be `kafka-1:9092,kafka-2:9092,kafka-3:9092`
- Check backend logs: `docker-compose logs backend | grep -i kafka`
- Verify backend container can reach Kafka: `docker exec monkey_business_backend ping kafka-1`

### Port conflicts
- If ports 9092-9094 already in use, modify docker-compose.yml ports
- Remember to update KAFKA_BROKERS env var accordingly

## What's Next

After Phase 1 is complete:
1. ✅ Kafka infrastructure running (3 brokers, 3 ZK nodes)
2. ✅ Topics created and verified
3. ✅ Maven dependencies added
4. ✅ Application configuration in place
5. ✅ Caffeine cache configured

Ready for Phase 2: Order Processing Pipeline implementation

## Useful Commands Reference

```bash
# List topics
docker exec monkey_business_kafka1 kafka-topics \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --list

# Describe topic
docker exec monkey_business_kafka1 kafka-topics \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --describe --topic orders.submitted

# Monitor topic
docker exec monkey_business_kafka1 kafka-console-consumer \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --topic orders.submitted --from-beginning

# Check consumer groups
docker exec monkey_business_kafka1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --list

# Check consumer group lag
docker exec monkey_business_kafka1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --group order-validator-group --describe

# Delete topic (if needed)
docker exec monkey_business_kafka1 kafka-topics \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --delete --topic orders.submitted
```

---

**Status**: Phase 1 Complete ✅
