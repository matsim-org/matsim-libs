package org.matsim.codeexamples.extensions.dvrp;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

class RunDrtExample{
	// todo:
	// * have at least one drt use case in the "examples" project, so it can be addressed via ExamplesUtils
	// * remove the DrtRoute.class thing; use Attributable instead (Route will have to be made implement Attributable).  If impossible, move the DrtRoute
	// class thing to the core.
	// * move consistency checkers into the corresponding config groups.
	// * make MultiModeDrt and normal DRT the same.  Make config accordingly so that 1-mode drt is just multi-mode with one entry.


	private static final Logger log = Logger.getLogger( RunDrtExample.class ) ;

	private static final String DRT_A = "drt_A";
	private static final String DRT_B = "drt_B";
	private static final String DRT_C = "drt_C";

	public static void main( String[] args ){

		Config config;
		String configFile;
		if ( args!=null && args.length>=1 ) {
			config = ConfigUtils.loadConfig( args );
		} else {
			configFile = "scenarios/multi_mode_one_shared_taxi/multi_mode_one_shared_taxi_config.xml";
			config = ConfigUtils.loadConfig( configFile );
			config.controler().setOutputDirectory("output/RunDrtExample/multi_mode_one_shared_taxi");
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		}
		
		config.controler().setLastIteration( 1 );

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);

		@SuppressWarnings("unused")
		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode( DRT_A );
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_A.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode( DRT_B );
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_B.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode( DRT_C );
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_C.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore(), config.plansCalcRoute());
		}

		// configure mode choice so that travellers start using drt:
		config.strategy().addStrategySettings( new StrategySettings(  ).setStrategyName( DefaultStrategy.SubtourModeChoice ).setWeight( 0.1 ) );
		config.subtourModeChoice().setModes( new String [] {TransportMode.car, DRT_A, DRT_B, DRT_C} );

		// have a "normal" plans choice strategy:
		config.strategy().addStrategySettings( new StrategySettings(  ).setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 1. ) );

		// add params so that scoring works:
		config.planCalcScore().addModeParams( new ModeParams( DRT_A ) );
		config.planCalcScore().addModeParams( new ModeParams( DRT_B ) );
		config.planCalcScore().addModeParams( new ModeParams( DRT_C ) );

//		config.planCalcScore().addModeParams( new ModeParams( "drt_A_walk" ) );
//		config.planCalcScore().addModeParams( new ModeParams( "drt_B_walk" ) );
//		config.planCalcScore().addModeParams( new ModeParams( "drt_C_walk" ) );
		// now seems to work without these.  kai, dec'20

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new DvrpModule() ) ;
		controler.addOverridingModule( new MultiModeDrtModule( ) ) ;

		controler.configureQSimComponents( DvrpQSimComponents.activateModes( DRT_A, DRT_B, DRT_C ) ) ;

		controler.run() ;
	}

}
