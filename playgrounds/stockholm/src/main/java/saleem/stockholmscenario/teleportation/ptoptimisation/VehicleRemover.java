package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class VehicleRemover {
	TransitSchedule schedule;
	Vehicles vehicles; 
	public VehicleRemover(Vehicles vehicles, TransitSchedule schedule){
		this.vehicles=vehicles;
		this.schedule=schedule;
	}
	//Remove sample percent vehicles and the corresponding departures from transit schedule
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public void removeVehicles(double sample) {//sample represents percentage of vehicles to remove, ranges from 0 to 1
//		CollectionUtil cutil = new CollectionUtil();
//		ArrayList<Id<Vehicle>> removed = new ArrayList<Id<Vehicle>>();
//		ArrayList<Id<Vehicle>> vehids = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
//		int totalveh = vehids.size();
//		int numvehrem = (int)Math.ceil(sample * vehids.size()); 
//		for(int i=0;i<numvehrem;i++) {
//			int index = (int)Math.floor(totalveh * Math.random());//Randomly remove vehilces
//			Id<Vehicle> vehid = vehids.get(index);
//			if (!removed.contains(vehid)){//Try to remove if not removed already
//				removed.add(vehid);
//				this.vehicles.removeVehicle(vehids.get(index));
//			}else{
//				i--;
//			}
//			
//		} 
//				int a =0;
//
//	}
	/*  Removes departures from a certain line. All the route in the line are traversed, all departures for each route are traversed, 
	 *  and for each departure, it is removed with a fraction percent (10%) chance. Times are adjusted accordingly.
	 *  Corresponding vehicles are also removed.
	 *  */
	 @SuppressWarnings({ "rawtypes", "unchecked" })
	public void removeDeparturesFromLine(TransitLine tline, double fraction){
		CollectionUtil cutil = new CollectionUtil();
		DepartureTimesManager dtm = new DepartureTimesManager();
		ArrayList<TransitRoute> routes = cutil.toArrayList(tline.getRoutes().values().iterator());
		int routesize=routes.size();
		for(int index=0;index<routesize;index++) {
			TransitRoute troute = routes.get(index);
			ArrayList<Departure> departures = cutil.toArrayList(troute.getDepartures().values().iterator());
			departures = dtm.sortDepartures(departures);//Sort as per time
			int depsize = departures.size();
			for(int i=0;i<depsize;i++) {
				if(Math.random()<=fraction){
					Departure departure = departures.get(i);
					if(troute.getDepartures().size()==1){
						double time = 115200;//Hypothetical departure after end time;delete all other departures; effectively a route that doesn't exist.
						Departure hypodeparture = schedule.getFactory().createDeparture(Id.create("DepHypo"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
						hypodeparture.setVehicleId(departure.getVehicleId());
						//Making sure at least one hypothetical departure after simulation end time exists, which doesnt affect the results,
						//as atleast one departure is neccessary by design
						troute.addDeparture(hypodeparture);
						troute.removeDeparture(departure);
						
					}else{
						troute.removeDeparture(departure);
						vehicles.removeVehicle(departure.getVehicleId());

					}
					System.out.println("Deleted Departure: " + departure.getId() + " " + troute.getId() + " " + departure.getVehicleId());
					if(i+1<depsize){
						double time = dtm.adjustTimeDepartureRemoved(departures, i);
						Departure nextdeparture = departures.get(i+1);//Remove next departure and add again with adjusted time
						troute.removeDeparture(nextdeparture);
						Departure newdeparture = schedule.getFactory().createDeparture(nextdeparture.getId(), time);
						newdeparture.setVehicleId(nextdeparture.getVehicleId());
						troute.addDeparture(newdeparture);
					}
				}
			}
			//Remove empty routes and lines
			if(troute.getDepartures().size()==0){
				tline.removeRoute(troute);
			}
		}
		if(tline.getRoutes().size()==0){
			schedule.removeTransitLine(tline);
		}
	}
	/*
	 * This function removes all departures whose vehicle have been deleted.
	 */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public void removeDeletedVehicleDepartures(){
//		int count = 0;
//		ArrayList<TransitLine> linestodelete = new ArrayList<TransitLine>();
//		CollectionUtil cutil = new CollectionUtil();
//		Vehicles vehicles = scenario.getTransitVehicles();
//		ArrayList<Id<Vehicle>> vehids = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
//		TransitSchedule schedule = scenario.getTransitSchedule();
//		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
//		Iterator<Id<TransitLine>> lineids = lines.keySet().iterator();
//		while(lineids.hasNext()){
//			ArrayList<TransitRoute> routestodelete = new ArrayList<TransitRoute>();
//			Id<TransitLine> lineid = lineids.next();
//			TransitLine tline = lines.get(lineid);
//			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
//			Iterator<Id<TransitRoute>> routeids = routes.keySet().iterator();
//			while(routeids.hasNext()){
//				ArrayList<Departure> departurestodelete = new ArrayList<Departure>();
//				Id<TransitRoute> routeid = routeids.next();
//				TransitRoute troute = routes.get(routeid);
//				Map<Id<Departure>, Departure> departures = troute.getDepartures();
//				Iterator<Id<Departure>> departureids = departures.keySet().iterator();
//				while(departureids.hasNext()){
//					Id<Departure> depid = departureids.next();
//					Departure departure = departures.get(depid);
//					Id<Vehicle> vehid = departure.getVehicleId();
//					if(!vehids.contains(vehid)){//If the vehicle does not exist
//						departurestodelete.add(departure);//To be deleted
//					}
//				}
//				Iterator<Departure> deptodltitr = departurestodelete.iterator();
//				while(deptodltitr.hasNext()){
//					Departure departure = deptodltitr.next();
//					count++;
//					System.out.println(count + ": Departure Removed: " + departure.getId() + " : " + departure.getDepartureTime());
//					troute.removeDeparture(departure);
//				}
//			}
//		}
//	}
}
