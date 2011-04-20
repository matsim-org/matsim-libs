package playground.wrashid.lib.obj.geoGrid;

import org.matsim.api.core.v01.Coord;

public class GridDataItem {

	private final Coord coord;
	private final double weight;
	private final double value;

	public GridDataItem(double value, double weight, Coord coord){
		this.value = value;
		this.weight = weight;
		this.coord = coord;
	}

	public Coord getCoord() {
		return coord;
	}

	public double getWeight() {
		return weight;
	}

	public double getValue() {
		return value;
	}
	
}
