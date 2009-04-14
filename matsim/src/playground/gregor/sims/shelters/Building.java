package playground.gregor.sims.shelters;

import org.matsim.api.basic.v01.Id;

import com.vividsolutions.jts.geom.Geometry;

public class Building {

	private final Id id;
	private final int popNight;
	private final int popDay;
	private final int floor;
	private final double space;
	private final int quakeProof;
	private final Geometry geo;
	
	
	
	public  Building(Id id, int popNight, int popDay, int floor, double space,
			int quakeProof, Geometry geo) {
		this.id = id;
		this.popNight = popNight;
		this.popDay = popDay;
		this.floor = floor;
		this.space = space;
		this.quakeProof = quakeProof;
		this.geo = geo;
		
	}

	public Id getId() {
		return this.id;
	}



	public int getPopNight() {
		return this.popNight;
	}



	public int getPopDay() {
		return this.popDay;
	}



	public int getFloor() {
		return this.floor;
	}



	public double getSpace() {
		return this.space;
	}



	public boolean isQuakeProof() {
		return this.quakeProof == 1;
	}



	public Geometry getGeo() {
		return this.geo;
	}
	
}
