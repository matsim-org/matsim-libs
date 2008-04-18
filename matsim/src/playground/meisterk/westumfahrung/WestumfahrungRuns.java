package playground.meisterk.westumfahrung;

import java.util.HashMap;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.basic.v01.Id;
import org.matsim.deqsim.EventsReaderDEQv1;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.filters.PersonIdFilter;
import org.matsim.plans.filters.RouteLinkFilter;
import org.matsim.plans.filters.SelectedPlanFilter;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;

public class WestumfahrungRuns {

	private Plans inputPlans = null;
	private NetworkLayer network = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		WestumfahrungRuns wuRuns = new WestumfahrungRuns();
		wuRuns.run(args);

	}

	private void run(String[] args) {

		Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		Gbl.getWorld().setNetworkLayer(network);

		inputPlans = playground.meisterk.MyRuns.initMatsimAgentPopulation(false, null);

//		this.transitNonTransitAverageTripDurAnalysis();
		this.analyseRouteSwitchers();

	}

	private void transitNonTransitAverageTripDurAnalysis() {

		// transit agents have ids > 1'000'000'000
		String TRANSIT_PERSON_ID_PATTERN = "[0-9]{10}";

		PersonIdFilter transitAgentsFilter = new PersonIdFilter(TRANSIT_PERSON_ID_PATTERN, null);

		Plans plansTransitAgents = new Plans();
		plansTransitAgents.setName("transit");
		Plans plansNonTransitAgents = new Plans();
		plansNonTransitAgents.setName("swiss");

		try {
			for (Person person : inputPlans.getPersons().values()) {
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

		Events events = new Events();

		HashMap<Plans, CalcLegTimes> calcMe = new HashMap<Plans, CalcLegTimes>();
		calcMe.put(plansTransitAgents, new CalcLegTimes(plansTransitAgents));
		calcMe.put(plansNonTransitAgents, new CalcLegTimes(plansNonTransitAgents));
		for (CalcLegTimes calcLegTimes : calcMe.values()) {
			events.addHandler(calcLegTimes);
		}

		EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
		eventsReader.readFile(Gbl.getConfig().events().getInputFile());

		// output
		double avgScoreSelected = 0.0;

		System.out.println("subpop\tsize\tscore\ttravel");

		for (Plans plans : new Plans[]{plansTransitAgents, plansNonTransitAgents}) {

			System.out.print(plans.getName());
			System.out.print("\t");

			System.out.print(plans.getPersons().size());
			System.out.print("\t");

			avgScoreSelected = 0.0;
			for (Person person : plans.getPersons().values()) {
				avgScoreSelected += person.getSelectedPlan().getScore();
			}
			avgScoreSelected /= plans.getPersons().size();
			System.out.print(Double.toString(avgScoreSelected));
			System.out.print("\t");

			CalcLegTimes calcLegTimes = calcMe.get(plans);
			System.out.print(Time.writeTime(calcLegTimes.getAverageTripDuration()));

			System.out.println();
		}

	}

	/**
	 * Gets all agents that use a set of links in one plans file and use another set in another plans file.
	 * For example. Find all agents that use the Westtangente in a scenario with out the Westumfahrung, that
	 * switch to the Westumfahrung in a case study where the Westumfahrung was included in the scenario.
	 * 
	 * Summarize their average trip travel times, the scores of their selected plans, and their home locations.
	 */
	private void analyseRouteSwitchers() {

		PersonIds personIds = new PersonIds();
		RouteLinkFilter routeLinkFilterBefore = new RouteLinkFilter(personIds);
		SelectedPlanFilter findAgentsOnLinksInSelectedPlan = new SelectedPlanFilter(routeLinkFilterBefore);

		routeLinkFilterBefore.addLink(new Id(106306));
		inputPlans.addAlgorithm(findAgentsOnLinksInSelectedPlan);
		inputPlans.runAlgorithms();

		for (IdI personId : personIds.getIds()) {
			System.out.println(personId.toString());
		}

	}

}
