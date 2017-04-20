package playground.dziemke.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class TripHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
    public static final Logger log = Logger.getLogger(TripHandler.class);
    private static boolean tripmodeWarn = true;

    private Map<Id<Trip>, Trip> trips = new HashMap<>();
	
	private Map<Id<Person>, Integer> activityEndCount = new HashMap <Id<Person>, Integer>();
	private Map<Id<Person>, Integer> activityStartCount = new HashMap <Id<Person>, Integer>();
	
	int noPreviousEndOfActivityCounter = 0;
	
	Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();
	
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		// store information from event to variables and print the information on console
		//String eventType = event.getEventType();
		Id<Link> linkId = event.getLinkId();
		//String linkShortened = linkId.toString().substring(0, 10) + "...";
		Id<Person> personId = event.getPersonId();
		double time_s = event.getTime();
		String actType = event.getActType();
		//Id facilityId =	event.getFacilityId();
		//System.out.println("Type: " + eventType + " - LinkId: " + linkShortened + " - PersonId: " + personId.toString()
		//		+ " - Time: " + time/60/60 + " - ActType: " + actType + " - FacilityId: " + facilityId);
		
		
		// count number of activity ends for every agent and store these numbers in a map
		if (!activityEndCount.containsKey(personId)) {
			activityEndCount.put(personId, 1);
		} else {
			int numberOfCompletedDepartures = activityEndCount.get(personId);
			activityEndCount.put(personId, numberOfCompletedDepartures + 1);
		}
		//System.out.println("Agent " + personId + " has " + activityEndCount.get(personId) + " activity ends.");
		
		
		// create an instance of the object "Trip"
		Trip trip = new Trip();
		Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
		trip.setTripId(tripId);
		trip.setPersonId(personId);
		trip.setDepartureLinkId(linkId);
		trip.setDepartureTime_s(time_s);
		//trip.setDepartureLegMode(legMode);
		trip.setActivityTypeBeforeTrip(actType);
		trip.setWeight(1.);
		trips.put(tripId, trip);
		
		
		// check if activity end link is the same as previous activity start link
		if (activityEndCount.get(personId) >= 2) {
			int numberOfLastArrival = activityStartCount.get(personId);
			Id<Trip> lastTripId = Id.create(personId + "_" + numberOfLastArrival, Trip.class);
			if (!trips.get(tripId).getDepartureLinkId().equals(trips.get(lastTripId).getArrivalLinkId())) {
				//System.err.println("Activity end link differs from previous activity start link.");
				throw new RuntimeException("Activity end link differs from previous activity start link.");
			} 
		}
		
		
		// check if type of ending activity is the same as type of previously started activity
		if (activityEndCount.get(personId) >= 2) {
			int numberOfLastArrival = activityStartCount.get(personId);
			Id<Trip> lastTripId = Id.create(personId + "_" + numberOfLastArrival, Trip.class);
			if (!trips.get(tripId).getActivityTypeBeforeTrip().equals(trips.get(lastTripId).getActivityTypeAfterTrip())) {
				//System.err.println("Type of ending activity is not the same as type of previously started activity.");
				throw new RuntimeException("Type of ending activity is not the same as type of previously started activity.");
			} 
		}
	}

	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		// store information from event to variables and print the information on console
		//String eventType = event.getEventType();
		Id<Link> linkId = event.getLinkId();
		//String linkShortened = linkId.toString().substring(0, 10) + "...";
		Id<Person> personId = event.getPersonId();
		double time_s = event.getTime();
		String actType = event.getActType();
		//Id facilityId =	event.getFacilityId();
		//System.out.println("Type: " + eventType + " - LinkId: " + linkShortened + " - PersonId: " + personId.toString()
		//		+ " - Time: " + time/60/60 + " - ActType: " + actType + " - FacilityId: " + facilityId);
		
		
		// count number of activity starts for every agent and store these numbers in a map
		if (!activityStartCount.containsKey(personId)) {
			activityStartCount.put(personId, 1);
		} else {
			int numberOfCompletedDepartures = activityStartCount.get(personId);
			activityStartCount.put(personId, numberOfCompletedDepartures + 1);
		}
		//System.out.println("Agent " + personId + " has " + activityEndCount.get(personId) + " activity ends and " + activityStartCount.get(personId) + " activity starts.");
		
		
		// add information to the object "Trip"
		Id<Trip> tripId = Id.create(personId + "_" + activityStartCount.get(personId), Trip.class);
		if (trips.get(tripId) != null) {
			trips.get(tripId).setArrivalLinkId(linkId);
			trips.get(tripId).setArrivalTime_s(time_s);
			//trips.get(tripId).setArrivalLegMode(legMode);
			trips.get(tripId).setActivityTypeAfterTrip(actType);
			trips.get(tripId).setTripComplete(true);
		} else {
			this.noPreviousEndOfActivityCounter++;
		}
		
		
		// check if number of activity ends and number of activity starts are the same
		if(activityStartCount.get(personId) != activityEndCount.get(personId)) {
			//System.err.println("Activity start count differs from activity end count.");
			throw new RuntimeException("Activity start count differs from activity end count.");
		}
		
		
		// checking leg modes is not applicable here
	}
	
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// store information from event to variables
		//String eventType = event.getEventType();
		Id<Link> linkId = event.getLinkId();
		//String linkShortened = linkId.toString().substring(0, 10) + "...";
//		Id<Person> personId = event.getDriverId();
		Id<Person> personId = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
		//double time = event.getTime();
		//Id vehicleId = event.getVehicleId();
		
		
		// add information concerning passed links to the object "Trip"
		Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
		if (trips.get(tripId).getLinks().isEmpty()) {
			if (trips.get(tripId).getDepartureLinkId().equals(linkId)) {
				trips.get(tripId).getLinks().add(linkId);
				//System.out.println("Added first link to trip " + tripId);
			} else {
				//System.err.println("First route link different from departure link!");
				throw new RuntimeException("First route link different from departure link!");
			}
		} else {
			trips.get(tripId).getLinks().add(linkId);
//			System.out.println("Added another link to trip " + tripId);
//			System.out.println("List of trip " + tripId + " has now " + trips.get(tripId).getLinks().size() + " elements");
		}		
	}
	
	
//	// --------------------------------------------------------------------------------------------------
	public void handleEvent(PersonArrivalEvent event) {
		// store information from event to variable
		String legMode = event.getLegMode();
		//System.out.println("Mode of current trip is " + legModeString);
        Id<Person> personId = event.getPersonId();
        // other information not needed

        // add information concerning leg mode to the object "Trip"
        Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
        trips.get(tripId).setMode(legMode);
        if (tripmodeWarn) {
            log.warn("Trip mode = Arrival leg mode; assumed that every leg has the same legMode");
            tripmodeWarn = false;
        }

	}
// --------------------------------------------------------------------------------------------------
	
	
	@Override
	public void reset(int iteration) {
	}
	
	
	public Map<Id<Trip>, Trip> getTrips() {
		return this.trips;
	}
	
	
	public int getNoPreviousEndOfActivityCounter() {
		return this.noPreviousEndOfActivityCounter;
	}


	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}


	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}
}
