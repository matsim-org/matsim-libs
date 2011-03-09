package playground.kai.test;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;


public class ScenarioLoaderAlternatives {

	public static void main ( String[] args ) throws IOException {
		
		String cFilename = "config.xml" ;
		
//		// option 1
//		{		
//			Controler ctrl = new Controler( cFilename ) ; // convenience implementation of 3
//		}
//		
//		// option 2
//		{
//			ScenarioLoader scl = new ScenarioLoaderImpl( cFilename ) ;
//
//			Config config = scl.getScenario().getConfig() ; // unschoen, 3 ist besser
//			config.scenario().isUseVehicles();
//
//			Scenario sc = scl.loadScenario() ;
//		}
		
		// option 3
		{
			Config config = ConfigUtils.loadConfig( cFilename ) ;
			
			config.scenario().isUseVehicles();
			
			ScenarioLoader scl = new ScenarioLoaderImpl( config ) ;
			Scenario sc = scl.loadScenario() ;
			
			Controler ctrl = new Controler( (ScenarioImpl) sc ) ; // besser mit "Scenario"
			
		}
		
		// discussed 9mar2011
//		{
//			Config config = ConfigUtils.loadConfig( filename ) ;
//			
//			Scenario sc = ScenarioUtils.loadScenario( config ) ;
//			
//			Controler ctrl = new Controler(sc) ;
//			ctrl.setOverwriteFiles(true ;)
//			ctrl.run();
//		}
		
//		// option 4
//		{
//			Config config = ConfigUtils.loadConfig( cFilename );
//			config.scenario().isUseVehicles();
//			
//			Controler ctrl = new Controler( config ) ; // convenience implementation of 3
//		}			
		
		
		
		

//		String configFileName = args[0] ;
//
//		{
//
//			// create scenario loader from configfile:
//			ScenarioLoader scl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader( configFileName ) ;
//
//			// load scenario:
//			Scenario sc = scl.loadScenario() ;
//
//		}
//
//		/////
//
//		{
//
////			// create my own scenario from nothing:
//////			Scenario scenario = new MyScenarioImpl() ;
////			Scenario scenario = (new ScenarioFactoryImpl()).createScenario() ;
////
////			// load scenario based on my own Scenario and based on configFileName:
////			ScenarioLoaderI scl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader( configFileName, scenario ) ;
////			
////			scl.loadScenario() ;
//			
//		}


	}

}
