package org.matsim.codeexamples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public final class RunAbcSimpleExample{

	private Config config = null ;

	public static void main ( String [] args ) {
		new RunAbcSimpleExample().run() ;
	}

	public final Config prepareConfig() {
		config = ConfigUtils.loadConfig( IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) ) ;
		return config ;
	}

	void run() {
		if ( config==null ) {
			prepareConfig() ;
		}

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.run() ;

	}

}
