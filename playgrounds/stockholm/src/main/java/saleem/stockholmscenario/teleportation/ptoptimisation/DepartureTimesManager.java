package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import saleem.stockholmscenario.teleportation.ptoptimisation.utils.OptimisationUtils;
import saleem.stockholmscenario.utils.CollectionUtil;

public class DepartureTimesManager {
	public ArrayList<Double> getDepartureTimes(Iterator<Departure> deps){//Return sorted times of the departures
		OptimisationUtils outils = new OptimisationUtils();
		ArrayList<Double> times = new ArrayList<Double>();
		while(deps.hasNext()){
			times.add(deps.next().getDepartureTime());
		}
		return outils.sortTimes(times);
		 
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void updateDepartures(TransitSchedule schedule, TransitRoute troute, ArrayList<Double> updatedtimes ){//Update departures times
		Map<Id<Departure>, Departure> departures = troute.getDepartures();
		CollectionUtil cutil = new CollectionUtil(); 
		Iterator<Departure> depiter = sortDepartures(cutil.toArrayList(troute.getDepartures().values().iterator())).iterator();//To avoid concurrent changing
		Iterator<Double> timesiter = updatedtimes.iterator();
		while(timesiter.hasNext()){
			
			Departure departure = departures.get(depiter.next().getId());
			troute.removeDeparture(departure);
			Departure newdeparture = schedule.getFactory().createDeparture(departure.getId(), timesiter.next());
			newdeparture.setVehicleId(departure.getVehicleId());
			troute.addDeparture(newdeparture);
		}
		
	}
	public ArrayList<Departure> sortDepartures(ArrayList<Departure> deps){
		Collections.sort(deps, new Comparator<Departure>() {

	        public int compare(Departure a, Departure b) {
	            return (int)(a.getDepartureTime() - b.getDepartureTime());
	        }
	    });
		return deps;
	}
}
