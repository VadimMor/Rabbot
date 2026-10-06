#!/bin/bash
# Создаёт топики Rabbot. Идемпотентно: существующие топики пропускаются.
set -euo pipefail
KT=/opt/kafka/bin/kafka-topics.sh
B=${BOOTSTRAP:-kafka:9092}

# имя            партиции  retention.ms   (1 день = 86400000)
TOPICS="
trend.raw            6   259200000
trend.enriched       6   604800000
content.requested    6   604800000
content.generated    6   604800000
publish.due          12  604800000
publish.result       6   604800000
publish.due.dlq      3   1209600000
metrics.raw          6   604800000
outbox.relay.dlq     3   1209600000
"

echo "$TOPICS" | while read -r name parts retention; do
  [ -z "$name" ] && continue
  $KT --bootstrap-server "$B" --create --if-not-exists \
      --topic "$name" --partitions "$parts" --replication-factor 1 \
      --config retention.ms="$retention" --config min.insync.replicas=1
done

echo "Topics:"
$KT --bootstrap-server "$B" --list
