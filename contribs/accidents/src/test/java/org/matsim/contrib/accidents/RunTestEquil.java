package org.matsim.contrib.accidents;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
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

public class RunTestEquil {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test1() {
	String configFile = utils.getPackageInputDirectory() + "/equil_scenario/config.xml";
	String outputDirectory = utils.getOutputDirectory();
	String runId = "run1";

    Config config = ConfigUtils.loadConfig( configFile );
    config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

    config.controller().setOutputDirectory( outputDirectory );
    config.controller().setRunId( runId );
    config.controller().setLastIteration(0);

    AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config,  AccidentsConfigGroup.class);
    accidentsSettings.setEnableAccidentsModule(true);

    final Scenario scenario = ScenarioUtils.loadScenario(config);
    Controler controler = new Controler (scenario);
    controler.addOverridingModule(new AccidentsModule() );

    scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(10);
    scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setNumberOfLanes(3);
    scenario.getNetwork().getLinks().get(Id.createLinkId("15")).setFreespeed(10);
    scenario.getNetwork().getLinks().get(Id.createLinkId("15")).setNumberOfLanes(2);

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

    controler.run();

    BufferedReader br = IOUtils.getBufferedReader(outputDirectory + "ITERS/it.0/run1.0.accidentCosts_BVWP.csv");

	String line = null;
	try {
		line = br.readLine();
	} catch (IOException e) {
		e.printStackTrace();
	} // headers

	try {
		int lineCounter = 0;
		while ((line = br.readLine()) != null) {

			String[] columns = line.split(";");
			for (int column = 0; column < columns.length; column++) {

                //	link 22
				if (lineCounter == 1 && column == 121) {
					double accidentCosts = Double.valueOf(columns[column]);
					int agents = 100;
					int lengthKM = 35;
					double accidentCostsManualCalculation = (agents  * lengthKM  * 61.785) / 1000. * 10;
					Assertions.assertEquals(accidentCostsManualCalculation, accidentCosts , 0.01, "wrong accident costs");
				}
				// link 1
				if (lineCounter == 11 && column == 121) {
					double accidentCosts = Double.valueOf(columns[column]);
					int agents = 100;
					int lengthKM = 10;
					double accidentCostsManualCalculation = (agents  * lengthKM  * 61.785) / 1000. * 10;
					Assertions.assertEquals(accidentCostsManualCalculation, accidentCosts , 0.01, "wrong accident costs");
				}

				//  link 6
				if (lineCounter == 16 && column == 121) {
					double accidentCosts = Double.valueOf(columns[column]);
					int agents = 100;
					int lengthKM = 10;
					double accidentCostsManualCalculation = (agents  * lengthKM  * 34.735) / 1000. * 10;
					Assertions.assertEquals(accidentCostsManualCalculation, accidentCosts , 0.01, "wrong accident costs");
				}

				//  link 15
				if (lineCounter == 6 && column == 121) {
					double accidentCosts = Double.valueOf(columns[column]);
					int agents = 100;
					int lengthKM = 5;
					double accidentCostsManualCalculation = (agents  * lengthKM  * 31.63) / 1000. * 10;
					Assertions.assertEquals(accidentCostsManualCalculation, accidentCosts , 0.01, "wrong accident costs");
				}
			}

			lineCounter++;
		}
	} catch (IOException e) {
		e.printStackTrace();
	}
}
}
