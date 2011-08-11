package playground.gregor.grips.helper;

import org.geotools.geometry.Geometry;

public class Building {

	private int numOfPersons;
	private Geometry geometry;

	public int getNumOfPersons() {
		return this.numOfPersons;
	}
	public void setNumOfPersons(int numOfPersons) {
		this.numOfPersons = numOfPersons;
	}
	public Geometry getGeometry() {
		return this.geometry;
	}
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

}
