package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@CommandLine.Command(
	name = "downsample-population",
	description = "Iteratively down-samples a MATSim population to multiple target sample sizes. " +
		"The input population is assumed to already represent a given share of a 100% population " +
		"(defined by --sample-size). The values provided via --samples are interpreted as absolute " +
		"target shares relative to a 100% population, not relative to the input. "
)
public class DownSamplePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DownSamplePopulation.class);

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to the input population file. The population is assumed to represent the share specified by --sample-size.")
	private Path input;

	@CommandLine.Option(names = "--sample-size", description = "Sample size of the input population relative to a 100% population (range: (0,1]).", required = true)
	private double sampleSize;

	@CommandLine.Option(names = "--samples", description = "Target sample sizes relative to a 100% population (range: (0,1]). These values are absolute shares, NOT relative to --sample-size. Example: with --sample-size=0.25 and --samples=0.1 0.05, the tool will create a 10% and 5% sample of the full population.", arity = "1..*", required = true)
	private List<Double> samples;

    @Override
    public Integer call() throws Exception {

		if (sampleSize <= 0 || sampleSize > 1)
			throw new IllegalArgumentException("--sample-size must be in (0,1]");

		for (double s : samples) {
			if (s <= 0 || s > 1)
				throw new IllegalArgumentException("All --samples must be in (0,1]");
			if (s > sampleSize)
				throw new IllegalArgumentException("Target sample " + s + " is larger than input sample size " + sampleSize);
		}

        Population population = PopulationUtils.readPopulation(input.toString());
        ScenarioUtils.putScale(population, sampleSize);

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
