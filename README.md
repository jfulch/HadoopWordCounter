# HadoopWordCounter (Replit-Aligned)

This repository contains a classic Hadoop MapReduce WordCount implemented against legacy Hadoop 1.x libraries bundled manually in `lib/`.

## Runtime Environment
- Java: 17 (required; Hadoop 1.x will fail on Java 25 due to removed JAAS APIs)
- Hadoop: Not installed system-wide; the local run uses `hadoop-core-1.2.1.jar` in classpath (local job runner mode).

## Files of Interest
- `WordCount.java` – Mapper + Reducer + `main` job setup.
- `.replit` – Replit config (compile/run commands updated to match `lib/` contents).
- `run-replit-style.sh` – Optional helper script to run locally with Java 17.
- `lib/` – Legacy dependency jars (Hadoop core + commons + httpclient + Jackson).

## How to Run Locally (Mac / Linux)
```bash
# Ensure Java 17 active
export JAVA_HOME=$(/usr/libexec/java_home -v 17) 2>/dev/null || true
export PATH="$JAVA_HOME/bin:$PATH"

# Run using helper script
./run-replit-style.sh

# Or do it manually:
javac -classpath .:./lib/hadoop-core-1.2.1.jar:./lib/commons-logging-1.2.jar:./lib/commons-configuration-1.10.jar:./lib/commons-lang-2.6.jar:./lib/commons-httpclient-3.1.jar:./lib/org.codehaus.jackson.core.jar:./lib/org.codehaus.jackson.mapper.jar -d . WordCount.java
rm -rf out
java -classpath .:./lib/hadoop-core-1.2.1.jar:./lib/commons-logging-1.2.jar:./lib/commons-configuration-1.10.jar:./lib/commons-lang-2.6.jar:./lib/commons-httpclient-3.1.jar:./lib/org.codehaus.jackson.core.jar:./lib/org.codehaus.jackson.mapper.jar WordCount in out
head -20 out/part-r-00000
```

## Replit Usage
Pushing this repo to Replit will pick up `.replit` automatically:
- Compile: uses explicit classpath, no Maven needed.
- Run: executes the job and prints top 20 results.

You may optionally add `run-replit-style.sh`; Replit users can open the shell and run:
```bash
chmod +x run-replit-style.sh
./run-replit-style.sh
```
(Not required—`.replit` already handles normal execution.)

## Output
Results appear in `out/part-r-00000` with lines formatted as:
```
<word>\t<count>
```
Words include punctuation as-is; e.g. `"Twinkle,"` is distinct from `Twinkle`. To normalize, modify the mapper:
```java
String raw = itr.nextToken().toLowerCase().replaceAll("[^a-z0-9]", "");
if (!raw.isEmpty()) { word.set(raw); context.write(word, one); }
```
(Recompile after changes.)

## Migrating to Modern Hadoop (Optional)
To move to Hadoop 3.x you would:
1. Replace the jars in `lib/` with Hadoop 3.x client dependencies.
2. Restore Hadoop dependencies inside `pom.xml` (they were removed to avoid confusion).
3. Adjust code only if API incompatibilities arise (WordCount usually portable).

## Troubleshooting
| Symptom | Cause | Fix |
|---------|-------|-----|
| `UnsupportedOperationException: getSubject` | Using Java 25+ | Switch to Java 17 (`export JAVA_HOME=...`) |
| `NoClassDefFoundError: HttpMethod` | Missing httpclient jar | Ensure `lib/commons-httpclient-3.1.jar` present |
| `Output directory exists` | Hadoop local refuses overwrite | `rm -rf out` then rerun |
| Empty output file | Bad classpath or compile failed | Recompile with full classpath |

## License / Attribution
Educational use for CSCI-572 homework. JARs originate from Apache / Commons projects; confirm licensing for redistribution if publishing broadly.

---
**Quick Run Recap:** `./run-replit-style.sh` → inspect `out/part-r-00000`.

## HW3 Inverted Index Guide

This repository now contains code suitable for HW3 (unigram and selected bigram inverted indices) using local Hadoop 1.x jars.

### Environment
* Use Java 17 only (legacy Hadoop 1.2.1 breaks on 25+). Activate per shell:
	```bash
	export JAVA_HOME=$(/usr/libexec/java_home -v 17)
	export PATH="$JAVA_HOME/bin:$PATH"
	java -version
	```
* Input data directories (after unzipping HW3 data):
	* `devdata/` – 5 smaller files (fast iteration)
	* `fulldata/` – 74 larger files (final unigram run)

### 1. Unigram Inverted Index (Fulldata)
Target: build `unigram_index.txt` listing each word and per-document counts.

