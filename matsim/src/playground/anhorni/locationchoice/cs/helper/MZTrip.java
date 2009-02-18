package playground.anhorni.locationchoice.cs.helper;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.utils.geometry.Coord;

public class MZTrip {
	
	private Id id = null;
	
	// Nelson: E_X, E_Y
	private Coord coord = null;
	
	// F58
	private double startTime = 0.0;
	
	// F514
	private double endTime = 0.0;
	
	private String wmittel;
	private String ausmittel;

	
	public MZTrip(Id id, Coord coord, double startTime, double endTime) {
		super();
		this.id = id;
		this.coord = coord;
		this.startTime = startTime;
		this.endTime = endTime;
	}


	public Id getId() {
		return id;
	}


	public void setId(Id id) {
		this.id = id;
	}


	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}


	public String getWmittel() {
		return wmittel;
	}


	public void setWmittel(String wmittel) {
		this.wmittel = wmittel;
	}


	public String getAusmittel() {
		return ausmittel;
	}


	public void setAusmittel(String ausmittel) {
		this.ausmittel = ausmittel;
	}
}
