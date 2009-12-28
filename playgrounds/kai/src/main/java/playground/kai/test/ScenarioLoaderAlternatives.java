package playground.kai.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;


public class ScenarioLoaderAlternatives {

	public static void main ( String[] args ) {

		String configFileName = args[0] ;

		{

			// create scenario loader from configfile:
			ScenarioLoader scl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader( configFileName ) ;

			// load scenario:
			Scenario sc = scl.loadScenario() ;

		}

		/////

		{

//			// create my own scenario from nothing:
////			Scenario scenario = new MyScenarioImpl() ;
//			Scenario scenario = (new ScenarioFactoryImpl()).createScenario() ;
//
//			// load scenario based on my own Scenario and based on configFileName:
//			ScenarioLoaderI scl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader( configFileName, scenario ) ;
//			
//			scl.loadScenario() ;
			
		}


	}

}
