package playground.meisterk.org.matsim.run.westumfahrung;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonIdRecorder;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.population.filters.ActLinkFilter;
import org.matsim.population.filters.PersonFilter;
import org.matsim.population.filters.PersonIdFilter;
import org.matsim.population.filters.RouteLinkFilter;
import org.matsim.population.filters.SelectedPlanFilter;
import org.matsim.world.World;

import playground.meisterk.org.matsim.run.facilities.ShopsOf2005ToFacilities;


/**
 * Compare two scenarios (network, plans, events) with each other.
 * Contains several analyses that were performed for the Westumfahrung Zurich study.
 *
 * @author meisterk
 *
 */
public class CompareScenarios {

	static class CaseStudyResult {

		private final String name;
		private final PopulationImpl plans;
		private final CalcLegTimes calcLegTimes;
		private final PlanAverageScore planAverageScore;
		private final CalcAverageTripLength calcAverageTripLength;

		public CaseStudyResult(final String name, final PopulationImpl plans,
				final CalcLegTimes calcLegTimes, final PlanAverageScore planAverageScore,
				final CalcAverageTripLength calcAverageTripLength) {
			super();
			this.name = name;
			this.plans = plans;
			this.calcLegTimes = calcLegTimes;
			this.planAverageScore = planAverageScore;
			this.calcAverageTripLength = calcAverageTripLength;
		}

		public String getName() {
			return this.name;
		}

		public PopulationImpl getRouteSwitchers() {
			return this.plans;
		}

		public CalcLegTimes getRouteSwitchersLegTimes() {
			return this.calcLegTimes;
		}

		public PlanAverageScore getRouteSwitchersAverageScore() {
			return this.planAverageScore;
		}

		public CalcAverageTripLength getCalcAverageTripLength() {
			return this.calcAverageTripLength;
		}


	}

	private static final Logger log = Logger.getLogger(ShopsOf2005ToFacilities.class);

	// transit agents have ids > 1'000'000'000
	private static final String TRANSIT_PERSON_ID_PATTERN = "[0-9]{10}";
	private static final String NON_TRANSIT_PERSON_ID_PATTERN = "[0-9]{1,9}";

	// compare 2 scenarios
	private String scenarioNameBefore = "before";
	private String scenarioNameAfter = "after";
	private final String[] scenarioNames = new String[]{this.scenarioNameBefore, this.scenarioNameAfter};

	// analyses
	private static final int TRANSIT_AGENTS_ANALYSIS_NAME = 0;
	private static final int NON_TRANSIT_AGENTS_ANALYSIS_NAME = 1;
	private static final int ROUTE_SWITCHERS_ANALYSIS_NAME = 2;
	private static final int WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME = 3;
	private final TreeMap<Integer, String> analysisNames = new TreeMap<Integer, String>();

	// analysisRegions
	private final HashSet<Id> weststrasseLinkIds = new HashSet<Id>();

	private final TreeMap<String, String> plansInputFilenames = new TreeMap<String, String>();
	private final TreeMap<String, String> eventsInputFilenames = new TreeMap<String, String>();
	private final TreeMap<String, String> networkInputFilenames = new TreeMap<String, String>();
	private final TreeMap<String, String> linkSetFilenames = new TreeMap<String, String>();
	private final TreeMap<String, HashSet<Id>> linkSets = new TreeMap<String, HashSet<Id>>();
	private final TreeMap<String, String> linkSetNames = new TreeMap<String, String>();

	private String scenarioComparisonFilename = null;
	private final ArrayList<String> scenarioComparisonLines = new ArrayList<String>();

