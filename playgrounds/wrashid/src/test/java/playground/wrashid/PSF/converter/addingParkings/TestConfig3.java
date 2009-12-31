package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class TestConfig3 extends MatsimTestCase {

	public void testConfig(){
		String configFilePath = getPackageInputDirectory() + "config3.xml";
		Config config = loadConfig(configFilePath);
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		
		new OptimizedChargerTestGeneral(controler).optimizedChargerTest();
	}
	
} 
