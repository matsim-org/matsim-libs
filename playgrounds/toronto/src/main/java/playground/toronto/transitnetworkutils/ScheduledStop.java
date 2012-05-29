package playground.toronto.transitnetworkutils;

import java.util.ArrayList;
import org.matsim.core.utils.collections.Tuple;

/**
 * A simple data structure to represent generalized scheduled transit stops. The ArrayList 'times'
 * stores arrival/departure Tuples. The times are stored as strings which can later be parsed into
 * actual hours/minutes.
 * 
 * @author pkucirek
 *
 */

public class ScheduledStop {
	
	private String id;
	private ArrayList<Tuple<String, String>> times;
	
	// TODO Add handling of waypoints ('stops' that do not allow access/egress and have no scheduled times)

	public ScheduledStop(String id){
		this.id = id;
		this.times = new ArrayList<Tuple<String,String>>();
	}
	
	public String getId(){
		return this.id;
	}
	
	public ArrayList<Tuple<String, String>> getTimes(){
		return this.times;
	}
	public void AddTime(Tuple<String,String> arrivalDepartureTime){
		this.times.add(arrivalDepartureTime);
	}
}
