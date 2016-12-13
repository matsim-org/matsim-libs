package playground.sebhoerl.av_paper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.misc.Time;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.aggregate_events.Trip;

public class RunAnalysis {
	static public class TravelTimeHandler implements EventsToTrips.TripHandler {
		final private BinCalculator binCalculator;
		final private Map<String, List<List<Double>>> histogram = new HashMap<>();
		
		public TravelTimeHandler(BinCalculator binCalculator) {
			this.binCalculator = binCalculator;
			
			for (String mode : Arrays.asList("car", "taxi", "pt", "walk")) {
				List<List<Double>> modeBin = new LinkedList<List<Double>>();
				histogram.put(mode, modeBin);
				
				for (int i = 0; i < binCalculator.getBins(); i++) {
					modeBin.add(new LinkedList<Double>());
				}
			}
		}
		
		@Override
		public void handleTrip(PersonExperiencedTrip trip) {
			if (trip.getPerson().toString().contains("Taxi")) return;
			if (trip.getPerson().toString().contains("pt")) return;
			
			List<List<Double>> modeBin = histogram.get(trip.getMode());
			
			if (modeBin != null && binCalculator.isCoveredValue(trip.getStartTime())) {
				modeBin.get(binCalculator.getIndex(trip.getStartTime())).add(trip.getEndTime() - trip.getStartTime());
			}
		}
		
		public Map<String, List<List<Double>>> getTravelTimeHistogram() {
			return histogram;
		}
	}
	
	static public class DistanceHandler implements EventsToLegs.LegHandler {
		private Map<String, Double> vehicleDistances = new HashMap<>();
		private Map<String, Double> personDistances = new HashMap<>();
		
		public DistanceHandler() {
			personDistances.put("car", 0.0);
			personDistances.put("taxi", 0.0);
			personDistances.put("pt", 0.0);
			
			vehicleDistances.put("car", 0.0);
			vehicleDistances.put("taxi", 0.0);
			vehicleDistances.put("pt", 0.0);
		}
		
		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			if (leg.getAgentId().toString().contains("pt")) {
				vehicleDistances.put("pt", vehicleDistances.get("pt") + leg.getLeg().getRoute().getDistance());
			} else if (leg.getAgentId().toString().contains("Taxi")) {
				vehicleDistances.put("taxi", vehicleDistances.get("taxi") + leg.getLeg().getRoute().getDistance());
			} else {
				String mode = leg.getLeg().getMode();
				
				if (mode.equals("car")) {
					vehicleDistances.put("car", vehicleDistances.get("car") + leg.getLeg().getRoute().getDistance());
				}
				
				if (personDistances.containsKey(mode)) {
					personDistances.put(mode, personDistances.get(mode) + leg.getLeg().getRoute().getDistance());
				}
			}
		}
		
		public Map<String, Double> getVehicleDistances() {
			return vehicleDistances;
		}
		
