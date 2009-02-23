package playground.anhorni.locationchoice.cs.helper;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;

public class ZHFacility  {
	
	private Id id = null;
	private Coord mappedPosition = null;	
	private Coord exactPosition = null;
	private Id linkId;
	
	private Id retailerID = null;
	private int size_descr;
	private double dHalt;
	private double hrs_week;
	
	
	
	public ZHFacility(Id id, Coord mappedPosition, Coord exactPosition,
			Id linkId, Id retID, int size_descr, double halt, double hrs_week) {
		super();
		this.id = id;
		this.mappedPosition = mappedPosition;
		this.exactPosition = exactPosition;
		this.linkId = linkId;
		this.retailerID = retID;
		this.size_descr = size_descr;
		this.dHalt = halt;
		this.hrs_week = hrs_week;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}


	public void setCenter(Coord mappedPosition) {
		this.mappedPosition = mappedPosition;
	}
	
	public double getCrowFlyDistance(Coord other) {
		return this.mappedPosition.calcDistance(other);
	}

	public Coord getExactPosition() {
		return exactPosition;
	}

	public void setExactPosition(Coord exactPosition) {
		this.exactPosition = exactPosition;
	}

	public Id getLinkId() {
		return linkId;
	}

	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}

	public Coord getMappedPosition() {
		return mappedPosition;
	}

	public void setMappedPosition(Coord mappedPosition) {
		this.mappedPosition = mappedPosition;
	}

	public Id getRetailerID() {
		return retailerID;
	}

	public void setRetailerID(Id retailerID) {
		this.retailerID = retailerID;
	}

	public int getSize_descr() {
		return size_descr;
	}

	public void setSize_descr(int size_descr) {
		this.size_descr = size_descr;
	}

	public double getDHalt() {
		return dHalt;
	}

	public void setDHalt(double halt) {
		dHalt = halt;
	}

	public double getHrs_week() {
		return hrs_week;
	}

	public void setHrs_week(double hrs_week) {
		this.hrs_week = hrs_week;
	}
}
