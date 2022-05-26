package org.matsim.codeexamples.extensions.dvrp;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

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
			// (we need a config file so that we have a relative path to other input files)

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
			drtConfig.setMode( DRT_A ).setStopDuration(60.).setMaxWaitTime(900.).setMaxTravelTimeAlpha(1.3).setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_A.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode( DRT_B ).setStopDuration(60.).setMaxWaitTime(900.).setMaxTravelTimeAlpha(1.3).setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_B.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode( DRT_C ).setStopDuration(60.).setMaxWaitTime(900.).setMaxTravelTimeAlpha(1.3).setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_C.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
			multiModeDrtCfg.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore(), config.plansCalcRoute());
		}
		{
			// clear strategy settings from config file:
			config.strategy().clearStrategySettings();

			// configure mode choice so that travellers start using drt:
			config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DefaultStrategy.ChangeSingleTripMode ).setWeight( 0.1 ) );
			config.changeMode().setModes( new String[]{TransportMode.car, DRT_A, DRT_B, DRT_C} );

			// have a "normal" plans choice strategy:
			config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 1. ) );
		}
		{
			// add params so that scoring works:
			config.planCalcScore().addModeParams( new ModeParams( DRT_A ) );
			config.planCalcScore().addModeParams( new ModeParams( DRT_B ) );
			config.planCalcScore().addModeParams( new ModeParams( DRT_C ) );
		}
		Scenario scenario = ScenarioUtils.createScenario( config ) ;
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
		ScenarioUtils.loadScenario( scenario );
		// yyyy in long run, try to get rid of the route factory thing

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new DvrpModule() ) ;
		controler.addOverridingModule( new MultiModeDrtModule( ) ) ;

		controler.configureQSimComponents( DvrpQSimComponents.activateModes( DRT_A, DRT_B, DRT_C ) ) ;
		// yyyy in long run, try to get rid of the above line

		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		otfVisConfigGroup.setLinkWidth( 5 );
		otfVisConfigGroup.setDrawNonMovingItems( true );
//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run() ;
	}

}
