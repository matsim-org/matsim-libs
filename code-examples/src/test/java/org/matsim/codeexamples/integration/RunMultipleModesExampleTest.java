package org.matsim.codeexamples.integration;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

public class RunMultipleModesExampleTest{
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRun() {
		try{
			RunMultipleModesExample runner = new RunMultipleModesExample();
			Config config = runner.prepareConfig();

			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.controler().setLastIteration( 1 );

			runner.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong; see stack trace" ) ;
		}
	}


}
