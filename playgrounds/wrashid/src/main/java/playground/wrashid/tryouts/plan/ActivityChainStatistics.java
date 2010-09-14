package playground.wrashid.tryouts.plan;

import java.util.HashMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

/*
 * just reads an input plans file and produces an output on the console of activity chains and their occurance in percentage.
 * TODO: ###########################  refactor class and move class to "lib.tools.plan" (implementation of interface "New Population" not required.
 */

public class ActivityChainStatistics extends NewPopulation {

	static HashMap<String, Integer> hm = new HashMap<String, Integer>();
	static int numberOfAgents = 0;

	public static void main(String[] args) {

		ScenarioImpl sc = new ScenarioImpl();

		String inputPlansFile = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\plans.xml.gz";
		String outputPlansFile = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\modified_plans.xml";
		String networkFile = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\network.xml.gz";
		// String facilitiesPath =
		// "/data/matsim/switzerland/ivt/studies/switzerland/facilities/facilities.xml.gz";

		// String inputPlansFile = "./test/scenarios/chessboard/plans.xml";
		// String outputPlansFile = "./plans1.xml";
		// String networkFile = "./test/scenarios/chessboard/network.xml";
		// String facilitiesPath = "./test/scenarios/chessboard/facilities.xml";

		// new MatsimFacilitiesReader(sc).readFile(facilitiesPath);

		Population inPop = sc.getPopulation();

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inputPlansFile);

		ActivityChainStatistics dp = new ActivityChainStatistics(net, inPop, outputPlansFile);
		dp.run(inPop);
		// dp.writeEndPlans();

		for (String activityChain : hm.keySet()) {
			System.out.println(activityChain + "\t" + (double) hm.get(activityChain) / (double) numberOfAgents + "%");
		}

	}

	public ActivityChainStatistics(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {

		if (person.getPlans().size() != 1) {
			System.err.println("Person got more than one plan");
		} else {

			numberOfAgents++;

			Plan plan = person.getPlans().get(0);
			boolean keepPlan = true;

			// only keep person if every leg is a car leg
			String activityChain = "";

			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					activityChain += ((Activity) planElement).getType() + "-";
				}
			}

			Integer value = hm.get(activityChain);

			if (value == null) {
				hm.put(activityChain, 0);
			}

			value = hm.get(activityChain);

			value++;

			hm.put(activityChain, value);

		}

	}
}
