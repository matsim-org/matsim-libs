package org.matsim.contrib.multimodal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

public class RunMultimodalExampleTest{
	private static final Logger log = LogManager.getLogger( RunMultimodalExampleTest.class ) ;
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void main(){
		URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "berlin" ), "config_multimodal.xml" );;

		String [] args = { url.toString(),
				"--config:controler.outputDirectory" , utils.getOutputDirectory(),
				"--config:routing.networkRouteConsistencyCheck", "disable"
		} ;;

		try{
			RunMultimodalExample.main( args );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assertions.fail();
		}

	}
}
