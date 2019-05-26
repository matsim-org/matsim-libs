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

		URL context = ExamplesUtils.getTestScenarioURL( "equil" );
		URL config = IOUtils.newUrl( context, "config.xml" );

		System.out.println( "configfile=" + config.toString() ) ;

		String [] args = { config.toString() } ;

		RunAbcExampleStatic.main( args );

	}
}
