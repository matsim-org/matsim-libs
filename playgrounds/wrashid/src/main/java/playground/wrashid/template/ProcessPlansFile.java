package playground.wrashid.template;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
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

		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inputPlansFile);

		ProcessPlansFile dp = new ProcessPlansFile(inPop, outputPlansFile);
		dp.run(inPop);
		dp.writeEndPlans();
	}

	public ProcessPlansFile(PopulationImpl plans, String filename) {
		super(plans, filename);
	}

	@Override
	public void run(Person person) {
		this.popWriter.writePerson(person);

	}
}
