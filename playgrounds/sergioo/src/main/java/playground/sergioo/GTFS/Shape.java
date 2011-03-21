package playground.sergioo.GTFS;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;

public class Shape {
	
	//Attributes
	private SortedMap<Integer,Coord> points;
	
	//Methods
	/**
	 * Constructs 
	 */
	public Shape() {
		points = new TreeMap<Integer,Coord>();
	}
	/**
	 * @return the points
	 */
	public SortedMap<Integer,Coord> getPoints() {
		return points;
	}
	/**
	 * Adds a new point
	 * @param point
	 */
	public void addPoint(Coord point, int pos) {
		points.put(pos,point);
	}
	
}
