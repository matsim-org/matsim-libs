package playground.kai.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoaderI;

public class ScenarioLoaderAlternatives {

	public static void main ( String[] args ) {

		String configFileName = args[0] ;

		{

			ScenarioLoaderI scl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader( configFileName ) ;

			Scenario sc = scl.loadScenario() ;

		}

		/////

		{

			Scenario scenario = (new ScenarioFactoryImpl()).createScenario() ;
			
//			Scenario scenario = new MyScenarioImpl() ;

//			scenario.setPopulation( new MyPopulation() ) ;

			ScenarioLoaderI scl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader( configFileName, scenario ) ;
			
			scl.loadScenario() ;
			
		}


	}

}
