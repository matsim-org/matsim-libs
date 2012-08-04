package playground.toronto.transitnetworkutils;

import java.util.ArrayList;

import org.matsim.core.utils.collections.Tuple;

/**
 * A data structure to represent a stop-name-agnostic transit route object. 
 * 
 * @author pkucirek
 */

public class ScheduledRoute {

	public String id;
	public String routename;
	public String branch;
	public String direction;
	public String mode;
	
	public ArrayList<ScheduledStop> stops;
	
	public ScheduledRoute(){
		this.stops = new ArrayList<ScheduledStop>();
	}
	public ScheduledRoute(String id, String direction){
		this.id = id;
		this.direction = direction;
		this.stops = new ArrayList<ScheduledStop>();
		this.mode = "bus";
	}

	public ScheduledRoute(String routename, String direction, String branch, ArrayList<String> stops){
		this.routename = routename;
		this.direction = direction;
		this.branch = branch;
		this.stops = new ArrayList<ScheduledStop>();
		this.mode = "bus";
		this.id = this.routename + "," + this.direction + "," + this.branch;
		
		for (String s:stops) this.stops.add(new ScheduledStop(s));
	}
	
	
	
	
	public int getStopLength(){
		return this.stops.size();
	}
	public ScheduledStop getStop(int index){
		return this.stops.get(index);
	}
	public void AddStop(ScheduledStop stop){
		this.stops.add(stop);
	}
	
	public ArrayList<String> getStopSequence(){
		
		ArrayList<String> sequence = new ArrayList<String>();
		
		for (ScheduledStop stop:this.stops){
			sequence.add(stop.getId());
		}
		
		return sequence;
	}
	
	public int CountStops(){
		
		return this.stops.size();
		
	}

	/**
	 * A function which exports a list of stops for the route, which are paired with their average offset time
	 * from the first stop in the sequence.
	 * 
	 * @return
	 */
	public ArrayList<Tuple<String,String>> exportStopOffsetsForMatsim(){
		
		ArrayList<Tuple<String,String>> result =  new ArrayList<Tuple<String,String>>();
		
		//TODO
		
		return result;
		
	}
	
	public ArrayList<String> exportDepartureTimesForMatsim(){
		
		ArrayList<String> result = new ArrayList<String>();
		
		ScheduledStop firstStop = this.stops.get(0);
		
		
		
		return result;
		
	}
}
