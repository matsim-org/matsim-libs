package playground.ciarif.models.subtours;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;

public class Subtour {
		
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private int id;
	private ArrayList<Integer> nodes;
	private int purpose; // 0 := work; 1 := edu; 2 := shop 3:=leisure
	private int mode;
	private Coord start_coord;
	private int prev_subtour;
	private double distance; 
	private int start_udeg;
	private String starting_time;
	private int prev_mode;
	

	public Subtour() {
		super();
		this.nodes=new ArrayList<Integer>();
	}

	
	//////////////////////////////////////////////////////////////////////
	// Setters methods
	//////////////////////////////////////////////////////////////////////
	
	public void setStarting_time(String starting_time) {
		this.starting_time = starting_time;
	}

	public void setPrev_mode(int prev_mode) {
		this.prev_mode = prev_mode;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public void setStart_coord(Coord start_coord) {
		this.start_coord = start_coord;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setNodes(ArrayList<Integer> nodes) {
		this.nodes = nodes;
	}
	
	public void setPurpose(int purpose) {
		this.purpose = purpose;
	}
	
	public void setNode (Integer node) {
		this.nodes.add(node);
	}
	
	public void setPrev_subtour(int prev_subtour) {
		this.prev_subtour = prev_subtour;
	}


	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public void setStart_udeg(int start_udeg) {
		this.start_udeg = start_udeg;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Getters methods
	//////////////////////////////////////////////////////////////////////
	
	public String getStarting_time() {
		return starting_time;
	}


	public int getPrev_mode() {
		return prev_mode;
	}
	
	public double getDistance() {
		return distance;
	}

	public Coord getStart_coord() {
		return start_coord;
	}
	
	public int getId() {
		return id;
	}
	
	public ArrayList<Integer> getNodes() {
		return nodes;
	}
	
	public int getPurpose() {
		return purpose;
	}
	
	public int getPrev_subtour() {
		return prev_subtour;
	}

	public int getMode() {
		return mode;
	}
	
	public int getStart_udeg() {
		return start_udeg;
	}

}
