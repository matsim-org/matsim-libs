package org.matsim.codeexamples.config.commandline;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class RunWithCommandLineOptionsExampleTest{
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testMain(){

		RunWithCommandLineOptionsExample.main( new String [] {"--config:controler.outputDirectory=" + utils.getPackageInputDirectory()} ) ;

	}
}
