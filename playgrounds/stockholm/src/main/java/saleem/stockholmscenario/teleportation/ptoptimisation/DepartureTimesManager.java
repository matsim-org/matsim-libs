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
	public ArrayList<Departure> sortDepartures(ArrayList<Departure> deps){
		Collections.sort(deps, new Comparator<Departure>() {

	        public int compare(Departure a, Departure b) {
	            return (int)(a.getDepartureTime() - b.getDepartureTime());
	        }
	    });
		return deps;
	}
	//Adjust times for added departure
	public double adjustTimeDepartureAdded(ArrayList<Departure> deps, int i){//Inserts a new departure based on the index i, either to the left or right in the middle 
		double time = deps.get(i).getDepartureTime();
		double adjustment = 3600;//Default value, applicable when only one departure in the list
		int size = deps.size();
		if(Math.random()<0.5){//With equal probability add or subtract the adjustment
			if(size > 1 && i+1 < size){
				adjustment = (deps.get(i+1).getDepartureTime()-time)/2;//Middle
			}else{
				System.out.println();//Just for debugging purpose
			}
			time = time + adjustment;
		}else{
			if(size > 1 && i-1 >= 0){
				adjustment = (time - deps.get(i-1).getDepartureTime())/2;//Middle between 
			}
			else{
				System.out.println();//Just for debugging purpose
			}
			time = time - adjustment;
			if(time<0){//Not before start of day
				time=0;
			}
		}
		return time;
	}
	//Adjust times for deleted departure
	public double adjustTimeDepartureRemoved(ArrayList<Departure> deps, int i){//Moves the next departure to the deleted one backward a little to adjust the gap
		double time = deps.get(i).getDepartureTime();
		double adjustment = (deps.get(i+1).getDepartureTime()-time)/2;//Middle
		time = deps.get(i+1).getDepartureTime() - adjustment;
		return time;
	}
}
