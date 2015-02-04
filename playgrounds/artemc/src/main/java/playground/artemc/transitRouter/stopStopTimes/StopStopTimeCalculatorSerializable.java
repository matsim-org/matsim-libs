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

public class StopStopTimeCalculatorSerializable implements VehicleArrivesAtFacilityEventHandler, PersonLeavesVehicleEventHandler, Serializable {

	private final Map<String, Map<String, StopStopTimeData>> stopStopTimes = new HashMap<String, Map<String, StopStopTimeData>>(5000);
	private final Map<String, Map<String, Double>> scheduledStopStopTimes = new HashMap<String, Map<String, Double>>(5000);
	private final Map<String, Tuple<String, Double>> inTransitVehicles = new HashMap<String, Tuple<String,Double>>(1000);
	private final Set<String> vehicleIds = new HashSet<String>();
	private double timeSlot;
	private boolean useVehicleIds = true;
	private static int scheduleCalls = 0;
	private static int totalCalls = 0;
	private static double stopTimesInflation = 0;

	//Constructors
	public StopStopTimeCalculatorSerializable(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public static void printCallStatisticsAndReset(){
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StopStopTimeCalculatorSerializable.class);
		logger.warn("stop times read from schedule vs total (S:T) = " + scheduleCalls + " : " + totalCalls);
		logger.warn("inflation of recorded times called vs their scheduled time:" +stopTimesInflation/(double)(totalCalls -scheduleCalls));
		scheduleCalls = 0;
		totalCalls = 0;
		stopTimesInflation=0;
	}
	public StopStopTimeCalculatorSerializable(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		Map<String, Map<String, Integer>> numObservations = new HashMap<String, Map<String, Integer>>();
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				for(int s=0; s<route.getStops().size()-1; s++) {
					Map<String, StopStopTimeData> map = stopStopTimes.get(route.getStops().get(s).getStopFacility().getId().toString());
					if(map==null) {
						map = new HashMap<String, StopStopTimeData>(2);
						stopStopTimes.put(route.getStops().get(s).getStopFacility().getId().toString(), map);
					}
					map.put(route.getStops().get(s+1).getStopFacility().getId().toString(), new StopStopTimeDataArray((int) (totalTime/timeSlot)+1));
					Map<String, Double> map2 = scheduledStopStopTimes.get(route.getStops().get(s).getStopFacility().getId().toString());
					Map<String, Integer> map3 = numObservations.get(route.getStops().get(s).getStopFacility().getId().toString());
					Double stopStopTime;
					Integer num;
					if(map2==null) {
						map2 = new HashMap<String, Double>(2);
						scheduledStopStopTimes.put(route.getStops().get(s).getStopFacility().getId().toString(), map2);
						map3 = new HashMap<String, Integer>(2);
						numObservations.put(route.getStops().get(s).getStopFacility().getId().toString(), map3);
						stopStopTime = 0.0;
						num = 0;
					}
					else {
						stopStopTime = map2.get(route.getStops().get(s+1).getStopFacility().getId().toString());
						num = map3.get(route.getStops().get(s+1).getStopFacility().getId().toString());
						if(stopStopTime==null) {
							stopStopTime = 0.0;
							num = 0;
						}
					}
					map2.put(route.getStops().get(s+1).getStopFacility().getId().toString(), stopStopTime+route.getStops().get(s+1).getArrivalOffset()-route.getStops().get(s).getDepartureOffset());
					map3.put(route.getStops().get(s+1).getStopFacility().getId().toString(), ++num);
				}
				for(Departure departure:route.getDepartures().values())
					vehicleIds.add(departure.getVehicleId().toString());
			}
		for(Entry<String, Map<String, Double>> entry:scheduledStopStopTimes.entrySet())
			for(Entry<String, Double> entry2:entry.getValue().entrySet())
				entry.getValue().put(entry2.getKey(), entry2.getValue()/numObservations.get(entry.getKey()).get(entry2.getKey()));
	}

	//Methods
	public StopStopTime getStopStopTimes() {
		return new StopStopTime() {
			@Override
			public double getStopStopTime(Id stopOId, Id stopDId, double time) {
				return StopStopTimeCalculatorSerializable.this.getStopStopTime(stopOId, stopDId, time);
			}
			@Override
			public double getStopStopTimeVariance(Id stopOId, Id stopDId, double time) {
				return StopStopTimeCalculatorSerializable.this.getStopStopTimeVariance(stopOId, stopDId, time);
			}
		};
	}
	private double getStopStopTime(Id stopOId, Id stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId.toString()).get(stopDId.toString());
		totalCalls++;
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0) {
			scheduleCalls++;
			return scheduledStopStopTimes.get(stopOId.toString()).get(stopDId.toString());
		}
		else {
			stopTimesInflation += stopStopTimeData.getStopStopTime((int) (time / timeSlot))/scheduledStopStopTimes.get(stopOId.toString()).get(stopDId.toString());
			return stopStopTimeData.getStopStopTime((int) (time / timeSlot));
		}
	}
	private double getStopStopTimeVariance(Id stopOId, Id stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId.toString()).get(stopDId.toString());
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0)
			return 0;
		else
			return stopStopTimeData.getStopStopTimeVariance((int) (time/timeSlot));
	}
	@Override
	public void reset(int iteration) {
		for(Map<String, StopStopTimeData> map:stopStopTimes.values())
			for(StopStopTimeData stopStopTimeData:map.values())
				stopStopTimeData.resetStopStopTimes();
		inTransitVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(!useVehicleIds || vehicleIds.contains(event.getVehicleId().toString())) {
			Tuple<String, Double> route = inTransitVehicles.remove(event.getVehicleId().toString());
			if(route!=null)
				try {
					stopStopTimes.get(route.getFirst()).get(event.getFacilityId().toString()).addStopStopTime((int) (route.getSecond()/timeSlot), event.getTime()-route.getSecond());
				} catch(Exception e) {
					//System.out.println("No: "+route.getFirst()+"-->"+event.getFacilityId());
				}
			inTransitVehicles.put(event.getVehicleId().toString(), new Tuple<String, Double>(event.getFacilityId().toString(), event.getTime()));
		}
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if((!useVehicleIds || vehicleIds.contains(event.getVehicleId().toString())) && event.getPersonId().toString().startsWith("pt_") && event.getPersonId().toString().contains(event.getVehicleId().toString()))
			inTransitVehicles.remove(event.getVehicleId().toString());
	}
	public void setUseVehicleIds(boolean useVehicleIds) {
		this.useVehicleIds = useVehicleIds;
	}
}
