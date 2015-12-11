package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class VehicleAdder {
	Scenario scenario;
	TransitSchedule schedule;
	Vehicles vehicles;
	Map<Id<Vehicle>, Vehicle> vehicleinstances;
	public VehicleAdder(Scenario scenario){
		this.scenario=scenario;
		this.schedule=scenario.getTransitSchedule();
		this.vehicles = scenario.getTransitVehicles();
		this.vehicleinstances=this.vehicles.getVehicles();
	}
	//To add vehicles, one must actually add a departure and assign it the vehicle, and then ad vehicle to the fleet of vehicles.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addVehicles(double sample) {//sample represents percentage of vehicles to remove, ranges from 0 to 1
		CollectionUtil cutil = new CollectionUtil();
		ArrayList<Id<Vehicle>> vehids = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
		int numvehadd = (int)Math.ceil(sample * vehids.size());//How many vehicles to add 
		for(int i=0;i<numvehadd;i++) {
			Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(numvehadd * Math.random()), Vehicle.class);
			if(!(vehicles.getVehicles().containsKey(vehid))){//If same vehicle has not been added before
				VehicleType vtype = addVehicleToDeparture(vehid);
				//Add a vehicle of same type as returned departure type
				Vehicle veh = new VehicleImpl(vehid, vtype);
				vehicles.addVehicle(veh);
			}else{
				i--;//Try again
			}
		} 
		vehicles.getVehicleTypes().get(Id.create("BUS", VehicleType.class));
	}
	//Adds vehicle id to departure and returns type of departure, i.e. bus, train etc.
	public VehicleType addVehicleToDeparture(Id<Vehicle>  vehid){
		double time = Math.ceil(86400 * Math.random());//Random time with in 24 hours
		Departure departure = schedule.getFactory().createDeparture(Id.create("DepAdded"+(int)Math.floor(1000000 * Math.random()), Departure.class), time);
		departure.setVehicleId(vehid);
		return addDepartureToRoute(departure, vehid);
	}
	//Adds departure to troute and returns type of route, i.e. bus, train etc.
	//Assuming no empty lines exist
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public VehicleType addDepartureToRoute(Departure departure, Id<Vehicle>   vehid){
		CollectionUtil cutil = new CollectionUtil();
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		ArrayList<Id<TransitLine>> lineids = cutil.toArrayList(lines.keySet().iterator());
		Id<TransitLine> lineid = lineids.get((int)Math.floor(lineids.size() * Math.random()));
		TransitLine tline = lines.get(lineid);
		Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
		ArrayList<Id<TransitRoute>> routeids = cutil.toArrayList(routes.keySet().iterator());
		Id<TransitRoute> routeid = routeids.get((int)Math.floor(routeids.size() * Math.random()));
		TransitRoute troute = routes.get(routeid);
		troute.addDeparture(departure);
		VehicleType vtype = vehicleinstances.get(troute.getDepartures().get(troute.getDepartures().keySet().iterator().next()).getVehicleId()).getType();
		return vtype;
	}
}
