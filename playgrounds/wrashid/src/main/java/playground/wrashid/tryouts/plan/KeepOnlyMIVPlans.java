package playground.wrashid.tryouts.plan;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

/*
 * removes non miv plans from input file...
 *  (facilities needed for some plans files...)
 *   => probably can just remove things related to faclities and it should work (or look at earlier revision)
 *    * TODO: refactor class and move class to "lib.tools.plan" (implementation of interface "New Population" not required.
 */

public class KeepOnlyMIVPlans extends NewPopulation {
	public static void main(String[] args) {

		ScenarioImpl sc = new ScenarioImpl();

		String inputPlansFile = "/data/matsim/wrashid/input/plans/teleatlas/census2000v2_dilZh30km_miv_only/plans.xml.gz";
		String outputPlansFile = "/data/matsim/wrashid/input/plans/teleatlas/census2000v2_dilZh30km_miv_only/plans1.xml.gz";
		String networkFile = "/data/matsim/switzerland/ivt/studies/switzerland/networks/teleatlas/network.xml.gz";
		String facilitiesPath = "/data/matsim/switzerland/ivt/studies/switzerland/facilities/facilities.xml.gz";

		//String inputPlansFile = "./test/scenarios/chessboard/plans.xml";
		//String outputPlansFile = "./plans1.xml";
		//String networkFile = "./test/scenarios/chessboard/network.xml";
		//String facilitiesPath = "./test/scenarios/chessboard/facilities.xml";

		new MatsimFacilitiesReader(sc).readFile(facilitiesPath);

		Population inPop = sc.getPopulation();

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inputPlansFile);

		KeepOnlyMIVPlans dp = new KeepOnlyMIVPlans(net, inPop, outputPlansFile);
		dp.run(inPop);
		dp.writeEndPlans();
	}

	public KeepOnlyMIVPlans(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {

		if(person.getPlans().size() != 1){
			System.err.println("Person got more than one plan");
		} else {

			Plan plan = person.getPlans().get(0);
			boolean keepPlan = true;

			// only keep person if every leg is a car leg
			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof Leg){
					if(((Leg)planElement).getMode() != TransportMode.car){
						keepPlan = false;
					}
				}
			}

			if(keepPlan){
				this.popWriter.writePerson(person);
			}

		}

	}
}
