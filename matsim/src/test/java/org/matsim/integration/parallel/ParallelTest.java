package org.matsim.integration.parallel;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

public class ParallelTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public void testParallel() {
		final URL url = ExamplesUtils.getTestScenarioURL( "equil" );
		final Config config = ConfigUtils.loadConfig( IOUtils.newUrl( url, "config.xml" ) );

		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		
		config.qsim().setEndTime( 24.*3600. );
		
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		
		int thisCpn = 0 ;
		
		for ( Node node : scenario.getNetwork().getNodes().values() ) {
			if ( Integer.parseInt( node.getId().toString() ) <= 11 ) {
				node.getAttributes().putAttribute( QSim.CPN_ATTRIBUTE, 0 ) ;
				if ( thisCpn==0 ) {
					node.getAttributes().putAttribute( QSim.IS_LOCAL_ATTRIBUTE, true ) ;
				} else {
					node.getAttributes().putAttribute( QSim.IS_LOCAL_ATTRIBUTE, false ) ;
				}
			} else {
				node.getAttributes().putAttribute( QSim.CPN_ATTRIBUTE, 1 ) ;
				if ( thisCpn==1 ) {
					node.getAttributes().putAttribute( QSim.IS_LOCAL_ATTRIBUTE, true ) ;
				} else {
					node.getAttributes().putAttribute( QSim.IS_LOCAL_ATTRIBUTE, false ) ;
				}
			}
		}
		
		final Controler controler = new Controler( scenario );

		controler.run() ;
	}
	
}
