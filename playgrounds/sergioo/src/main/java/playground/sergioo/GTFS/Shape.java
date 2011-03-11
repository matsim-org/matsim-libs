package playground.sergioo.GTFS;

import java.util.ArrayList;
import java.util.List;

public class Shape {
	
	//Attributes
	private List<Location> points;
	
	//Methods
	/**
	 * Constructs 
	 */
	public Shape() {
		points = new ArrayList<Location>();
	}
	/**
	 * @return the location
	 */
	public Location getPoints(int pos) {
		return points.get(pos);
	}
	/**
	 * Adds a new point
	 * @param point
	 */
	public void addPoint(Location point) {
		points.add(point);
	}
	
}
