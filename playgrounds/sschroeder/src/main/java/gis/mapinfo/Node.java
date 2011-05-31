package gis.mapinfo;

import org.matsim.api.core.v01.Coord;

public class Node {
	private Coord coord;

	public Node(Coord coord) {
		super();
		this.coord = coord;
	}

	public Coord getCoord() {
		return coord;
	}
	
	
}