package playground.ciarif.models.subtours;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;

public class Subtour {
		
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private int id;
	private ArrayList<Integer> nodes;
	private int purpose; // 0 := work; 1 := edu; 2 := shop 3:=leisure
	private int mode;
	private CoordI start_coord;
	private int prev_subtour;
	private double distance; 
	

	public Subtour() {
		super();
		this.nodes=new ArrayList<Integer>();
	}

	
	//////////////////////////////////////////////////////////////////////
	// Setters methods
	//////////////////////////////////////////////////////////////////////
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public void setStart_coord(CoordI start_coord) {
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
	
	//////////////////////////////////////////////////////////////////////
	// Getters methods
	//////////////////////////////////////////////////////////////////////
	
	public double getDistance() {
		return distance;
	}

	public CoordI getStart_coord() {
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

			
}
