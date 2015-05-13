package opdytsintegration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opdytsintegration.roadinvestment.RoadInvestmentDecisionVariable;
import opdytsintegration.roadinvestment.RoadInvestmentObjectiveFunction;
import opdytsintegration.roadinvestment.RoadInvestmentState;
import opdytsintegration.roadinvestment.RoadInvestmentStateFactory;
import optdyts.ObjectiveFunction;
import optdyts.algorithms.SimulationNoiseVarianceEstimator;
import optdyts.surrogatesolutions.Transition;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TestEquilnet {

	static void estimateSimulationNoise() {

		System.out.println("STARTED ...");

		final Controler controler = new Controler(
				"/Nobackup/Profilen/workspace/MATSim/examples/equil/config.xml");

		controler.getConfig().getModule("controler")
				.addParam("lastIteration", "500");

		// controler.getConfig().getModule("global")
		// .addParam("randomSeed", "null");

		final File out = new File("output/equil");
		if (out.exists()) {
			IOUtils.deleteDirectory(out);
		}

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
		final ObjectiveFunction<RoadInvestmentState> objectiveFunction = new RoadInvestmentObjectiveFunction();
		final Set<RoadInvestmentDecisionVariable> decisionVariables = new LinkedHashSet<RoadInvestmentDecisionVariable>();

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.1, 1.0,
				link2freespeed, link2capacity));

		List<RoadInvestmentDecisionVariable> shuffle = new ArrayList<RoadInvestmentDecisionVariable>(
				decisionVariables);
		Collections.shuffle(shuffle);
		decisionVariables.clear();
		decisionVariables.addAll(shuffle);

		final double simulatedNoiseVariance = 208.32;
		final double g2max = 2 * simulatedNoiseVariance;

		final MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable>(
				decisionVariables, objectiveFunction, simulatedNoiseVariance,
				g2max, stateFactory);
		controler.addControlerListener(predictor);
		controler.run();

		final List<Transition<RoadInvestmentState, RoadInvestmentDecisionVariable>> transitions = predictor
				.getDecisionVariable2TransitionSequence().values().iterator()
				.next().getTransitions();
		final SimulationNoiseVarianceEstimator<RoadInvestmentState, RoadInvestmentDecisionVariable> varest = new SimulationNoiseVarianceEstimator<>();
		varest.computeEstimatedSimulationNoise(transitions.subList(
				transitions.size() / 2, transitions.size()));
		System.out.println("estimated simulation noise is "
				+ varest.getEstimatedSimulationNoise());

		System.out.println("... DONE.");
	}

	static void solveFictitiousProblem() {

		System.out.println("STARTED ...");

		final Controler controler = new Controler(
				"/Nobackup/Profilen/workspace/MATSim/examples/equil/config.xml");
		controler.getConfig().getModule("controler")
				.addParam("lastIteration", "100");
		controler
				.getConfig()
				.getModule("global")
				.addParam("randomSeed",
						Long.toString((new Random()).nextLong()));

		final File out = new File("output/equil");
		if (out.exists()) {
			IOUtils.deleteDirectory(out);
		}

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
		final ObjectiveFunction<RoadInvestmentState> objectiveFunction = new RoadInvestmentObjectiveFunction();
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

		final double simulatedNoiseVariance = 208.32;
		final double g2max = 4 * simulatedNoiseVariance;

		final MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable>(
				decisionVariables, objectiveFunction, simulatedNoiseVariance,
				g2max, stateFactory);
		controler.addControlerListener(predictor);
		controler.run();

		System.out.println("... DONE.");

	}

	public static void main(String[] args) throws FileNotFoundException {

		// estimateSimulationNoise();
		solveFictitiousProblem();

	}
}
