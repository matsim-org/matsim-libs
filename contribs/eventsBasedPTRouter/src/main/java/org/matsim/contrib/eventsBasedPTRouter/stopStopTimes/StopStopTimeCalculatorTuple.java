package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class StopStopTimeCalculatorTuple implements VehicleArrivesAtFacilityEventHandler, PersonLeavesVehicleEventHandler {

	private final Map<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, StopStopTimeData> stopStopTimes = new HashMap<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, StopStopTimeData>();
	private final Map<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, Double> scheduledStopStopTimes = new HashMap<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, Double>();
	private final Map<Id<Vehicle>, Tuple<Id<TransitStopFacility>, Double>> inTransitVehicles = new HashMap<Id<Vehicle>, Tuple<Id<TransitStopFacility>, Double>>(1000);
	private final Set<Id<Vehicle>> vehicleIds = new HashSet<Id<Vehicle>>();
	private final double timeSlot;
	private final int totalTime;

	//Constructors
	public StopStopTimeCalculatorTuple(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime().seconds()-config.qsim().getStartTime().seconds()));
	}
	public StopStopTimeCalculatorTuple(final TransitSchedule transitSchedule, final double timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		this.totalTime = totalTime;
		Map<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, Integer> numObservations = new HashMap<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, Integer>();
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				for(int s=0; s<route.getStops().size()-1; s++) {
					Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>> key = new Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>(route.getStops().get(s).getStopFacility().getId(),
							route.getStops().get(s+1).getStopFacility().getId());
					StopStopTimeData data = stopStopTimes.get(key);
					if(data==null)
						stopStopTimes.put(key, new StopStopTimeDataArray(TimeBinUtils.getTimeBinCount(totalTime, timeSlot)));
					Double sTime = scheduledStopStopTimes.get(key);
					Integer num = numObservations.get(key);
					if(sTime==null) {
						sTime = 0.0;
						scheduledStopStopTimes.put(key, sTime);
						num = 0;
						numObservations.put(key, num);
					}
					scheduledStopStopTimes.put(key, (num*sTime+route.getStops().get(s+1).getArrivalOffset().seconds()
							- route.getStops().get(s).getDepartureOffset().seconds())/++num);
					numObservations.put(key, num);
				}
				for(Departure departure:route.getDepartures().values())
					vehicleIds.add(departure.getVehicleId());
			}
		System.out.println(stopStopTimes.size());
	}
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new StopStopTimeCalculatorTuple(scenario.getTransitSchedule(), 900, 30*3600);
	}
	//Methods
	public StopStopTime getStopStopTimes() {
		return new StopStopTime() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;
			@Override
			public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
				return StopStopTimeCalculatorTuple.this.getStopStopTime(stopOId, stopDId, time);
			}
			@Override
			public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
				return StopStopTimeCalculatorTuple.this.getStopStopTimeVariance(stopOId, stopDId, time);
			}
		};
	}
	private double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
		Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>> key = new Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>(stopOId, stopDId);
		StopStopTimeData stopStopTimeData = stopStopTimes.get(key);
		int timeBinIndex = TimeBinUtils.getTimeBinIndex(time, timeSlot, TimeBinUtils.getTimeBinCount(totalTime, timeSlot));
		if(stopStopTimeData.getNumData(timeBinIndex)==0)
			return scheduledStopStopTimes.get(key);
		else
			return stopStopTimeData.getStopStopTime(timeBinIndex);
	}
	private double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
		Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>> key = new Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>(stopOId, stopDId);
		StopStopTimeData stopStopTimeData = stopStopTimes.get(key);
		int timeBinIndex = TimeBinUtils.getTimeBinIndex(time, timeSlot, TimeBinUtils.getTimeBinCount(totalTime, timeSlot));
		if(stopStopTimeData.getNumData(timeBinIndex)==0)
			return 0;
		else
			return stopStopTimeData.getStopStopTimeVariance(timeBinIndex);
	}
	@Override
	public void reset(int iteration) {
		for(StopStopTimeData stopStopTimeData:stopStopTimes.values())
				stopStopTimeData.resetStopStopTimes();
		inTransitVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(vehicleIds.contains(event.getVehicleId())) {
			Tuple<Id<TransitStopFacility>, Double> route = inTransitVehicles.remove(event.getVehicleId());
			if(route!=null)
				stopStopTimes.get(new Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>(route.getFirst(),
						event.getFacilityId())).addStopStopTime(
								TimeBinUtils.getTimeBinIndex(route.getSecond(), timeSlot,
										TimeBinUtils.getTimeBinCount(totalTime, timeSlot)), event.getTime()-route.getSecond());
			inTransitVehicles.put(event.getVehicleId(), new Tuple<Id<TransitStopFacility>, Double>(event.getFacilityId(), event.getTime()));
		}
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(vehicleIds.contains(event.getVehicleId()) && event.getPersonId().toString().startsWith("pt_"+event.getVehicleId()+"_"))
			inTransitVehicles.remove(event.getVehicleId());
	}
}
