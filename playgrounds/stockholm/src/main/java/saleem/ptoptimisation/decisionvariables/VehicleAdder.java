package saleem.ptoptimisation.decisionvariables;

import java.util.ArrayList;
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

import saleem.stockholmmodel.utils.CollectionUtil;

/**
 * A class for adding departures to routes within existing lines.
 * 
 * @author Mohammad Saleem
 *
 */
public class VehicleAdder {
	private TransitSchedule schedule;
	private Vehicles vehicles;
	private Map<Id<Vehicle>, Vehicle> vehicleinstances;
	public VehicleAdder(Vehicles vehicles, TransitSchedule schedule){
		this.vehicles=vehicles; 
		this.schedule=schedule;
		this.vehicleinstances=vehicles.getVehicles();
	}
	/*  Removes departures from a certain line. All the route in the line are traversed, all departures for each route are traversed, 
	 *  and for each departure, it is selected with a fraction*100 percent chance. 
	 *  For each selected departure, a new independent departure is added.
	 *  Times of departures are adjusted accordingly, to keep gaps between departures reasonable.
	 *  Corresponding vehicles are also added.
	 *  */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addDeparturesToLine(TransitLine tline, double fraction){
		CollectionUtil cutil = new CollectionUtil();
		DepartureTimesManager dtm = new DepartureTimesManager();
		Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
		Iterator<Id<TransitRoute>> routeids = routes.keySet().iterator();
		while(routeids.hasNext()){
			TransitRoute troute = routes.get(routeids.next());
			ArrayList<Departure> departures = cutil.toArrayList(troute.getDepartures().values().iterator());
			ArrayList<Id<Departure>> depadded = new ArrayList<Id<Departure>>();
			departures = dtm.sortDepartures(departures);//Sort as per time
			int size = departures.size();
			Map<Id<Vehicle>, Vehicle> vehinstances = vehicles.getVehicles();
			for(int i=1;i<size;i++) {//For each departure, there is a "fraction" percent chance to add a new departure
				if(Math.random()<=fraction){
					Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(1000000 * Math.random()), Vehicle.class);
					if(!(vehicles.getVehicles().containsKey(vehid))){//If same vehicle has not been added before
						double time = departures.get(i).getDepartureTime();
						time = dtm.adjustTimeDepartureAdded(departures, i);//Adjust time for the departure to be added, so that gaps between departures are reasonable
						Departure departure = schedule.getFactory().createDeparture(Id.create("DepAdded"+(int)Math.floor(1000000 * Math.random()), Departure.class), time);
						departure.setVehicleId(vehid);
						if(!depadded.contains(departure.getId())){
							depadded.add(departure.getId());
							if(!troute.getDepartures().containsKey(departure.getId())){//If not already added a departure with the same id
								troute.addDeparture(departure);
								Vehicle vehicle = vehicleinstances.get(troute.getDepartures().get(troute.getDepartures().keySet().iterator().next()).getVehicleId());
								//To remove the chances of null pointer exception
								Iterator<Id<Departure>> iter = troute.getDepartures().keySet().iterator();
								while(vehicle==null){
									vehicle = vehicleinstances.get(troute.getDepartures().get(iter.next()).getVehicleId());
								}
								VehicleType vtype = vehicle.getType();
								Vehicle veh = new VehicleImpl(vehid, vtype);
								vehicles.addVehicle(veh);

							}
						}
						

					}
				}
			}
			departures = dtm.sortDepartures(departures);//Sort as per time
		}
	}
}
