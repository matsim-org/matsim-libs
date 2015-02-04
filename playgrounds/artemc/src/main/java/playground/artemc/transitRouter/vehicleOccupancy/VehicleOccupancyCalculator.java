package playground.artemc.transitRouter.vehicleOccupancy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;
import java.util.Map;

public class VehicleOccupancyCalculator implements VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler {

	private final Map<Tuple<Id, Id>, Map<Id, VehicleOccupancyData>> vehicleOccupancy = new HashMap<Tuple<Id, Id>, Map<Id, VehicleOccupancyData>>(1000);
	private double timeSlot;
	private Map<Id, Integer> ptVehicles = new HashMap<Id, Integer>();
	private Map<Id, Integer> capacities = new HashMap<Id, Integer>();
	private Map<Id, Tuple<Id, Id>> linesRoutesOfVehicle = new HashMap<Id, Tuple<Id, Id>>();
	private final Vehicles vehicles;
	
	//Constructors
	public VehicleOccupancyCalculator(final TransitSchedule transitSchedule, final Vehicles vehicles, final Config config) {
		this(transitSchedule, vehicles, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public VehicleOccupancyCalculator(final TransitSchedule transitSchedule, final Vehicles vehicles, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				Map<Id, VehicleOccupancyData> routeMap = new HashMap<Id, VehicleOccupancyData>(100);
				vehicleOccupancy.put(new Tuple<Id, Id>(line.getId(), route.getId()), routeMap);
				for(int s=0; s<route.getStops().size()-1; s++) {
					routeMap.put(route.getStops().get(s).getStopFacility().getId(), new VehicleOccupancyDataArray((int) (totalTime/timeSlot)+1));
				}
			}
		this.vehicles = vehicles;
	}

	//Methods
	public VehicleOccupancy getVehicleOccupancy() {
		return new VehicleOccupancy() {
			@Override
			public double getVehicleOccupancy(Id stopOId, Id lineId, Id routeId, double time) {
				return VehicleOccupancyCalculator.this.getVehicleOccupancy(stopOId, lineId, routeId, time);
			}
		};
	}
	private double getVehicleOccupancy(Id stopOId, Id lineId, Id routeId, double time) {
		return vehicleOccupancy.get(new Tuple<Id, Id>(lineId, routeId)).get(stopOId).getVehicleOccupancy((int) (time/timeSlot));
	}
	@Override
	public void reset(int iteration) {
		for(Map<Id, VehicleOccupancyData> map:vehicleOccupancy.values())
			for(VehicleOccupancyData vehicleOcupancyData:map.values())
				vehicleOcupancyData.resetVehicleOccupancies();
		this.ptVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Integer num = ptVehicles.get(event.getVehicleId());
		if(num!=null) {
			VehicleOccupancyData vehicleOccupancyData = vehicleOccupancy.get(linesRoutesOfVehicle.get(event.getVehicleId())).get(event.getFacilityId());
			if(vehicleOccupancyData!=null)
				vehicleOccupancyData.addVehicleOccupancy((int) (event.getTime()/timeSlot), num/(double)capacities.get(event.getVehicleId()));
		}
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Integer num = this.ptVehicles.get(event.getVehicleId());
		if(num!=null)
			this.ptVehicles.put(event.getVehicleId(), num-1);
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Integer num = this.ptVehicles.get(event.getVehicleId());
		if(num!=null)
			this.ptVehicles.put(event.getVehicleId(), num+1);
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.ptVehicles.put(event.getVehicleId(), 0);
		VehicleCapacity vehicleCapacity = vehicles.getVehicles().get(event.getVehicleId()).getType().getCapacity();
		this.capacities.put(event.getVehicleId(), vehicleCapacity.getSeats()+vehicleCapacity.getStandingRoom());
		linesRoutesOfVehicle.put(event.getVehicleId(), new Tuple<Id, Id>(event.getTransitLineId(), event.getTransitRouteId()));
	}
	

}
