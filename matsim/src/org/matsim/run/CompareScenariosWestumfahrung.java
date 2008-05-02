package org.matsim.run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.basic.v01.Id;
import org.matsim.deqsim.EventsReaderDEQv1;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.algorithms.PersonIdRecorder;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.filters.ActLinkFilter;
import org.matsim.plans.filters.PersonIdFilter;
import org.matsim.plans.filters.RouteLinkFilter;
import org.matsim.plans.filters.SelectedPlanFilter;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.utils.misc.Time;

/**
 * Compare two scenarios (network, plans, events) with each other.
 * Contains several analyses that were performed for the Westumfahrung Zurich study.
 * 
 * @author meisterk
 *
 */
public class CompareScenariosWestumfahrung {

	public class CaseStudyResult {

		private String name;
		private Plans plans;
		private CalcLegTimes calcLegTimes;
		private PlanAverageScore planAverageScore;
		private CalcAverageTripLength calcAverageTripLength;


//		public CaseStudyResult(String name, Plans plans,
//		CalcLegTimes calcLegTimes, PlanAverageScore planAverageScore) {
//		super();
//		this.name = name;
//		this.plans = plans;
//		this.calcLegTimes = calcLegTimes;
//		this.planAverageScore = planAverageScore;
//		}

		public CaseStudyResult(String name, Plans plans,
				CalcLegTimes calcLegTimes, PlanAverageScore planAverageScore,
				CalcAverageTripLength calcAverageTripLength) {
			super();
			this.name = name;
			this.plans = plans;
			this.calcLegTimes = calcLegTimes;
			this.planAverageScore = planAverageScore;
			this.calcAverageTripLength = calcAverageTripLength;
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

		public CalcAverageTripLength getCalcAverageTripLength() {
			return calcAverageTripLength;
		}


	}

	private Plans inputPlans = null;

	// transit agents have ids > 1'000'000'000
	private final String TRANSIT_PERSON_ID_PATTERN = "[0-9]{10}";
	private final String NON_TRANSIT_PERSON_ID_PATTERN = "[0-9]{1,9}";

	// compare 2 scenarios
	private String scenarioNameBefore = "before";
	private String scenarioNameAfter = "after";
	private String[] scenarioNames = new String[]{scenarioNameBefore, scenarioNameAfter};

	// analyses
	private final int TRANSIT_AGENTS_ANALYSIS_NAME = 0;
	private final int NON_TRANSIT_AGENTS_ANALYSIS_NAME = 1;
	private final int ROUTE_SWITCHERS_ANALYSIS_NAME = 2;
	private final int WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME = 3;
	private TreeMap<Integer, String> analysisNames = new TreeMap<Integer, String>();

	// analysisRegions
	private HashSet<Id> weststrasseLinkIds = new HashSet<Id>();
	private HashSet<Id> seebahnstrasseLinkIds = new HashSet<Id>();
	private HashSet<Id> rosengartenstrasseLinkIds = new HashSet<Id>();

	private HashSet<Id> westumfahrungLinkIds = new HashSet<Id>();

	private TreeMap<String, String> plansInputFilenames = new TreeMap<String, String>();
	private TreeMap<String, String> eventsInputFilenames = new TreeMap<String, String>();
	private TreeMap<String, String> networkInputFilenames = new TreeMap<String, String>();

	private String scenarioComparisonFilename = null;
	private ArrayList<String> scenarioComparisonLines = new ArrayList<String>();

	private String routeSwitchersListFilename = null;
	private ArrayList<String> routeSwitchersLines = new ArrayList<String>();


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		CompareScenariosWestumfahrung compareScenarios = new CompareScenariosWestumfahrung();
		compareScenarios.run(args);

	}

	private void run(String[] args) {

		System.out.println("Processing command line parameters...");
		this.processArgs(args);
		System.out.println("Processing command line parameters...done.");
		System.out.flush();
		System.out.println("Init...");
		this.init();
		System.out.println("Init...done.");
		System.out.flush();
		System.out.println("Performing analyses...");
		this.doAnalyses();
		System.out.println("Performing analyses...done.");
		System.out.flush();
		System.out.println("Writing out results...");
		this.writeResults();
		System.out.println("Writing out results...done.");
		System.out.flush();

	}

