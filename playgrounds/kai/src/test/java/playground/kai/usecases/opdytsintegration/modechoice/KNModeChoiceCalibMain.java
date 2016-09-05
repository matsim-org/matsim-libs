package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import floetteroed.opdyts.searchalgorithms.Simulator;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 * 
 */
class KNModeChoiceCalibMain {

	static void solveFictitiousProblem() {

		System.out.println("STARTED ...");

		final Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler() .setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		config.global().setRandomSeed(new Random().nextLong());
		
		// ===
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// ===

		DecisionVariableRandomizer<ModeChoiceDecisionVariable> randomizer = new DecisionVariableRandomizer<ModeChoiceDecisionVariable>() {
			@Override public List<ModeChoiceDecisionVariable> newRandomVariations( ModeChoiceDecisionVariable decisionVariable ) {
				final PlanCalcScoreConfigGroup scoreConfig = decisionVariable.getScoreConfig();
				List<ModeChoiceDecisionVariable> result = new ArrayList<>() ;
				for ( int ii=0 ; ii<2 ; ii++ ) {
					PlanCalcScoreConfigGroup newScoringConfig = new PlanCalcScoreConfigGroup() ;
					{
						ModeParams oldModeParams = scoreConfig.getModes().get( TransportMode.car ) ;
						ModeParams newModeParams = new ModeParams(TransportMode.car) ;
						newModeParams.setConstant( oldModeParams.getConstant() + 0.1 * MatsimRandom.getRandom().nextGaussian() );
						newModeParams.setMarginalUtilityOfDistance( oldModeParams.getMarginalUtilityOfDistance() + 0.1 * MatsimRandom.getRandom().nextGaussian() );
						newModeParams.setMarginalUtilityOfTraveling( oldModeParams.getMarginalUtilityOfTraveling() + 0.1 * MatsimRandom.getRandom().nextGaussian() );
						newScoringConfig.addModeParams(newModeParams);
					}
					result.add( new ModeChoiceDecisionVariable( newScoringConfig, scenario ) ) ;
				}
				return result ;
			}
		};

		boolean interpolate = true;
		int maxIterations = 10;
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10;

		final ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable( scenario.getConfig().planCalcScore(), scenario ) ;
		
		@SuppressWarnings("unchecked")
		final MATSimStateFactory<ModeChoiceDecisionVariable> stateFactory = new ModeChoiceStateFactory();
		
		Simulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>( stateFactory, scenario, new TimeDiscretization(5 * 3600, 10 * 60, 18)); 

		final ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction();

		final FixedIterationNumberConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion( 100, 10);

		RandomSearch<ModeChoiceDecisionVariable> randomSearch = new RandomSearch<>( simulator, randomizer,
				initialDecisionVariable, convergenceCriterion, maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, objectiveFunction, false);
		randomSearch.setLogFileName("./randomSearchLog.txt");
		
		// ===
		
		randomSearch.run(new SelfTuner(0.95));

		System.out.println("... DONE.");

	}

	public static void main(String[] args) {

		solveFictitiousProblem();

	}

}
