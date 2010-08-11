package playground.andreas.fggeoinfo;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

import playground.andreas.bln.pop.NewPopulation;

/**
 * Filter persons, depending on ID
 *
 * @author aneumann
 *
 */
public class MergePopulations extends NewPopulation {
	private int planswritten = 0;
	private int personsAdded = 0;
	
	private Population additionalPop;
	boolean added = false;

	public MergePopulations(Network network, Population plans, Population additionalPop, String filename) {
		super(network, plans, filename);
		this.additionalPop = additionalPop;
	}

	@Override
	public void run(Person person) {
		
		if(!this.added){
			for (Person personToAdd : this.additionalPop.getPersons().values()) {
				this.popWriter.writePerson(personToAdd);
				this.personsAdded++;
			}
			this.added = true;
		}
		
		this.popWriter.writePerson(person);
		this.planswritten++;

	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();
		ScenarioImpl scA = new ScenarioImpl();
		
		String inputDir = "d:\\Berlin\\FG Geoinformation\\Scenario\\Ausgangsdaten\\20100809_verwendet\\";

		String networkFile = inputDir + "network_modified_20100806_added_BBI_AS_cl.xml.gz";
		String inPlansFile = "d:\\Berlin\\BVG\\berlin-bvg09\\pop\\baseplan_900s.xml.gz";
//		String inPlansFile = inputDir + "baseplan_1x_900s_movedToBBI.xml.gz";
		String additionalPlansFile = inputDir + "pop_1x_generated_TXL_SXF.xml";
		String outPlansFile = inputDir + "pop_1x_merged_TXL_SXF.xml.gz";

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);
		
		Population additionalPop = scA.getPopulation();
		PopulationReader additionalPopReader = new MatsimPopulationReader(scA);
		additionalPopReader.readFile(additionalPlansFile);
		

		MergePopulations dp = new MergePopulations(net, inPop, additionalPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personsAdded + " new persons added; " + dp.planswritten + " old plans written to file - total pop should be " + (dp.personsAdded + dp.planswritten));
			
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
