import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Math.log;

@Slf4j
public class EntropySolver {
    private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));
    private static final Set<String> ACCEPTED = newHashSet();

    public static void main(String[] args) {
        Set<String> solutions = newHashSet();
        processInput(solutions);

        // precomputed best at first step
        log.info("Best Word: {}", "soare");

        String outcome;
        try {
            outcome = getOutcome();
        } catch(IOException exception) {
            log.error("Failed to read outcome line, terminating...");
            return;
        }

        filterRemainingWords(solutions, "soare", outcome);
        log.info("{} - {}", solutions.size(), solutions.size() < 50 ? solutions : "");

        while (solutions.size() > 1) {
            Map<String, Double> entropyMap = newHashMap();
            ACCEPTED.forEach(potentialInput -> {
                Map<Integer, Integer> patterns = newHashMap();
                solutions.forEach(solution -> patterns.merge(calculatePattern(potentialInput, solution), 1, Integer::sum));
                entropyMap.put(potentialInput, getEntropy(patterns));
            });

            String bestWord = getBestWord(entropyMap);
            log.info("Best Word: {}", bestWord);
            try {
                outcome = getOutcome();
            } catch(IOException exception) {
                log.error("Failed to read outcome line, terminating...");
                return;
            }
            filterRemainingWords(solutions, bestWord, outcome);
            log.info("{} - {}", solutions.size(), solutions.size() < 100 ? solutions : "");
        }
        log.info("Solution: {}", solutions.iterator().next());
    }

    private static String getBestWord(Map<String, Double> entropyMap) {
        final Set<Map.Entry<String, Double>> wordEntrySet = entropyMap.entrySet();
        final Map.Entry<String, Double> firstWord = wordEntrySet.stream().iterator().next();

        String bestWordKey = firstWord.getKey();
        Double bestWordValue = firstWord.getValue();

        for(String wordKey : entropyMap.keySet()) {
            double wordWeight = entropyMap.get(wordKey);
            if (wordWeight > bestWordValue) {
                bestWordKey = wordKey;
                bestWordValue = wordWeight;
            }
        }

        return bestWordKey;
    }

    private static Double getEntropy(Map<Integer, Integer> patterns) {
        return patterns.values()
                .stream()
                .mapToDouble(pattern -> pattern / (ACCEPTED.size() * 1.0))
                .map(probability -> probability * log(1 / probability))
                .sum();
    }

    static int calculatePattern(String word1, String word2) {
        int pattern = 0;
        for (int i = 0; i < word1.length(); i++) {
            double pow = Math.pow(3, (word1.length() - i - 1));
            // Green
            if (word1.charAt(i) == word2.charAt(i)) {
                pattern += (2 * pow);
            } else if (word2.contains(word1.charAt(i) + "")) {
                pattern += pow;
            }
        }
        return pattern;
    }

    private static void filterRemainingWords(Set<String> solutions, String wordUsed, String outcome) {
        solutions.removeIf(word -> !isWordStillValid(word, wordUsed, outcome));
    }

    private static boolean isWordStillValid(String currentWord, String wordUsed, String outcome) {
        for (int i = 0; i < currentWord.length(); i++) {
            char used = wordUsed.charAt(i);
            char current = currentWord.charAt(i);
            boolean currentContainsUsed = currentWord.contains(used + "");
            if (outcome.charAt(i) == 'x' && currentContainsUsed && (wordUsed + "!").split(used + "").length == 2) {
                return false;
            }
            if (outcome.charAt(i) == 'y' && (!currentContainsUsed || current == used)) {
                return false;
            }
            if (outcome.charAt(i) == 'g' && current != used) {
                return false;
            }
        }
        return true;
    }

    private static String getOutcome() throws IOException {
        return READER.readLine();
    }

    private static void processInput(Set<String> solutions) {
        fileStream("wordle_solutions.txt").filter(word -> word.length() == 5)
                .forEach(solutions::add);
        fileStream("wordle_accepted.txt").filter(word -> word.length() == 5)
                .forEach(ACCEPTED::add);
    }

    public static Stream<String> fileStream(String fileName) {
        try {
            return Files.lines(Paths.get("src", "main", "resources", fileName));
        } catch (IOException e) {
            log.error("Failed to open file");
            return newArrayList("").stream();
        }
    }
}