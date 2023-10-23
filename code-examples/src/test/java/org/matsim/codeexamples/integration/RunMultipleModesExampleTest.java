package org.matsim.codeexamples.integration;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

public class RunMultipleModesExampleTest{
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRun() {
		try{
			Config config = RunMultipleModesExample.prepareConfig() ;

			config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controller().setOutputDirectory( utils.getOutputDirectory() );
			config.controller().setLastIteration( 2 );

			Scenario scenario = RunMultipleModesExample.prepareScenario( config );

			Controler controler = RunMultipleModesExample.prepareControler( scenario );

			controler.run() ;
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong; see stack trace" ) ;
		}
	}


}
