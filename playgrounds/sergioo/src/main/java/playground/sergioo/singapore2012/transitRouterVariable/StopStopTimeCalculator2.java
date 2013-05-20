package playground.sergioo.singapore2012.transitRouterVariable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class StopStopTimeCalculator2 implements VehicleArrivesAtFacilityEventHandler {
	
	private final Map<Id, Map<Id, StopStopTimeData>> stopStopTimes = new ConcurrentHashMap<Id, Map<Id, StopStopTimeData>>(5000);
	private final Map<Id, Map<Id, Double>> scheduledStopStopTimes = new ConcurrentHashMap<Id, Map<Id, Double>>(5000);
	private final Map<Id, Tuple<Id, Double>> inTransitVehicles = new ConcurrentHashMap<Id, Tuple<Id,Double>>(1000);
	private final Set<Id> vehicleIds = new HashSet<Id>();
	private double timeSlot;
	public static int scheduled = 0;
	public static int recorded = 0;
	
	//Constructors
	public StopStopTimeCalculator2(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		Map<Id, Map<Id, Integer>> numObservations = new HashMap<Id, Map<Id, Integer>>();
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(int s=0; s<route.getStops().size()-1; s++) {
					Map<Id, StopStopTimeData> map = stopStopTimes.get(route.getStops().get(s).getStopFacility().getId());
					if(map==null) {
						map = new ConcurrentHashMap<Id, StopStopTimeData>(2);
						stopStopTimes.put(route.getStops().get(s).getStopFacility().getId(), map);
					}
					map.put(route.getStops().get(s+1).getStopFacility().getId(), new StopStopTimeDataArray((int) (totalTime/timeSlot)+1));
					Map<Id, Double> map2 = scheduledStopStopTimes.get(route.getStops().get(s).getStopFacility().getId());
					Map<Id, Integer> map3 = numObservations.get(route.getStops().get(s).getStopFacility().getId());
					Double stopStopTime;
					Integer num;
					if(map2==null) {
						map2 = new ConcurrentHashMap<Id, Double>(2);
						scheduledStopStopTimes.put(route.getStops().get(s).getStopFacility().getId(), map2);
						map3 = new ConcurrentHashMap<Id, Integer>(2);
						numObservations.put(route.getStops().get(s).getStopFacility().getId(), map3);
						stopStopTime = 0.0;
						num = 0;
					}
					else {
						stopStopTime = map2.get(route.getStops().get(s+1).getStopFacility().getId());
						num = map3.get(route.getStops().get(s+1).getStopFacility().getId());
						if(stopStopTime==null) {
							stopStopTime = 0.0;
							num = 0;
						}
					}
					map2.put(route.getStops().get(s+1).getStopFacility().getId(), stopStopTime+route.getStops().get(s+1).getArrivalOffset()-route.getStops().get(s).getDepartureOffset());
					map3.put(route.getStops().get(s+1).getStopFacility().getId(), ++num);
				}
		for(Entry<Id, Map<Id, Double>> entry:scheduledStopStopTimes.entrySet()) {
			for(Entry<Id, Double> entry2:entry.getValue().entrySet())
				entry.getValue().put(entry2.getKey(), entry2.getValue()/numObservations.get(entry.getKey()).get(entry2.getKey()));
		}
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(Departure departure:route.getDepartures().values()) {
					vehicleIds.add(departure.getVehicleId());
				}
	}
		
	//Methods
	public StopStopTime getStopStopTimes() {
		return new StopStopTime() {
			@Override
			public double getStopStopTime(Id stopOId, Id stopDId, double time) {
				return StopStopTimeCalculator2.this.getStopStopTime(stopOId, stopDId, time);
			}
		};
	}
	private double getStopStopTime(Id stopOId, Id stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId).get(stopDId);
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0) {
			scheduled++;
			return scheduledStopStopTimes.get(stopOId).get(stopDId);
		}
		else {
			recorded++;
			return stopStopTimeData.getStopStopTime((int) (time/timeSlot));
		}
	}
	@Override
	public void reset(int iteration) {
		for(Map<Id, StopStopTimeData> map:stopStopTimes.values())
			for(StopStopTimeData stopStopTimeData:map.values())
				stopStopTimeData.resetStopStopTimes();
		inTransitVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(vehicleIds.contains(event.getVehicleId())) {
			Tuple<Id, Double> route = inTransitVehicles.remove(event.getVehicleId());
			if(route!=null)
				stopStopTimes.get(route.getFirst()).get(event.getFacilityId()).addStopStopTime((int) (route.getSecond()/timeSlot), event.getTime()-route.getSecond());
			inTransitVehicles.put(event.getVehicleId(), new Tuple<Id, Double>(event.getFacilityId(), event.getTime()));
		}
	}

}
