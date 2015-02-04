package playground.artemc.transitRouter.stopStopTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StopStopTimeCalculator implements VehicleArrivesAtFacilityEventHandler, PersonLeavesVehicleEventHandler, Serializable {
	
	private final Map<Id, Map<Id, StopStopTimeData>> stopStopTimes = new HashMap<Id, Map<Id, StopStopTimeData>>(5000);
	private final Map<Id, Map<Id, Double>> scheduledStopStopTimes = new HashMap<Id, Map<Id, Double>>(5000);
	private final Map<Id, Tuple<Id, Double>> inTransitVehicles = new HashMap<Id, Tuple<Id,Double>>(1000);
	private final Set<Id> vehicleIds = new HashSet<Id>();
	private double timeSlot;
	private boolean useVehicleIds = true;
	
	//Constructors
	public StopStopTimeCalculator(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public StopStopTimeCalculator(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		Map<Id, Map<Id, Integer>> numObservations = new HashMap<Id, Map<Id, Integer>>();
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				for(int s=0; s<route.getStops().size()-1; s++) {
					Map<Id, StopStopTimeData> map = stopStopTimes.get(route.getStops().get(s).getStopFacility().getId());
					if(map==null) {
						map = new HashMap<Id, StopStopTimeData>(2);
						stopStopTimes.put(route.getStops().get(s).getStopFacility().getId(), map);
					}
					map.put(route.getStops().get(s+1).getStopFacility().getId(), new StopStopTimeDataArray((int) (totalTime/timeSlot)+1));
					Map<Id, Double> map2 = scheduledStopStopTimes.get(route.getStops().get(s).getStopFacility().getId());
					Map<Id, Integer> map3 = numObservations.get(route.getStops().get(s).getStopFacility().getId());
					Double stopStopTime;
					Integer num;
					if(map2==null) {
						map2 = new HashMap<Id, Double>(2);
						scheduledStopStopTimes.put(route.getStops().get(s).getStopFacility().getId(), map2);
						map3 = new HashMap<Id, Integer>(2);
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
				for(Departure departure:route.getDepartures().values())
					vehicleIds.add(departure.getVehicleId());
			}
		for(Entry<Id, Map<Id, Double>> entry:scheduledStopStopTimes.entrySet())
			for(Entry<Id, Double> entry2:entry.getValue().entrySet())
				entry.getValue().put(entry2.getKey(), entry2.getValue()/numObservations.get(entry.getKey()).get(entry2.getKey()));
	}
		
	//Methods
	public StopStopTime getStopStopTimes() {
		return new StopStopTime() {
			@Override
			public double getStopStopTime(Id stopOId, Id stopDId, double time) {
				return StopStopTimeCalculator.this.getStopStopTime(stopOId, stopDId, time);
			}
			@Override
			public double getStopStopTimeVariance(Id stopOId, Id stopDId, double time) {
				return StopStopTimeCalculator.this.getStopStopTimeVariance(stopOId, stopDId, time);
			}
		};
	}
	private double getStopStopTime(Id stopOId, Id stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId).get(stopDId);
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0)
			return scheduledStopStopTimes.get(stopOId).get(stopDId);
		else
			return stopStopTimeData.getStopStopTime((int) (time/timeSlot));
	}
	private double getStopStopTimeVariance(Id stopOId, Id stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId).get(stopDId);
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0)
			return 0;
		else
			return stopStopTimeData.getStopStopTimeVariance((int) (time/timeSlot));
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
		if(!useVehicleIds || vehicleIds.contains(event.getVehicleId())) {
			Tuple<Id, Double> route = inTransitVehicles.remove(event.getVehicleId());
			if(route!=null)
				try {
					stopStopTimes.get(route.getFirst()).get(event.getFacilityId()).addStopStopTime((int) (route.getSecond()/timeSlot), event.getTime()-route.getSecond());
				} catch(Exception e) {
					//System.out.println("No: "+route.getFirst()+"-->"+event.getFacilityId());
				}
			inTransitVehicles.put(event.getVehicleId(), new Tuple<Id, Double>(event.getFacilityId(), event.getTime()));
		}
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if((!useVehicleIds || vehicleIds.contains(event.getVehicleId())) && event.getPersonId().toString().startsWith("pt_") && event.getPersonId().toString().contains(event.getVehicleId().toString()))
			inTransitVehicles.remove(event.getVehicleId());
	}
	public void setUseVehicleIds(boolean useVehicleIds) {
		this.useVehicleIds = useVehicleIds;
	}
}
