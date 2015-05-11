package opdytsintegration.zurich;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opdytsintegration.MATSimDecisionVariableSetEvaluator;
import optdyts.ObjectiveFunction;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TestZurich {

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final Controler controler = new Controler(
				"./test/input/zurich/config_base-case.xml");

		final File out = new File("test/input/zurich/output_base-case");
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

		final TestZurichStateFactory stateFactory = new TestZurichStateFactory();
		final ObjectiveFunction<TestZurichState> objectiveFunction = new TestZurichObjectiveFunction();
		final Set<TestZurichDecisionVariable> decisionVariables = new LinkedHashSet<TestZurichDecisionVariable>();

		// 9 DECISION VARIABLES

		// decisionVariables.add(new TestZurichDecisionVariable(0.25, 0.25,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.25, 0.5,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.25, 0.75,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.5, 0.25,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.5, 0.5,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.5, 0.75,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.75, 0.25,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.75, 0.5,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.75, 0.75,
		// link2freespeed, link2capacity));

		// 16 DECISION VARIABLES

		decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.33, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.67, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.0,
				link2freespeed, link2capacity));

		decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.33,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.33, 0.33,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.67, 0.33,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.33,
				link2freespeed, link2capacity));

		decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.67,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.33, 0.67,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.67, 0.67,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.67,
				link2freespeed, link2capacity));

		decisionVariables.add(new TestZurichDecisionVariable(0.0, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.33, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(0.67, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new TestZurichDecisionVariable(1.0, 1.0,
				link2freespeed, link2capacity));

		// 36 DECISION VARIABLES

		// decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.2, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.4, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.6, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.8, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.0,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.2,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.2, 0.2,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.4, 0.2,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.6, 0.2,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.8, 0.2,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.2,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.4,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.2, 0.4,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.4, 0.4,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.6, 0.4,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.8, 0.4,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.4,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.6,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.2, 0.6,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.4, 0.6,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.6, 0.6,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.8, 0.6,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.6,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new TestZurichDecisionVariable(0.0, 0.8,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.2, 0.8,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.4, 0.8,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.6, 0.8,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.8, 0.8,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(1.0, 0.8,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new TestZurichDecisionVariable(0.0, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.2, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.4, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.6, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(0.8, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new TestZurichDecisionVariable(1.0, 1.0,
		// link2freespeed, link2capacity));

		List<TestZurichDecisionVariable> shuffle = new ArrayList<TestZurichDecisionVariable>(
				decisionVariables);
		Collections.shuffle(shuffle);
		decisionVariables.clear();
		decisionVariables.addAll(shuffle);

		final double g2max = 2500;
		final double simulatedNoiseVariance = 1000;

		final MATSimDecisionVariableSetEvaluator<TestZurichState, TestZurichDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<TestZurichState, TestZurichDecisionVariable>(
				decisionVariables, objectiveFunction, simulatedNoiseVariance,
				g2max, stateFactory);
		controler.addControlerListener(predictor);
		controler.run();

		System.out.println("... DONE.");

	}
}
