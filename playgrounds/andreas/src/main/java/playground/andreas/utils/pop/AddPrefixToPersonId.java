package playground.andreas.utils.pop;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Adds a prefix to every person id in the given plans file.
 *
 * @author aneumann
 *
 */
public class AddPrefixToPersonId extends NewPopulation {

	private String preFix;
	
	public AddPrefixToPersonId(Network network, Population plans, String filename, String preFix) {
		super(network, plans, filename);
		this.preFix = preFix;
	}

	@Override
	public void run(Person person) {
		person.setId(new IdImpl(this.preFix + person.getId().toString()));
		this.popWriter.writePerson(person);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plan_korridor.xml.gz";
		String outPlansFile = "./plan_korridor_50x.xml.gz";

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		AddPrefixToPersonId dp = new AddPrefixToPersonId(net, inPop, outPlansFile, "gv_");
		dp.run(inPop);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
