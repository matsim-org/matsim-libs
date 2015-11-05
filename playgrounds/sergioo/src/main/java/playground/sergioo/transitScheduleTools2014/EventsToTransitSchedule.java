package playground.sergioo.transitScheduleTools2014;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class EventsToTransitSchedule implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final TransitSchedule existingSchedule;
	private TransitSchedule newSchedule;
	private final TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
	private final Map<Id<Vehicle>, Tuple<Id<Departure>, Tuple<Id<TransitLine>, Id<TransitRoute>>>> startDrivers = new HashMap<Id<Vehicle>, Tuple<Id<Departure>, Tuple<Id<TransitLine>, Id<TransitRoute>>>>();
	private final List<Tuple<Id<Vehicle>, VehicleType>> vehicles = new ArrayList<Tuple<Id<Vehicle>, VehicleType>>();
	private final Set<Id<Vehicle>> newVehicles = new HashSet<Id<Vehicle>>();
	
	public EventsToTransitSchedule(TransitSchedule transitSchedule, Vehicles vehicles) {
		existingSchedule = transitSchedule;
		newSchedule = factory.createTransitSchedule();
		for(TransitStopFacility stop:existingSchedule.getFacilities().values()) {
			TransitStopFacility newStop = factory.createTransitStopFacility(stop.getId(), stop.getCoord(), stop.getIsBlockingLane());
			newStop.setLinkId(stop.getLinkId());
			newStop.setName(stop.getName());
			newStop.setStopPostAreaId(stop.getStopPostAreaId());
			newSchedule.addStopFacility(newStop);
		}
		for(Vehicle vehicle:vehicles.getVehicles().values())
			this.vehicles.add(new Tuple<Id<Vehicle>, VehicleType>(vehicle.getId(), vehicle.getType()));
	}
	public TransitSchedule getNewSchedule() {
		return newSchedule;
	}
	public void setNewSchedule(TransitSchedule newSchedule) {
		this.newSchedule = newSchedule;
	}

	@Override
	public void reset(int iteration) {
		
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		TransitLine line = newSchedule.getTransitLines().get(event.getTransitLineId());
		if(line==null) {
			line = factory.createTransitLine(event.getTransitLineId());
			newSchedule.addTransitLine(line);
		}
		TransitRoute route = line.getRoutes().get(event.getTransitRouteId());
		if(route==null) {
			TransitRoute existingRoute = existingSchedule.getTransitLines().get(event.getTransitLineId()).getRoutes().get(event.getTransitRouteId());
			route = factory.createTransitRoute(event.getTransitRouteId(), existingRoute.getRoute(), existingRoute.getStops(), existingRoute.getTransportMode());
			line.addRoute(route);
		}
		startDrivers.put(event.getVehicleId(), new Tuple<Id<Departure>, Tuple<Id<TransitLine>, Id<TransitRoute>>>(event.getDepartureId(), new Tuple<Id<TransitLine>, Id<TransitRoute>>(event.getTransitLineId(), event.getTransitRouteId())));
	}
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Tuple<Id<Departure>, Tuple<Id<TransitLine>, Id<TransitRoute>>> startStop = startDrivers.remove(event.getVehicleId());
		if(startStop != null) {
			TransitRoute route = newSchedule.getTransitLines().get(startStop.getSecond().getFirst()).getRoutes().get(startStop.getSecond().getSecond());
			double time = event.getTime();
			if(!route.getStops().get(0).getStopFacility().getId().equals(event.getFacilityId())) {
				TransitRoute existingRoute = existingSchedule.getTransitLines().get(startStop.getSecond().getFirst()).getRoutes().get(startStop.getSecond().getSecond());
				time-=existingRoute.getStop(existingSchedule.getFacilities().get(event.getFacilityId())).getDepartureOffset();
			}
			Departure departure = factory.createDeparture(Id.create(startStop.getFirst().toString()+","+time,Departure.class), time);
			departure.setVehicleId(event.getVehicleId());
			newVehicles.add(event.getVehicleId());
			route.addDeparture(departure);
		}
	}
	public void printVehicles(String fileName) {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		for(Tuple<Id<Vehicle>, VehicleType> veh:this.vehicles) {
			if(vehicles.getVehicleTypes().get(veh.getSecond().getId())==null)
				vehicles.addVehicleType(veh.getSecond());
		}
		for(Id<Vehicle> id:newVehicles)
			vehicles.addVehicle(new VehicleImpl(id, this.vehicles.remove(0).getSecond()));
		new VehicleWriterV1(vehicles).writeFile(fileName);
	}
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new VehicleReaderV1(((MutableScenario)scenario).getTransitVehicles()).readFile(args[1]);
		EventsManager events = new EventsManagerImpl();
		EventsToTransitSchedule eventsToTransitSchedule = new EventsToTransitSchedule(scenario.getTransitSchedule(), ((MutableScenario)scenario).getTransitVehicles());
		events.addHandler(eventsToTransitSchedule);
		new MatsimEventsReader(events).readFile(args[2]);
		System.out.println(eventsToTransitSchedule.startDrivers.size());
		TransitSchedule newTransitSchedule = eventsToTransitSchedule.getNewSchedule();
		new TransitScheduleWriter(newTransitSchedule).writeFile(args[3]);
		eventsToTransitSchedule.printVehicles(args[5]);
		PrintWriter printWriter = new PrintWriter(args[4]);
		Set<String> noLines = new HashSet<String>();
		Set<String> noRoutes = new HashSet<String>();
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values()) {
			TransitLine newLine = newTransitSchedule.getTransitLines().get(line.getId());
			if(newLine==null)
				noLines.add(line.getId().toString());
			else
				for(TransitRoute route:line.getRoutes().values()) {
					TransitRoute newRoute = newLine.getRoutes().get(route.getId());
					if(newRoute==null)
						noRoutes.add(line.getId().toString()+"("+route.getId().toString()+")");
					else
						printWriter.println(line.getId().toString()+"("+route.getId().toString()+"):"+route.getDepartures().size()+","+newRoute.getDepartures().size());
				}
		}
		printWriter.println();
		for(String noLine:noLines)
			printWriter.println(noLine);
		printWriter.println();
		for(String noRoute:noRoutes)
			printWriter.println(noRoute);
		printWriter.close();
	}
	

}
