package playground.meisterk.westumfahrung;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.basic.v01.Id;
import org.matsim.deqsim.EventsReaderDEQv1;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.filters.PersonIdFilter;
import org.matsim.plans.filters.RouteLinkFilter;
import org.matsim.plans.filters.SelectedPlanFilter;
import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.misc.Time;

public class WestumfahrungRuns {

	public class CaseStudyResult {

		private String name;
		private Plans plans;
		private CalcLegTimes calcLegTimes;
		private PlanAverageScore planAverageScore;

		public CaseStudyResult(String name, Plans plans,
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

		public Plans getRouteSwitchers() {
			return plans;
		}

		public CalcLegTimes getRouteSwitchersLegTimes() {
			return calcLegTimes;
		}

		public PlanAverageScore getRouteSwitchersAverageScore() {
			return planAverageScore;
		}

	}

	private Plans inputPlans = null;

	// transit agents have ids > 1'000'000'000
	private final String TRANSIT_PERSON_ID_PATTERN = "[0-9]{10}";
	private final String NON_TRANSIT_PERSON_ID_PATTERN = "[0-9]{1,9}";

	// compare 2 scenarios. one might comapre as many as one wants, just have to expand the list of command line parameters
	private final String BEFORE = "before";
	private final String AFTER = "after";
	private final String[] scenarios = new String[]{BEFORE, AFTER};

	// analyses
	private final int TRANSIT_AGENTS_ANALYSIS_NAME = 0;
	private final int NON_TRANSIT_AGENTS_ANALYSIS_NAME = 1;
	private final int ROUTE_SWITCHERS_ANALYSIS_NAME = 2;
	private final int WESTTANGENTE_NEIGHBORS_ANALYSIS_NAME = 3;
	private TreeMap<Integer, String> analysisNames = new TreeMap<Integer, String>();

	private String networkInputFilename = null;
	private TreeMap<String, String> plansInputFilenames = new TreeMap<String, String>();
	private TreeMap<String, String> eventsInputFilenames = new TreeMap<String, String>();
	private String outFilename = null;
	private ArrayList<String> outLines = new ArrayList<String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		WestumfahrungRuns wuRuns = new WestumfahrungRuns();
		wuRuns.run(args);

	}

	private void run(String[] args) {

		if (args.length != 6) {
			System.out.println("Usage:");
			System.out.println("java WestumfahrungRuns network plans_before events_before plans_after events_after output");
			System.out.println("");
			System.out.println("You might populate your MATSim/input directory like the following:");
			System.out.println("");
			System.out.println(" events_after.dat.0 -> /home/meisterk/Desktop/westumfahrung_runs/run500/200.deq_events.dat.0");
			System.out.println(" events_after.dat.1 -> /home/meisterk/Desktop/westumfahrung_runs/run500/200.deq_events.dat.1");
			System.out.println(" events_before.dat.0 -> /home/meisterk/Desktop/westumfahrung_runs/run243/200.deq_events.dat.0");
			System.out.println(" events_before.dat.1 -> /home/meisterk/Desktop/westumfahrung_runs/run243/200.deq_events.dat.1");
			System.out.println(" events_before.dat.2 -> /home/meisterk/Desktop/westumfahrung_runs/run243/200.deq_events.dat.2");
			System.out.println(" events_before.dat.3 -> /home/meisterk/Desktop/westumfahrung_runs/run243/200.deq_events.dat.3");
			System.out.println(" network.xml -> /home/meisterk/sandbox00/ivt/studies/switzerland/networks/ivtch-changed-wu/network.xml");
			System.out.println(" plans_after.xml.gz -> /home/meisterk/Desktop/westumfahrung_runs/run500/200.plans.xml.gz");
			System.out.println(" plans_before.xml.gz -> /home/meisterk/Desktop/westumfahrung_runs/run243/200.plans.xml.gz");
			System.out.println("");
			System.out.println("and run ");
			System.out.println("");
			System.out.println("java WestumfahrungRuns input/network.xml input/plans_before.xml.gz input/events_before.dat input/plans_after input/events_after.dat output/westumfahrung.txt");
			System.out.println("");
			System.exit(-1);
		} else {
			networkInputFilename = args[0];
			int argsIndex = 1;
			for (String scenarioName : scenarios) {
				plansInputFilenames.put(scenarioName, args[argsIndex]);
				argsIndex++;
				eventsInputFilenames.put(scenarioName, args[argsIndex]);
				argsIndex++;
			}
			outFilename = args[5];
		}

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkInputFilename);
		Gbl.getWorld().setNetworkLayer(network);

