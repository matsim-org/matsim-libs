package playground.toronto.router.calculators;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * A handler which stores the 'as-simulated' transit travel data. Provides lookup functions for travel time from one
 * {@link TransitRouteStop} to the next for a given time, as well as the next scheduled departure time for a given 
 * {@link TransitRouteStop} and time. 
 * 
 * <em>During construction, this class assumes that all stops except the first and last stops have defined arrival and 
 * departure offsets. Be careful if your schedule is missing this information!</em>
 * 
 * @author pkucirek
 *
 */
public class TransitDataCache implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private static final Logger log = Logger.getLogger(TransitDataCache.class);
	private int noStopRecordWarningCount = 0;
	
	private Map<Id, Id[]> vehInfo; //vehicleId, line-route
	private final TransitSchedule schedule;
	private Map<TransitRoute, Map<Id, TransitRouteStop>> routeStops; //Maps stopFacilityId to TransitRouteStop
		
	//Stores all the departures from a given TransitRouteStop
	private Map<TransitRouteStop, TreeSet<Double>> departures = null;
	private Map<TransitRouteStop, TreeSet<Double>> updatedDepartures = null;
	private Map<TransitRouteStop, TreeSet<Double>> defaultDepartures = null;
	
	//Stores the travel times from one stop to the next for use in routing
	private Map<TransitRouteStop, TreeMap<Double, Double>> travelTimes = null; 
	private Map<TransitRouteStop, TreeMap<Double, Double>> updatedTravelTimes = null;
	private Map<TransitRouteStop, TreeMap<Double, Double>> defaultTravelTimes = null;
	
	//Temporary storage during events handling
	private HashMap<Id, Tuple<TransitRouteStop, Double>> vehicleDepartureCache;
	
	
	public TransitDataCache(final TransitSchedule schedule){
		this.schedule = schedule;
		this.vehInfo = new HashMap<Id, Id[]>();
		this.vehicleDepartureCache = new HashMap<Id, Tuple<TransitRouteStop,Double>>();
		
		this.init2();
	}
	
	
	private void init(){
		
		log.info("Building initial transit data cache from transit schedule...");
		
		//loads the initial schedule data into the maps
		//also creates the initial mapping to go from stopFacilityId to TransitRouteStop
		this.routeStops = new HashMap<TransitRoute, Map<Id,TransitRouteStop>>();
		this.defaultDepartures = new HashMap<TransitRouteStop, TreeSet<Double>>();
		this.defaultTravelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		this.updatedDepartures = new HashMap<TransitRouteStop, TreeSet<Double>>();
		this.updatedTravelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		
		for (TransitLine line: this.schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){				
				Map<Id, TransitRouteStop> stopFacilityMap = new HashMap<Id, TransitRouteStop>();
								
				TransitRouteStop prevStop = null;
								
				for (TransitRouteStop stop : route.getStops()){					
					stopFacilityMap.put(stop.getStopFacility().getId(), stop); //Memorizes the backwards mapping from stop facility to transit route stop.
					
					//For all stops except the last, store the travel time to the next stop.
					Double travelTime = null;
					TreeMap<Double,Double> prevStopTimes = null;
					if (prevStop != null) {
						travelTime = stop.getArrivalOffset() - prevStop.getDepartureOffset();
						
						if (travelTime < 0){
							log.error("Found negative travel time IN SCHEDULE for line " + line.getId()
									+ " route " + route.getId() + ", between stops " + prevStop.getStopFacility().getId() +
									" and " + stop.getStopFacility().getId(), new IOException());
						}
						
						prevStopTimes = new TreeMap<Double, Double>();
						prevStopTimes.put(0.0, travelTime);//Because the scheduled travel time is constant, map the time to '0' (there will be no more entries)
						this.defaultTravelTimes.put(prevStop, prevStopTimes);
						this.updatedTravelTimes.put(prevStop, prevStopTimes);
					}
					
					//Build the map of departures from the stop
					TreeSet<Double> stopDepartures = new TreeSet<Double>();
					for (Departure d : route.getDepartures().values()){						
						stopDepartures.add(d.getDepartureTime() + stop.getDepartureOffset());
					}				
					this.defaultDepartures.put(stop, stopDepartures);
					this.updatedDepartures.put(stop, stopDepartures);
					
					prevStop = stop;
				}
				 //Remove the last stop from the default departures since no departure should ever occur from the last stop.
				//this.defaultDepartures.remove(prevStop);
				//this.updatedDepartures.remove(prevStop);
				
				this.routeStops.put(route, stopFacilityMap);
			}
		}
		
		this.travelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		this.travelTimes.putAll(defaultTravelTimes);
		this.departures = new HashMap<TransitRouteStop, TreeSet<Double>>();
		this.departures.putAll(defaultDepartures);
		
		int checksum = this.getCheckSum(schedule);
		int timeSize = this.travelTimes.size();
		int defTimesSize = this.defaultTravelTimes.size();
		int upTimesSize = this.defaultTravelTimes.size();
		int depsSize = this.departures.size();
		int defDepsSize = this.defaultDepartures.size();
		int upDepsSize = this.updatedDepartures.size();
		
		log.info("Transit data cache built.");
	}

	
	private void init2(){
		log.info("Building initial transit data cache from transit schedule...");
		
		//loads the initial schedule data into the maps
		//also creates the initial mapping to go from stopFacilityId to TransitRouteStop
		this.routeStops = new HashMap<TransitRoute, Map<Id,TransitRouteStop>>();
		this.defaultDepartures = new HashMap<TransitRouteStop, TreeSet<Double>>();
		this.defaultTravelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();

		for (TransitLine line: this.schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){				
				Map<Id, TransitRouteStop> stopFacilityMap = new HashMap<Id, TransitRouteStop>();
				
				TransitRouteStop prevStop = null;
				for (TransitRouteStop nextStop : route.getStops()){
					stopFacilityMap.put(nextStop.getStopFacility().getId(), nextStop); //Memorizes the backwards mapping from stop facility to transit route stop.
					
					if (prevStop != null){
						//Store the departure times
						TreeSet<Double> departures = new TreeSet<Double>();
						for (Departure dep : route.getDepartures().values()){
							departures.add(prevStop.getDepartureOffset() + dep.getDepartureTime());
						}
						this.defaultDepartures.put(prevStop, departures);
						
						//Store the travel time
						double travelTime = nextStop.getArrivalOffset() - prevStop.getDepartureOffset();
						if (travelTime < 0){
							log.error("Found negative travel time IN SCHEDULE for line " + line.getId()
									+ " route " + route.getId() + ", between stops " + prevStop.getStopFacility().getId() +
									" and " + nextStop.getStopFacility().getId(), new IOException());
						}
						TreeMap<Double, Double> prevStopTimes = new TreeMap<Double, Double>();
						prevStopTimes.put(0.0, travelTime);//Because the scheduled travel time is constant, map the time to '0' (there will be no more entries)
						this.defaultTravelTimes.put(prevStop, prevStopTimes);
					}
					
					prevStop = nextStop;
				}
				
				this.routeStops.put(route, stopFacilityMap);
			}
		}
		
		this.departures = new HashMap<TransitRouteStop, TreeSet<Double>>();
		this.travelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		this.updatedDepartures = new HashMap<TransitRouteStop, TreeSet<Double>>();
		this.updatedTravelTimes = new HashMap<TransitRouteStop, TreeMap<Double,Double>>();
		this.departures.putAll(defaultDepartures);
		this.travelTimes.putAll(defaultTravelTimes);
		this.updatedDepartures.putAll(defaultDepartures);
		this.updatedTravelTimes.putAll(defaultTravelTimes);
		
		log.info("Transit data cache built.");
		
	}
	
	private int getCheckSum(TransitSchedule schedule){
		int totalStops = 0;
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				totalStops += route.getStops().size() - 1;
			}
		}
		
		return totalStops;
	}
	
	private void checkMaps(){
		HashSet<TransitRouteStop> noDepStops = new HashSet<TransitRouteStop>();
		HashSet<TransitRouteStop> noTimeStops = new HashSet<TransitRouteStop>();
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (int i = 0; i < route.getStops().size() - 1; i++){
					TransitRouteStop stop = route.getStops().get(i);
					
					if (! this.defaultDepartures.containsKey(stop))
						noDepStops.add(stop);
					if (!this.defaultTravelTimes.containsKey(stop))
						noTimeStops.add(stop);
				}
			}
		}
		
		log.warn(noDepStops.size() + " stops were somehow not mapped in departures, " + noTimeStops.size() + " stops were somehow not mapped in times.");
		
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
	 * @return The next available departure time for the {@link TransitRoute} of interest (eg, 9:03AM), 2*MIDNIGHT if no such departure is found.
	 */
	public double getNextDepartureTime(final TransitRouteStop stop,final double now){
		
		Double e = this.departures.get(stop).ceiling(now);
		if (e == null){ //no departures were found after this time.
			return Double.POSITIVE_INFINITY;
		}
		return e;
	}
		
	/**
	 * Gets the dynamic time from one transit stop to the next. Travel times are stored in a ordered map; the as-simulated travel time at any 
	 * given point in time is returned as the next-lowest observed time. For example, if at 6:00 it took 10 minutes to travel to the next stop,
	 * and you are looking for travel time at 6:10, then this method would return 10 minutes. If no lower estimate is found, this method will
	 * return the the as-scheduled travel time.
	 * 
	 * @param stop The {@link TransitRouteStop} of interest.
	 * @param now The current time.
	 * @return The travel time to the next stop on the route, based on the current time or closest match to the current time.
	 * @throws Exception 
	 */
	public double getCurrentTravelTime(final TransitRouteStop stop, double now){
				
		Entry<Double, Double> t = this.travelTimes.get(stop).floorEntry(now);
		if (t == null){ //current time is lower than lowest key, therefore get the lowest entry. Wait times are not handled here.
			t = this.defaultTravelTimes.get(stop).firstEntry();
		}
		
		if (t.getValue() < 0){
			log.warn("Negative travel time was found at TransitRouteStop " + stop.getStopFacility().getId() + " (route not specified)! Returning 0.");
			return 0.0;
		}
		
		return t.getValue();
	}
	
	/**
	 * Prints all recorded travel times at a specified stop. Used for debugging.
	 * 
	 * @param stop
	 */
	public void checkTimesForStop(final TransitRouteStop stop){
		TreeMap<Double, Double> test = this.travelTimes.get(stop);
		for (Double e : test.descendingKeySet())
			System.out.println("At time " + Time.writeTime(e) + " travel time is " + test.get(e));
	}
	
	/**
	 * Prints all updated travel times at a specified stop. Used for debugging.
	 * @param stop
	 */
	public void checkUpdatedTimesForStop(final TransitRouteStop stop){
		TreeMap<Double, Double> test = this.updatedTravelTimes.get(stop);
		for (Double e : test.descendingKeySet())
			System.out.println("At time " + Time.writeTime(e) + " travel time is " + test.get(e));
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleDepartureCache.clear();
		this.vehInfo.clear();
		this.noStopRecordWarningCount= 0;
		
		//Flush old maps, replaces them with new ones. 		
		this.departures.clear();
		this.departures.putAll(updatedDepartures);
		this.updatedDepartures.clear();
		this.travelTimes.clear();
		this.travelTimes.putAll(updatedTravelTimes);
		this.updatedTravelTimes.clear();
		
		//Check and correct to make sure that all transit stops have mapped travel times and departures
		//this.travelTimes.putAll(updatedTravelTimes); // Default times = 0.0->[defaultStaticTravelTime]
		for (TransitRouteStop key : this.defaultDepartures.keySet()){
			if (this.departures.get(key) == null){
				//For some reason no actual departures were recorded
				Id lineId = null; Id routeId = null; boolean k = false;
				for (TransitLine line : this.schedule.getTransitLines().values()){ //Find the line & route because there's no backwards mapping.
					if (k) break;
					for (TransitRoute route : line.getRoutes().values()){
						if (route.getStops().contains(key)){
							lineId = line.getId();
							routeId = route.getId();
							k=true;
							break;
						}
					}
				}
				if (this.noStopRecordWarningCount++ < 40){
					log.warn("No departures were recorded for line " + lineId + " route " + routeId + " at stop " 
						+ key.getStopFacility().getId() + "! TransitDataCache is using scheduled departure times for this stop. This could cause problems!");
				}
				else if (this.noStopRecordWarningCount == 40){
					log.warn("No departures were recorded for line " + lineId + " route " + routeId + " at stop " 
							+ key.getStopFacility().getId() + "! TransitDataCache is using scheduled departure times for this stop. This could cause problems!");
					log.warn("Future warnings are supressed.");
				}
				this.departures.put(key, this.defaultDepartures.get(key));
			}
			
			if (this.travelTimes.get(key) == null){
				//For some reason no actual travel times were recorded
				Id lineId = null; Id routeId = null; boolean k = false;
				for (TransitLine line : this.schedule.getTransitLines().values()){ //Find the line & route because there's no backwards mapping.
					if (k) break;
					for (TransitRoute route : line.getRoutes().values()){
						if (route.getStops().contains(key)){
							lineId = line.getId();
							routeId = route.getId();
							k=true;
							break;
						}
					}
				}
				if (this.noStopRecordWarningCount++ < 40){
					log.warn("No travel times were recorded for line " + lineId + " route " + routeId + " at stop " 
						+ key.getStopFacility().getId() + "! TransitDataCache is using scheduled travel times for this stop. This could cause problems!");
				}
				else if (this.noStopRecordWarningCount == 40){
					log.warn("No travel times were recorded for line " + lineId + " route " + routeId + " at stop " 
							+ key.getStopFacility().getId() + "! TransitDataCache is using scheduled travel times for this stop. This could cause problems!");
					log.warn("Future warnings are supressed.");
				}
				this.travelTimes.put(key, this.defaultTravelTimes.get(key));
			}
		}
		
		if (noStopRecordWarningCount > 0){
			log.warn(this.noStopRecordWarningCount + " transit route stops were not visited during the previous iteratino.");
		}
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
				TreeSet<Double> set = new TreeSet<Double>();
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
		if (this.vehInfo.containsKey(event.getVehicleId())){
			TransitRouteStop trStop = this.getVehicleStop(event.getVehicleId(), event.getFacilityId());
			if (trStop == null){
				log.error("Could not find a TransitRouteStop for vehicle " + event.getVehicleId() + " " 
						+ this.vehInfo.get(event.getVehicleId()).toString() + " at stop " + event.getFacilityId() + "!");
			}
			
			if (!this.vehicleDepartureCache.containsKey(event.getVehicleId()))
				return; //Vehicle 'arrives' at its very first stop, but really just appears there. We're not interested in this so it's skipped.
			
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
