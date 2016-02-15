package playground.sergioo.transitScheduleTools2014;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	private static final TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
	private static final double TOTAL_PEAK_TIME_M = 3.5*3600;
	private static final double TOTAL_PEAK_TIME_A = 3.5*3600;
	private static final double TOTAL_OFF_TIME = 5*3600;
	private static final double TOTAL_TIME = 30*3600;
	private final Map<Id<Vehicle>, Tuple<Id<Departure>, Tuple<Id<TransitLine>, Id<TransitRoute>>>> startDrivers = new HashMap<Id<Vehicle>, Tuple<Id<Departure>, Tuple<Id<TransitLine>, Id<TransitRoute>>>>();
	private final List<Tuple<Id<Vehicle>, VehicleType>> vehicles = new ArrayList<Tuple<Id<Vehicle>, VehicleType>>();
	private final Set<Id<Vehicle>> newVehicles = new HashSet<Id<Vehicle>>();
	private Vehicles vehiclesM;
	
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
		vehiclesM = VehicleUtils.createVehiclesContainer();
		for(Tuple<Id<Vehicle>, VehicleType> veh:this.vehicles)
			if(vehiclesM.getVehicleTypes().get(veh.getSecond().getId())==null)
				vehiclesM.addVehicleType(veh.getSecond());
		for(Id<Vehicle> id:newVehicles)
			vehiclesM.addVehicle(new VehicleImpl(id, this.vehicles.remove(0).getSecond()));
		new VehicleWriterV1(vehiclesM).writeFile(fileName);
	}
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new VehicleReaderV1(((MutableScenario)scenario).getTransitVehicles()).readFile(args[1]);
		EventsManager events = new EventsManagerImpl();
		EventsToTransitSchedule eventsToTransitSchedule = new EventsToTransitSchedule(scenario.getTransitSchedule(), ((MutableScenario)scenario).getTransitVehicles());
		events.addHandler(eventsToTransitSchedule);
		new MatsimEventsReader(events).readFile(args[2]);
		System.out.println(eventsToTransitSchedule.startDrivers.size());
		TransitSchedule newTransitSchedule = eventsToTransitSchedule.newSchedule;
		new TransitScheduleWriter(newTransitSchedule).writeFile(args[3]);
		eventsToTransitSchedule.printVehicles(args[4]);
		Vehicles newVehicles = scenario.getTransitVehicles();
		TransitSchedule avTransitSchedule = factory.createTransitSchedule();
		Vehicles avVehicles = VehicleUtils.createVehiclesContainer();
		for(VehicleType veh:newVehicles.getVehicleTypes().values())
			if(avVehicles.getVehicleTypes().get(veh.getId())==null)
				avVehicles.addVehicleType(veh);
		Map<Id<TransitRoute>, Map<VehicleType, Integer>> allNumTypes = new HashMap<>();
		for(TransitLine transitLine:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute transitRoute:transitLine.getRoutes().values()) {
				Map<VehicleType, Integer> numTypes = new HashMap<>();
				for(Departure departure:transitRoute.getDepartures().values()) {
					VehicleType vehicleType = newVehicles.getVehicles().get(departure.getVehicleId()).getType();
					Integer numType = numTypes.get(vehicleType);
					if(numType==null)
						numType = 0;
					numTypes.put(vehicleType, numType+1);
				}
				allNumTypes.put(transitRoute.getId(), numTypes);
			}
		for(TransitStopFacility transitStop:newTransitSchedule.getFacilities().values())
			avTransitSchedule.addStopFacility(transitStop);
		for(TransitLine transitLine:newTransitSchedule.getTransitLines().values()) {
			TransitLine newTransitLine = factory.createTransitLine(transitLine.getId());
			avTransitSchedule.addTransitLine(newTransitLine);
			newTransitLine.setName(transitLine.getName());
			for(TransitRoute transitRoute:transitLine.getRoutes().values()) {
				TransitRoute newTransitRoute = factory.createTransitRoute(transitRoute.getId(), transitRoute.getRoute(), transitRoute.getStops(), transitRoute.getTransportMode());
				newTransitLine.addRoute(newTransitRoute);
				int numDepsPeakM = 0, numDepsPeakA = 0, numDepsOffPeak = 0, numDepsOff = 0; 
				Map<VehicleType, Integer> numTypes = allNumTypes.get(transitRoute.getId());
				for(Departure departure:transitRoute.getDepartures().values()) {
					if(isPeakM(departure.getDepartureTime()))
						numDepsPeakM++;
					else if(isPeakA(departure.getDepartureTime()))
						numDepsPeakA++;
					else if(isOff(departure.getDepartureTime()))
						numDepsOff++;
					else
						numDepsOffPeak++;
				}
				double diffPeakM = TOTAL_PEAK_TIME_M/Math.max(1,numDepsPeakM), diffPeakA = TOTAL_PEAK_TIME_A/Math.max(1,numDepsPeakA), diffOff = TOTAL_OFF_TIME/Math.max(1,numDepsOff), diffOffPeak = (24*3600-TOTAL_PEAK_TIME_M-TOTAL_PEAK_TIME_A-TOTAL_OFF_TIME)/Math.max(1,numDepsOffPeak), diff = diffOffPeak;
				int k = 1;
				for(double time=0; time<TOTAL_TIME; time+=diff) {
					String id = ""+k++;
					Departure newDeparture = factory.createDeparture(Id.create(id, Departure.class), time);
					Vehicle vehicle = new VehicleImpl(Id.create(transitRoute.getId().toString()+"_"+id, Vehicle.class), getVehicleType(numTypes));
					newDeparture.setVehicleId(vehicle.getId());
					newTransitRoute.addDeparture(newDeparture);
					avVehicles.addVehicle(vehicle);
					if(isPeakM(time))
						diff = diffPeakM;
					else if(isPeakA(time))
						diff = diffPeakA;
					else if(isOff(time))
						diff = diffOff;
					else
						diff = diffOffPeak;
				}
			}
		}
		for(TransitLine transitLine:scenario.getTransitSchedule().getTransitLines().values()) {
			if(avTransitSchedule.getTransitLines().get(transitLine.getId())==null) {
				avTransitSchedule.addTransitLine(transitLine);
				for(TransitRoute transitRoute:transitLine.getRoutes().values()) {
					Map<VehicleType, Integer> numTypes = allNumTypes.get(transitRoute.getId());
					int k=1;
					for(Departure departure:transitRoute.getDepartures().values()) {
						Vehicle vehicle = new VehicleImpl(Id.create(transitRoute.getId().toString()+"_"+k++, Vehicle.class), getVehicleType(numTypes));
						departure.setVehicleId(vehicle.getId());
						avVehicles.addVehicle(vehicle);
					}
					Set<Departure> newDeps = new HashSet<>();
					for(Departure departure:transitRoute.getDepartures().values())
						if(departure.getDepartureTime()<(TOTAL_TIME-24*3600)) {
							String id = ""+k++;
							Departure newDeparture = factory.createDeparture(Id.create(id, Departure.class), departure.getDepartureTime()+24*3600);
							Vehicle vehicle = new VehicleImpl(Id.create(transitRoute.getId().toString()+"_"+id, Vehicle.class), getVehicleType(numTypes));
							newDeparture.setVehicleId(vehicle.getId());
							avVehicles.addVehicle(vehicle);
							newDeps.add(newDeparture);
						}
					for(Departure departure:newDeps)
						transitRoute.addDeparture(departure);
				}
			}
			else
				for(TransitRoute transitRoute:transitLine.getRoutes().values())
					if(avTransitSchedule.getTransitLines().get(transitLine.getId()).getRoutes().get(transitRoute.getId())==null) {
						Map<VehicleType, Integer> numTypes = allNumTypes.get(transitRoute.getId());
						avTransitSchedule.getTransitLines().get(transitLine.getId()).addRoute(transitRoute);
						int k=1;
						for(Departure departure:transitRoute.getDepartures().values()) {
							Vehicle vehicle = new VehicleImpl(Id.create(transitRoute.getId().toString()+"_"+k++, Vehicle.class), getVehicleType(numTypes));
							departure.setVehicleId(vehicle.getId());
							avVehicles.addVehicle(vehicle);
						}
						Set<Departure> newDeps = new HashSet<>();
						for(Departure departure:transitRoute.getDepartures().values())
							if(departure.getDepartureTime()<(TOTAL_TIME-24*3600)) {
								String id = ""+k++;
								Departure newDeparture = factory.createDeparture(Id.create(id, Departure.class), departure.getDepartureTime()+24*3600);
								Vehicle vehicle = new VehicleImpl(Id.create(transitRoute.getId().toString()+"_"+id, Vehicle.class), getVehicleType(numTypes));
								newDeparture.setVehicleId(vehicle.getId());
								avVehicles.addVehicle(vehicle);
								newDeps.add(newDeparture);
							}
						for(Departure departure:newDeps)
							transitRoute.addDeparture(departure);
					}
		}
		new TransitScheduleWriter(avTransitSchedule).writeFile(args[5]);
		new VehicleWriterV1(avVehicles).writeFile(args[6]);
		PrintWriter printWriter = new PrintWriter(args[7]);
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
	private static VehicleType getVehicleType(Map<VehicleType, Integer> numTypes) {
		int i=0;
		for(Integer n:numTypes.values())
			i+=n;
		double r = Math.random()*i, tot = 0;
		for(Entry<VehicleType, Integer> e:numTypes.entrySet()) {
			tot += e.getValue();
			if(r<tot)
				return e.getKey();
		}
		return null;
	}
	private static boolean isPeakM(double time) {
		time = time%(24*3600);
		return time>=6*3600 && time<9.5*3600;
	}
	private static boolean isPeakA(double time) {
		time = time%(24*3600);
		return time>=18*3600 && time<21.5*3600;
	}
	private static boolean isOff(double time) {
		time = time%(24*3600);
		return time<5*3600;
	}

}