	private void writeResults() {

		File scenarioComparisonFile = new File(scenarioComparisonFilename);
		File routeSwitchersFile = new File(routeSwitchersListFilename);
		try {
			FileUtils.writeLines(scenarioComparisonFile, "UTF-8", scenarioComparisonLines);
			FileUtils.writeLines(routeSwitchersFile, "UTF-8", routeSwitchersLines);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void processArgs(String[] args) {


		if (args.length != 8) {
			System.out.println("Usage:");
			System.out.println("java CompareScenarios args");
			System.out.println("");
			System.out.println("args[0]: scenarioNameBefore");
			System.out.println("args[1]: network_before.xml");
			System.out.println("args[2]: plans_before.xml.gz");
			System.out.println("args[3]: events_before.dat");
			System.out.println("args[4]: scenarioNameAfter");
			System.out.println("args[5]: network_after.xml");
			System.out.println("args[6]: plans_after.xml.gz");
			System.out.println("args[7]: events_after.dat");
			System.out.println("");
			System.exit(-1);
		} else {
			int argsIndex = 0;
			for (int ii=0; ii<=1; ii++) {
				switch(ii) {
				case 0:
					scenarioNameBefore = args[argsIndex];
					break;
				case 1:
					scenarioNameAfter = args[argsIndex];
					break;
				}
				scenarioNames[ii] = args[argsIndex];
				argsIndex++;
				networkInputFilenames.put(scenarioNames[ii], args[argsIndex]);
				argsIndex++;
				plansInputFilenames.put(scenarioNames[ii], args[argsIndex]);
				argsIndex++;
				eventsInputFilenames.put(scenarioNames[ii], args[argsIndex]);
				argsIndex++;
			}
			scenarioComparisonFilename = "output/" + args[0] + "_vs_" + args[4] + ".txt";
			routeSwitchersListFilename = "output/" + args[0] + "_vs_" + args[4] + "_routeSwitchers.txt";
		}

	}

	private void init() {

		analysisNames.put(new Integer(TRANSIT_AGENTS_ANALYSIS_NAME), "transit");
		analysisNames.put(new Integer(NON_TRANSIT_AGENTS_ANALYSIS_NAME), "non transit");
		analysisNames.put(new Integer(ROUTE_SWITCHERS_ANALYSIS_NAME), "route switchers");
		analysisNames.put(new Integer(WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME), "weststrasse neighbors");

		List<String> lines = new ArrayList<String>();

		File streetLinksFile = null;

		// build up westtangente streets
		for (String street : new String[]{"seebahnstrasse", "weststrasse", "rosengartenstrasse"}) {
			streetLinksFile = new File("input/" + street + ".txt");
			try {
				lines = FileUtils.readLines(streetLinksFile, "UTF-8");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (street.equals("seebahnstrasse")) {
				for (String line : lines) {
					seebahnstrasseLinkIds.add(new IdImpl(Integer.parseInt(line)));
				}
			} else if (street.equals("weststrasse")) {
				for (String line : lines) {
					weststrasseLinkIds.add(new IdImpl(Integer.parseInt(line)));
				}
			} else if (street.equals("rosengartenstrasse")) {
				for (String line : lines) {
					rosengartenstrasseLinkIds.add(new IdImpl(Integer.parseInt(line)));
				}
			}
		}

		// build up westumfahrung
		for (int linkNr = 3000000; linkNr <= 3000005; linkNr++) {
			westumfahrungLinkIds.add(new IdImpl(linkNr));
		}

	}

	/**
	 * Gets all agents that use a set of links in one plans file and use another set in another plans file.
	 * For example. Find all agents that use the Westtangente in a scenario with out the Westumfahrung, that
	 * switch to the Westumfahrung in a case study where the Westumfahrung was included in the scenario.
	 * 
	 * Summarize their average trip travel times, the scores of their selected plans, and their home locations.
	 */
	private void doAnalyses() {

		TreeMap<Integer, TreeMap<String, PersonIdRecorder>> personIdRecorders = new TreeMap<Integer, TreeMap<String, PersonIdRecorder>>();

		for (Integer analysis : analysisNames.keySet()) {
			personIdRecorders.put(analysis, new TreeMap<String, PersonIdRecorder>());
		}
		TreeMap<String, Plans> scenarioPlans = new TreeMap<String, Plans>();
		TreeMap<String, NetworkLayer> scenarioNetworks = new TreeMap<String, NetworkLayer>();

		PersonIdRecorder personIdRecorder = null;
		PlansAlgorithm filterAlgorithm = null;
		for (String scenarioName : scenarioNames) {

			NetworkLayer network = new NetworkLayer();
			new MatsimNetworkReader(network).readFile(networkInputFilenames.get(scenarioName));
			scenarioNetworks.put(scenarioName, network);
			Gbl.getWorld().setNetworkLayer(network);

			//Plans plans = playground.meisterk.MyRuns.initMatsimAgentPopulation(plansInputFilenames.get(scenarioName), false, null);
			Plans plans = new Plans(false);
			PlansReaderI plansReader = new MatsimPlansReader(plans);
			plansReader.readFile(plansInputFilenames.get(scenarioName));
			plans.printPlansCount();

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

					if (scenarioName.equals(scenarioNameBefore)) {
						for (Id linkId : weststrasseLinkIds) {
							routeLinkFilter.addLink(linkId);
						}
						for (Id linkId : seebahnstrasseLinkIds) {
							routeLinkFilter.addLink(linkId);
						}
						for (Id linkId : rosengartenstrasseLinkIds) {
							routeLinkFilter.addLink(linkId);
						}
					} else if (scenarioName.equals(scenarioNameAfter)) {
						for (Id linkId : westumfahrungLinkIds) {
							routeLinkFilter.addLink(linkId);
						}
					}
					break;
				case WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME:
					ActLinkFilter homeAtTheWeststrasseFilter = new ActLinkFilter(".*h.*", personIdRecorder);
					filterAlgorithm = new SelectedPlanFilter(homeAtTheWeststrasseFilter);

					for (Id linkId : weststrasseLinkIds) {
						homeAtTheWeststrasseFilter.addLink(linkId);
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
		HashSet<Id> routeSwitchersPersonIds = (HashSet<Id>) personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(scenarioNameAfter).getIds().clone();
		routeSwitchersPersonIds.retainAll(personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(scenarioNameBefore).getIds());

		HashSet<Id> neighborsPersonIds = personIdRecorders.get(WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME).get(scenarioNameBefore).getIds();
		HashSet<Id> transitAgentsIds = personIdRecorders.get(TRANSIT_AGENTS_ANALYSIS_NAME).get(scenarioNameBefore).getIds();
		HashSet<Id> nonTransitAgentsIds = personIdRecorders.get(NON_TRANSIT_AGENTS_ANALYSIS_NAME).get(scenarioNameBefore).getIds();

		System.out.println("Agents before: " + personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(scenarioNameBefore).getIds().size());
		System.out.println("Agents after: " + personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(scenarioNameAfter).getIds().size());
		System.out.println("Route switchers: " + routeSwitchersPersonIds.size());
		System.out.println("number of neighbors: " + neighborsPersonIds.size());
		System.out.println("number of transit agents: " + transitAgentsIds.size());
		System.out.println("number of non transit agents: " + nonTransitAgentsIds.size());

		Iterator<Id> personIterator = null;
		HashSet<Id> subPop = new HashSet<Id>();
		for (Integer analysis : analysisNames.keySet()) {

			ArrayList<CaseStudyResult> results = new ArrayList<CaseStudyResult>();
			for (String scenarioName : scenarioNames) {

				// choose right network
				Gbl.getWorld().setNetworkLayer(scenarioNetworks.get(scenarioName));

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
				case WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME:
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

				Act homeActivity = null;
				if (analysis.intValue() == ROUTE_SWITCHERS_ANALYSIS_NAME) {
					if (scenarioName.equals(scenarioNames[0])) {
						routeSwitchersLines.add("person\thome_link\thome_x\thome_y");
						for (Person person : plansSubPop.getPersons().values()) {
							ActIterator actIterator = person.getSelectedPlan().getIteratorAct();
							while (actIterator.hasNext()) {
								homeActivity = (Act) actIterator.next();
								if (Pattern.matches(".*h.*", homeActivity.getType())) {
									continue;
								}
							}
							routeSwitchersLines.add(new String(
									person.getId() + "\t" +
									homeActivity.getLinkId().toString() + "\t" +
									homeActivity.getCoord().getX() + "\t" +
									homeActivity.getCoord().getY()
							));
						}
					}
				}

				PlanAverageScore planAverageScore = new PlanAverageScore();
				plansSubPop.addAlgorithm(planAverageScore);
				CalcAverageTripLength calcAverageTripLength = new CalcAverageTripLength();
				plansSubPop.addAlgorithm(calcAverageTripLength);
				plansSubPop.runAlgorithms();

				Events events = new Events();

				CalcLegTimes calcLegTimes = new CalcLegTimes(plansSubPop);
				events.addHandler(calcLegTimes);

				results.add(new CaseStudyResult(scenarioName, plansSubPop, calcLegTimes, planAverageScore, calcAverageTripLength));

				EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
				System.out.println("events filename: " + eventsInputFilenames.get(scenarioName));
				eventsReader.readFile(eventsInputFilenames.get(scenarioName));

			}
			scenarioComparisonLines.add("Analysis: " + analysisNames.get(analysis));
			this.writeComparison(results);
			scenarioComparisonLines.add("");

		}

	}

	private void writeComparison(List<CaseStudyResult> results) {

		scenarioComparisonLines.add("casestudy\tn_{agents}\tscore_{avg}\tt_{trip, avg}\td_{trip, avg}[m]");

		for (CaseStudyResult result : results) {

			scenarioComparisonLines.add( 
					result.getName() + "\t" + 
					result.getRouteSwitchers().getPersons().size() + "\t" + 
					result.getRouteSwitchersAverageScore().getAverage() + "\t" + 
					Time.writeTime(result.calcLegTimes.getAverageTripDuration()) + "\t" +
					result.getCalcAverageTripLength().getAverageTripLength()
			);

		}

	}
}
