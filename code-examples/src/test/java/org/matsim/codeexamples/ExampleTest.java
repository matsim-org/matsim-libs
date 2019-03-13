package org.matsim.codeexamples;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

public class ExampleTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	// for this to work, need following in pom (scope test not transitive!):
//			<dependency>
//			<groupId>org.matsim</groupId>
//			<artifactId>matsim</artifactId>
//			<type>test-jar</type>
//			<version>...</version>
//			<scope>test</scope>
//		</dependency>
	
	@Test
	public void testMatsimTestUtils() {
		Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "/config.xml" ) ;
		
		// !! redefine the output directory: !!
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// ---
		
		controler.run() ;

		// ---

		// compare population file against some reference:
//		Population popReference = null;
//		PopulationUtils.equalPopulation( popReference, scenario.getPopulation() ) ;

		// ---

		// compare events file against some reference:
		//		String eventsFileReference = null;
//		String eventsFileActual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
//		EventsFileComparator.compare( eventsFileReference, eventsFileActual ) ;

	}
	
	@Test
	public void testExampleUtils() {
		final URL url = ExamplesUtils.getTestScenarioURL( "equil" );
		// (works when matsim-examples is specified as dependency in pom.xml)

		final URL configUrl = IOUtils.newUrl( url, "config.xml" );;
		Config config = ConfigUtils.loadConfig( configUrl ) ;

		// !! redefine the output directory: !!
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		
		config.controler().setLastIteration( 1 );
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// ---
		
		controler.run() ;
	}

	@Test
	public void testRunAbcExample() {
		RunAbcSimpleExample abc = new RunAbcSimpleExample() ;
		Config config = abc.prepareConfig() ;
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		config.controler().setLastIteration( 2 );
		abc.run() ;
	}
	
}
