package playground.sergioo.GTFS;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;

public class Shape {
	
	//Attributes
	private List<Coord> points;
	
	//Methods
	/**
	 * Constructs 
	 */
	public Shape() {
		points = new ArrayList<Coord>();
	}
	/**
	 * @return the points
	 */
	public List<Coord> getPoints() {
		return points;
	}
	/**
	 * Adds a new point
	 * @param point
	 */
	public void addPoint(Coord point) {
		points.add(point);
	}
	
}
