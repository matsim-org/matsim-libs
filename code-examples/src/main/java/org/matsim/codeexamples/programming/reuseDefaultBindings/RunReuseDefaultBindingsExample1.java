package org.matsim.codeexamples.programming.reuseDefaultBindings;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

class RunReuseDefaultBindingsExample1{
	private static final Logger log = LogManager.getLogger( RunReuseDefaultBindingsExample1.class );

	public static void main( String[] args ){

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) ) ;
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( PrepareForSim.class ).toInstance( new MyPrepareForSim( scenario ) ) ;
			}
		} ) ;

		controler.run() ;
	}

	private static class MyPrepareForSim implements PrepareForSim {
		private final PrepareForSim delegate;
		MyPrepareForSim( Scenario scenario ) {
			com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig(), new AbstractModule(){
				@Override public void install(){
					install( new NewControlerModule() );
					install( new ControlerDefaultCoreListenersModule() );
					install( new ControlerDefaultsModule() );
					install( new ScenarioByInstanceModule( scenario ) );
				}
			} );;
			this.delegate = injector.getInstance( PrepareForSim.class );
		}

		@Override public void run(){
			delegate.run() ;
			log.info( "running own prepareForSim method" );
		}
	}

}
