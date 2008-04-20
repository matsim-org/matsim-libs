package playground.meisterk.westumfahrung;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.basic.v01.Id;
import org.matsim.deqsim.EventsReaderDEQv1;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.filters.PersonIdFilter;
import org.matsim.plans.filters.RouteLinkFilter;
import org.matsim.plans.filters.SelectedPlanFilter;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;

public class WestumfahrungRuns {

	public class ScenarioResult {

		private String name;
		private Plans plans;
		private CalcLegTimes calcLegTimes;
		private PlanAverageScore planAverageScore;

		public ScenarioResult(String name, Plans plans,
				CalcLegTimes calcLegTimes, PlanAverageScore planAverageScore) {
			super();
			this.name = name;
			this.plans = plans;
			this.calcLegTimes = calcLegTimes;
			this.planAverageScore = planAverageScore;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Plans getPlans() {
			return plans;
		}
		public void setPlans(Plans plans) {
			this.plans = plans;
		}
		public CalcLegTimes getCalcLegTimes() {
			return calcLegTimes;
		}
		public void setCalcLegTimes(CalcLegTimes calcLegTimes) {
			this.calcLegTimes = calcLegTimes;
		}
		public PlanAverageScore getPlanAverageScore() {
			return planAverageScore;
		}
		public void setPlanAverageScore(PlanAverageScore planAverageScore) {
			this.planAverageScore = planAverageScore;
		}

	}

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

//		this.transitNonTransitAverageTripDurAnalysis();
		this.analyseRouteSwitchers();

	}

