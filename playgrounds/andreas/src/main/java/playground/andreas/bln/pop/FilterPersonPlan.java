package playground.andreas.bln.pop;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * Filter persons, not using a specific TransportMode.
 * 
 * @author aneumann
 * 
 */
public class FilterPersonPlan extends NewPopulation {
	private int planswritten = 0;
	private int personshandled = 0;

	public FilterPersonPlan(PopulationImpl plans, String filename) {
		super(plans, filename);
	}

	@Override
	public void run(Person person) {
		
		this.personshandled++;
		
		if(person.getPlans().size() != 1){
			System.err.println("Person got more than one plan");
		} else {
			
			Plan plan = person.getPlans().get(0);
			boolean keepPlan = true;
			
			// only keep person if every leg is a car leg
			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof LegImpl){
					if(((LegImpl)planElement).getMode() != TransportMode.car && ((LegImpl)planElement).getMode() != TransportMode.pt){
						keepPlan = false;
					}
				}
			}
			
			if(keepPlan){
				this.popWriter.writePerson(person);
				this.planswritten++;
			}
			
		}

	}
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		
		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./baseplan.xml.gz";
		String outPlansFile = "./baseplan_car_pt_only.xml.gz";

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(net).readFile(networkFile);

		PopulationImpl inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inPlansFile);

		FilterPersonPlan dp = new FilterPersonPlan(inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
