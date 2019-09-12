package org.matsim.contrib.accidents;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RunTestEquil {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    
    @Test 
    public void test1() 
    {
	    String configFile = utils.getPackageInputDirectory() + "/equil_scenario/config.xml";
	    String outputDirectory = utils.getOutputDirectory();
	    String runId = "run1";
    
    Config config = ConfigUtils.loadConfig( configFile );
    config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
    
    config.controler().setOutputDirectory( outputDirectory );
    config.controler().setRunId( runId );
    //Is better to take the initial config where just one of the routes is selected or better take the plans of the tenth iteration?
    config.controler().setLastIteration(0);
    
    
    AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config,  AccidentsConfigGroup.class);
    accidentsSettings.setEnableAccidentsModule(true);
    
    final Scenario scenario = ScenarioUtils.loadScenario(config);
    Controler controler = new Controler (scenario);
    controler.addOverridingModule(new AccidentsModule() );
    
    //TODO: changing through programming the free speed and number o lanes of some links so they would not be allways categorized with tehe same roadtype.
    //wich implications has the type of the config file?
    
    controler.run();

    //the total costs of link 1 differ to the one which I manually calculate
    //Link 1 three vehicles are not taken into account for the calculation, the ones before 06.00, it takes the ones at this time at the next day (29:45 for example) instead
    
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
				
				if (lineCounter == 1 && column == 121) {
					double accidentCosts = Double.valueOf(columns[column]);
					Assert.assertEquals("wrong accident costs", 2162.475, accidentCosts , 0.01);	
					//nachgerechnet: STIMMT!
				}
				
				if (lineCounter == 12 && column == 121) {
					double accidentCosts = Double.valueOf(columns[column]);
					Assert.assertEquals("wrong accident costs", 617.85, accidentCosts , 0.01);
					//nachgerechnet: STIMMT NICHT!
				}
									
			}
			
			lineCounter++;
		}
	} catch (IOException e) {
		e.printStackTrace();
	}
}
}
