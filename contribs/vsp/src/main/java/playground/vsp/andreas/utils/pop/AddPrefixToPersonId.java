package playground.vsp.andreas.utils.pop;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

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
        PopulationUtils.changePersonId( ((Person) person), Id.create(this.preFix + person.getId().toString(), Person.class) ) ;
        this.popWriter.writePerson(person);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plan_korridor.xml.gz";
		String outPlansFile = "./plan_korridor_50x.xml.gz";

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);

		AddPrefixToPersonId dp = new AddPrefixToPersonId(net, inPop, outPlansFile, "gv_");
		dp.run(inPop);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
