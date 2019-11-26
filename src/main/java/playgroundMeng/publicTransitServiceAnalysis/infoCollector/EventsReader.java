package playgroundMeng.publicTransitServiceAnalysis.infoCollector;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator.Builder;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.others.ActivitiesEventHandler;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class EventsReader {
	private static EventsReader reader = null;
	private final PtAccessabilityConfig ptAccessabilityConfig;
	private final Network network;
	private TravelTimeCalculator travelTimeCalculator;
	private ActivitiesEventHandler activitiesEventHandler;
	private List<Trip> trips = new ArrayList<Trip>();

	private EventsReader() {
		this.ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
		this.network = ptAccessabilityConfig.getNetwork();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		setTravelTimeCalculator();
		setActivitiesEventHandler();
		eventsManager.addHandler(travelTimeCalculator);
		eventsManager.addHandler(activitiesEventHandler);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(ptAccessabilityConfig.getEventFile());
		this.trips = activitiesEventHandler.getTrips();

	}

	public static EventsReader getInstance() {
		if (reader == null)
			reader = new EventsReader();
		return reader;
	}

	private void setActivitiesEventHandler() {
		this.activitiesEventHandler = new ActivitiesEventHandler(network);

	}

	private void setTravelTimeCalculator() {
		Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setMaxTime(36 * 3600);
		builder.setTimeslice(ptAccessabilityConfig.getAnalysisTimeSlice());
		this.travelTimeCalculator = builder.build();
	}

	public List<Trip> getTrips() {
		return trips;
	}

	public TravelTimeCalculator getTravelTimeCalculator() {
		return travelTimeCalculator;
	}
}
