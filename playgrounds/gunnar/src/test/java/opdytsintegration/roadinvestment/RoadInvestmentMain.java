package opdytsintegration.roadinvestment;

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

import opdytsintegration.MATSimDecisionVariableSetEvaluator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class RoadInvestmentMain {

	static void solveFictitiousProblem() {

		System.out.println("STARTED ...");

		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
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

		// final double convergenceNoiseVarianceScale = 0.001;
		final double maximumRelativeGap = 0.05;

		final MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<RoadInvestmentState, RoadInvestmentDecisionVariable>(
				decisionVariables, objectiveFunction,
				// convergenceNoiseVarianceScale,
				stateFactory, 5, maximumRelativeGap);

		predictor.setMemory(1);
		predictor.setBinSize_s(10 * 60);
		predictor.setStartBin(6 * 5);
		predictor.setBinCnt(6 * 20);
		controler.addControlerListener(predictor);
		controler.run();

		// predictor.testCrossValidation();

		System.out.println("... DONE.");

	}

	public static void main(String[] args) throws FileNotFoundException {

		solveFictitiousProblem();

	}
}
