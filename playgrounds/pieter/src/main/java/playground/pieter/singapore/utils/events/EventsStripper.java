package playground.pieter.singapore.utils.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.singapore.utils.Sample;
import playground.pieter.singapore.utils.events.listeners.TrimEventsWithPersonIds;

public class EventsStripper {

	private class FindTransitDriverIdsFromVehicleIds implements
			TransitDriverStartsEventHandler {
		final HashSet<String> transitDriverIds = new HashSet<>();
		final HashSet<String> transitVehicleIds;

		public FindTransitDriverIdsFromVehicleIds(
				HashSet<String> transitVehicleIds) {
			this.transitVehicleIds = transitVehicleIds;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			String driver =event.getDriverId().toString();

			String car = event.getVehicleId().toString();
			for (String vehId : transitVehicleIds) {
				if (car.equals(vehId))
					transitDriverIds.add(driver);

			}

		}

		public HashSet<String> getTransitDriverIds() {
			return transitDriverIds;
		}


	}

	private String[] choiceSet;
    private Scenario scenario = ScenarioUtils
			.createScenario(ConfigUtils.createConfig());

	public EventsStripper(List<String> ids) {

		this.populateList(ids);
	}
	private EventsStripper(String[] ids) {
		choiceSet = ids;
		
	}

	public EventsStripper(String plansFile) {
		this.populateList(plansFile);
	}

	private void populateList(List<String> ids) {

		choiceSet = new String[ids.size()];
		for (int i = 0; i < choiceSet.length; i++) {
			choiceSet[i] = ids.get(i);
		}

		scenario = null;
	}

	private void populateList(String plansFile) {
		MatsimPopulationReader pn = new MatsimPopulationReader(scenario);
		pn.readFile(plansFile);
		ArrayList<Id> ids = new ArrayList<>();
		CollectionUtils.addAll(ids, scenario.getPopulation().getPersons()
				.keySet().iterator());
		choiceSet = new String[ids.size()];
		for (int i = 0; i < choiceSet.length; i++) {
			choiceSet[i] = ids.get(i).toString();
		}
	}

	public void stripEvents(String inFileName, String outfileName,
			double frequency, boolean listenForTransitDrivers) {
        EventsManager events = EventsUtils.createEventsManager();
		int N = choiceSet.length;
		int M = (int) ((double) N * frequency);
		HashSet<String> sampledIds = new HashSet<>();
		for (int i : Sample.sampleMfromN(M, N)) {
			sampledIds.add(choiceSet[i]);
		}
		TrimEventsWithPersonIds filteredWriter = new TrimEventsWithPersonIds(
				outfileName, sampledIds, listenForTransitDrivers);
		events.addHandler(filteredWriter);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		filteredWriter.closeFile();
		if (listenForTransitDrivers
				&& filteredWriter.getTransitVehicleIds() != null) {
			FindTransitDriverIdsFromVehicleIds transitDriverFinder = new FindTransitDriverIdsFromVehicleIds(
					filteredWriter.getTransitVehicleIds());
			events = EventsUtils.createEventsManager();
			events.addHandler(transitDriverFinder);
			reader = new EventsReaderXMLv1(events);
			reader.parse(inFileName);
			sampledIds.addAll(transitDriverFinder.transitDriverIds);
			sampledIds.addAll(filteredWriter.getTransitVehicleIds());
			events = EventsUtils.createEventsManager();
			filteredWriter = new TrimEventsWithPersonIds(outfileName,
					sampledIds, false);
			events.addHandler(filteredWriter);
			reader = new EventsReaderXMLv1(events);
			reader.parse(inFileName);
			filteredWriter.closeFile();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//
//		ArrayList<String> ids = new ArrayList<String>();
//		ids.add("4101962"); //transit user
//		ids.add("77878"); //car user
		try{
		EventsStripper stripper = new EventsStripper(args[4].split(","));
		stripper.stripEvents(args[0], args[1], Double.parseDouble(args[2]), Boolean.parseBoolean(args[3]));
		}catch(ArrayIndexOutOfBoundsException e){
			System.out.println("Strips events file to a target events file.\n" +
					"Arguments:\n" +
					"inFileName (events file) outfileName (events file) frequency (0-1, i.e. fraction of ids to actually use)\n" +
					"extracttransitDriverEvents(true/false) ids (comma-separated list of person ids, no spaces");
		}
	}

}
