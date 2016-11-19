package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

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
	public static void main(String[] args) {
		boolean equil = true ;
		boolean calib = true ;
		boolean modeChoice = false ;
		boolean assignment = false ;

		final Config config = KNBerlinControler.prepareConfig(args, assignment, equil, modeChoice) ;
		
		config.transitRouter().setDirectWalkFactor(1.e7);
		
		String outputDirectory = "" ;
		if ( calib ) {
			if ( equil ) {
				config.plans().setInputFile("/Users/nagel/kairuns/equil/relaxed/output_plans.xml.gz");
				outputDirectory = "/Users/nagel/kairuns/equil/opdyts/" ;
			} else {
				config.plans().setInputFile("/Users/nagel/kairuns/a100/relaxed/output_plans.xml.gz");
				outputDirectory = "/Users/nagel/kairuns/a100/opdyts/" ;
			}
		} else {
			if ( equil ) {
				outputDirectory = "/Users/nagel/kairuns/equil/relaxed/" ;
			} else {
				outputDirectory = "/Users/nagel/kairuns/a100/relaxed/" ;
			}
		}
		config.controler().setOutputDirectory(outputDirectory);

		run(config, equil, calib, assignment, outputDirectory, false) ;
	}
	
	public static void run( Config config, boolean equil, boolean calib, boolean assignment, String outputDirectory, boolean testcase ) {

		OutputDirectoryLogging.catchLogEntries();

		System.out.println("STARTED ...");

		
		config.planCalcScore().setBrainExpBeta(1.);

		config.strategy().clearStrategySettings();
		{
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() );
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		if ( !equil ) {
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute.name() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode.name() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
			// note that changeMode _always_ includes ReRoute!
		}
		if ( equil ) {
			config.changeMode().setModes(new String[]{"car","pt"});
		} else {
			config.changeMode().setModes(new String[]{"car","pt","bike","walk"});
		}

		// ===

		boolean unterLindenQuiet = false ;
		final Scenario scenario = KNBerlinControler.prepareScenario(equil, unterLindenQuiet, config) ;

		// ===

		AbstractModule overrides = KNBerlinControler.prepareOverrides(assignment);
		overrides = AbstractModule.override(Arrays.asList(overrides), new AbstractModule(){
			@Override public void install() {
				bind( CharyparNagelScoringParametersForPerson.class ).to( EveryIterationScoringParameters.class ) ;
			}
		}  ) ;

		// ===

		if ( calib ) {

			final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 3600, 24);
			final MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>( new MATSimStateFactoryImpl<>(), 
					scenario, timeDiscretization); 
			simulator.addOverridingModule( overrides ) ;

			int maxIterations = 10 ;
			int maxTransitions = Integer.MAX_VALUE ;
			int populationSize = 10 ;
			boolean interpolate = true ;
			boolean includeCurrentBest = false ;
			final ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable( scenario.getConfig().planCalcScore(), scenario );
			final FixedIterationNumberConvergenceCriterion convergenceCriterion ;
			if ( testcase ) {
				convergenceCriterion= new FixedIterationNumberConvergenceCriterion(2, 1 );
			} else {
				convergenceCriterion= new FixedIterationNumberConvergenceCriterion(100, 10 );
			}
			RandomSearch<ModeChoiceDecisionVariable> randomSearch = new RandomSearch<>( simulator,
					new ModeChoiceRandomizer(scenario) ,
					initialDecisionVariable ,
					convergenceCriterion ,
					maxIterations, maxTransitions, populationSize, 
					MatsimRandom.getRandom(),
					interpolate,
					new ModeChoiceObjectiveFunction(equil),
					includeCurrentBest ) ;

			randomSearch.setLogPath( outputDirectory );

			// ---

			randomSearch.run(new SelfTuner(0.95));

		} else {
			config.controler().setLastIteration(1000);
			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
			config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

			Controler controler = new Controler( scenario ) ;
			controler.addOverridingModule( overrides );
			controler.run();

		}

		System.out.println("... DONE.");

	}

}
