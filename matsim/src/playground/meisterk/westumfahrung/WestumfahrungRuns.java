package playground.meisterk.westumfahrung;

import java.util.ArrayList;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.filters.AbstractPersonFilter;
import org.matsim.plans.filters.PersonIdFilter;
import org.matsim.utils.identifiers.IdI;

public class WestumfahrungRuns {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Gbl.createConfig(args);

		WestumfahrungRuns.transitNonTransitAverageTripDurAnalysis();

	}

	private static void transitNonTransitAverageTripDurAnalysis() {

		// transit agents have ids > 1'000'000'000
		String TRANSIT_PERSON_ID_PATTERN = "[0-9]{10}";

		PersonIdFilter transitAgentsFilter = new PersonIdFilter(TRANSIT_PERSON_ID_PATTERN);

		NetworkLayer network = playground.meisterk.MyRuns.initWorldNetwork();
		Plans plans = playground.meisterk.MyRuns.initMatsimAgentPopulation(false, null);

		Plans plansTransitAgents = new Plans();
		Plans plansNonTransitAgents = new Plans();

		try {
			for (Person person : plans.getPersons().values()) {
				if (transitAgentsFilter.judge(person)) {
					plansTransitAgents.addPerson(person);
				} else {
					plansNonTransitAgents.addPerson(person);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Number of transit agents: " + plansTransitAgents.getPersons().size());
		System.out.println("Number of non transit agents: " + plansNonTransitAgents.getPersons().size());

		System.out.println();

//		Events events = new Events();
//		CalcLegTimes calcLegTimes = new CalcLegTimes(plansTransitAgents);
//		events.addHandler(calcLegTimes);
//		playground.meisterk.MyRuns.readEvents(events, network);
//
//		System.out.println(calcLegTimes.getAverageTripDuration());

	}


}
