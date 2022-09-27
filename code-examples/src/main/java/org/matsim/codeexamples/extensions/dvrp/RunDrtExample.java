package org.matsim.codeexamples.extensions.dvrp;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import org.matsim.contrib.otfvis.OTFVisLiveModule;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

class RunDrtExample{
	// todo:
	// * have at least one drt use case in the "examples" project, so it can be addressed via ExamplesUtils
	// * remove the DrtRoute.class thing; use Attributable instead (Route will have to be made implement Attributable).  If impossible, move the DrtRoute
	// class thing to the core.
	// * move consistency checkers into the corresponding config groups.
	// * make MultiModeDrt and normal DRT the same.  Make config accordingly so that 1-mode drt is just multi-mode with one entry.


	private static final String DRT_A = "drt_A";
	private static final String DRT_B = "drt_B";
	private static final String DRT_C = "drt_C";

	public static void main( String... args ) {
		run(true, args);
	}

	public static void run(boolean otfvis, String... args ){
		Config config;
		if ( args!=null && args.length>=1 ) {
			config = ConfigUtils.loadConfig( args );
		} else {
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "dvrp-grid" ), "multi_mode_one_shared_taxi_config.xml" ) );
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		}
		
		config.controler().setLastIteration( 1 );

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);

		@SuppressWarnings("unused")
		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
		// (config group needs to be "materialized")

		MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.mode = DRT_A;
			drtConfig.stopDuration = 60.;
			drtConfig.maxWaitTime=900;
			drtConfig.maxTravelTimeAlpha = 1.3;
			drtConfig.maxTravelTimeBeta=10. * 60.;
			drtConfig.rejectRequestIfMaxWaitOrTravelTimeViolated= false ;
			drtConfig.vehiclesFile="one_shared_taxi_vehicles_A.xml";
			drtConfig.changeStartLinkToLastLinkInSchedule=true;
			drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.mode = DRT_B;
			drtConfig.stopDuration = 60.;
			drtConfig.maxWaitTime=900;
			drtConfig.maxTravelTimeAlpha = 1.3;
			drtConfig.maxTravelTimeBeta=10. * 60.;
			drtConfig.rejectRequestIfMaxWaitOrTravelTimeViolated= false ;
			drtConfig.vehiclesFile="one_shared_taxi_vehicles_B.xml";
			drtConfig.changeStartLinkToLastLinkInSchedule=true;
			drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.mode = DRT_C;
			drtConfig.stopDuration = 60.;
			drtConfig.maxWaitTime=900;
			drtConfig.maxTravelTimeAlpha = 1.3;
			drtConfig.maxTravelTimeBeta=10. * 60.;
			drtConfig.rejectRequestIfMaxWaitOrTravelTimeViolated= false ;
			drtConfig.vehiclesFile="one_shared_taxi_vehicles_C.xml";
			drtConfig.changeStartLinkToLastLinkInSchedule=true;
			drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
			multiModeDrtCfg.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore(), config.plansCalcRoute());
		}
		{
			// clear strategy settings from config file:
			config.strategy().clearStrategySettings();

			// configure mode innovation so that travellers start using drt:
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

		if (otfvis) {
			OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
			otfVisConfigGroup.setLinkWidth(5);
			otfVisConfigGroup.setDrawNonMovingItems(true);
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run() ;
	}

}
