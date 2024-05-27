package org.matsim.core.controler;

import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

public class AbstractModuleTest{
	private static final Logger log = LogManager.getLogger( AbstractModuleTest.class );

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void test1() {

		Config config = ConfigUtils.createConfig() ;
		config.controller().setOutputDirectory( utils.getOutputDirectory() );

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Module module = new AbstractModule(){
			@Override public void install(){
				install(  new ControlerDefaultsModule() );
				install(  new ControlerDefaultCoreListenersModule() );
				install(  new NewControlerModule() );
				install(  new ScenarioByInstanceModule( scenario ) );

				this.addTravelTimeBinding( TransportMode.ride ).toProvider( new Provider<TravelTime>(){
					@Inject @Named( TransportMode.car ) TravelTime carTTime ;
					@Override public TravelTime get(){
						return carTTime ;
					}
				} );

				bind( Abc.class ) ;
				bind( Def.class ) ;

			}
		};

		com.google.inject.Injector injector = Injector.createInjector( config, module );

		Abc abc = injector.getInstance( Abc.class ) ;
		abc.run() ;

		Def def = injector.getInstance( Def.class ) ;
		def.run() ;

	}

	private static class Abc {
		@Inject Map<String,TravelTime> map ;
		@Inject @Named(TransportMode.car ) TravelTime carTTime ;
//		@Inject @Named(TransportMode.bike ) TravelTime bikeTTime ;

		void run () {
			for( Map.Entry<String, TravelTime> entry : map.entrySet() ){
				log.info( "mode=" + entry.getKey() + "; ttime=" + entry.getValue() );
			}
			log.info( "" );
			log.info( "carTTime=" + carTTime );
			log.info( "" );
//			log.info( "bikeTTime=" + bikeTTime );
//			log.info( "" );
		}

	}

	private static class Def {
		@Inject Map<String,TravelTime> map ;
		@Inject @Named(TransportMode.car ) TravelTime carTTime ;
//		@Inject @Named(TransportMode.bike ) TravelTime bikeTTime ;

		void run () {
			for( Map.Entry<String, TravelTime> entry : map.entrySet() ){
				log.info( "mode=" + entry.getKey() + "; ttime=" + entry.getValue() );
			}
			log.info( "" );
			log.info( "carTTime=" + carTTime );
			log.info( "" );
//			log.info( "bikeTTime=" + bikeTTime );
//			log.info( "" );
		}

	}

}
