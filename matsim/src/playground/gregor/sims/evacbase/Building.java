package playground.gregor.sims.evacbase;

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
	
	private int shelterSpace;
	
	
	public  Building(Id id, int popNight, int popDay, int floor, double space,
			int quakeProof, Geometry geo) {
		this.id = id;
		this.popNight = popNight;
		this.popDay = popDay;
		this.floor = floor;
		this.space = space;
		this.quakeProof = quakeProof;
		this.geo = geo;
		
		this.shelterSpace = (int)((((floor - 1.) * space)/2.)/10);
		
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

	public int getShelterSpace() {
		return this.shelterSpace;
	}
	
	public void setShelterSpace(int space) {
		this.shelterSpace = space;
	}
	
}
