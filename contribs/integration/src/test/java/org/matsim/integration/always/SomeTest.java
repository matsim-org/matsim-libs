package org.matsim.integration.always;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class SomeTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void doTest() {
		System.out.println("available ram: " + (Runtime.getRuntime().maxMemory() / 1024/1024));
		
		final String FN = "matsimExamples/tutorial/lesson-3/network.xml" ;
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario( config ) ;
		
		try {
			new MatsimNetworkReader(sc).readFile(FN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			new MatsimNetworkReader(sc).readFile("../" + FN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			new MatsimNetworkReader(sc).readFile("../../" + FN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			new MatsimNetworkReader(sc).readFile("../../../" + FN);
			// this is the one that works locally
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		Assert.assertTrue(true);
	}
	
	@Test
	public void doAccessibilityTest() {
		Config config = ConfigUtils.createConfig() ;
		
		config.network().setInputFile("../../../matsimExamples/countries/za/nmbm/network/network.xml.gz" );
		config.facilities().setInputFile("../../../matsimExamples/countries/za/nmbm/facilities/facilities.xml.gz" );
		
		config.controler().setLastIteration(0);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler(scenario) ;
		controler.setOverwriteFiles(true);
		
		// l, s, t, e, w, minor, h

		ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities() ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				if ( option.getType().equals("w") ) {
					opportunities.addActivityFacility(fac);
				}
			}
		}
		
		Double aa = 1. ;
		Double bb = 2. ;
		Double cc = aa + bb / aa ;
		
		PtMatrix ptMatrix = null  ;
		GridBasedAccessibilityControlerListenerV3 listener = 
				new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, scenario.getNetwork( ));
		listener.setComputingAccessibilityForFreeSpeedCar(true);
		listener.generateGridsAndMeasuringPointsByNetwork(scenario.getNetwork(), 100. );
		
		controler.addControlerListener(listener);
		
		controler.run() ;
		
	}

}