		analysisNames.put(new Integer(TRANSIT_AGENTS_ANALYSIS_NAME), "transit");
		analysisNames.put(new Integer(NON_TRANSIT_AGENTS_ANALYSIS_NAME), "non transit");
		analysisNames.put(new Integer(ROUTE_SWITCHERS_ANALYSIS_NAME), "route switchers");
		analysisNames.put(new Integer(WESTTANGENTE_NEIGHBORS_ANALYSIS_NAME), "westtangente neighbors");

		this.buildBeforeAfterAnalyses();

		File outFile = new File(outFilename);
		try {
			FileUtils.writeLines(outFile, "UTF-8", outLines);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Gets all agents that use a set of links in one plans file and use another set in another plans file.
	 * For example. Find all agents that use the Westtangente in a scenario with out the Westumfahrung, that
	 * switch to the Westumfahrung in a case study where the Westumfahrung was included in the scenario.
	 * 
	 * Summarize their average trip travel times, the scores of their selected plans, and their home locations.
	 */
	private void buildBeforeAfterAnalyses() {

		HashSet<Id> westtangenteLinkIds = new HashSet<Id>();
		westtangenteLinkIds.add(new IdImpl(106121));
		westtangenteLinkIds.add(new IdImpl(106122));
		westtangenteLinkIds.add(new IdImpl(106118));
		westtangenteLinkIds.add(new IdImpl(107166));
		westtangenteLinkIds.add(new IdImpl(108039));
		westtangenteLinkIds.add(new IdImpl(108041));
		westtangenteLinkIds.add(new IdImpl(110650));
		westtangenteLinkIds.add(new IdImpl(106304));
		westtangenteLinkIds.add(new IdImpl(106303));
		westtangenteLinkIds.add(new IdImpl(106116));
		westtangenteLinkIds.add(new IdImpl(108049));
		westtangenteLinkIds.add(new IdImpl(106110));
		westtangenteLinkIds.add(new IdImpl(106721));
		westtangenteLinkIds.add(new IdImpl(106722));

		HashSet<Id> westumfahrungLinkIds = new HashSet<Id>();
		for (int linkNr = 3000000; linkNr <= 3000005; linkNr++) {
			westumfahrungLinkIds.add(new IdImpl(linkNr));
		}

		TreeMap<Integer, TreeMap<String, PersonIdRecorder>> personIdRecorders = new TreeMap<Integer, TreeMap<String, PersonIdRecorder>>();

		for (Integer analysis : analysisNames.keySet()) {
			personIdRecorders.put(analysis, new TreeMap<String, PersonIdRecorder>());
		}
		TreeMap<String, Plans> scenarioPlans = new TreeMap<String, Plans>();

		PersonIdRecorder personIdRecorder = null;
		PlansAlgorithm filterAlgorithm = null;
		for (String scenarioName : scenarios) {

			Plans plans = playground.meisterk.MyRuns.initMatsimAgentPopulation(plansInputFilenames.get(scenarioName), false, null);
			scenarioPlans.put(scenarioName, plans);

			for (Integer analysis : analysisNames.keySet()) {

				personIdRecorder = new PersonIdRecorder();

				// distinguish person filtering by analysis type
				switch(analysis.intValue()) {
				case TRANSIT_AGENTS_ANALYSIS_NAME:
					filterAlgorithm = new PersonIdFilter(TRANSIT_PERSON_ID_PATTERN, personIdRecorder);
					break;
				case NON_TRANSIT_AGENTS_ANALYSIS_NAME:
					filterAlgorithm = new PersonIdFilter(NON_TRANSIT_PERSON_ID_PATTERN, personIdRecorder);
					break;
				case ROUTE_SWITCHERS_ANALYSIS_NAME:
					RouteLinkFilter routeLinkFilter = new RouteLinkFilter(personIdRecorder);
					filterAlgorithm = new SelectedPlanFilter(routeLinkFilter);

					if (scenarioName.equals(BEFORE)) {
						for (Id linkId : westtangenteLinkIds) {
							routeLinkFilter.addLink(linkId);
						}
					} else if (scenarioName.equals(AFTER)) {
						for (Id linkId : westumfahrungLinkIds) {
							routeLinkFilter.addLink(linkId);
						}
					}
					break;
				case WESTTANGENTE_NEIGHBORS_ANALYSIS_NAME:
					ActLinkFilter homeAtTheWesttangenteFilter = new ActLinkFilter(".*h.*", personIdRecorder);
					filterAlgorithm = new SelectedPlanFilter(homeAtTheWesttangenteFilter);

					for (Id linkId : westtangenteLinkIds) {
						homeAtTheWesttangenteFilter.addLink(linkId);
					}
					break;
				default:
					break;
				}

				personIdRecorders.get(analysis).put(scenarioName, personIdRecorder);
				plans.addAlgorithm(filterAlgorithm);
			}
			plans.runAlgorithms();

		}

		// make this nicer, because all analyses are of the same kind :-)
		HashSet<Id> routeSwitchersPersonIds = (HashSet<Id>) personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(AFTER).getIds().clone();
		routeSwitchersPersonIds.retainAll(personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(BEFORE).getIds());

		HashSet<Id> neighborsPersonIds = personIdRecorders.get(WESTTANGENTE_NEIGHBORS_ANALYSIS_NAME).get(BEFORE).getIds();
		HashSet<Id> transitAgentsIds = personIdRecorders.get(TRANSIT_AGENTS_ANALYSIS_NAME).get(BEFORE).getIds();
		HashSet<Id> nonTransitAgentsIds = personIdRecorders.get(NON_TRANSIT_AGENTS_ANALYSIS_NAME).get(BEFORE).getIds();

		System.out.println("Agents before: " + personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(BEFORE).getIds().size());
		System.out.println("Agents after: " + personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(AFTER).getIds().size());
		System.out.println("Route switchers: " + routeSwitchersPersonIds.size());
		System.out.println("number of neighbors: " + neighborsPersonIds.size());
		System.out.println("number of transit agents: " + transitAgentsIds.size());
		System.out.println("number of non transit agents: " + nonTransitAgentsIds.size());

		Iterator<Id> personIterator = null;
		HashSet<Id> subPop = new HashSet<Id>();
		for (Integer analysis : analysisNames.keySet()) {

			ArrayList<CaseStudyResult> results = new ArrayList<CaseStudyResult>();
			for (String scenarioName : scenarios) {

				Plans plansSubPop = new Plans(false);
				switch(analysis.intValue()) {
				case TRANSIT_AGENTS_ANALYSIS_NAME:
					personIterator = transitAgentsIds.iterator();
					break;
				case NON_TRANSIT_AGENTS_ANALYSIS_NAME:
					personIterator = nonTransitAgentsIds.iterator();
					break;
				case ROUTE_SWITCHERS_ANALYSIS_NAME:
					personIterator = routeSwitchersPersonIds.iterator();
					break;
				case WESTTANGENTE_NEIGHBORS_ANALYSIS_NAME:
					personIterator = neighborsPersonIds.iterator();
					break;
				default:
					break;
				}

				while(personIterator.hasNext()) {
					try {
						plansSubPop.addPerson(scenarioPlans.get(scenarioName).getPerson(personIterator.next()));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				PlanAverageScore planAverageScore = new PlanAverageScore();
				plansSubPop.addAlgorithm(planAverageScore);
				plansSubPop.runAlgorithms();

				Events events = new Events();

				CalcLegTimes calcLegTimes = new CalcLegTimes(plansSubPop);
				events.addHandler(calcLegTimes);

				results.add(new CaseStudyResult(scenarioName, plansSubPop, calcLegTimes, planAverageScore));

				EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
				System.out.println("events filename: " + eventsInputFilenames.get(scenarioName));
				eventsReader.readFile(eventsInputFilenames.get(scenarioName));

			}
			outLines.add("Analysis: " + analysisNames.get(analysis));
			this.writeComparison(results);
			outLines.add("");

		}

	}

	private void writeComparison(List<CaseStudyResult> results) {

		outLines.add("casestudy\tsize\tscore\ttravel");

		for (CaseStudyResult result : results) {

			outLines.add( 
					result.getName() + "\t" + 
					result.getRouteSwitchers().getPersons().size() + "\t" + 
					result.getRouteSwitchersAverageScore().getAverage() + "\t" + 
					Time.writeTime(result.calcLegTimes.getAverageTripDuration())
			);

		}

	}
}