		public Map<String, Double> getPersonDistances() {
			return personDistances;
		}
	}
	
	static public class PTHandler implements EventsToTrips.TripHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
		private double accumulatedWalkTime = 0.0;
		private double accumulatedWalkCount = 0.0;
		
		private double accumulatedWaitingTime = 0.0;
		private double accumulatedWaitingCount = 0.0;
		
		final private BinCalculator binCalculator;
		final private List<List<Double>> waitingTimes = new LinkedList<>();
		
		private Map<Id<Person>, Double> ongoing = new HashMap<>();
		
		public PTHandler(BinCalculator binCalculator) {
			this.binCalculator = binCalculator;
			
			for (int i = 0; i < binCalculator.getBins(); i++) {
				waitingTimes.add(new LinkedList<Double>());
			}
		}
		
		@Override
		public void reset(int iteration) {}
		
		@Override
		public void handleTrip(PersonExperiencedTrip trip) {
			accumulatedWalkTime += trip.getWalkTime();
			accumulatedWalkCount += 1.0;
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			Double startTime = ongoing.remove(event.getPersonId());
			
			if (startTime != null && binCalculator.isCoveredValue(startTime)) {
				int bin = binCalculator.getIndex(startTime);
				
				waitingTimes.get(bin).add(event.getTime() - startTime);
				accumulatedWaitingTime += event.getTime() - startTime;
				accumulatedWaitingCount += 1;
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals("pt")) {
				ongoing.put(event.getPersonId(), event.getTime());
			}
		}
		
		public double getWaitingTime() {
			return accumulatedWaitingTime / accumulatedWaitingCount;
		}
		
		public double getWalkingTime() {
			return accumulatedWalkTime / accumulatedWalkCount;
		}
		
		public List<List<Double>> getDetailedWaitingTimes() {
			return waitingTimes;
		}
	}
	
	static public class AVHandler implements EventsToActivities.ActivityHandler, EventsToLegs.LegHandler {
		final private Map<String, List<Double>> stateHistogram = new HashMap<>();
		final private BinCalculator binCalculator;
		
		private double accumulatedPickupTime = 0.0;	
		private double accumulatedPickupDistance = 0.0;
		
		private double accumulatedDropoffTime = 0.0;
		private double accumulatedDropoffDistance = 0.0;
		
		final Map<Id<Person>, String> current = new HashMap<>();
		
		public AVHandler(BinCalculator binCalculator) {
			this.binCalculator = binCalculator;
			
			stateHistogram.put("AVIdle", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			stateHistogram.put("AVPickup", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			stateHistogram.put("AVDropoff", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			stateHistogram.put("AVPickupDrive", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			stateHistogram.put("AVDropoffDrive", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
		}
		
		@Override
		public void handleActivity(PersonExperiencedActivity experiencedActivity) {
			if (!experiencedActivity.getAgentId().toString().contains("Taxi")) return;
			
			Activity activity = experiencedActivity.getActivity();
			double endTime = activity.getEndTime();
			
			current.put(experiencedActivity.getAgentId(), activity.getType());
			
			if (endTime == Time.UNDEFINED_TIME) {
				endTime = 30 * 3600;
			}
			
			List<Double> bin = stateHistogram.get(activity.getType());
			if (bin != null) {
				for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(activity.getStartTime(), endTime)) {
					bin.set(entry.getIndex(), bin.get(entry.getIndex()) + entry.getWeight());
					
					if (entry.getWeight() < 0.0) {
						System.err.println(String.format("%f :: %f", activity.getStartTime(), endTime));
					}
				}
			}
			
			if (activity.getType().equals("AVPickupDrive")) {
				accumulatedPickupTime += endTime - activity.getStartTime();
			} else if (activity.getType().equals("AVDropoffDrive")) {
				accumulatedDropoffTime += endTime - activity.getStartTime();
			}
		}
		
		public Map<String, List<Double>> getStateHistogram() {
			return stateHistogram;
		}

		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			if (!leg.getAgentId().toString().contains("Taxi")) return;
			String mode = current.get(leg.getAgentId());
			
			if (mode != null) {
				if (mode.equals("AVPickup")) {
					accumulatedPickupDistance += leg.getLeg().getRoute().getDistance();
				} else if (mode.equals("AVDropoff")) {
					accumulatedDropoffDistance += leg.getLeg().getRoute().getDistance();
				}
			}
		}
		
		public double getPickupTime() {
			return accumulatedPickupTime;
		}
		
		public double getDropoffTime() {
			return accumulatedDropoffTime;
		}
		
		public double getPickupDistance() {
			return accumulatedPickupDistance;
		}
		
		public double getDropoffDistance() {
			return accumulatedDropoffDistance;
		}
	}
	
	static public class ModeShareHandler implements EventsToTrips.TripHandler {
		final private Map<String, List<Double>> enrouteHistogram = new HashMap<>();
		final private Map<String, List<Double>> departureHistogram = new HashMap<>();
		final private Map<String, List<Double>> arrivalHistogram = new HashMap<>();
		final private BinCalculator binCalculator;
		
		public ModeShareHandler(BinCalculator binCalculator) {
			this.binCalculator = binCalculator;
			
			enrouteHistogram.put("car", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			enrouteHistogram.put("pt", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			enrouteHistogram.put("walk", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			enrouteHistogram.put("taxi", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			
			departureHistogram.put("car", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			departureHistogram.put("pt", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			departureHistogram.put("walk", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			departureHistogram.put("taxi", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			
			arrivalHistogram.put("car", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			arrivalHistogram.put("pt", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			arrivalHistogram.put("walk", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
			arrivalHistogram.put("taxi", new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
		}
		
		@Override
		public void handleTrip(PersonExperiencedTrip trip) {
			if (trip.getPerson().toString().contains("Taxi")) return;
			if (trip.getPerson().toString().contains("pt")) return;
			
			List<Double> enrouteBin = enrouteHistogram.get(trip.getMode());
			List<Double> departureBin = departureHistogram.get(trip.getMode());
			List<Double> arrivalBin = arrivalHistogram.get(trip.getMode());
			
			if (enrouteBin != null) {
				for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(trip.getStartTime(), trip.getEndTime())) {
					enrouteBin.set(entry.getIndex(), enrouteBin.get(entry.getIndex()) + entry.getWeight());
				}
				
				if (binCalculator.isCoveredValue(trip.getStartTime())) {
					int index = binCalculator.getIndex(trip.getStartTime());
					departureBin.set(index, departureBin.get(index) + 1.0);
				}
				
				if (binCalculator.isCoveredValue(trip.getEndTime())) {
					int index = binCalculator.getIndex(trip.getEndTime());
					arrivalBin.set(index, arrivalBin.get(index) + 1.0);
				}
			}
		}
		
		public Map<String, List<Double>> getEnrouteHistogram() {
			return enrouteHistogram;
		}
		
		public Map<String, List<Double>> getArrivalHistogram() {
			return arrivalHistogram;
		}
		
		public Map<String, List<Double>> getDepartureHistogram() {
			return departureHistogram;
		}
	}
	
	static public class WaitingTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler {
		@Override
		public void reset(int iteration) {}
		
		final private BinCalculator binCalculator;
		final private Map<Id<Person>, Double> waiting = new HashMap<>();
		
		final private List<Double> accumulatedWaitingTimes;
		final private List<Double> accumulatedTripCounts;
		final private List<List<Double>> waitingTimes = new LinkedList<>();
		
		public WaitingTimeHandler(BinCalculator binCalculator) {
			this.binCalculator = binCalculator;
			
			accumulatedWaitingTimes = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));
			accumulatedTripCounts = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));
			
			for (int i = 0; i < binCalculator.getBins(); i++) {
				waitingTimes.add(new LinkedList<Double>());
			}
		}
		
		@Override
		public void handleEvent(PersonArrivalEvent event) {

		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			Double start = waiting.remove(event.getPersonId());
			
			if (start != null && binCalculator.isCoveredValue(start)) {
				int bin = binCalculator.getIndex(start);

				waitingTimes.get(bin).add(event.getTime() - start);
				accumulatedWaitingTimes.set(bin, accumulatedWaitingTimes.get(bin) + (event.getTime() - start - 0.0));
				accumulatedTripCounts.set(bin, accumulatedTripCounts.get(bin) + 1.0);
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals("taxi")) {
				waiting.put(event.getPersonId(), event.getTime());
			}
		}
		
		public List<Double> getAverageHistogram() {
			List<Double> histogram = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));
			
			for (int i = 0; i < binCalculator.getBins(); i++) {
				histogram.set(i, accumulatedWaitingTimes.get(i) / accumulatedTripCounts.get(i));
			}
			
			return histogram;
		}
		
		public List<List<Double>> getDetailHistogram() {
			return waitingTimes;
		}
	}
	
	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/0.xml.gz", "/home/sebastian/paper/results/new/0.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/100.xml.gz", "/home/sebastian/paper/results/new/100.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/200.xml.gz", "/home/sebastian/paper/results/new/200.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/300.xml.gz", "/home/sebastian/paper/results/new/300.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/400.xml.gz", "/home/sebastian/paper/results/new/400.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/500.xml.gz", "/home/sebastian/paper/results/new/500.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/750.xml.gz", "/home/sebastian/paper/results/new/750.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/1000.xml.gz", "/home/sebastian/paper/results/new/1000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/1500.xml.gz", "/home/sebastian/paper/results/new/1500.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/2000_2.xml.gz", "/home/sebastian/paper/results/new/2000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/3000.xml.gz", "/home/sebastian/paper/results/new/3000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/4000.xml.gz", "/home/sebastian/paper/results/new/4000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/6000.xml.gz", "/home/sebastian/paper/results/new/6000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/results/new/8000.xml.gz", "/home/sebastian/paper/results/new/8000.json");
		
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_0.xml.gz", "/home/sebastian/paper/km/results/2_0.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_100.xml.gz", "/home/sebastian/paper/km/results/2_100.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_200.xml.gz", "/home/sebastian/paper/km/results/2_200.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_300.xml.gz", "/home/sebastian/paper/km/results/2_300.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_400.xml.gz", "/home/sebastian/paper/km/results/2_400.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_500.xml.gz", "/home/sebastian/paper/km/results/2_500.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_750.xml.gz", "/home/sebastian/paper/km/results/2_750.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_1000.xml.gz", "/home/sebastian/paper/km/results/2_1000.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_1500.xml.gz", "/home/sebastian/paper/km/results/2_1500.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_2000.xml.gz", "/home/sebastian/paper/km/results/2_2000.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_3000.xml.gz", "/home/sebastian/paper/km/results/2_3000.json.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km2_4000.xml.gz", "/home/sebastian/paper/km/results/2_4000.json.json");
		
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/100.xml.gz", "/home/sebastian/paper/km/results/km4/100.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/200.xml.gz", "/home/sebastian/paper/km/results/km4/200.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/300.xml.gz", "/home/sebastian/paper/km/results/km4/300.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/400.xml.gz", "/home/sebastian/paper/km/results/km4/400.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/500.xml.gz", "/home/sebastian/paper/km/results/km4/500.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/750.xml.gz", "/home/sebastian/paper/km/results/km4/750.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/1000.xml.gz", "/home/sebastian/paper/km/results/km4/1000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/2000.xml.gz", "/home/sebastian/paper/km/results/km4/2000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/3000.xml.gz", "/home/sebastian/paper/km/results/km4/3000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/4000.xml.gz", "/home/sebastian/paper/km/results/km4/4000.json");

		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/10000.xml.gz", "/home/sebastian/paper/km/results/km4/10000.json");
		//run("/home/sebastian/sioux/config.xml", "/home/sebastian/paper/km/results/km4/12000.xml.gz", "/home/sebastian/paper/km/results/km4/12000.json");
		
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/1_500.xml.gz", "/home/sebastian/avtaxi/results/1_500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/1_1000.xml.gz", "/home/sebastian/avtaxi/results/1_1000.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/1_1500.xml.gz", "/home/sebastian/avtaxi/results/1_1500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/1_2000.xml.gz", "/home/sebastian/avtaxi/results/1_2000.json");
		
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/2_500.xml.gz", "/home/sebastian/avtaxi/results/2_500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/2_1000.xml.gz", "/home/sebastian/avtaxi/results/2_1000.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/2_1500.xml.gz", "/home/sebastian/avtaxi/results/2_1500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/2_2000.xml.gz", "/home/sebastian/avtaxi/results/2_2000.json");
		
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/3_500.xml.gz", "/home/sebastian/avtaxi/results/3_500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/3_1000.xml.gz", "/home/sebastian/avtaxi/results/3_1000.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/3_1500.xml.gz", "/home/sebastian/avtaxi/results/3_1500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/3_2000.xml.gz", "/home/sebastian/avtaxi/results/3_2000.json");
		
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/4_500.xml.gz", "/home/sebastian/avtaxi/results/4_500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/4_1000.xml.gz", "/home/sebastian/avtaxi/results/4_1000.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/4_1500.xml.gz", "/home/sebastian/avtaxi/results/4_1500.json");
		run("/home/sebastian/avtaxi/config.xml", "/home/sebastian/avtaxi/results/4_2000.xml.gz", "/home/sebastian/avtaxi/results/4_2000.json");
	}
	
	private static void run(String configPath, String eventsPath, String outputPath) throws JsonGenerationException, JsonMappingException, IOException {		
        Config config = ConfigUtils.loadConfig(configPath);
        config.plans().setInputFile(null);
        
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        EventsManagerImpl events = new EventsManagerImpl();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        
        EventsToLegs events2legs = new EventsToLegs(scenario);
        EventsToActivities events2activities = new EventsToActivities();
        EventsToTrips events2trips = new EventsToTrips(events2legs, events2activities);
        
        events.addHandler(events2legs);
        events.addHandler(events2activities);
        
        BinCalculator binCalculator = BinCalculator.createByInterval(0 * 3600, 30 * 3600, 300);
        
        ModeShareHandler modeShareHandler = new ModeShareHandler(binCalculator);
        WaitingTimeHandler waitingTimeHandler = new WaitingTimeHandler(binCalculator);
        AVHandler avHandler = new AVHandler(binCalculator);
        PTHandler ptHandler = new PTHandler(binCalculator);
        DistanceHandler distanceHandler = new DistanceHandler();
        TravelTimeHandler travelTimeHandler = new TravelTimeHandler(binCalculator);
        
        events2activities.addActivityHandler(avHandler);
        events2trips.addTripHandler(modeShareHandler);
        events2trips.addTripHandler(ptHandler);
        events2trips.addTripHandler(travelTimeHandler);
        events2legs.addLegHandler(avHandler);
        events2legs.addLegHandler(distanceHandler);
        events.addHandler(waitingTimeHandler);
        events.addHandler(ptHandler);
        
        reader.readFile(eventsPath);
        events2activities.finish();
        events2trips.finalize();
        
        (new ObjectMapper()).writeValue(new File(outputPath), Arrays.asList(
        		modeShareHandler.getEnrouteHistogram(),			// 0
        		modeShareHandler.getArrivalHistogram(),			// 1
        		modeShareHandler.getDepartureHistogram(),		// 2
        		waitingTimeHandler.getAverageHistogram(),		// 3
        		waitingTimeHandler.getDetailHistogram(),		// 4
        		avHandler.getStateHistogram(),					// 5
        		ptHandler.getWaitingTime(),						// 6
        		ptHandler.getWalkingTime(),						// 7
        		avHandler.getPickupTime(),						// 8
        		avHandler.getPickupDistance(),					// 9
        		avHandler.getDropoffTime(),						// 10
        		avHandler.getDropoffDistance(),					// 11
        		distanceHandler.getPersonDistances(),			// 12
        		distanceHandler.getVehicleDistances(),			// 13
        		ptHandler.getDetailedWaitingTimes(),			// 14
        		travelTimeHandler.getTravelTimeHistogram()));	// 15
	}
}
