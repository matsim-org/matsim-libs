package playground.sergioo.weeklySimulation.preprocess;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
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

import playground.sergioo.weeklySimulation.util.misc.Time;

public class WeeklyDepartures {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(args[1]);
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = factory.createTransitSchedule();
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		for(VehicleType vehicleType:scenario.getTransitVehicles().getVehicleTypes().values())
			vehicles.addVehicleType(vehicleType);
		for(TransitStopFacility stop: scenario.getTransitSchedule().getFacilities().values())
			transitSchedule.addStopFacility(stop);
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values()) {
			TransitLine newLine = factory.createTransitLine(line.getId());
			newLine.setName(line.getName());
			for(TransitRoute route:line.getRoutes().values()) {
				TransitRoute newRoute = factory.createTransitRoute(route.getId(), route.getRoute(), route.getStops(), route.getTransportMode());
				newRoute.setDescription(route.getDescription());
				Map<Id<Departure>, Departure> addDepartures = new HashMap<>();
				for(Entry<Id<Departure>, Departure> departureEntry:route.getDepartures().entrySet())
					if(departureEntry.getValue().getDepartureTime()<Time.MIDNIGHT)
						addDepartures.put(departureEntry.getKey(), departureEntry.getValue());
				for(int i=0; i<Time.Week.values().length; i++)
					for(Entry<Id<Departure>, Departure> departureEntry:addDepartures.entrySet()) {
						Departure departure = factory.createDeparture(Id.create(Time.Week.values()[i].getShortName()+departureEntry.getKey().toString(), Departure.class), i*Time.MIDNIGHT+departureEntry.getValue().getDepartureTime());
						Id<Vehicle> vehicleId = departureEntry.getValue().getVehicleId();
						Vehicle vehicle = new VehicleImpl(Id.create(vehicleId+Time.Week.values()[i].getShortName(),Vehicle.class), scenario.getTransitVehicles().getVehicles().get(vehicleId).getType());
						vehicles.addVehicle(vehicle);
						departure.setVehicleId(vehicle.getId());
						newRoute.addDeparture(departure);
					}
				newLine.addRoute(newRoute);
			}
			transitSchedule.addTransitLine(newLine);
		}
		new TransitScheduleWriter(transitSchedule).writeFile(args[2]);
		new VehicleWriterV1(vehicles).writeFile(args[3]);
	}

}
