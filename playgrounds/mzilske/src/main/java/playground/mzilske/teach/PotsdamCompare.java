package playground.mzilske.teach;

import java.io.FileNotFoundException;
import java.util.ArrayList;
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.cdr.CallProcess;
import playground.mzilske.cdr.CellTower;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.SyntheticCellTowerDistribution;
import playground.mzilske.cdr.ZoneTracker;
import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;
import playground.mzilske.cdr.Zones;
import d4d.Sighting;

public class PotsdamCompare {

	private static final int TIME_BIN_SIZE = 60*60;
	private static final int MAX_TIME = 25 * TIME_BIN_SIZE;

	public static void main(String[] args) throws FileNotFoundException {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("input/potsdam/network.xml");
		//		VolumesAnalyzer volumesAnalyzer1 = calculateVolumes(scenario.getNetwork(), "output42/ITERS/it.10/10.events.xml.gz");
		//		VolumesAnalyzer volumesAnalyzer2 = calculateVolumes(scenario.getNetwork(), "output37/ITERS/it.10/10.events.xml.gz");
		//		for (Link link : scenario.getNetwork().getLinks().values()) {
		//			int[] volumesForLink1 = getVolumesForLink(volumesAnalyzer1, link);
		//			int[] volumesForLink2 = getVolumesForLink(volumesAnalyzer2, link);
		//			for (int i = 0; i < volumesForLink1.length; ++i) {
		//				System.out.println(volumesForLink1[i] - volumesForLink2[i]);
		//			}
		//		}
		//		
		new MatsimPopulationReader(scenario).readFile("output42/ITERS/it.10/10.plans.xml.gz");


		final Zones cellularCoverage = SyntheticCellTowerDistribution.naive(scenario.getNetwork());
		EventsManager events = EventsUtils.createEventsManager();

		Map<Id, Id> initialPersonInZone = new HashMap<Id, Id>();
		LinkToZoneResolver linkToZoneResolver = new LinkToZoneResolver() {

			@Override
			public Id resolveLinkToZone(Id linkId) {
				return cellularCoverage.locate(scenario.getNetwork().getLinks().get(linkId).getCoord());
			}

		};
		for (Person p : scenario.getPopulation().getPersons().values()) {
				Id linkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
				System.out.println(linkId);
				initialPersonInZone.put(p.getId(), linkToZoneResolver.resolveLinkToZone(linkId));
		}
		
		ZoneTracker zoneTracker = new ZoneTracker(events, linkToZoneResolver, initialPersonInZone);
		CallProcess callProcess = new CallProcess(events, scenario.getPopulation(), zoneTracker);

		events.addHandler(zoneTracker);
		events.addHandler(new CallProcessTicker(callProcess));

		new MatsimEventsReader(events).readFile("output42/ITERS/it.10/10.events.xml.gz");
		callProcess.dump();


		List<Sighting> sightings = callProcess.getSightings();

		runSimulationFromSightings(cellularCoverage, sightings);

	}

	public static void runSimulationFromSightings(final Zones cellularCoverage,
			List<Sighting> sightings) throws FileNotFoundException {
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
		QSimConfigGroup tmp = new QSimConfigGroup();
		tmp.setFlowCapFactor(0.01);
		tmp.setStorageCapFactor(0.01);
		tmp.setRemoveStuckVehicles(false);
		tmp.setEndTime(24*60*60);
		config.addQSimConfigGroup(tmp);
		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario2).readFile("input/potsdam/network.xml");

		

		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);
				
			}
			System.out.println(sighting.getCellTowerId().toString());
			CellTower cellTower = cellularCoverage.cellTowers.get(sighting.getCellTowerId().toString());
			cellTower.nSightings++;
			sightingsOfPerson.add(sighting);
		}


		cellularCoverage.buildCells();
		
		PopulationFromSightings.readSampleWithOneRandomPointForEachSightingInNewCell(scenario2, cellularCoverage, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, cellularCoverage, allSightings);

		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);
		controler.run();
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
