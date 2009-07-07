package playground.wrashid.template;

import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;

import playground.andreas.bln.DuplicatePlans;
import playground.andreas.bln.NewPopulation;

/*
 * In KeepOnlyMIVPlans, you find an example where facilities are also involved...
 */

public class ProcessPlansFile extends NewPopulation {
	public static void main(String[] args) {

		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());

		String inputPlansFile = "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String outputPlansFile = "./test.xml.gz";
		String networkFile = "./test/scenarios/berlin/network.xml.gz";

		PopulationImpl inPop = new PopulationImpl();

		NetworkLayer net = new NetworkLayer();
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

	public void run(PersonImpl person) {
		this.popWriter.writePerson(person);

	}
}
