package saleem.ptoptimisation.decisionvariables;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmmodel.utils.CollectionUtil;
/**
 * A class to remove departures from routes within existing lines.
 * 
 * @author Mohammad Saleem
 *
 */
public class VehicleRemover {
	TransitSchedule schedule;
	Vehicles vehicles; 
	public VehicleRemover(Vehicles vehicles, TransitSchedule schedule){
		this.vehicles=vehicles;
		this.schedule=schedule;
	}
	/*  Removes departures from a certain line. All the route in the line are traversed, all departures for each route are traversed, 
	 *  and for each departure, it is removed with a fraction*100 percent chance. 
	 *  Times of departures are adjusted accordingly, to keep gaps between departures reasonable.
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
					if(troute.getDepartures().size()==1){//If there is only one departure in the route, and the departure is selected for deletion
						double time = 115200;//Hypothetical departure after end time; effectively the route doesn't exist.
						Departure hypodeparture = schedule.getFactory().createDeparture(
									Id.create("DepHypo"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
						hypodeparture.setVehicleId(departure.getVehicleId());
						//Making sure at least one hypothetical departure after simulation end time exists, which doesnt affect the results,
						//as atleast one departure is neccessary by design
						troute.addDeparture(hypodeparture);
						troute.removeDeparture(departure);
						
					}else{
						troute.removeDeparture(departure);
						vehicles.removeVehicle(departure.getVehicleId());

					}
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
		}
	}
}