Run (script assumes `fulldata/` present):
```bash
chmod +x scripts/run-unigrams.sh
./scripts/run-unigrams.sh
head -20 unigram_index.txt
```
Output format sample:
```
information	5722018435.txt:12 5722018440.txt:8 5722018459.txt:3
retrieval	5722018435.txt:5 5722018447.txt:9
```
Each line: `<word><TAB><docID>:<count> ...` (order not guaranteed; counts aggregated per doc).

### 2. Selected Bigram Index (Devdata)
Target bigrams:
```
computer science
information retrieval
power politics
los angeles
bruce willis
```
Run:
```bash
chmod +x scripts/run-bigrams.sh
./scripts/run-bigrams.sh
cat selected_bigram_index.txt | grep 'information retrieval'
```
Sample line:
```
information retrieval	fileA.txt:2 fileC.txt:7
```

### 3. Fast Development Using devdata for Unigrams
You can quickly test the unigram logic on `devdata/` before the heavier `fulldata/` run:
```bash
INPUT_DIR=devdata OUTPUT_DIR=unigram_out \
	zsh scripts/run-unigrams.sh  # temporarily edit script or duplicate it
head -10 unigram_index.txt
```
If you prefer not to edit the script, copy it:
```bash
cp scripts/run-unigrams.sh scripts/run-unigrams-dev.sh
sed -i '' 's/INPUT_DIR="fulldata"/INPUT_DIR="devdata"/' scripts/run-unigrams-dev.sh
chmod +x scripts/run-unigrams-dev.sh
./scripts/run-unigrams-dev.sh
```

### 4. Cleaning Rules (Per HW Spec)
* Lowercase all text.
* Replace punctuation & digits with spaces (regex used: `[^a-z]+`).
* Handle lines with or without leading `docID<TAB>content`. If missing, filename becomes docID.
* Ignore empty tokens.

### 5. Screenshot Guidance (Rubric)
Take two screenshots:
1. Contents of output folder for unigrams (e.g. finder/terminal showing `unigram_out/` with `part-r-00000`).
2. Contents of output folder for bigrams (`bigrams_out/`).
Include the two source files: `InvertedUnigramIndex.java` & `SelectedBigramIndex.java` (already present).

### 6. Verifying Correctness
Checks:
* `wc -l unigram_index.txt` → large number of unique tokens for fulldata.
* Grep a known word across multiple docs:
	```bash
	grep '^information\b' unigram_index.txt
	```
* Ensure each target bigram appears (even if absent in some docs). Example:
	```bash
	for b in "computer science" "information retrieval" "power politics" "los angeles" "bruce willis"; do
		grep "^$b\t" selected_bigram_index.txt || echo "[missing] $b";
	done
	```

### 7. Performance Tips
If fulldata run is slow or memory heavy, you can truncate each `.txt` file (keep filenames intact) to ~50K tokens while developing, then restore originals for final run.

### 8. Optional Combined Run Script
You can now run both jobs with one command using `scripts/run-all.sh`.

#### Usage
```bash
./scripts/run-all.sh [--mode dev|full] [--skip-unigrams] [--skip-bigrams] \
	[--unigram-input DIR] [--bigram-input DIR]
```

#### Modes
* `--mode full` (default): unigrams on `data/fulldata`, bigrams on `data/devdata`.
* `--mode dev`: both unigrams and bigrams on `data/devdata` (fast iteration).

#### Examples
```bash
# Full homework run (large unigrams + selected bigrams)
./scripts/run-all.sh

# Fast dev run (everything on devdata)
./scripts/run-all.sh --mode dev

# Only unigrams, with custom output dir
./scripts/run-all.sh --skip-bigrams --unigram-input data/devdata

# Only bigrams
./scripts/run-all.sh --skip-unigrams
```

#### Output Summary
The script prints a summary block showing line counts and sample lines. Final artifacts still copied to:
* `unigram_index.txt`
* `selected_bigram_index.txt`

#### Help
```bash
./scripts/run-all.sh --help
```

### 9. Common Issues
| Symptom | Cause | Fix |
|---------|-------|-----|
| Missing `unigram_index.txt` | Script failed | Check terminal for compile errors; re-run with Java 17 |
| No bigram line | Bigram absent or cleaning removed it | Verify lowercase match and spacing; check regex |
| Slow fulldata run | Large files | Use devdata for quick tests, then fulldata for final output |

### 10. Why Mapper Emits `docID:count` (Design Note)
HW text suggests emitting `(word, docID)` and counting in reducer; current implementation pre-aggregates counts locally to reduce shuffle volume. Output format matches rubric—this optimization is acceptable unless explicitly disallowed.

---
**Quick HW3 Recap:** Run `scripts/run-unigrams.sh` for fulldata → `unigram_index.txt`; run `scripts/run-bigrams.sh` for devdata → `selected_bigram_index.txt`.
Or use `./scripts/run-all.sh` for a combined run.
