package org.matsim.codeexamples.extensions.dvrp;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.codeexamples.config.RunFromConfigfileExample;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.dynagent.run.DynActivityEngineModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

class RunDrtExample{
	// todo:
	// * have at least one drt use case in the "examples" project, so it can be addressed via ExamplesUtils
	// * remove the DrtRoute.class thing; use Attributable instead (Route will have to be made implement Attributable).  If impossible, move the DrtRoute
	// class thing to the core.
	// * move consistency checkers into the corresponding config groups.
	// * make MultiModeDrt and normal DRT the same.  Make config accordingly so that 1-mode drt is just multi-mode with one entry.


	private static final Logger log = Logger.getLogger( RunDrtExample.class ) ;

	public static void main( String[] args ){

		URL url = ExamplesUtils.getTestScenarioURL( "equil" );
		URL newUrl = IOUtils.newUrl( url, "config.xml" );;

		Config config = ConfigUtils.loadConfig( "scenarios/cottbus_robotaxi/config_reduced.xml" ) ;

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );

		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

//		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
//		List<String> cc = qsimComponentsConfig.getActiveComponents();
//		for( String str : cc ){
//			log.info( "component=" + str ) ;
//			if ( str.startsWith( "@" ) ) {
//				Class.forName(  ) ;
//			}
//		}
//		cc.remove( ActivityEngineModule.COMPONENT_NAME ) ;
//		cc.add( DynActivityEngineModule.COMPONENT_NAME ) ;

		MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMaxTravelTimeAlpha( 1.3 );
			drtConfig.setVehiclesFile( "taxis_2000.xml" );
			drtConfig.setMaxTravelTimeBeta( 5. * 60. );
			drtConfig.setStopDuration( 60. );
			drtConfig.setMaxWaitTime( Double.MAX_VALUE );
			drtConfig.setMode( TransportMode.drt );
			mm.addParameterSet( drtConfig );
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMaxTravelTimeAlpha( 1.3 );
			drtConfig.setVehiclesFile( "taxis_5000.xml" );
			drtConfig.setMaxTravelTimeBeta( 5. * 60. );
			drtConfig.setStopDuration( 60. );
			drtConfig.setMaxWaitTime( Double.MAX_VALUE );
			drtConfig.setMode( "drt2" );
			mm.addParameterSet( drtConfig );
		}


		config.controler().setLastIteration( 1 );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(  );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
			stratSets.setWeight( 0.1 );
			config.strategy().addStrategySettings( stratSets );
			//
			config.subtourModeChoice().setModes( new String [] {TransportMode.car, TransportMode.drt,"drt2"} );
		}
		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(  );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
			stratSets.setWeight( 1. );
			config.strategy().addStrategySettings( stratSets );
		}

		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( TransportMode.drt ) ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt2" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_walk" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}

		config.addConfigConsistencyChecker( new DrtConfigConsistencyChecker() );
		config.addConfigConsistencyChecker( new DvrpConfigConsistencyChecker() );

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		for ( Iterator<? extends Person> it = scenario.getPopulation().getPersons().values().iterator() ; it.hasNext() ; ) {
			it.next() ;
			if ( MatsimRandom.getRandom().nextDouble()<0.99 ) {
				it.remove();
			}
		}

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new DvrpModule() ) ;
		controler.addOverridingModule( new MultiModeDrtModule( ) ) ;

		controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt , "drt2") ) ;

		controler.run() ;
	}


}
