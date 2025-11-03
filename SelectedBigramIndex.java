import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

/**
 * Builds a selected bigram inverted index for five specified bigrams.
 * Output format: bigram -> docID:count docID:count ...
 */
public class SelectedBigramIndex {

    public static class BigramMapper extends Mapper<Object, Text, Text, Text> {
        private static final Pattern NON_ALPHA = Pattern.compile("[^a-z]+");
        private static final Set<String> TARGET_BIGRAMS = new HashSet<>(Arrays.asList(
                "computer science",
                "information retrieval",
                "power politics",
                "los angeles",
                "bruce willis"
        ));
        private String currentFile = "";
        private final Text outKey = new Text();
        private final Text outVal = new Text();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            FileSplit split = (FileSplit) context.getInputSplit();
            currentFile = split.getPath().getName();
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) return;

            String docID;
            String content;
            int tabIdx = line.indexOf('\t');
            if (tabIdx >= 0) {
                docID = line.substring(0, tabIdx).trim();
                content = line.substring(tabIdx + 1);
            } else {
                docID = currentFile; // fallback
                content = line;
            }
            if (docID.isEmpty()) docID = currentFile;

            String lowered = content.toLowerCase();
            String cleaned = NON_ALPHA.matcher(lowered).replaceAll(" ");
            cleaned = cleaned.trim();
            if (cleaned.isEmpty()) return;
            String[] tokens = cleaned.split("\\s+");
            if (tokens.length < 2) return;

            HashMap<String, Integer> localCounts = new HashMap<>();
            for (int i = 0; i < tokens.length - 1; i++) {
                String w1 = tokens[i];
                String w2 = tokens[i + 1];
                if (w1.isEmpty() || w2.isEmpty()) continue;
                String bigram = w1 + " " + w2;
                if (TARGET_BIGRAMS.contains(bigram)) {
                    localCounts.put(bigram, localCounts.getOrDefault(bigram, 0) + 1);
                }
            }
            for (Map.Entry<String, Integer> e : localCounts.entrySet()) {
                outKey.set(e.getKey());
                outVal.set(docID + ":" + e.getValue());
                context.write(outKey, outVal);
            }
        }
    }

    public static class InvertedReducer extends Reducer<Text, Text, Text, Text> {
        private final Text outVal = new Text();
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            HashMap<String, Integer> docCounts = new HashMap<>();
            for (Text tv : values) {
                String v = tv.toString();
                int colon = v.indexOf(':');
                if (colon <= 0 || colon == v.length() - 1) continue;
                String docID = v.substring(0, colon);
                int count;
                try { count = Integer.parseInt(v.substring(colon + 1)); } catch (NumberFormatException nfe) { continue; }
                docCounts.put(docID, docCounts.getOrDefault(docID, 0) + count);
            }
            if (docCounts.isEmpty()) return;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : docCounts.entrySet()) {
                sb.append(e.getKey()).append(':').append(e.getValue()).append(' ');
            }
            outVal.set(sb.toString().trim());
            context.write(key, outVal);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: SelectedBigramIndex <inputDir> <outputDir>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "selected bigram inverted index");
        job.setJarByClass(SelectedBigramIndex.class);
        job.setMapperClass(BigramMapper.class);
        job.setReducerClass(InvertedReducer.class);
        job.setCombinerClass(InvertedReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
