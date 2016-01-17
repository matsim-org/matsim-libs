package playground.pieter.singapore.utils.events;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.singapore.utils.Sample;
import playground.pieter.singapore.utils.events.listeners.GetPersonIdsCrossingLinkSelection;
import playground.pieter.singapore.utils.events.listeners.TrimEventsWithPersonIds;

public class EventsExtractedForSelectedLinks {

    private final Scenario scenario = ScenarioUtils
			.createScenario(ConfigUtils.createConfig());
	private final HashSet<String> linkIds = new HashSet<>();

	private EventsExtractedForSelectedLinks(String idFile, String networkFile,
                                            String eventsINPUTFile, String eventsOUTPUTFile) throws IOException {

		this.populateList(idFile, networkFile);
		this.stripEvents(eventsINPUTFile, eventsOUTPUTFile, 0.1);
	}

	private void populateList(String idFile, String networkFile)
			throws IOException {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		BufferedReader reader = new BufferedReader(new FileReader(idFile));
		String line = reader.readLine();
		line = reader.readLine();
		// first line is the string 'ID'\
		while (line != null) {
			linkIds.add(line);
			line = reader.readLine();

		}
	}

	void stripEvents(String inFileName, String outfileName,
                     double frequency) {
        EventsManager events = EventsUtils.createEventsManager();
		// first, read through the events file and get all person ids crossing
		// the link set
		GetPersonIdsCrossingLinkSelection idFinder = new GetPersonIdsCrossingLinkSelection(
				linkIds);
		events.addHandler(idFinder);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		ArrayList<String> personIds = new ArrayList<>();
		personIds.addAll(idFinder.getPersonIds());
		int N = personIds.size();
		int M = (int) ((double) N * frequency);
		HashSet<String> sampledIds = new HashSet<>();
		for (int i : Sample.sampleMfromN(M, N)) {
			sampledIds.add(personIds.get(i));
		}
		// re-initialise the events manager to fileter events by person id
		events = null;
		events = EventsUtils.createEventsManager();
		TrimEventsWithPersonIds trimmer = new TrimEventsWithPersonIds(
				outfileName, sampledIds,false);
		events.addHandler(trimmer);
		reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		trimmer.closeFile();
	}

	public static void main(String[] args) throws IOException {
		new EventsExtractedForSelectedLinks(args[0], args[1], args[2], args[3]);

	}

}
