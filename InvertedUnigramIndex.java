import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
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
 * Builds a unigram inverted index: word -> docID:count docID:count ...
 * Assumes each input line optionally starts with docID + '\t' + content.
 * Cleans punctuation & digits to spaces and lowercases tokens.
 */
public class InvertedUnigramIndex {

    public static class UnigramMapper extends Mapper<Object, Text, Text, Text> {
        private static final Pattern NON_ALPHA = Pattern.compile("[^a-z]+");
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

            // Normalize
            String lowered = content.toLowerCase();
            String cleaned = NON_ALPHA.matcher(lowered).replaceAll(" ");
            cleaned = cleaned.trim();
            if (cleaned.isEmpty()) return;

            String[] tokens = cleaned.split("\\s+");
            HashMap<String, Integer> localCounts = new HashMap<>();
            for (String token : tokens) {
                if (token.isEmpty()) continue;
                localCounts.put(token, localCounts.getOrDefault(token, 0) + 1);
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
                try {
                    count = Integer.parseInt(v.substring(colon + 1));
                } catch (NumberFormatException nfe) {
                    continue; // skip malformed
                }
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
            System.err.println("Usage: InvertedUnigramIndex <inputDir> <outputDir>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "unigram inverted index");
        job.setJarByClass(InvertedUnigramIndex.class);
        job.setMapperClass(UnigramMapper.class);
        job.setReducerClass(InvertedReducer.class);
        // Optional combiner (safe because aggregation is associative per docID)
        job.setCombinerClass(InvertedReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