	private String routeSwitchersListFilename = null;
	private final ArrayList<String> routeSwitchersLines = new ArrayList<String>();


	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		CompareScenarios compareScenarios = new CompareScenarios();
		compareScenarios.run(args);

	}

	private void run(final String[] args) {

		ScenarioImpl scenario = new ScenarioImpl();
		Config config = scenario.getConfig();
		config.global().setLocalDtdBase("dtd/");
		System.out.println(config.global().getLocalDtdBase());

		log.info("Processing command line parameters...");
		this.processArgs(args);
		log.info("Processing command line parameters...done.");
		System.out.flush();
		log.info("Init...");
		this.init();
		log.info("Init...done.");
		System.out.flush();
		log.info("Performing analyses...");
		this.doAnalyses(scenario.getWorld());
		log.info("Performing analyses...done.");
		System.out.flush();
		log.info("Writing out results...");
		this.writeResults();
		log.info("Writing out results...done.");
		System.out.flush();

	}

	private void writeResults() {

		File scenarioComparisonFile = new File(this.scenarioComparisonFilename);
		File routeSwitchersFile = new File(this.routeSwitchersListFilename);
		try {
			FileUtils.writeLines(scenarioComparisonFile, "UTF-8", this.scenarioComparisonLines);
			FileUtils.writeLines(routeSwitchersFile, "UTF-8", this.routeSwitchersLines);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void processArgs(final String[] args) {

		List<String> lines = new ArrayList<String>();
		File scenarioNameInputFile = null;

		final int NUMBER_OF_SCENARIOS = 2;

		if (args.length != 10) {
			System.out.println("Usage:");
			System.out.println("java CompareScenarios args");
			System.out.println("");
			System.out.println("args[0]: name_before.txt");
			System.out.println("args[1]: network_before.xml");
			System.out.println("args[2]: plans_before.xml.gz");
			System.out.println("args[3]: events_before.dat");
			System.out.println("args[4]: linkset_before.txt");
			System.out.println("args[5]: name_after.txt");
			System.out.println("args[6]: network_after.xml");
			System.out.println("args[7]: plans_after.xml.gz");
			System.out.println("args[8]: events_after.dat");
			System.out.println("args[9]: linkset_after.txt");
			System.out.println("");
			throw new RuntimeException();
		} else {
			int argsIndex = 0;
			for (int scenarioCounter = 0; scenarioCounter < NUMBER_OF_SCENARIOS; scenarioCounter++) {
				scenarioNameInputFile = new File(args[argsIndex]);
				try {
					lines = FileUtils.readLines(scenarioNameInputFile, "UTF-8");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				switch(scenarioCounter) {
				case 0:
					this.scenarioNameBefore = lines.get(0);
					this.scenarioNames[scenarioCounter] = lines.get(0);
					break;
				case 1:
					this.scenarioNameAfter = lines.get(0);
					this.scenarioNames[scenarioCounter] = lines.get(0);
					break;
				}
				argsIndex++;
				this.networkInputFilenames.put(this.scenarioNames[scenarioCounter], args[argsIndex]);
				argsIndex++;
				this.plansInputFilenames.put(this.scenarioNames[scenarioCounter], args[argsIndex]);
				argsIndex++;
				this.eventsInputFilenames.put(this.scenarioNames[scenarioCounter], args[argsIndex]);
				argsIndex++;
				this.linkSetFilenames.put(this.scenarioNames[scenarioCounter], args[argsIndex]);
				argsIndex++;
			}
		}

	}

	private void init() {

		this.analysisNames.put(Integer.valueOf(TRANSIT_AGENTS_ANALYSIS_NAME), "transit");
		this.analysisNames.put(Integer.valueOf(NON_TRANSIT_AGENTS_ANALYSIS_NAME), "non transit");
		this.analysisNames.put(Integer.valueOf(ROUTE_SWITCHERS_ANALYSIS_NAME), "route switchers");
		this.analysisNames.put(Integer.valueOf(WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME), "weststrasse neighbors");

		List<String> lines = new ArrayList<String>();

		File linkSetFile = null;
		HashSet<Id> linkSet = null;

		for (String scenarioName : this.scenarioNames) {

			linkSetFile = new File(this.linkSetFilenames.get(scenarioName));
			try {
				lines = FileUtils.readLines(linkSetFile, "UTF-8");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			linkSet = new HashSet<Id>();
			for (String line : lines) {
				try {
					linkSet.add(new IdImpl(Integer.parseInt(line, 10)));
				} catch (NumberFormatException e) {
					log.info("Reading in " + line + " link set...");
					this.linkSetNames.put(scenarioName, line);
				}
			}
			this.linkSets.put(scenarioName, linkSet);
		}

		// build up weststrasse
		linkSetFile  = new File("input/linksets/weststrasse.txt");
		try {
			lines = FileUtils.readLines(linkSetFile, "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
//		linkSet = new HashSet<Id>();
		for (String line : lines) {
			try {
				this.weststrasseLinkIds.add(new IdImpl(Integer.parseInt(line)));
			} catch (NumberFormatException e) {
				log.info("Reading in " + line + " link set...");
			}
		}

		// output file names
		this.scenarioComparisonFilename =
			"output/" +
			this.scenarioNameBefore +
			"_vs_" +
			this.scenarioNameAfter +
			"__" +
			this.linkSetNames.get(this.scenarioNameBefore) +
			"_to_" +
			this.linkSetNames.get(this.scenarioNameAfter) +
			"__summary" +
			".txt";
		this.routeSwitchersListFilename =
			"output/" +
			this.scenarioNameBefore +
			"_vs_" +
			this.scenarioNameAfter +
			"__" +
			this.linkSetNames.get(this.scenarioNameBefore) +
			"_to_" +
			this.linkSetNames.get(this.scenarioNameAfter) +
			"__routeswitchers" +
			".txt";

	}

	/**
	 * Gets all agents that use a set of links in one plans file and use another set in another plans file.
	 * For example: Find all agents that use the Westtangente in a scenario without the Westumfahrung, that
	 * switch to the Westumfahrung in a case study where the Westumfahrung was included in the scenario.
	 *
	 * Summarize their average trip travel times, the scores of their selected plans, and their home locations.
	 */
	private void doAnalyses(World world) {

		TreeMap<Integer, TreeMap<String, PersonIdRecorder>> personIdRecorders = new TreeMap<Integer, TreeMap<String, PersonIdRecorder>>();

		for (Integer analysis : this.analysisNames.keySet()) {
			personIdRecorders.put(analysis, new TreeMap<String, PersonIdRecorder>());
		}
		TreeMap<String, PopulationImpl> scenarioPlans = new TreeMap<String, PopulationImpl>();
		TreeMap<String, NetworkLayer> scenarioNetworks = new TreeMap<String, NetworkLayer>();

		PersonIdRecorder personIdRecorder = null;
		PersonFilter filterAlgorithm = null;

		for (String scenarioName : this.scenarioNames) {

			ScenarioImpl scenario = new ScenarioImpl();
			NetworkLayer network = scenario.getNetwork();
			new MatsimNetworkReader(scenario).readFile(this.networkInputFilenames.get(scenarioName));
			scenarioNetworks.put(scenarioName, network);
			world.setNetworkLayer(network);
			world.complete();

			//Plans plans = playground.meisterk.MyRuns.initMatsimAgentPopulation(plansInputFilenames.get(scenarioName), false, null, network);
			PopulationImpl plans = scenario.getPopulation();
			PopulationReader plansReader = new MatsimPopulationReader(scenario);
			plansReader.readFile(this.plansInputFilenames.get(scenarioName));
			plans.printPlansCount();

			scenarioPlans.put(scenarioName, plans);

			for (Integer analysis : this.analysisNames.keySet()) {

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

					for (Id linkId : this.linkSets.get(scenarioName)) {
						routeLinkFilter.addLink(linkId);
					}
					break;
				case WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME:
					ActLinkFilter homeAtTheWeststrasseFilter = new ActLinkFilter(".*h.*", personIdRecorder);
					filterAlgorithm = new SelectedPlanFilter(homeAtTheWeststrasseFilter);

					for (Id linkId : this.weststrasseLinkIds) {
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
		HashSet<Id> routeSwitchersPersonIds = (HashSet<Id>) personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(this.scenarioNameAfter).getIds().clone();
		routeSwitchersPersonIds.retainAll(personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(this.scenarioNameBefore).getIds());

		HashSet<Id> neighborsPersonIds = personIdRecorders.get(WESTSTRASSE_NEIGHBORS_ANALYSIS_NAME).get(this.scenarioNameBefore).getIds();
		HashSet<Id> transitAgentsIds = personIdRecorders.get(TRANSIT_AGENTS_ANALYSIS_NAME).get(this.scenarioNameBefore).getIds();
		HashSet<Id> nonTransitAgentsIds = personIdRecorders.get(NON_TRANSIT_AGENTS_ANALYSIS_NAME).get(this.scenarioNameBefore).getIds();

		log.info("Agents before: " + personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(this.scenarioNameBefore).getIds().size());
		log.info("Agents after: " + personIdRecorders.get(ROUTE_SWITCHERS_ANALYSIS_NAME).get(this.scenarioNameAfter).getIds().size());
		log.info("Route switchers: " + routeSwitchersPersonIds.size());
		log.info("number of neighbors: " + neighborsPersonIds.size());
		log.info("number of transit agents: " + transitAgentsIds.size());
		log.info("number of non transit agents: " + nonTransitAgentsIds.size());

		Iterator<Id> personIterator = null;
//		HashSet<Id> subPop = new HashSet<Id>();
		for (Integer analysis : this.analysisNames.keySet()) {

			ArrayList<CaseStudyResult> results = new ArrayList<CaseStudyResult>();
			for (String scenarioName : this.scenarioNames) {

				// choose right network
				world.setNetworkLayer(scenarioNetworks.get(scenarioName));

				ScenarioImpl subScenario = new ScenarioImpl();
				subScenario.setNetwork(scenarioNetworks.get(scenarioName));
				PopulationImpl plansSubPop = subScenario.getPopulation();
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
						plansSubPop.addPerson(scenarioPlans.get(scenarioName).getPersons().get(personIterator.next()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				ActivityImpl homeActivity = null;
				if (analysis.intValue() == ROUTE_SWITCHERS_ANALYSIS_NAME) {
					if (scenarioName.equals(this.scenarioNames[0])) {
						this.routeSwitchersLines.add("person\thome_link\thome_x\thome_y");
						for (Person person : plansSubPop.getPersons().values()) {
							for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
								if (pe instanceof ActivityImpl) {
									homeActivity = (ActivityImpl) pe;
									if (Pattern.matches(".*h.*", homeActivity.getType())) {
										continue;
									}
								}
							}
							this.routeSwitchersLines.add(
									person.getId().toString() + "\t" +
									homeActivity.getLinkId().toString() + "\t" +
									Double.toString(homeActivity.getCoord().getX()) + "\t" +
									Double.toString(homeActivity.getCoord().getY()));

//							routeSwitchersLines.add(new String(
//									person.getId() + "\t" +
//									homeActivity.getLinkId().toString() + "\t" +
//									homeActivity.getCoord().getX() + "\t" +
//									homeActivity.getCoord().getY()
//							));
						}
					}
				}

				PlanAverageScore planAverageScore = new PlanAverageScore();
				planAverageScore.run(plansSubPop);
				CalcAverageTripLength calcAverageTripLength = new CalcAverageTripLength(scenarioNetworks.get(scenarioName));
				calcAverageTripLength.run(plansSubPop);

				EventsManagerImpl events = new EventsManagerImpl();

				CalcLegTimes calcLegTimes = new CalcLegTimes(plansSubPop);
				events.addHandler(calcLegTimes);

				results.add(new CaseStudyResult(scenarioName, plansSubPop, calcLegTimes, planAverageScore, calcAverageTripLength));

//				EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
//				log.info("events filename: " + this.eventsInputFilenames.get(scenarioName));
//				eventsReader.readFile(this.eventsInputFilenames.get(scenarioName));
				throw new RuntimeException("reading binary events is no longer supported.");

			}
			this.scenarioComparisonLines.add("Analysis: " + this.analysisNames.get(analysis));
			this.writeComparison(results);
			this.scenarioComparisonLines.add("");

		}

	}

	private void writeComparison(final List<CaseStudyResult> results) {

		this.scenarioComparisonLines.add("casestudy\tn_{agents}\tscore_{avg}\tt_{trip, avg}\td_{trip, avg}[m]");

		for (CaseStudyResult result : results) {

			this.scenarioComparisonLines.add(
					result.getName() + "\t" +
					result.getRouteSwitchers().getPersons().size() + "\t" +
					result.getRouteSwitchersAverageScore().getAverage() + "\t" +
					Time.writeTime(result.calcLegTimes.getAverageTripDuration()) + "\t" +
					result.getCalcAverageTripLength().getAverageTripLength()
			);

		}

	}
}
