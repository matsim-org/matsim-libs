package org.matsim.codeexamples.config.commandline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class RunWithCommandLineOptionsExampleTest{
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void testMain(){

		RunWithCommandLineOptionsExample.main( new String [] {"--config:controler.outputDirectory=" + utils.getPackageInputDirectory()} ) ;

	}
}
