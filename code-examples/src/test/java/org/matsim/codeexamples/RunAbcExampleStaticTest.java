package org.matsim.codeexamples;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

import static org.junit.Assert.*;

public class RunAbcExampleStaticTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test public void main(){

//		URL context = ExamplesUtils.getTestScenarioURL( "equil" );
//		URL config = IOUtils.extendUrl( context, "config.xml" );
//		final String configString = config.toString();
//		System.out.println( "configfile=" + configString ) ;
		// does not work, not exactly sure why.  kai, jul'19

		String [] args = {"scenarios/equil/config.xml"
			  ,"--config:controler.outputDirectory=" + utils.getOutputDirectory()
			  ,"--config:controler.lastIteration=2"
		} ;

		RunAbcExampleStatic.main( args );

	}
}
