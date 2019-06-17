package org.matsim.codeexamples.extensions.dvrp;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

class RunDrtExample{
	// todo:
	// * have at least one drt use case in the "examples" project, so it can be addressed via ExamplesUtils
	// * remove the DrtRoute.class thing; use Attributable instead (Route will have to be made implement Attributable).  If impossible, move the DrtRoute
	// class thing to the core.
	// * move consistency checkers into the corresponding config groups.
	// * make MultiModeDrt and normal DRT the same.  Make config accordingly so that 1-mode drt is just multi-mode with one entry.


	private static final Logger log = Logger.getLogger( RunDrtExample.class ) ;

	public static void main( String[] args ){

		String configFile ;
		if ( args!=null && args.length>=1 ) {
			configFile = args[0] ;
		} else {
			configFile = "scenarios/multi_mode_one_shared_taxi/multi_mode_one_shared_taxi_config.xml";
		}
		
		Config config = ConfigUtils.loadConfig( configFile );

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);

		@SuppressWarnings("unused")
		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode("drt_A");
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRequestRejection(false);
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_A.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{

			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode("drt_B");
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRequestRejection(false);
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_B.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}
		{

			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode("drt_C");
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRequestRejection(false);
			drtConfig.setVehiclesFile("one_shared_taxi_vehicles_C.xml");
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore());
		}

		config.controler().setOutputDirectory("output/RunDrtExample/multi_mode_one_shared_taxi");
		config.controler().setLastIteration( 1 );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(  );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
			stratSets.setWeight( 0.1 );
			config.strategy().addStrategySettings( stratSets );
			//
			config.subtourModeChoice().setModes( new String [] {TransportMode.car, "drt_A", "drt_B", "drt_C"} );
		}
		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(  );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
			stratSets.setWeight( 1. );
			config.strategy().addStrategySettings( stratSets );
		}

		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_A") ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_B" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_C" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_A_walk" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_B_walk" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( "drt_C_walk" ) ;
			config.planCalcScore().addModeParams( modeParams );
		}

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new DvrpModule() ) ;
		controler.addOverridingModule( new MultiModeDrtModule( ) ) ;

		controler.configureQSimComponents( DvrpQSimComponents.activateModes( "drt_A" , "drt_B", "drt_C") ) ;

		controler.run() ;
	}
	
	public static void adjustDrtConfig(DrtConfigGroup drtCfg, PlanCalcScoreConfigGroup planCalcScoreCfg) {
		DrtStageActivityType drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
		if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stopbased)) {
			if (planCalcScoreCfg.getActivityParams(drtStageActivityType.drtStageActivity) == null) {
				addDrtStageActivityParams(planCalcScoreCfg, drtStageActivityType.drtStageActivity);
			}
		}
		if (!planCalcScoreCfg.getModes().containsKey(drtStageActivityType.drtWalk)) {
			addDrtWalkModeParams(planCalcScoreCfg, drtStageActivityType.drtWalk);
		}
	}

	private static void addDrtStageActivityParams(PlanCalcScoreConfigGroup planCalcScoreCfg, String stageActivityType) {
		PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(stageActivityType);
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		planCalcScoreCfg.getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(params));
		planCalcScoreCfg.addActivityParams(params);
		log.info("drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
	}

	private static void addDrtWalkModeParams(PlanCalcScoreConfigGroup planCalcScoreCfg, String drtWalkMode) {
		PlanCalcScoreConfigGroup.ModeParams drtWalk = new PlanCalcScoreConfigGroup.ModeParams(drtWalkMode);
		PlanCalcScoreConfigGroup.ModeParams walk = planCalcScoreCfg.getModes().get(TransportMode.walk);
		drtWalk.setConstant(walk.getConstant());
		drtWalk.setMarginalUtilityOfDistance(walk.getMarginalUtilityOfDistance());
		drtWalk.setMarginalUtilityOfTraveling(walk.getMarginalUtilityOfTraveling());
		drtWalk.setMonetaryDistanceRate(walk.getMonetaryDistanceRate());
		planCalcScoreCfg.getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtWalk));
		log.info("drt_walk scoring parameters not set. Adding default values (same as for walk mode).");
	}


}
