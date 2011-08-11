package playground.gregor.grips.helper;

import com.vividsolutions.jts.geom.Geometry;


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
	public void setGeometry(Geometry geo) {
		this.geometry = geo;
	}

}
