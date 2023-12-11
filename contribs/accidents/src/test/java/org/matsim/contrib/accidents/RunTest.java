package org.matsim.contrib.accidents;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup.AccidentsComputationMethod;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura, mmayobre
 *
 *
 */
public class RunTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test1() {

		String configFile = utils.getPackageInputDirectory() + "/trial_scenario/trial_scenario_config.xml";
		String outputDirectory = utils.getOutputDirectory();
		String runId = "run1";

		Config config = ConfigUtils.loadConfig(configFile);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.controller().setOutputDirectory(outputDirectory);
		config.controller().setRunId(runId);

		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
		accidentsSettings.setEnableAccidentsModule(true);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// pre-process network
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.getAttributes().putAttribute(accidentsSettings.getAccidentsComputationMethodAttributeName(), AccidentsComputationMethod.BVWP.toString());

			int numberOfLanesBVWP;
			if (link.getNumberOfLanes() > 4){
				numberOfLanesBVWP = 4;
			} else {
				numberOfLanesBVWP = (int) link.getNumberOfLanes();
			}

			if (link.getFreespeed() > 16.) {
				link.getAttributes().putAttribute( AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME, "1,0," + numberOfLanesBVWP);
			} else {
				link.getAttributes().putAttribute( AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME, "1,2," + numberOfLanesBVWP);
			}
		}

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AccidentsModule() );

		controler.run();

		BufferedReader br = IOUtils.getBufferedReader(outputDirectory + "ITERS/it.0/run1.0.accidentCosts_BVWP.csv");

		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			int lineCounter = 0;
			while ((line = br.readLine()) != null) {

				String[] columns = line.split(";");
				for (int column = 0; column < columns.length; column++) {

					if (lineCounter == 0 && column == 25) {
						double accidentCosts = Double.valueOf(columns[column]);
						Assertions.assertEquals(10.38, accidentCosts , MatsimTestUtils.EPSILON, "wrong accident costs");
					}

					if (lineCounter == 1 && column == 25) {
						double accidentCosts = Double.valueOf(columns[column]);
						Assertions.assertEquals(16.68, accidentCosts , MatsimTestUtils.EPSILON, "wrong accident costs");
					}

				}

				lineCounter++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
