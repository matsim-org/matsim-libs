package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.scenario.ScenarioUtils;

class KNBerlinControler {
	
	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;
		
		// ### prepare the config:
		Config config = ConfigUtils.loadConfig( "/Users/nagel/kairuns/a100/config.xml" ) ;
		
		// paths:
//		config.network().setInputFile("/Users/nagel/");
		config.controler().setOutputDirectory("/Users/nagel/kairuns/a100/output/");
		
		config.controler().setLastIteration(100); 
		config.controler().setWriteSnapshotsInterval(0);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(100);
		config.vspExperimental().setWritingOutputEvents(true);
		
		config.global().setNumberOfThreads(6);
		config.qsim().setNumberOfThreads(5);
		config.parallelEventHandling().setNumberOfThreads(1);
		
		double sampleFactor = 0.02 ;
		config.controler().setMobsim( MobsimType.qsim.toString() );
		config.qsim().setFlowCapFactor( sampleFactor );
//		config.qsim().setStorageCapFactor( Math.pow( sampleFactor, -0.25 ) );
		config.qsim().setStorageCapFactor(0.03);
		config.qsim().setTrafficDynamics( TrafficDynamics.withHoles );
		config.qsim().setUsingFastCapacityUpdate(true);
//		config.controler().setMobsim(MobsimType.JDEQSim.toString());
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00") ;
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "5" ) ;
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;
		
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		{
			ModeRoutingParams pars = config.plansCalcRoute().getOrCreateModeRoutingParams("pt") ;
			pars.setBeelineDistanceFactor(1.5);
			pars.setTeleportedModeSpeed( 20. / 3.6 );
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("pt2") ;
			pars.setBeelineDistanceFactor(1.5);
			pars.setTeleportedModeSpeed( 40. / 3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeParams params = config.planCalcScore().getOrCreateModeParams("pt") ;
			params.setConstant(-2.);
			params.setMarginalUtilityOfTraveling(0.);
		}
		{
			ModeParams params = new ModeParams("pt2") ;
			params.setConstant(-4.);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
		
		{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode.toString() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}
		config.setParam( ChangeLegMode.CONFIG_MODULE, ChangeLegMode.CONFIG_PARAM_MODES, "walk,bike,car,pt,pt2" );
		
//		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
//			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
//		}
		
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency();
		
		// ===
		
		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
//			if ( link.getCapacity() <= 2000. ) {
//				link.setFreespeed(20./3.6);
//			}
			if ( link.getFreespeed() >= 51./3.6 ) {
				link.setFreespeed( link.getFreespeed() * 1.2 ) ;
			}
		}

		// ===

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles ) ;
		controler.addControlerListener(new KaiAnalysisListener()) ;
//		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
//		controler.setMobsimFactory(new OldMobsimFactory()) ;

		// run everything:
		controler.run();
	
	}
	
}