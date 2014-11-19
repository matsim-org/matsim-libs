package playground.sergioo.singapore2012.transitRouterVariable.old;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.singapore2012.transitRouterVariable.vehicleOccupancy.VehicleOccupancy;
import playground.sergioo.singapore2012.transitRouterVariable.vehicleOccupancy.VehicleOccupancyData;
import playground.sergioo.singapore2012.transitRouterVariable.vehicleOccupancy.VehicleOccupancyDataArray;

public class VehicleOccupancyCalculator implements VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler {

	private final Map<Tuple<Id<TransitLine>, Id<TransitRoute>>, Map<Id<TransitStopFacility>, VehicleOccupancyData>> vehicleOccupancy = new HashMap<Tuple<Id<TransitLine>, Id<TransitRoute>>, Map<Id<TransitStopFacility>, VehicleOccupancyData>>(1000);
	private double timeSlot;
	private Map<Id<Vehicle>, TransitVehicle> ptVehicles = new HashMap<Id<Vehicle>, TransitVehicle>();
	private Map<Id<Vehicle>, Tuple<Id<TransitLine>, Id<TransitRoute>>> linesRoutesOfVehicle = new HashMap<Id<Vehicle>, Tuple<Id<TransitLine>, Id<TransitRoute>>>();
	private TransitQSimEngine transitQSimEngine;

	//Constructors
	public VehicleOccupancyCalculator(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public VehicleOccupancyCalculator(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				Map<Id<TransitStopFacility>, VehicleOccupancyData> routeMap = new HashMap<Id<TransitStopFacility>, VehicleOccupancyData>(100);
				vehicleOccupancy.put(new Tuple<Id<TransitLine>, Id<TransitRoute>>(line.getId(), route.getId()), routeMap);
				for(int s=0; s<route.getStops().size()-1; s++) {
					routeMap.put(route.getStops().get(s).getStopFacility().getId(), new VehicleOccupancyDataArray((int) (totalTime/timeSlot)+1));
				}
			}
	}

	//Methods
	public VehicleOccupancy getVehicleOccupancy() {
		return new VehicleOccupancy() {
			@Override
			public double getVehicleOccupancy(Id<TransitStopFacility> stopOId, Id<TransitLine> lineId, Id<TransitRoute> routeId, double time) {
				return VehicleOccupancyCalculator.this.getVehicleOccupancy(stopOId, lineId, routeId, time);
			}
		};
	}
	private double getVehicleOccupancy(Id<TransitStopFacility> stopOId, Id<TransitLine> lineId, Id<TransitRoute> routeId, double time) {
		return vehicleOccupancy.get(new Tuple<Id<TransitLine>, Id<TransitRoute>>(lineId, routeId)).get(stopOId).getVehicleOccupancy((int) (time/timeSlot));
	}
	public void setPtEngine(TransitQSimEngine transitQSimEngine) {
		this.transitQSimEngine = transitQSimEngine;
	}
	@Override
	public void reset(int iteration) {
		for(Map<Id<TransitStopFacility>, VehicleOccupancyData> map:vehicleOccupancy.values())
			for(VehicleOccupancyData vehicleOcupancyData:map.values())
				vehicleOcupancyData.resetVehicleOccupancies();
		this.ptVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		TransitVehicle transitVehicle = ptVehicles.get(event.getVehicleId());
		if(transitVehicle!=null) {
			VehicleOccupancyData vehicleOccupancyData = vehicleOccupancy.get(linesRoutesOfVehicle.get(event.getVehicleId())).get(event.getFacilityId());
			if(vehicleOccupancyData!=null)
				vehicleOccupancyData.addVehicleOccupancy((int) (event.getTime()/timeSlot), transitVehicle.getPassengers().size()/(double)transitVehicle.getPassengerCapacity());
		}
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(ptVehicles.isEmpty())
			for(MobsimAgent mobsimAgent:transitQSimEngine.getPtDrivers())
				this.ptVehicles.put(((AbstractTransitDriverAgent)mobsimAgent).getVehicle().getId(), ((AbstractTransitDriverAgent)mobsimAgent).getVehicle());
		linesRoutesOfVehicle.put(event.getVehicleId(), new Tuple<Id<TransitLine>, Id<TransitRoute>>(event.getTransitLineId(), event.getTransitRouteId()));
	}

}
