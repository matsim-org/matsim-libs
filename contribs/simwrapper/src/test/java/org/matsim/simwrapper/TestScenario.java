package org.matsim.simwrapper;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A test scenario based on kelheim example.
 */
public class TestScenario extends MATSimApplication {

	private final SimWrapper sw;

	public TestScenario(SimWrapper sw) {
		this.sw = sw;
	}

	public static Config loadConfig(MatsimTestUtils utils) {

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(1);

		return config;
	}

	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		// TODO: update the network so this is not necessary anymore
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				HashSet<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		SplittableRandom rnd = new SplittableRandom(0);
		DefaultAnalysisMainModeIdentifier mmi = new DefaultAnalysisMainModeIdentifier();

		// Generate reference modes randomly
		Function<TripStructureUtils.Trip, String> genMode = t -> {
			double r = rnd.nextDouble();
			if (r < 0.1)
				return "car";
			else if (r < 0.2)
				return "pt";
			else if (r < 0.3)
				return "bike";

			return mmi.identifyMainMode(t.getLegsOnly());
		};

		// Assign reference modes to persons
		for (Person person : scenario.getPopulation().getPersons().values()) {

			if (rnd.nextDouble() < 0.5)
				continue;

			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
			String ref = trips.stream().map(genMode).collect(Collectors.joining("-"));

			person.getAttributes().putAttribute(TripAnalysis.ATTR_REF_ID, person.getId().toString());
			person.getAttributes().putAttribute(TripAnalysis.ATTR_REF_MODES, ref);
		}
	}

	@Override
	protected void prepareControler(Controler controler) {
		controler.addOverridingModule(new SimWrapperModule(sw));
	}
}
