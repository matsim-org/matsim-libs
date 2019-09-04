package org.matsim.codeexamples;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

public class RunAbcSimpleExampleTest{
	private static final Logger log = Logger.getLogger(RunAbcSimpleExampleTest.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void main(){
		try{
			RunAbcSimpleExample.main( new String []{"scenarios/equil/config.xml"
				  , "--config:controler.outputDirectory=" + utils.getOutputDirectory()
				  , "--config:controler.lastIteration=2"
			} );
		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}

	}

	@Test
	@Ignore
	public void main2(){
		try{
			RunAbcSimpleExample.main( new String []{ IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ).toString()
				  , "--config:controler.outputDirectory=" + utils.getOutputDirectory()
				  , "--config:controler.lastIteration=2"
			} );
		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}
	}
}
