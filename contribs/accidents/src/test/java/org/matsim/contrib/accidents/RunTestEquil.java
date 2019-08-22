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

public class RunTestEquil {
	
	private static String configFile;
	private static String outputDirectory;
    private static String runId;
    
    @Rule public MatsimTestUtils utils = new MatsimTestUtils();
    
    @Test 
    public void test1() 
    {
    configFile = utils.getPackageInputDirectory() + "/equil_scenario/config_test.xml";
    outputDirectory = utils.getOutputDirectory();
    runId = "run1";
    
    Config config = ConfigUtils.loadConfig(configFile);
    config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
    
    config.controler().setOutputDirectory(outputDirectory);
    config.controler().setRunId(runId);
    
    AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config,  AccidentsConfigGroup.class);
    accidentsSettings.setEnableAccidentsModule(true);
    
    final Scenario scenario = ScenarioUtils.loadScenario(config);
    Controler controler = new Controler (scenario);
    
    controler.addOverridingModule(new AccidentsModule(scenario));
    
    //TODO: changing through programming the free speed of some links so they would not be allways categorized as planfrei
    //wich implications has the type of the config file?
    //TODO: changing manually the config file so it runs with the test
    
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