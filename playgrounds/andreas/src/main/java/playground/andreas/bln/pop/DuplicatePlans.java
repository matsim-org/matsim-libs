package playground.andreas.bln.pop;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

/**
 * Create numberOfcopies additional persons for a given plan.
 *
 * @author aneumann
 *
 */
public class DuplicatePlans extends NewPopulation {

	private final int numberOfCopies;

	public DuplicatePlans(Network network, Population plans, String filename, int numberOfCopies) {
		super(network, plans, filename);
		this.numberOfCopies = numberOfCopies;
	}

	@Override
	public void run(Person person) {
		// Keep old person untouched
		this.popWriter.writePerson(person);
		Id personId = person.getId();

		for (int i = 1; i < this.numberOfCopies + 1; i++) {

			person.setId(new IdImpl(personId.toString() + "X" + i));
			this.popWriter.writePerson(person);

		}

	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plan_korridor.xml.gz";
		String outPlansFile = "./plan_korridor_50x.xml.gz";

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		DuplicatePlans dp = new DuplicatePlans(net, inPop, outPlansFile, 49);
		dp.run(inPop);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
