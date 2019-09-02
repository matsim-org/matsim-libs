package org.matsim.contrib.accidents;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
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

    //First looking if the program run and then comparing the results
    
//    BufferedReader br = IOUtils.getBufferedReader(outputDirectory + "");
//    
//    String line = null;
//    try {
//    	line = br.readLine();  	
//    } catch (IOException e) {
//    e.printStackTrace();
//    }
//    
//    try  {
//    	int lineCounter=0;
//    	while ((line = br.readLine()) != null) {
//    		String [] columns = line.split(";");
//    		
//    	}
//    	}
//    }
}
}
