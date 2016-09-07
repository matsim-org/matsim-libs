package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class PTCapacityAdjuster {
	public void adjustCapacity(Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		Iterator<TransitLine> lines = schedule.getTransitLines().values().iterator();
		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		while(lines.hasNext()) {
			TransitLine tline = lines.next();
			Vehicle vehicle = vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().
					  iterator().next().getVehicleId());
			if(Math.random()<=factorline && !vehicle.getType().getId().toString().equals("FERRY")){//With factorline*100 percent probability; Exclude Ferries
				Iterator<TransitRoute> routes = tline.getRoutes().values().iterator();
				while(routes.hasNext()) {
					TransitRoute route = routes.next();
					if(Math.random()<=factorroute){//With factorroute*100 percent probability
						Iterator<Departure> departures = route.getDepartures().values().iterator();
						while(departures.hasNext()) {
							Departure departure = departures.next();
							if(inPeakHour(departure.getDepartureTime())){
								Vehicle veh = vehicleinstances.get(departure.getVehicleId());
								if(!veh.getType().getId().toString().startsWith("L")){//If capacity not already increased
									vehicles.removeVehicle(veh.getId());
									VehicleType vtype = vehicles.getVehicleTypes().get(Id.create("L"+veh.getType().getId().toString(), VehicleType.class));
									veh = new VehicleImpl(veh.getId(), vtype);
									vehicles.addVehicle(veh);

								}
								
								
							}else{
								Vehicle veh = vehicleinstances.get(departure.getVehicleId());
								if(!veh.getType().getId().toString().startsWith("S")){
									vehicles.removeVehicle(veh.getId());
									VehicleType vtype = vehicles.getVehicleTypes().get(Id.create("S"+veh.getType().getId().toString(), VehicleType.class));
									veh = new VehicleImpl(veh.getId(), vtype);
									vehicles.addVehicle(veh);

								}
							}
						}
						
					}
				}
			}
		}
	}
	public boolean inPeakHour(double time){
		return (time>=25200 && time<=32400) || (time>=59400 && time<=66600);
	}
}