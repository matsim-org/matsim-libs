/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.World;

import playground.yu.utils.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ActOrderChecker extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	// ------------------------------------------------------------------------
	public static class ActOder {
		public static String getActOder(Plan plan) {
			StringBuffer acts = new StringBuffer("");
			for (ActIterator ai = plan.getIteratorAct(); ai.hasNext();)
				acts.append(ai.next().getType());
			return acts.toString();
		}
	}

	// --------------------------------------------------------------------------
	private Id personId;
	private Map<Id, String> actsMap = new HashMap<Id, String>();

	public Map<Id, String> getActsMap() {
		return actsMap;
	}

	public void run(Person person) {
		personId = person.getId();
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		actsMap.put(personId, ActOder.getActOder(plan));
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String plansFilenameA = args[1];
		final String plansFilenameB = args[2];
		final String outputFilename = args[3];

		Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		Population populationA = new Population();
		ActOrderChecker aocA = new ActOrderChecker();
		populationA.addAlgorithm(aocA);
		new MatsimPopulationReader(populationA).readFile(plansFilenameA);
		populationA.runAlgorithms();

		Population populationB = new Population();
		ActOrderChecker aocB = new ActOrderChecker();
		populationB.addAlgorithm(aocB);
		new MatsimPopulationReader(populationB).readFile(plansFilenameB);
		populationB.runAlgorithms();

		SimpleWriter writer = new SimpleWriter(outputFilename);
		if (writer != null) {
			writer.writeln("personId\toriginal ActChain\tcurrent ActChain");
			Map<Id, String> actsA = aocA.getActsMap();
			Map<Id, String> actsB = aocB.getActsMap();
			int c = 0, changed = 0;
			for (Id personId : actsA.keySet()) {
				c++;
				String actChainA = actsA.get(personId);
				String actChainB = actsB.get(personId);
				if (!actChainA.equals(actChainB)) {
					writer.writeln(personId + "\t" + actChainA + "\t"
							+ actChainB);
					changed++;
				}
				writer.writeln(personId + "\t" + actChainA + "\t" + actChainB);
			}
			writer.writeln("agents :\t" + c + "\tchanged :\t" + changed);
			writer.close();
		}

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
