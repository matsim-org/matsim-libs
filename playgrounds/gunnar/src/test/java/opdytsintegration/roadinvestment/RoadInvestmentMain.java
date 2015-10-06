package opdytsintegration.roadinvestment;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opdytsintegration.MATSimDecisionVariableSetEvaluator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controler.TerminationCriterion;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.convergencecriteria.ObjectiveFunctionChangeConvergenceCriterion;
import floetteroed.opdyts.logging.AlphaStatistic;
import floetteroed.opdyts.logging.InterpolatedObjectiveFunctionValue;
import floetteroed.opdyts.searchalgorithms.TrajectorySamplingSelfTuner;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class RoadInvestmentMain {

	static void solveFictitiousProblem() throws FileNotFoundException {

		System.out.println("STARTED ...");

		final PrintWriter writer = new PrintWriter("./RoadInvestmentMainTestLog.txt");
		writer.println("equilgapweight\tuniformityweight");
		
		final TrajectorySamplingSelfTuner selfTuner = new TrajectorySamplingSelfTuner(
				0.0, 0.0, 0.0, 0.95, 1.0);

		final int replications = 5;
		for (int replication = 0; replication < replications; replication++) {

			Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
			config.controler()
					.setOverwriteFileSetting(
							OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setLastIteration(100);
			config.global().setRandomSeed(new Random().nextLong());

			final Controler controler = new Controler(config);

			Map<Link, Double> link2freespeed = new LinkedHashMap<Link, Double>();
			Map<Link, Double> link2capacity = new LinkedHashMap<Link, Double>();
			for (Link link : controler.getScenario().getNetwork().getLinks()
					.values()) {
				link2freespeed.put(link, link.getFreespeed());
				link2capacity.put(link, link.getCapacity());
			}
			link2freespeed = Collections.unmodifiableMap(link2freespeed);
			link2capacity = Collections.unmodifiableMap(link2capacity);

			final RoadInvestmentStateFactory stateFactory = new RoadInvestmentStateFactory();
			final RoadInvestmentObjectiveFunction objectiveFunction = new RoadInvestmentObjectiveFunction();
			final Set<RoadInvestmentDecisionVariable> decisionVariables = new LinkedHashSet<RoadInvestmentDecisionVariable>();

			// 9 DECISION VARIABLES

			// decisionVariables.add(new EquilnetDecisionVariable(0.25, 0.25,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.25, 0.5,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.25, 0.75,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.5, 0.25,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.5, 0.5,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.5, 0.75,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.75, 0.25,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.75, 0.5,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.75, 0.75,
			// link2freespeed, link2capacity));

			// 16 DECISION VARIABLES

			// decisionVariables.add(new EquilnetDecisionVariable(0.0, 0.0,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.33, 0.0,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.67, 0.0,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(1.0, 0.0,
			// link2freespeed, link2capacity));
			//
			// decisionVariables.add(new EquilnetDecisionVariable(0.0, 0.33,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.33, 0.33,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.67, 0.33,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(1.0, 0.33,
			// link2freespeed, link2capacity));
			//
			// decisionVariables.add(new EquilnetDecisionVariable(0.0, 0.67,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.33, 0.67,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.67, 0.67,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(1.0, 0.67,
			// link2freespeed, link2capacity));
			//
			// decisionVariables.add(new EquilnetDecisionVariable(0.0, 1.0,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.33, 1.0,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(0.67, 1.0,
			// link2freespeed, link2capacity));
			// decisionVariables.add(new EquilnetDecisionVariable(1.0, 1.0,
			// link2freespeed, link2capacity));

			// 36 DECISION VARIABLES

			decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.0,
					link2freespeed, link2capacity));

			decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.2,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.2,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.2,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.2,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.2,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.2,
					link2freespeed, link2capacity));

			decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.4,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.4,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.4,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.4,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.4,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.4,
					link2freespeed, link2capacity));

			decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.6,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.6,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.6,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.6,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.6,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.6,
					link2freespeed, link2capacity));

			decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.8,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.8,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.8,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.8,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.8,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.8,
					link2freespeed, link2capacity));

			decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 1.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 1.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 1.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 1.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 1.0,
					link2freespeed, link2capacity));
			decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 1.0,
					link2freespeed, link2capacity));

			List<RoadInvestmentDecisionVariable> shuffle = new ArrayList<RoadInvestmentDecisionVariable>(
					decisionVariables);
			Collections.shuffle(shuffle);
			decisionVariables.clear();
			decisionVariables.addAll(shuffle);

			final int minimumAverageIterations = 5;
			final ObjectiveFunctionChangeConvergenceCriterion convergenceCriterion = new ObjectiveFunctionChangeConvergenceCriterion(
					1e-1, 1e-1, minimumAverageIterations);

			final TrajectorySampler trajectorySampler = new TrajectorySampler(
					decisionVariables, objectiveFunction, convergenceCriterion,
					MatsimRandom.getRandom(),
					selfTuner.getEquilibriumGapWeight(),
					selfTuner.getUniformityWeight());
			
			// TODO
			writer.println(selfTuner.getEquilibriumGapWeight() + "\t" + selfTuner.getUniformityWeight());
			writer.flush();
			
			trajectorySampler.addStatistic("./mylog.txt",
					new InterpolatedObjectiveFunctionValue());
			trajectorySampler.addStatistic("./mylog.txt", new AlphaStatistic(
					decisionVariables));

			final MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable>(
					trajectorySampler, decisionVariables, stateFactory);

			predictor.setMemory(1);
			predictor.setBinSize_s(10 * 60);
			predictor.setStartBin(6 * 5);
			predictor.setBinCnt(6 * 20);
			controler.addControlerListener(predictor);
			controler.setTerminationCriterion(new TerminationCriterion() {
				@Override
				public boolean continueIterations(int iteration) {
					return !predictor.foundSolution();
				}
			});
			controler.run();

			final DecisionVariable finalDecisionVariable = trajectorySampler
					.getConvergedDecisionVariables().iterator().next();
			selfTuner.registerSamplingStageSequence(trajectorySampler
					.getSamplingStages(), trajectorySampler
					.getFinalObjectiveFunctionValue(finalDecisionVariable),
					trajectorySampler.getInitialGradientNorm(),
					finalDecisionVariable);

		}

		writer.flush();
		writer.close();
		
		System.out.println("... DONE.");

	}

	public static void main(String[] args) throws FileNotFoundException {

		solveFictitiousProblem();

	}
}
