#!/usr/bin/env bash
# Unigram inverted index runner.
# Usage: ./scripts/run-unigrams.sh [INPUT_DIR] [OUTPUT_DIR]
# Defaults: INPUT_DIR=data/fulldata OUTPUT_DIR=unigram_out
set -euo pipefail

export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}
export PATH="$JAVA_HOME/bin:$PATH"

CLASS="InvertedUnigramIndex"
DEFAULT_INPUT_DIR="data/fulldata"
DEFAULT_OUTPUT_DIR="unigram_out"

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  echo "Usage: $0 [INPUT_DIR] [OUTPUT_DIR]";
  echo "Defaults: INPUT_DIR=$DEFAULT_INPUT_DIR OUTPUT_DIR=$DEFAULT_OUTPUT_DIR";
  exit 0;
fi

# Allow override via args or env vars.
INPUT_DIR="${INPUT_DIR:-${1:-$DEFAULT_INPUT_DIR}}"
OUTPUT_DIR="${OUTPUT_DIR:-${2:-$DEFAULT_OUTPUT_DIR}}"

if [ ! -d "$INPUT_DIR" ]; then
  echo "[error] Input directory '$INPUT_DIR' not found. Expected relative path (e.g. data/fulldata)." >&2
  exit 1
fi

echo "[info] Using JAVA_HOME=$JAVA_HOME"
echo "[run] Building $CLASS for input '$INPUT_DIR' → output '$OUTPUT_DIR'"

rm -rf "$OUTPUT_DIR" unigram_index.txt || true

JARS_CLASSPATH=".:./lib/hadoop-core-1.2.1.jar:./lib/commons-logging-1.2.jar:./lib/commons-configuration-1.10.jar:./lib/commons-lang-2.6.jar:./lib/commons-httpclient-3.1.jar:./lib/org.codehaus.jackson.core.jar:./lib/org.codehaus.jackson.mapper.jar"
echo "[compile] javac -classpath $JARS_CLASSPATH -d . ${CLASS}.java"
javac -classpath "$JARS_CLASSPATH" -d . ${CLASS}.java
echo "[run] java -classpath $JARS_CLASSPATH ${CLASS} $INPUT_DIR $OUTPUT_DIR"
java -classpath "$JARS_CLASSPATH" ${CLASS} "$INPUT_DIR" "$OUTPUT_DIR"

if [ -f "$OUTPUT_DIR/part-r-00000" ]; then
  cp "$OUTPUT_DIR/part-r-00000" unigram_index.txt
  echo "[done] unigram_index.txt created (source: $OUTPUT_DIR/part-r-00000)"
  echo "[sample] First 5 lines:"; head -5 unigram_index.txt || true
else
  echo "[error] Part file missing at $OUTPUT_DIR/part-r-00000" >&2
  exit 2
fi
