package playground.andreas.bln.pop;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * Reset all "personal" attributes of a person
 * 
 * @author aneumann
 * 
 */
public class StripPersonPlan extends NewPopulation {
	private int planswritten = 0;
	private int personshandled = 0;

	public StripPersonPlan(PopulationImpl plans, String filename) {
		super(plans, filename);
	}

	@Override
	public void run(PersonImpl person) {
		
		this.personshandled++;
		
		person.setAge(Integer.MIN_VALUE);
		person.setCarAvail(null);
		person.setEmployed(null);
		person.setLicence(null);
		person.setSex(null);

		this.popWriter.writePerson(person);
	}
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		
		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plans3.xml.gz";
		String outPlansFile = "./plans3_stripped.xml.gz";

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);

		PopulationImpl inPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inPlansFile);

		StripPersonPlan dp = new StripPersonPlan(inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
