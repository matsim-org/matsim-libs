package saleem.ptoptimisation.decisionvariables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.matsim.pt.transitSchedule.api.Departure;
/**
 * A helper class for sorting departure times and to ensure reasonable gaps among departures.
 * 
 * @author Mohammad Saleem
 *
 */
public class DepartureTimesManager {
	//Sort departures per time
	public ArrayList<Departure> sortDepartures(ArrayList<Departure> deps){
		Collections.sort(deps, new Comparator<Departure>() {

	        public int compare(Departure a, Departure b) {
	            return (int)(a.getDepartureTime() - b.getDepartureTime());
	        }
	    });
		return deps;
	}
	/*Adjust times for added departure, ensuring reasonable (not too small or too big) gaps between departures
	 * Inserts a new departure based on the index i, either to the left or right in the middle
	 */
	public double adjustTimeDepartureAdded(ArrayList<Departure> deps, int i){
		double time = deps.get(i).getDepartureTime();
		double adjustment = 3600;//Default value, applicable when only one departure in the list
		int size = deps.size();
		if(Math.random()<0.5){//With equal probability add or subtract the adjustment
			if(size > 1 && i+1 < size){
				adjustment = (deps.get(i+1).getDepartureTime()-time)/2;//Middle
			}
			time = time + adjustment;
		}else{
			if(size > 1 && i-1 >= 0){
				adjustment = (time - deps.get(i-1).getDepartureTime())/2;//Middle between 
			}
			time = time - adjustment;
			if(time<0){//Not before start of day
				time=0;
			}
		}
		return time;
	}
	//Adjust times for deleted departure. Moves the next departure to the deleted one backward a little to adjust the gap
	public double adjustTimeDepartureRemoved(ArrayList<Departure> deps, int i){
		double time = deps.get(i).getDepartureTime();
		double adjustment = (deps.get(i+1).getDepartureTime()-time)/2;//Middle
		time = deps.get(i+1).getDepartureTime() - adjustment;
		return time;
	}
}
