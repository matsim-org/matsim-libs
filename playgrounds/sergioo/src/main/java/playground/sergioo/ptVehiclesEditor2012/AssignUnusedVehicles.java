package playground.sergioo.ptVehiclesEditor2012;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;

public class AssignUnusedVehicles {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(args[1]);
		Set<Id<Vehicle>> vehicleIds = new HashSet<Id<Vehicle>>();
		for(Vehicle vehicle:((ScenarioImpl)scenario).getTransitVehicles().getVehicles().values())
			vehicleIds.add(vehicle.getId());
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(Departure departure:route.getDepartures().values())
					vehicleIds.remove(departure.getVehicleId());
		for(TransitRoute route:scenario.getTransitSchedule().getTransitLines().get(Id.create(args[2], TransitLine.class)).getRoutes().values())
			for(Departure departure:route.getDepartures().values()) {
				Id<Vehicle> id = vehicleIds.iterator().next();
				departure.setVehicleId(id);
				vehicleIds.remove(id);
			}
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(args[3]);
	}

}
