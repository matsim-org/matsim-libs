package playground.toronto.router;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * A handler which stores the 'as-simulated' transit travel data.
 * 
 * <em>This class assumes that all stops except the first and last stops have defined arrival and departure offsets!</em>
 * 
 * @author pkucirek
 *
 */
public class TransitDataCache implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private static final Logger log = Logger.getLogger(TransitDataCache.class);
	
	private Map<Id, Id[]> vehInfo; //vehicleId, line-route
	private final TransitSchedule schedule;
	private Map<TransitRoute, Map<Id, TransitRouteStop>> routeStops; //Maps stopFacilityId to TransitRouteStop
		
	//Stores all the departures from a given TransitRouteStop
	private Map<TransitRouteStop, SortedSet<Double>> departures = null;
	private Map<TransitRouteStop, SortedSet<Double>> updatedDepartures = null;
	
	//Stores the travel times from one stop to the next for use in routing
	private Map<TransitRouteStop, TreeMap<Double, Double>> travelTimes = null; 
	private Map<TransitRouteStop, TreeMap<Double, Double>> updatedTravelTimes = null;
	
	//TODO remember default travel times and add them to the used travel times so that there is always at least one mapping in the treemap.
	
	//Temporary storage
	private HashMap<Id, Tuple<TransitRouteStop, Double>> vehicleDepartureCache;
	
	
	public TransitDataCache(final TransitSchedule schedule){
		this.schedule = schedule;
		this.updatedDepartures = new HashMap<TransitRouteStop, SortedSet<Double>>();
		this.updatedTravelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		this.vehInfo = new HashMap<Id, Id[]>();
		this.vehicleDepartureCache = new HashMap<Id, Tuple<TransitRouteStop,Double>>();
		
		this.init();
	}
	
	private void init(){
		
		log.info("Building initial transit data cache from transit schedule...");
		
		//loads the initial schedule data into the maps
		//also creates the initial mapping to go from stopFacilityId to TransitRouteStop
		this.routeStops = new HashMap<TransitRoute, Map<Id,TransitRouteStop>>();
		this.travelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		this.departures = new HashMap<TransitRouteStop, SortedSet<Double>>();
		
		for (TransitLine line: this.schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){				
				Map<Id, TransitRouteStop> stopFacilityMap = new HashMap<Id, TransitRouteStop>();
				
				TransitRouteStop prevStop = null;			
				
				for (TransitRouteStop stop : route.getStops()){				
					stopFacilityMap.put(stop.getStopFacility().getId(), stop); //Memorizes the backwards mapping from stop facility to transit route stop.
					
					//For all stops except the first, store the travel time to the next stop.
					Double travelTime = null;
					TreeMap<Double,Double> prevStopTimes = null;
					if (prevStop != null) {
						travelTime = stop.getArrivalOffset() - prevStop.getDepartureOffset();
						
						if (travelTime < 0){
							System.err.println("Check here!");
						}
						
						prevStopTimes = new TreeMap<Double, Double>();
						prevStopTimes.put(0.0, travelTime);//Because the travel time is constant, map the time to '0'
						this.travelTimes.put(prevStop, prevStopTimes);
					}
					
					//Build the map of departures from the stop
					SortedSet<Double> stopDepartures = new TreeSet<Double>();
					for (Departure d : route.getDepartures().values()){						
						stopDepartures.add(d.getDepartureTime() + stop.getDepartureOffset());
					}				
					this.departures.put(stop, stopDepartures);
					
					prevStop = stop;
				}
				this.routeStops.put(route, stopFacilityMap);
			}
		}
		log.info("Transit data cache built.");
	}

	
	private TransitRouteStop getVehicleStop(Id vehicleId, Id facilityId){
		Id[] signature = this.vehInfo.get(vehicleId);
		return this.routeStops.get(this.schedule.getTransitLines().get(signature[0]) //get line
				.getRoutes().get(signature[1])) //get route
				.get(facilityId); //get TransitRouteStop
	}
	
	/**
	 * Gets next scheduled departure from the TransitRouteStop, as simulated.
	 * 
	 * @param stop The {@link TransitRouteStop} of interest.
	 * @param now The current time (eg, 9:00AM)
	 * @return The next available departure time for the {@link TransitRoute} of interest (eg, 9:03AM), or
	 * the first departure the next day if none are found.
	 */
	public double getNextDepartureTime(final TransitRouteStop stop,final double now){
		SortedSet<Double> subSet = this.departures.get(stop).tailSet(now);
		
		if (subSet.size() == 0){ //no departures were found after this time.
			return 2 * Time.MIDNIGHT; //Get the earliest departure time the next day. 
		}
		return subSet.first();
	}
		
	/**
	 * Gets the dynamic time from one transit stop to the next.
	 * 
	 * @param stop The {@link TransitRouteStop} of interest.
	 * @param now The current time.
	 * @return The travel time to the next stop on the route, based on the current time or closest match to the current time.
	 */
	public double getCurrentTravelTime(final TransitRouteStop stop, double now){
		
		Entry<Double, Double> t = this.travelTimes.get(stop).floorEntry(now);
		if (t == null){ //current time is lower than lowest key, therefore get the lowest entry. Wait times are not handled here.
			t = this.travelTimes.get(stop).firstEntry();
		}
		
		if (t.getValue() < 0){
			System.err.println("Check here!");
		}
		
		return t.getValue();
	}
	
	public void checkTimesForStop(final TransitRouteStop stop){
		TreeMap<Double, Double> test = this.travelTimes.get(stop);
		for (Double e : test.descendingKeySet())
			System.out.println("At time " + Time.writeTime(e) + " travel time is " + test.get(e));
	}
	
	public void checkUpdatedTimesForStop(final TransitRouteStop stop){
		TreeMap<Double, Double> test = this.updatedTravelTimes.get(stop);
		for (Double e : test.descendingKeySet())
			System.out.println("At time " + Time.writeTime(e) + " travel time is " + test.get(e));
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleDepartureCache.clear();
		this.vehInfo.clear();
		
		//Flush old maps, replaces them with new ones. 		
		this.departures.clear();
		this.departures.putAll(updatedDepartures);
		this.updatedDepartures.clear();
		this.travelTimes.clear();
		this.travelTimes.putAll(updatedTravelTimes);
		this.updatedTravelTimes.clear();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if (this.vehInfo.containsKey(event.getVehicleId())){
			TransitRouteStop trStop = this.getVehicleStop(event.getVehicleId(), event.getFacilityId());
			if (trStop == null){
				log.error("Could not find a TransitRouteStop for vehicle " + event.getVehicleId() + " " 
						+ this.vehInfo.get(event.getVehicleId()).toString() + " at stop " + event.getFacilityId() + "!");
			}
			
			if (!this.updatedDepartures.containsKey(trStop)){
				SortedSet<Double> set = new TreeSet<Double>();
				set.add(event.getTime());
				this.updatedDepartures.put(trStop, set);
			}else{
				this.updatedDepartures.get(trStop).add(event.getTime());
			}
			
			this.vehicleDepartureCache.put(event.getVehicleId(), new Tuple<TransitRouteStop, Double>(trStop, event.getTime()));
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (event.getVehicleId().equals(new IdImpl("VehType9_8376"))){
			int q = 0; 
			q++;
		}
		
		
		if (this.vehInfo.containsKey(event.getVehicleId())){
			TransitRouteStop trStop = this.getVehicleStop(event.getVehicleId(), event.getFacilityId());
			if (trStop == null){
				log.error("Could not find a TransitRouteStop for vehicle " + event.getVehicleId() + " " 
						+ this.vehInfo.get(event.getVehicleId()).toString() + " at stop " + event.getFacilityId() + "!");
			}
			
			if (!this.vehicleDepartureCache.containsKey(event.getVehicleId()))
				return; //Vehicle 'arrives' at its very first stop, but really just appears there. We're not interested in this.
			
			//Calculates and stores the travel time from the previous stop to this stop, and stores it with the PREVIOUS stop.
			Tuple<TransitRouteStop, Double> tup = this.vehicleDepartureCache.get(event.getVehicleId());
			double travelTime = event.getTime() - tup.getSecond();
			if (!this.updatedTravelTimes.containsKey(tup.getFirst())){
				TreeMap<Double, Double> map = new TreeMap<Double, Double>();
				map.put(tup.getSecond(), travelTime);
				this.updatedTravelTimes.put(tup.getFirst(), map);
			}else{
				this.updatedTravelTimes.get(tup.getFirst()).put(tup.getSecond(), travelTime);
			}
			this.vehicleDepartureCache.remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehInfo.put(event.getVehicleId(), 
				new Id[]{event.getTransitLineId(), event.getTransitRouteId()});
	}

	
	
}
