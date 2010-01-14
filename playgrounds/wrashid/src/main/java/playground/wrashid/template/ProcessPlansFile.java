package playground.wrashid.template;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

import playground.wrashid.tryouts.plan.NewPopulation;

/*
 * In KeepOnlyMIVPlans, you find an example where facilities are also involved...
 */

public class ProcessPlansFile extends NewPopulation {
	public static void main(String[] args) {

		ScenarioImpl sc = new ScenarioImpl();

		String inputPlansFile = "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String outputPlansFile = "./test.xml.gz";
		String networkFile = "./test/scenarios/berlin/network.xml.gz";

		PopulationImpl inPop = sc.getPopulation();

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(net).readFile(networkFile);

		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inputPlansFile);

		ProcessPlansFile dp = new ProcessPlansFile(net, inPop, outputPlansFile);
		dp.run(inPop);
		dp.writeEndPlans();
	}

	public ProcessPlansFile(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {
		this.popWriter.writePerson(person);

	}
}
