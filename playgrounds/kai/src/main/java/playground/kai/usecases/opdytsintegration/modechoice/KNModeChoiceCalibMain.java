package playground.kai.usecases.opdytsintegration.modechoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 * 
 */
class KNModeChoiceCalibMain {

	static void solveFictitiousProblem() {
		OutputDirectoryLogging.catchLogEntries();

		System.out.println("STARTED ...");

		final Config config = ConfigUtils.loadConfig("examples/equil-extended/config.xml");

		config.controler() .setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(10);

		config.global().setRandomSeed(4711);

		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		//		{
		//			ModeRoutingParams pars = new ModeRoutingParams(TransportMode.walk) ;
		//			pars.setBeelineDistanceFactor(1.3);
		//			pars.setTeleportedModeSpeed(4/3.6);
		//			config.plansCalcRoute().addModeRoutingParams(pars);
		//		}
		//		{
		//			ModeRoutingParams pars = new ModeRoutingParams(TransportMode.bike) ;
		//			pars.setBeelineDistanceFactor(1.3);
		//			pars.setTeleportedModeSpeed(8/3.6);
		//			config.plansCalcRoute().addModeRoutingParams(pars);
		//		}
		//		{
		//			ModeRoutingParams pars = new ModeRoutingParams(TransportMode.pt) ;
		//			pars.setBeelineDistanceFactor(1.3);
		//			pars.setTeleportedModeSpeed(8/3.6);
		//			config.plansCalcRoute().addModeRoutingParams(pars);
		//		}
		//		{
		//			ModeRoutingParams pars = new ModeRoutingParams("pt2") ;
		//			pars.setBeelineDistanceFactor(1.3);
		//			pars.setTeleportedModeSpeed(16./3.6);
		//			config.plansCalcRoute().addModeRoutingParams(pars);
		//		}
		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
		}
		//		config.planCalcScore().addModeParams(new ModeParams("pt2"));
		for ( ModeParams modeParams : config.planCalcScore().getModes().values() ) {
			modeParams.setConstant(0.);
			modeParams.setMarginalUtilityOfTraveling(0.); // only marg utl of time as resource
			modeParams.setMarginalUtilityOfDistance(0.); // possibly make this larger
			modeParams.setMonetaryDistanceRate(0.);
		}
		config.planCalcScore().getModes().get( TransportMode.car ).setMonetaryDistanceRate(-0.3/1000); // per meter!
		config.planCalcScore().getModes().get( TransportMode.pt ).setMonetaryDistanceRate(-0.2/1000); // per meter!
		//		config.planCalcScore().getModes().get( "pt2" ).setMonetaryDistanceRate(-0.2/1000); // per meter!
		{
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() );
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultStrategy.ChangeTripMode.name() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}

		config.changeMode().setIgnoreCarAvailability(true);
		config.changeMode().setModes( new String[] {TransportMode.car, TransportMode.pt} );
		//		config.changeMode().setModes( new String[] {TransportMode.car, TransportMode.pt, "pt2"} );

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn);

		// ===

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// ===

		//		final TimeDiscretization timeDiscretization = new TimeDiscretization(5 * 3600, 10 * 60, 18);
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 96*3600, 1);
		final MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>( new MATSimStateFactoryImpl<>(), 
				scenario, timeDiscretization); 
		simulator.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				bindScoringFunctionFactory().to(CharyparNagelScoringFunctionFactory.class);
				bind(CharyparNagelScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
			}
		} ) ;

//		final RandomSearch.Builder<ModeChoiceDecisionVariable> builder = new RandomSearch.Builder<ModeChoiceDecisionVariable>().
//				setSimulator(simulator).
//				setRandomizer(new ModeChoiceRandomizer(scenario)).
//				setInitialDecisionVariable(new ModeChoiceDecisionVariable( scenario.getConfig().planCalcScore(), scenario )).
//				setConvergenceCriterion(new FixedIterationNumberConvergenceCriterion( 100, 10)).
//				setRnd(MatsimRandom.getRandom()).
//				setObjectiveFunction(new ModeChoiceObjectiveFunction()) ;
//		final RandomSearch<ModeChoiceDecisionVariable> randomSearch = builder.build() ;
		
		int maxIterations = 10 ;
		int maxTransitions = Integer.MAX_VALUE ;
		int populationSize = 10 ;
		boolean interpolate = true ;
		boolean includeCurrentBest = false ;
		RandomSearch<ModeChoiceDecisionVariable> randomSearch = new RandomSearch<>( simulator,
				new ModeChoiceRandomizer(scenario) ,
				new ModeChoiceDecisionVariable( scenario.getConfig().planCalcScore(), scenario ) ,
				new FixedIterationNumberConvergenceCriterion(100, 10 ) ,
				maxIterations, maxTransitions, populationSize, 
				MatsimRandom.getRandom(),
				interpolate,
				new ModeChoiceObjectiveFunction(),
				includeCurrentBest ) ;
		
		randomSearch.setLogPath("./");

		// ===

		randomSearch.run(new SelfTuner(0.95));

		System.out.println("... DONE.");

	}

	public static void main(String[] args) {

		solveFictitiousProblem();

	}

}
