package playground.mzilske.cdr;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;
import d4d.Sighting;

public class CompareMain {

	private static final int TIME_BIN_SIZE = 60*60;
	private static final int MAX_TIME = 24 * TIME_BIN_SIZE - 1;
	private static final int dailyRate = 100;
	private CallProcessTicker ticker;
	private CallProcess callProcess;
	private VolumesAnalyzer volumesAnalyzer1;
	private LinkToZoneResolver linkToZoneResolver;
	private Scenario scenario;
	

	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("input/potsdam/network.xml");
		new MatsimPopulationReader(scenario).readFile("output-homogeneous-37/ITERS/it.0/0.plans.xml.gz");
		EventsManager events = EventsUtils.createEventsManager();
		CompareMain compareMain = new CompareMain(scenario, events);
		new MatsimEventsReader(events).readFile("output-homogeneous-37/ITERS/it.0/0.events.xml.gz");
		compareMain.finish();
	}

	public void finish() {

		ticker.finish();
		
		callProcess.dump();


		List<Sighting> sightings = callProcess.getSightings();

		VolumesAnalyzer volumesAnalyzer2 = runSimulationFromSightings(scenario.getNetwork(), linkToZoneResolver, sightings);
		

		dumpVolumesCompareAllDay(scenario.getNetwork(), volumesAnalyzer1, volumesAnalyzer2);
		dumpVolumesCompareTimebins(scenario.getNetwork(), volumesAnalyzer1, volumesAnalyzer2);
	}

	CompareMain(Scenario scenario, EventsManager events) {
		super();
		this.scenario = scenario;

		Map<Id, Id> initialPersonInZone = new HashMap<Id, Id>();
		

		// final Zones cellularCoverage = SyntheticCellTowerDistribution.naive(scenario.getNetwork());
		
		LinkToZoneResolver trivialLinkToZoneResolver = new LinkToZoneResolver() {

			@Override
			public Id resolveLinkToZone(Id linkId) {
				return linkId;	
			}
			
			public IdImpl chooseLinkInZone(String zoneId) {
				return new IdImpl(zoneId);
			}
			
		};
		
		linkToZoneResolver = trivialLinkToZoneResolver;
		
		// linkToZoneResolver = new CellularCoverageLinkToZoneResolver(cellularCoverage, scenario.getNetwork());
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Id linkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
			System.out.println(linkId);
			initialPersonInZone.put(p.getId(), linkToZoneResolver.resolveLinkToZone(linkId));
		}

		ticker = new CallProcessTicker();
		events.addHandler(ticker);
		
		
		ZoneTracker zoneTracker = new ZoneTracker(events, linkToZoneResolver, initialPersonInZone);
		callProcess = new CallProcess(null, scenario.getPopulation(), zoneTracker, dailyRate);
		ticker.addHandler(zoneTracker);

		ticker.addHandler(callProcess);
		ticker.addSteppable(callProcess);

		volumesAnalyzer1 = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, scenario.getNetwork());
		events.addHandler(volumesAnalyzer1);
	}

	public static void dumpVolumesCompareAllDay(final Network network, VolumesAnalyzer volumesAnalyzer1, VolumesAnalyzer volumesAnalyzer2) {
		int squares = 0;
		for (Link link : network.getLinks().values()) {
			int[] volumesForLink1 = getVolumesForLink(volumesAnalyzer1, link);
			int[] volumesForLink2 = getVolumesForLink(volumesAnalyzer2, link);
			int sum1 = 0;
			int sum2 = 0;
			for (int i = 0; i < volumesForLink1.length; ++i) {
				sum1 += volumesForLink1[i];
				sum2 += volumesForLink2[i];
			}
			int diff = sum2 - sum1;
			squares += diff * diff;
		}
		System.out.println(Math.sqrt(squares));
	}
	
	public static void dumpVolumesCompareTimebins(final Network network, VolumesAnalyzer volumesAnalyzer1, VolumesAnalyzer volumesAnalyzer2) {
		int squares = 0;
		for (Link link : network.getLinks().values()) {
			int[] volumesForLink1 = getVolumesForLink(volumesAnalyzer1, link);
			int[] volumesForLink2 = getVolumesForLink(volumesAnalyzer2, link);
			for (int i = 0; i < volumesForLink1.length; ++i) {
				int diff = volumesForLink1[i] - volumesForLink2[i];
				squares += diff * diff;
				if (diff != 0) {
					System.out.println(Arrays.toString(volumesForLink1));
					System.out.println(Arrays.toString(volumesForLink2));
					System.out.println("=== " + link.getId());
				}
			}
		}
		System.out.println(Math.sqrt(squares));
	}

	public static VolumesAnalyzer runSimulationFromSightings(Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings) {
		Config config = ConfigUtils.createConfig();
		ActivityParams sightingParam = new ActivityParams("sighting");
		// sighting.setOpeningTime(0.0);
		// sighting.setClosingTime(0.0);
		sightingParam.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setPerforming_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(-6);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		config.controler().setLastIteration(0);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);



		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}


		PopulationFromSightings.readSampleWithOneRandomPointForEachSightingInNewCell(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);
		controler.run();
		return controler.getVolumes();
	}

	public static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Link link) {
		int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
		int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(link.getId());
		if(maybeVolumes == null) {
			return new int[maxSlotIndex + 1];
		}
		return maybeVolumes;
	}

	public static VolumesAnalyzer calculateVolumes(Network network, String eventsFilename) {
		VolumesAnalyzer volumesAnalyzer = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(volumesAnalyzer);
		new MatsimEventsReader(events).readFile(eventsFilename);
		return volumesAnalyzer;
	}

}
