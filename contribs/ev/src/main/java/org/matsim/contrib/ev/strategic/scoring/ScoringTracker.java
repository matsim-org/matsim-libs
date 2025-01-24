package org.matsim.contrib.ev.strategic.scoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

/**
 * This class tracks the charging scorign procses and writes out all scoring
 * contributions to an analysis file.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ScoringTracker {
    static public final String OUTPUT_NAME = "sevc_scores.csv.gz";

    private final int interval;
    private final OutputDirectoryHierarchy outputHierarchy;

    private BufferedWriter writer = null;

    public ScoringTracker(OutputDirectoryHierarchy outputHierarchy, int interval) {
        this.outputHierarchy = outputHierarchy;
        this.interval = interval;
    }

    public void start(int iteration, boolean isLastIteration) {
        if (interval > 0 && (iteration % interval == 0 || isLastIteration)) {
            String path = outputHierarchy.getIterationFilename(iteration, OUTPUT_NAME);
            writer = IOUtils.getBufferedWriter(path);

            try {
                writer.write(String.join(";", new String[] {
                        "time", "person_id", "score", "cause", "value"
                }) + "\n");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void trackScore(double time, Id<Person> personId, String dimension, double score, Double value) {
        if (writer != null) {
            try {
                writer.write(String.join(";", new String[] {
                        String.valueOf(time),
                        personId.toString(), String.valueOf(score), dimension,
                        value == null ? "" : String.valueOf(value)
                }) + "\n");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void finish() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            writer = null;
        }
    }
}
