package saleem.ptoptimisation.decisionvariables;
/**
 * A class for adding new transit lines to transit schedule
 * or removing existing transit lines from transit schedule.
 * 
 * @author Mohammad Saleem
 *
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmmodel.utils.CollectionUtil;

public class LineAdderRemover {
	/*This function deletes random lines from transit schedule, 
	 *where each transit line is selected with probability of factorline*100 percent for deletion
	 */
	public void deleteRandomLines(TransitSchedule schedule, Vehicles vehicles, double factorline){
		CollectionUtil<TransitLine> cutilforlines = new CollectionUtil<TransitLine>();
		ArrayList<TransitLine> lines = cutilforlines.toArrayList(schedule.getTransitLines().values().iterator());

		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		for(int i=0;i<lines.size();i++) {
			TransitLine tline = lines.get(i);
			Vehicle vehicle = vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().
					  iterator().next().getVehicleId());
			if(Math.random()<=factorline && vehicle.getType().getId().toString().equals("BUS")){//With factorline*100 percent probability; Only Busses
				deleteAllRoutes(tline, schedule, vehicles);
			}
		}
	}
	/*This function adds random transit lines into transit schedule, 
	 *where each transit line is selected with probability of factorline*100 percent
	 *For each selected line, a new independent line is added.
	 */
	public void addRandomLines(Scenario scenario, TransitSchedule schedule, Vehicles vehicles, double factorline){
		CollectionUtil<TransitLine> cutilforlines = new CollectionUtil<TransitLine>();
		ArrayList<TransitLine> lines = cutilforlines.toArrayList(schedule.getTransitLines().values().iterator());
		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		for(int i=0;i<lines.size();i++) {
			TransitLine tline = lines.get(i);
			Vehicle vehicle = vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().
					  iterator().next().getVehicleId());
			if(Math.random()<=factorline && vehicle.getType().getId().toString().equals("BUS") 
					){//With factorline*100 percent probability; Only Busses
				VehicleType type = vehicle.getType();
				addNewLine(tline, scenario, schedule, vehicles, type);
			}
		}
	}
	//This function creates a new Transit line
	public void addNewLine(TransitLine tline, Scenario scenario, TransitSchedule schedule, Vehicles vehicles, VehicleType type){
		RouteAdderRemover rad = new RouteAdderRemover();
		Map<Id<TransitStopFacility>, TransitRouteStop> stops = rad.getTransitStops(schedule);
		boolean empty = true;
		Iterator<TransitRoute> troutes = tline.getRoutes().values().iterator();
		TransitRoute troute = null;
		while(troutes.hasNext()){//If an empty line (already dropped line) is selected, Skip it
			troute = troutes.next();
			if((troute.getDepartures().values()
					.iterator().next().getDepartureTime()!=115200 || troute.getDepartures().size()>1)){
				empty = false;
				break;
			}
		}
		if(empty){
			return;
		}
		//Add two routes, forward and reverse
		TransitRoute forward = rad.createTransiteRoute(scenario, schedule, schedule.getFactory(), troute, stops);
		if(forward!=null){
			rad.addDeparturestoRoute(schedule.getFactory(), forward, vehicles, type);
			TransitLine line = schedule.getFactory().createTransitLine(Id.create("Line"+schedule.getTransitLines().size()+500, TransitLine.class));
			line.addRoute(forward);
			TransitRoute reverse = rad.createReverseTransiteRoute(scenario, schedule, schedule.getFactory(), forward);
			if(reverse!=null){
				rad.addDeparturestoRoute(schedule.getFactory(), reverse, vehicles, type);
				line.addRoute(reverse);
			}
			schedule.addTransitLine(line);
		}
	}
	//Delete all routes from an exiting transit line, hence effectively making it non-existent
	public void deleteAllRoutes(TransitLine tline, TransitSchedule schedule, Vehicles vehicles){//With 10 % chance of selecting a line, and 10% chance of removing each of its route.
		CollectionUtil<Departure> cutilfordepartures = new CollectionUtil<Departure>();
		TransitScheduleFactory tschedulefact = schedule.getFactory();
		Iterator<TransitRoute> routes = tline.getRoutes().values().iterator();
		while(routes.hasNext()) {
			TransitRoute troute = routes.next();
			if(!(troute.getDepartures().values()
					.iterator().next().getDepartureTime()==115200 && troute.getDepartures().size()==1)){
				ArrayList<Departure> departures = cutilfordepartures.toArrayList(troute.getDepartures().values().iterator());
				for(int k=0; k<departures.size();k++){
					Departure departure=departures.get(k);
					troute.removeDeparture(departure);
					if(troute.getDepartures().size()==0){
						double time = 115200;//Hypothetical departure after end time; delete all other departures; effectively a route that doesn't exist.
						Departure hypodeparture = tschedulefact.createDeparture(Id.create("DepHypo"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
						hypodeparture.setVehicleId(departure.getVehicleId());
						troute.addDeparture(hypodeparture);
					}
					else{
						vehicles.removeVehicle(departure.getVehicleId());//To remove the vehicles used in the route
					}
				}

			}
		}
	}
}

