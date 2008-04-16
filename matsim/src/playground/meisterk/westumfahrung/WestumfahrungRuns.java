package playground.meisterk.westumfahrung;

import java.util.ArrayList;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.filters.AbstractPersonFilter;

import playground.meisterk.MyRuns;

public class WestumfahrungRuns {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Gbl.createConfig(args);
		
		WestumfahrungRuns.runAverageTripDurAnalysis();
		
	}

	private static void runAverageTripDurAnalysis() {
		
		String nonTransitPersonIdPattern = "[0-9]{10}";
		ArrayList<PlansAlgorithm> plansAlgos = new ArrayList<PlansAlgorithm>();
		
		PersonIdFilter personIdFilter = new PersonIdFilter(nonTransitPersonIdPattern, true);
		plansAlgos.add((PlansAlgorithm) personIdFilter);

		NetworkLayer network = playground.meisterk.MyRuns.initWorldNetwork();
		playground.meisterk.MyRuns.initMatsimAgentPopulation(true, plansAlgos);
		
		System.out.println(
				"Number of persons with id matching '" + nonTransitPersonIdPattern + "': " + 
				((AbstractPersonFilter) personIdFilter).getCount());
		
		Plans filteredPlans = personIdFilter.getPlans();

		System.out.println(
				"Number of persons with id matching '" + nonTransitPersonIdPattern + "': " + 
				filteredPlans.getPersons().size());

		System.out.println();
		
		Events events = new Events();
		CalcLegTimes calcLegTimes = new CalcLegTimes(filteredPlans);
		events.addHandler(calcLegTimes);
		playground.meisterk.MyRuns.readEvents(events, network);
		
	}
	
	
}
