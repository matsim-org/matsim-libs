package playground.kai.usecases.opdytsintegration.modechoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.TimeDiscretization;
import playground.kairuns.run.KNBerlinControler;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 * 
 */
class KNModeChoiceCalibMain {

	static void solveFictitiousProblem(String[] args) {
		OutputDirectoryLogging.catchLogEntries();

		System.out.println("STARTED ...");

		boolean assignment = false ;
		boolean equil = true ;
		final Config config = KNBerlinControler.prepareConfig(args, assignment, equil) ;

		// ===

		final Scenario scenario = KNBerlinControler.prepareScenario(equil, config) ;

		// ===
		
		AbstractModule overrides = KNBerlinControler.prepareOverrides(assignment);

		// ===

		//		final TimeDiscretization timeDiscretization = new TimeDiscretization(5 * 3600, 10 * 60, 18);
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 96*3600, 1);
		final MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>( new MATSimStateFactoryImpl<>(), 
				scenario, timeDiscretization); 
		simulator.addOverridingModule( overrides ) ;

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

		solveFictitiousProblem(args);

	}

}
