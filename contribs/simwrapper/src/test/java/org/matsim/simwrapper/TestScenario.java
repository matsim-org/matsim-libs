package org.matsim.simwrapper;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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
	}

	@Override
	protected void prepareControler(Controler controler) {
		controler.addOverridingModule(new SimWrapperModule(sw));
	}
}
