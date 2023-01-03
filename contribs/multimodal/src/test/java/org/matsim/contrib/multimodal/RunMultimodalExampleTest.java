package org.matsim.contrib.multimodal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

public class RunMultimodalExampleTest{
	private static final Logger log = LogManager.getLogger( RunMultimodalExampleTest.class ) ;
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void main(){
		URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "berlin" ), "config_multimodal.xml" );;

		String [] args = { url.toString(),
				"--config:controler.outputDirectory" , utils.getOutputDirectory()
		} ;

		try{
			RunMultimodalExample.main( args );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail();
		}

	}
}
