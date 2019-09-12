package org.matsim.contrib.accidents;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
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
	private static String configFile;
	private static String outputDirectory;
	private static String runId;	
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void test1() {
		
		configFile = utils.getPackageInputDirectory() + "/trial_scenario/trial_scenario_config.xml";
		outputDirectory = utils.getOutputDirectory();
		runId = "run1";
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
		accidentsSettings.setEnableAccidentsModule(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AccidentsModule() );
		
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
					
					if (lineCounter == 0 && column == 25) {
						double accidentCosts = Double.valueOf(columns[column]);
						Assert.assertEquals("wrong accident costs", 10.37988, accidentCosts , 0.01);	
						//Manuel nachgerechnet: STIMMT!
					}
					
					if (lineCounter == 1 && column == 25) {
						double accidentCosts = Double.valueOf(columns[column]);
						Assert.assertEquals("wrong accident costs", 16.68195, accidentCosts , 0.01);
						//Manuel nachgerechnet: STIMMT!
					}
										
				}
				
				lineCounter++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}
}
