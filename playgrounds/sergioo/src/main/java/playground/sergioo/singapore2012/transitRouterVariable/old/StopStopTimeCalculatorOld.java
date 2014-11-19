package playground.sergioo.singapore2012.transitRouterVariable.old;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTime;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeData;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeDataArray;

public class StopStopTimeCalculatorOld implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler {
	
	private final Map<String, StopStopTimeData> stopStopTimes = new ConcurrentHashMap<String, StopStopTimeData>(6602);
	private final Map<String, Double> scheduledStopStopTimes = new ConcurrentHashMap<String, Double>(6602);
	private final Map<Id<Vehicle>, Tuple<Id<TransitStopFacility>, Double>> inTransitVehicles = new ConcurrentHashMap<Id<Vehicle>, Tuple<Id<TransitStopFacility>,Double>>(1000);
	private final Set<Id<Vehicle>> vehicleIds = new HashSet<Id<Vehicle>>();
	private double timeSlot;
	public static int scheduled = 0;
	public static int recorded = 0;
	
	//Constructors
	public StopStopTimeCalculatorOld(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public StopStopTimeCalculatorOld(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		Map<String, Integer> numObservations = new HashMap<String, Integer>();
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(int s=0; s<route.getStops().size()-1; s++) {
					String key = route.getStops().get(s).getStopFacility().getId()+">"+route.getStops().get(s+1).getStopFacility().getId();
					stopStopTimes.put(key, new StopStopTimeDataArray((int) (totalTime/timeSlot)+1));
					Double stopStopTime = scheduledStopStopTimes.get(key);
					Integer num;
					if(stopStopTime == null) {
						stopStopTime = 0.0;
						num = 0; 
					}
					else
						num = numObservations.get(key);
					scheduledStopStopTimes.put(key, stopStopTime+route.getStops().get(s+1).getArrivalOffset()-route.getStops().get(s).getDepartureOffset());
					numObservations.put(key, ++num);
				}
		for(Entry<String, Double> entry:scheduledStopStopTimes.entrySet())
			scheduledStopStopTimes.put(entry.getKey(), entry.getValue()/numObservations.get(entry.getKey()));
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(Departure departure:route.getDepartures().values()) {
					vehicleIds.add(departure.getVehicleId());
					inTransitVehicles.put(departure.getVehicleId(), new Tuple<Id<TransitStopFacility>, Double>(route.getStops().get(0).getStopFacility().getId(), 0.0));
				}
	}
		
	//Methods
	public StopStopTime getStopStopTimes() {
		return new StopStopTime() {
			@Override
			public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
				return StopStopTimeCalculatorOld.this.getStopStopTime(stopOId, stopDId, time);
			}
			@Override
			public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
				return 0;
			}
		};
	}
	private double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
		String key = stopOId.toString()+">"+stopDId.toString();
		StopStopTimeData stopStopTimeData = stopStopTimes.get(key);
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0) {
			scheduled++;
			return scheduledStopStopTimes.get(key);
		}
		else {
			recorded++;
			return stopStopTimeData.getStopStopTime((int) (time/timeSlot));
		}
	}
	@Override
	public void reset(int iteration) {
		for(StopStopTimeData stopStopTimeData:stopStopTimes.values())
			stopStopTimeData.resetStopStopTimes();
		inTransitVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if(vehicleIds.contains(event.getVehicleId()))
			inTransitVehicles.put(event.getVehicleId(), new Tuple<Id<TransitStopFacility>, Double>(event.getFacilityId(), event.getTime()));
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(vehicleIds.contains(event.getVehicleId())) {
			Tuple<Id<TransitStopFacility>, Double> route = inTransitVehicles.remove(event.getVehicleId());
			if(!route.getFirst().equals(event.getVehicleId())) {
				String key = route.getFirst().toString()+">"+event.getFacilityId().toString();
				stopStopTimes.get(key).addStopStopTime((int) (route.getSecond()/timeSlot), event.getTime()-route.getSecond());
			}
		}
	}

}
