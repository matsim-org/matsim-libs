/**
 * 
 */
package playground.dziemke.cemdapMatsimCadyts;

import org.matsim.api.core.v01.Coord;

public class Zone {

	private int id;
	private Coord coord;

	public Zone(int id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public Coord getCoord() {
		return this.coord;
	}
	
	public void setCoord(Coord coord) {
		this.coord = coord;
	}

}