package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@CommandLine.Command(
        name = "downsample-population",
        description = "Down sample a population"
)
public class DownSamplePopulation implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(DownSamplePopulation.class);

    @CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to population")
    private Path input;

    @CommandLine.Option(names = "--sample-size", description = "Sample size of the given input data in (0, 1]", required = true)
    private double sampleSize;

    @CommandLine.Option(names = "--samples", description = "Desired down-sampled sizes in (0, 1]", arity = "1..*", required = true)
    private List<Double> samples;

    @Override
    public Integer call() throws Exception {

        Population population = PopulationUtils.readPopulation(input.toString());

        samples.sort(Comparator.comparingDouble(Double::doubleValue).reversed());

        // original prefix
        String orig = String.format("%dpct", Math.round(sampleSize * 100));

        for (Double sample : samples) {

            // down-sample previous samples
            PopulationUtils.sampleDown(population, sample / sampleSize);
            sampleSize = sample;

            String path;
            if (input.toString().contains(orig))
                path = input.toString().replace(orig, String.format("%dpct", Math.round(sampleSize * 100)));
            else
                path = input.toString().replace(".xml", String.format("-%dpct.xml", Math.round(sampleSize * 100)));

            log.info("Writing {} sample to {}", sampleSize, path);

            PopulationUtils.writePopulation(population, path);
        }

        return 0;

    }
}
