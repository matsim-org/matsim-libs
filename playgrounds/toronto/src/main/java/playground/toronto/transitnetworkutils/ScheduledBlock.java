package playground.toronto.transitnetworkutils;

import java.util.ArrayList;

import org.matsim.core.utils.collections.Tuple;

public class ScheduledBlock {
	
	private String id;
	public ArrayList<String> stopNames;
	private String associatedRoute;
	private String associatedDirection;
	public ArrayList<Tuple<String,String>> fromto;
	public ArrayList<ArrayList<String>> lines;
	
	public ScheduledBlock(){
		
	}
	public ScheduledBlock(String id, String route, String direction){
		this.id = id;
		this.associatedRoute = route;
		this.associatedDirection = direction;
		this.lines = new ArrayList<ArrayList<String>>();
		this.fromto = new ArrayList<Tuple<String,String>>();
	}
	public ScheduledBlock(String id, String route, String direction, ArrayList<String> stops){
		this.stopNames = stops;
		this.id = id;
		this.associatedRoute = route;
		this.associatedDirection = direction;
		this.lines = new ArrayList<ArrayList<String>>();
		this.fromto = new ArrayList<Tuple<String,String>>();
	}
	
	public String getID(){
		return this.id;
	}
	public void setID(String id){
		this.id = id;
	}
	public String getRoute(){
		return this.associatedRoute;
	}
	public String getDir(){
		return this.associatedDirection;
	}

	public int compareTo(ScheduledBlock other)
	{
		return this.id.compareTo(other.getID());
	}
}
