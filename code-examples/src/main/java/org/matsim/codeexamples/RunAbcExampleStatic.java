package org.matsim.codeexamples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

class RunAbcExampleStatic{

	public static void main( String[] args ){

		Config config = prepareConfig( args );

		Scenario scenario = prepareScenario( config );

		Controler controler = prepareControler( scenario );

		run( controler );

	}

	static void run( Controler controler ){
		controler.run() ;
	}

	static Controler prepareControler( Scenario scenario ){
		return new Controler( scenario );
	}

	static Scenario prepareScenario( Config config ){
		return ScenarioUtils.loadScenario( config );
	}

	static Config prepareConfig( String[] args ){
		Config config ;
		if ( args!=null && args.length > 0 ){
			config = ConfigUtils.loadConfig( args[0] );
		} else {
			throw new RuntimeException("need a config file; aborting") ;
		}
		return config;
	}

}
