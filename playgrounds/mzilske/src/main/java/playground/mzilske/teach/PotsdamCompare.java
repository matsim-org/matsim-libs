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
import org.matsim.core.config.ConfigUtils;
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

		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
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
