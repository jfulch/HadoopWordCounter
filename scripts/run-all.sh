#!/usr/bin/env bash
# Combined runner for HW3 indices.
# Builds & runs unigram and selected bigram inverted indices.
#
# Usage:
#   ./scripts/run-all.sh [--mode dev|full] [--skip-unigrams] [--skip-bigrams] \
#       [--unigram-input DIR] [--bigram-input DIR]
#
# Defaults:
#   --mode full  -> unigrams: data/fulldata  | bigrams: data/devdata
#   --mode dev   -> unigrams: data/devdata   | bigrams: data/devdata
#
# Examples:
#   ./scripts/run-all.sh                 # full mode (big + small)
#   ./scripts/run-all.sh --mode dev      # fast iteration using only devdata
#   ./scripts/run-all.sh --skip-bigrams  # run only unigrams
#   ./scripts/run-all.sh --unigram-input data/devdata --bigram-input data/devdata
#
# Exit codes: 0 success, non-zero on any failure.

set -euo pipefail

export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}
export PATH="$JAVA_HOME/bin:$PATH"

MODE="full"
RUN_UNIGRAMS=1
RUN_BIGRAMS=1
UNIGRAM_INPUT=""
BIGRAM_INPUT=""
UNIGRAM_OUT="unigram_out"
BIGRAM_OUT="bigrams_out"

while [ $# -gt 0 ]; do
  case "$1" in
    --mode)
      MODE="$2"; shift 2;;
    --skip-unigrams)
      RUN_UNIGRAMS=0; shift;;
    --skip-bigrams)
      RUN_BIGRAMS=0; shift;;
    --unigram-input)
      UNIGRAM_INPUT="$2"; shift 2;;
    --bigram-input)
      BIGRAM_INPUT="$2"; shift 2;;
    -h|--help)
      sed -n '1,40p' "$0"; exit 0;;
    *)
      echo "[warn] Unknown arg: $1"; shift;;
  esac
done

if [ -z "$UNIGRAM_INPUT" ]; then
  if [ "$MODE" = "dev" ]; then UNIGRAM_INPUT="data/devdata"; else UNIGRAM_INPUT="data/fulldata"; fi
fi
if [ -z "$BIGRAM_INPUT" ]; then
  BIGRAM_INPUT="data/devdata"  # always devdata per HW spec
fi

echo "[info] JAVA_HOME=$JAVA_HOME"
[ "$RUN_UNIGRAMS" -eq 1 ] && echo "[info] Unigram input=$UNIGRAM_INPUT output=$UNIGRAM_OUT" || echo "[info] Skipping unigrams"
[ "$RUN_BIGRAMS" -eq 1 ] && echo "[info] Bigram input=$BIGRAM_INPUT output=$BIGRAM_OUT" || echo "[info] Skipping bigrams"

total_errors=0

if [ "$RUN_UNIGRAMS" -eq 1 ]; then
  if [ ! -d "$UNIGRAM_INPUT" ]; then
    echo "[error] Unigram input directory '$UNIGRAM_INPUT' missing"; total_errors=$((total_errors+1))
  else
    echo "[stage] Running unigrams"
    ./scripts/run-unigrams.sh "$UNIGRAM_INPUT" "$UNIGRAM_OUT" || total_errors=$((total_errors+1))
  fi
fi

if [ "$RUN_BIGRAMS" -eq 1 ]; then
  if [ ! -d "$BIGRAM_INPUT" ]; then
    echo "[error] Bigram input directory '$BIGRAM_INPUT' missing"; total_errors=$((total_errors+1))
  else
    echo "[stage] Running bigrams"
    ./scripts/run-bigrams.sh "$BIGRAM_INPUT" "$BIGRAM_OUT" || total_errors=$((total_errors+1))
  fi
fi

echo "[summary]"
if [ "$RUN_UNIGRAMS" -eq 1 ]; then
  if [ -f unigram_index.txt ]; then
    echo "  Unigram index: lines=$(wc -l < unigram_index.txt) sample:"; head -3 unigram_index.txt || true
  else
    echo "  Unigram index: MISSING"
  fi
fi
if [ "$RUN_BIGRAMS" -eq 1 ]; then
  if [ -f selected_bigram_index.txt ]; then
    echo "  Bigram index sample target lines:";
    grep -E '^(computer science|information retrieval|power politics|los angeles|bruce willis)\t' selected_bigram_index.txt || true
  else
    echo "  Bigram index: MISSING"
  fi
fi

if [ $total_errors -gt 0 ]; then
  echo "[done] Completed with $total_errors error(s)." >&2
  exit 1
else
  echo "[done] All requested jobs completed successfully.";
fi
