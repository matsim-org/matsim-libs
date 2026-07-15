package org.matsim.contrib.vsp.scoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(
	name = "add-person-specific-modal-asc",
	description = "Adds a person specific ASC per transport mode for the provided modes."
)
public class AddPersonSpecifcAscsToPopulation implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(AddPersonSpecifcAscsToPopulation.class);

	@CommandLine.Option(names = "--population", description = "Path to input population", required = true)
	private String inputPopulationPath;
	@CommandLine.Option(names = "--output-population", description = "Path to output population", required = true)
	private String outputPopulationPath;
	@CommandLine.Option(names = "--sigma-values", description = "Sigma values for uniform distribution per mode. Format: Key=value, pairs separated by comma. " +
		"Distribution is: y = (randomDouble[0,1] - 0.5) * 2 * sigma + mean.",
		split = ",",
		defaultValue = TransportMode.bike + "=3.0")
	private Map<String, Double> sigmaPerMode;
	@CommandLine.Option(names = "--mean-values", description = "Mean values for uniform distribution per mode. Format: Key=value, pairs separated by comma. " +
		"Distribution is: y = (randomDouble[0,1] - 0.5) * 2 * sigma + mean.",
		split = ",",
		defaultValue = TransportMode.bike + "=0.0")
	private Map<String, Double> meanPerMode;

	public static void main(String[] args) {
		new AddPersonSpecifcAscsToPopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {
//		The lists have to have the same number of elements and the same elements! one sigma and mean per mode
		if (!sigmaPerMode.keySet().equals(meanPerMode.keySet())) {
			log.fatal("The provided modes for sigma values {} and the provided modes for mean values {} are not the same. Aborting!",
				sigmaPerMode.keySet(), meanPerMode.keySet());
			throw new IllegalStateException("");
		}

//		SortedSet makes for the random number generator to be called in the same order and keeps the log info below sorted alphabetically.
		SortedSet<String> modes = new TreeSet<>(sigmaPerMode.keySet());

		Scenario inputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		log.info("initialize done");
		StreamingPopulationWriter popWriter = new StreamingPopulationWriter();
		popWriter.writeStartPlans(outputPopulationPath);

		StreamingPopulationReader spr = getStreamingPopulationReader(inputScenario, modes, popWriter);
		spr.readFile(inputPopulationPath);
		popWriter.writeEndPlans();
		log.info("Population with person specific ASCs for modes {} written to {}.", modes, outputPopulationPath);

		return 0;
	}

	@NotNull
	private StreamingPopulationReader getStreamingPopulationReader(Scenario inputScenario, SortedSet<String> modes, StreamingPopulationWriter popWriter) {
		SplittableRandom splittableRandom = new SplittableRandom(1234);

		StreamingPopulationReader spr = new StreamingPopulationReader(inputScenario);
		spr.addAlgorithm(person -> {
			Map<String, Map<ModeUtilityParameters.Type, Double>> modeTasteVariations = PersonUtils.getModeTasteVariations(person);
			if (modeTasteVariations == null) {
				modeTasteVariations = new LinkedHashMap<>();
			}
			for (String mode : modes) {
				// uniform distribution
				double modeConstant = (splittableRandom.nextDouble() - 0.5) * 2 * sigmaPerMode.get(mode) + meanPerMode.get(mode);
				modeTasteVariations.computeIfAbsent(mode, (k) -> new LinkedHashMap<>())
					.put(ModeUtilityParameters.Type.constant, modeConstant);
				PersonUtils.setModeTasteVariations(person, modeTasteVariations);
			}
			popWriter.writePerson(person);
		});
		return spr;
	}
}
