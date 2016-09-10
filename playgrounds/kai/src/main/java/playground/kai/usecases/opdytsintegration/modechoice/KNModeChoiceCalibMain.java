package playground.kai.usecases.opdytsintegration.modechoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import floetteroed.opdyts.searchalgorithms.Simulator;
import opdytsintegration.MATSimSimulator;
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

		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
		}
		for ( ModeParams modeParams : config.planCalcScore().getModes().values() ) {
			modeParams.setConstant(0.);
			modeParams.setMarginalUtilityOfTraveling(0.); // only marg utl of time as resource
			modeParams.setMarginalUtilityOfDistance(0.); // possibly make this larger
			modeParams.setMonetaryDistanceRate(0.);
		}
		config.planCalcScore().getModes().get( TransportMode.car ).setMonetaryDistanceRate(-0.3/1000); // per meter!
		config.planCalcScore().getModes().get( TransportMode.pt ).setMonetaryDistanceRate(-0.2/1000); // per meter!
		config.planCalcScore().getModes().get( TransportMode.bike ).setMonetaryDistanceRate(0.) ;
		config.planCalcScore().getModes().get( TransportMode.walk ).setMonetaryDistanceRate(0.) ;
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

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn);
		
		// ===
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// ===

		DecisionVariableRandomizer<ModeChoiceDecisionVariable> randomizer = new ModeChoiceRandomizer(scenario);

		final ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable( scenario.getConfig().planCalcScore(), scenario ) ;
		
		final MATSimStateFactory<ModeChoiceDecisionVariable> stateFactory = new MATSimStateFactoryImpl<>();
		
		Simulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>( stateFactory, scenario, new TimeDiscretization(5 * 3600, 10 * 60, 18)); 

		final ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction();

		final FixedIterationNumberConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion( 100, 10);

		RandomSearch.Builder<ModeChoiceDecisionVariable> builder = new RandomSearch.Builder<>() ;
		builder.setSimulator(simulator).setRandomizer(randomizer).setInitialDecisionVariable(initialDecisionVariable).
		setConvergenceCriterion(convergenceCriterion).setRnd(MatsimRandom.getRandom()).setObjectiveFunction(objectiveFunction) ;
		RandomSearch<ModeChoiceDecisionVariable> randomSearch = builder.build() ;
		randomSearch.setLogPath("./");
		
		// ===
		
		randomSearch.run(new SelfTuner(0.95));

		System.out.println("... DONE.");

	}

	public static void main(String[] args) {

		solveFictitiousProblem();

	}

}
