package org.matsim.core.scoring.functions;

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters.Type;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class writes person specific information from {@link IndividualPersonScoringParameters} to the output.
 */
public class IndividualPersonScoringOutputWriter implements IterationEndsListener {


	@Inject
	private ScoringParametersForPerson scoring;

	private boolean outputWritten = false;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		if (outputWritten)
			return;

		if (!(scoring instanceof IndividualPersonScoringParameters params))
			return;

		OutputDirectoryHierarchy io = event.getServices().getControlerIO();

		String output = io.getOutputFilename("person_util_variations.csv.gz");

		ScoringConfigGroup config = event.getServices().getConfig().scoring();

		// Collect relevant agents, modes amd types
		Set<String> subpopulations = new LinkedHashSet<>();
		Set<String> excludeSubpopulations = new LinkedHashSet<>();
		Set<String> modes = new LinkedHashSet<>();
		Set<ModeUtilityParameters.Type> paramsTypes = new LinkedHashSet<>();

		for (Map.Entry<String, ScoringConfigGroup.ScoringParameterSet> e : config.getScoringParametersPerSubpopulation().entrySet()) {

			TasteVariationsConfigParameterSet tasteVariationsParams = e.getValue().getTasteVariationsParams();
			if (tasteVariationsParams != null) {
				subpopulations.add(e.getKey());
				excludeSubpopulations.addAll(tasteVariationsParams.getExcludeSubpopulations());
				paramsTypes.addAll(tasteVariationsParams.getVariationsOf());
				modes.addAll(e.getValue().getModes().keySet());
			}
		}

		// Special case when null is present as subpopulation
		boolean allRelevant = subpopulations.contains(null);

		// Write scoring information for each person
		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(output), CSVFormat.DEFAULT)) {

			csv.print("person");

			for (String mode : modes) {
				for (Type type : paramsTypes) {
					csv.print(mode + "-" + type);
				}
			}
			csv.println();

			for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {

				ScoringParameters scoringParameters = params.getScoringParameters(person);
				Map<String, ModeUtilityParameters> modeParams = scoringParameters.modeParams;

				String subpopulation = PopulationUtils.getSubpopulation(person);

				if (excludeSubpopulations.contains(subpopulation))
					continue;

				if (!allRelevant && !subpopulations.contains(subpopulation))
					continue;

				csv.print(person.getId());
				for (String mode : modes) {
					for (Type type : paramsTypes) {
						csv.print(getValue(modeParams.get(mode), type));
					}
				}
				csv.println();
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}


		outputWritten = true;
	}

	private double getValue(ModeUtilityParameters params, Type type) {
		return switch (type) {
			case constant -> params.constant;
			case dailyUtilityConstant -> params.dailyUtilityConstant;
			case dailyMoneyConstant -> params.dailyMoneyConstant;
			case monetaryDistanceCostRate -> params.monetaryDistanceCostRate;
			case marginalUtilityOfDistance_m -> params.marginalUtilityOfDistance_m;
			case marginalUtilityOfTraveling_s -> params.marginalUtilityOfTraveling_s;
		};
	}
}
