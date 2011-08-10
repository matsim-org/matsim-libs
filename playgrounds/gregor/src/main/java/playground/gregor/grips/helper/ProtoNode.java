package playground.gregor.grips.helper;


import com.vividsolutions.jts.geom.Coordinate;

public class ProtoNode {


	private String id;
	private Coordinate coord;

	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Coordinate getCoord() {
		return this.coord;
	}
	public void setCoord(Coordinate coord) {
		this.coord = coord;
	}


}
