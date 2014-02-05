package org.matsim.integration.always;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		Assert.assertTrue(true);
	}

}
