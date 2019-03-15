package org.matsim.codeexamples.config.commandline;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

public class RunWithCommandLineOptionsExampleTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testMain(){

		RunWithCommandLineOptionsExample.main( new String [] {"--config:controler.outputDirectory=" + utils.getPackageInputDirectory()} ) ;

	}
}