	private void transitNonTransitAverageTripDurAnalysis() {

		ArrayList<ScenarioResult> results = new ArrayList<ScenarioResult>();
		Events events = new Events();
		String scenarioName = null;

		// transit agents have ids > 1'000'000'000
		String TRANSIT_PERSON_ID_PATTERN = "[0-9]{10}";
		String NON_TRANSIT_PERSON_ID_PATTERN = "[0-9]{1,9}";

		inputPlans = playground.meisterk.MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), false, null);

		System.out.println("Filtering agents...");
		PersonIdRecorder transitAgentsIdRecorder = new PersonIdRecorder();
		PersonIdFilter transitAgentsFilter = new PersonIdFilter(TRANSIT_PERSON_ID_PATTERN, transitAgentsIdRecorder);
		inputPlans.addAlgorithm(transitAgentsFilter);

		PersonIdRecorder nonTransitAgentsIdRecorder = new PersonIdRecorder();
		PersonIdFilter nonTransitAgentsFilter = new PersonIdFilter(NON_TRANSIT_PERSON_ID_PATTERN, nonTransitAgentsIdRecorder);
		inputPlans.addAlgorithm(nonTransitAgentsFilter);
		inputPlans.runAlgorithms();
		
		System.out.println("Filtering agents...done.");

		Plans plansTransitAgents = null;
		Plans plansNonTransitAgents = null;
		
		try {

			System.out.println("Building transit agents...");
			plansTransitAgents = new Plans();
			for (IdI personId : transitAgentsIdRecorder.getIds()) {
				plansTransitAgents.addPerson(inputPlans.getPerson(personId));
			}
			System.out.println("Building transit agents...done.");

			System.out.println("Building non transit agents...");
			plansNonTransitAgents = new Plans();
			for (IdI personId : nonTransitAgentsIdRecorder.getIds()) {
				plansNonTransitAgents.addPerson(inputPlans.getPerson(personId));
			}
			System.out.println("Building non transit agents...done.");

			for (Plans aPlans : new Plans[]{plansNonTransitAgents, plansTransitAgents}) {

				if (aPlans.equals(plansNonTransitAgents)) {
					scenarioName = "swiss";
				} else if (aPlans.equals(plansTransitAgents)) {
					scenarioName = "transit";
				}

				PlanAverageScore planAverageScore = new PlanAverageScore();
				aPlans.addAlgorithm(planAverageScore);

				aPlans.runAlgorithms();

				CalcLegTimes calcLegTimes = new CalcLegTimes(aPlans);
				results.add(new ScenarioResult(scenarioName, aPlans, calcLegTimes, planAverageScore));
				events.addHandler(calcLegTimes);

			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
		eventsReader.readFile(Gbl.getConfig().events().getInputFile());

		this.compareResults(results);
	}

	/**
	 * Gets all agents that use a set of links in one plans file and use another set in another plans file.
	 * For example. Find all agents that use the Westtangente in a scenario with out the Westumfahrung, that
	 * switch to the Westumfahrung in a case study where the Westumfahrung was included in the scenario.
	 * 
	 * Summarize their average trip travel times, the scores of their selected plans, and their home locations.
	 */
	private void analyseRouteSwitchers() {


		String BEFORE = "before";
		String AFTER = "after";
		String[] scenarios = new String[]{BEFORE, AFTER};
		
		TreeMap<String, String> scenarioPlansFilenames = new TreeMap<String, String>();
		scenarioPlansFilenames.put(BEFORE, Gbl.getConfig().plans().getInputFile());
		scenarioPlansFilenames.put(AFTER, "/home/meisterk/Desktop/westumfahrung_runs/run500/200.plans.xml.gz");

		TreeMap<String, Plans> scenarioPlans = new TreeMap<String, Plans>();
		TreeMap<String, PersonIdRecorder> personIdRecorders = new TreeMap<String, PersonIdRecorder>();
		
		TreeMap<String, String> scenarioEventsFilenames = new TreeMap<String, String>();
		scenarioEventsFilenames.put(BEFORE, Gbl.getConfig().events().getInputFile());
		scenarioEventsFilenames.put(AFTER, "/home/meisterk/Desktop/westumfahrung_runs/run500/200.deq_events.dat");
		
		TreeMap<String, IdI> filterLinks = new TreeMap<String, IdI>();
		
		for (String scenarioName : scenarios) {

			Plans plans = playground.meisterk.MyRuns.initMatsimAgentPopulation(scenarioPlansFilenames.get(scenarioName), false, null);
			scenarioPlans.put(scenarioName, plans);
			
			PersonIdRecorder personIds = new PersonIdRecorder();
			RouteLinkFilter routeLinkFilter = new RouteLinkFilter(personIds);
			SelectedPlanFilter findAgentsOnLinksInSelectedPlan = new SelectedPlanFilter(routeLinkFilter);
			personIdRecorders.put(scenarioName, personIds);
			
			if (scenarioName.equals(BEFORE)) {
				routeLinkFilter.addLink(new Id(106306));
			} else if (scenarioName.equals(AFTER)) {
				routeLinkFilter.addLink(new Id(101204));
			}

			plans.addAlgorithm(findAgentsOnLinksInSelectedPlan);
			plans.runAlgorithms();

		}

		HashSet<IdI> routeSwitchers = personIdRecorders.get(AFTER).getIds();
		routeSwitchers.retainAll(personIdRecorders.get(BEFORE).getIds());

		System.out.println("Agents before: " + personIdRecorders.get(BEFORE).getIds().size());
		System.out.println("Agents after: " + personIdRecorders.get(AFTER).getIds().size());
		System.out.println("Route switchers: " + routeSwitchers.size());

		ArrayList<ScenarioResult> results = new ArrayList<ScenarioResult>();
		for (String scenarioName : scenarios) {
			
			Plans aPlans = new Plans();
			Iterator<IdI> personIterator = routeSwitchers.iterator();
			
			while(personIterator.hasNext()) {
				try {
					aPlans.addPerson(scenarioPlans.get(scenarioName).getPerson(personIterator.next()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			PlanAverageScore planAverageScore = new PlanAverageScore();
			aPlans.addAlgorithm(planAverageScore);
			aPlans.runAlgorithms();
			
			Events events = new Events();
			
			CalcLegTimes calcLegTimes = new CalcLegTimes(aPlans);
			events.addHandler(calcLegTimes);

			results.add(new ScenarioResult(scenarioName, aPlans, calcLegTimes, planAverageScore));

			EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
			eventsReader.readFile(scenarioEventsFilenames.get(scenarioName));
			
		}
	
		this.compareResults(results);

	}

	private void compareResults(List<ScenarioResult> results) {

		System.out.println("subpop\tsize\tscore\ttravel");

		for (ScenarioResult result : results) {

			System.out.print(result.getName());
			System.out.print("\t");

			System.out.print(result.getPlans().getPersons().size());
			System.out.print("\t");

			System.out.print(result.getPlanAverageScore().getAverage());
			System.out.print("\t");

			System.out.print(Time.writeTime(result.calcLegTimes.getAverageTripDuration()));

			System.out.println();

		}

	}


}
