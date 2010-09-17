package playground.andreas.utils.pop;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

/**
 * Filter persons, depending on ID
 *
 * @author aneumann
 *
 */
public class FilterPerson extends NewPopulation {
	private int planswritten = 0;
	private int personshandled = 0;

	public FilterPerson(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {

		this.personshandled++;
		
		boolean keepPerson = true;

		if(person.getId().toString().contains("X5") || 
				person.getId().toString().contains("X6") || 
				person.getId().toString().contains("X7") || 
				person.getId().toString().contains("X8") || 
				person.getId().toString().contains("X9")){
			keepPerson = false;
		}

		if(keepPerson){
			this.popWriter.writePerson(person);
			this.planswritten++;
		}

	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "F:/network.xml.gz";
		String inPlansFile = "F:/baseplan_10x.xml.gz";
		String outPlansFile = "F:/baseplan_5x.xml.gz";

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		FilterPerson dp = new FilterPerson(net, inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
