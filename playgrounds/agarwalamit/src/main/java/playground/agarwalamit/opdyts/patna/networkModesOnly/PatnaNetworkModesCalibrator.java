/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import opdytsintegration.MATSimSimulator2;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.OpdytsConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.opdyts.plots.BestSolutionVsDecisionVariableChart;
import playground.agarwalamit.opdyts.plots.OpdytsConvergenceChart;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.opdytsintegration.modechoice.EveryIterationScoringParameters;

/**
 * @author amit
 */

public class PatnaNetworkModesCalibrator {

	private static final OpdytsScenario PATNA_1_PCT = OpdytsScenario.PATNA_1Pct;
	private static boolean isPlansRelaxed = true;

	public static void main(String[] args) {
		String configFile;
		String OUT_DIR = null;

		if ( args.length>0 ) {
			configFile = args[0];
			OUT_DIR = args[1];

			isPlansRelaxed = Boolean.valueOf(args[2]);;
		} else {
			configFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_networkModes/"+"/config_networkModesOnly.xml";
			OUT_DIR = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/";
		}

		Config config = ConfigUtils.loadConfig(configFile, new OpdytsConfigGroup());
		OpdytsConfigGroup opdytsConfigGroup = (OpdytsConfigGroup) config.getModules().get(OpdytsConfigGroup.GROUP_NAME);

		if(args.length==0) {
			opdytsConfigGroup.setIterationsToUpdateDecisionVariableTrial(5);
			opdytsConfigGroup.setPopulationSize(4);
		}

		String relaxedPlansDir = OUT_DIR+"/initialPlans2RelaxedPlans/";
		if (! isPlansRelaxed ) {
			// relax the plans first.
			config.controler().setOutputDirectory(relaxedPlansDir);
			PatnaNetworkModesPlansRelaxor relaxor = new PatnaNetworkModesPlansRelaxor();
			relaxor.run(config);
		}

		OUT_DIR = OUT_DIR+"/calibration_variationSize_"+opdytsConfigGroup.getVariationSizeOfRandomizeDecisionVariable()+"_AvgIts"+opdytsConfigGroup.getNumberOfIterationsForAveraging()+"/";
		config.plans().setInputFile(relaxedPlansDir+"/output_plans.xml.gz");

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things

		config.controler().setOutputDirectory(OUT_DIR);
		opdytsConfigGroup.setOutputDirectory(OUT_DIR);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// == opdyts settings
		MATSimOpdytsIntegrationRunner<ModeChoiceDecisionVariable> factories = new MATSimOpdytsIntegrationRunner<ModeChoiceDecisionVariable>(scenario);
		List<String> modes2consider = Arrays.asList("car","bike","motorbike");
		DistanceDistribution referenceStudyDistri = new PatnaNetworkModesOneBinDistanceDistribution(PATNA_1_PCT);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,referenceStudyDistri);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = factories.newMATSimSimulator( new MATSimStateFactoryImpl<>());

		String finalOUT_DIR = OUT_DIR;
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
//				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);

				addControlerListenerBinding().toInstance(new ShutdownListener() {
					@Override
					public void notifyShutdown(ShutdownEvent event) {
						// remove the unused iterations
						String dir2remove = event.getServices().getControlerIO().getOutputPath()+"/ITERS/";
						IOUtils.deleteDirectoryRecursively(new File(dir2remove).toPath());

						// post-process
						String opdytsConvergencefile = finalOUT_DIR +"/opdyts.con";
						if (new File(opdytsConvergencefile).exists()) {
							OpdytsConvergenceChart opdytsConvergencePlotter = new OpdytsConvergenceChart();
							opdytsConvergencePlotter.readFile(finalOUT_DIR +"/opdyts.con");
							opdytsConvergencePlotter.plotData(finalOUT_DIR +"/convergence.png");
						}
						BestSolutionVsDecisionVariableChart bestSolutionVsDecisionVariableChart = new BestSolutionVsDecisionVariableChart(new ArrayList<>(modes2consider));
						bestSolutionVsDecisionVariableChart.readFile(finalOUT_DIR +"/opdyts.log");
						bestSolutionVsDecisionVariableChart.plotData(finalOUT_DIR +"/decisionVariableVsASC.png");
					}
				});
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(referenceStudyDistri);

		//search algorithm
		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC, PATNA_1_PCT, null, modes2consider);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, modes2consider, PATNA_1_PCT);

		factories.run(decisionVariableRandomizer, initialDecisionVariable, objectiveFunction);
	}
}