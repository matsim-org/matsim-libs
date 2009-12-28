package playground.anhorni.choiceSetGeneration.helper;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;

public class ZHFacility  {
	
	private Id id = null;
	private Coord mappedPosition = null;	
	private Coord exactPosition = null;
	private Id linkId;
	private String name;
	
	private Id retailerID = null;
	private int size_descr;
	private double dHalt;
	private double hrs_week;
	
	private double accessibility02;
	private double accessibility10;
	private double accessibility20;
	

	public ZHFacility(Id id, String name, Coord mappedPosition, Coord exactPosition,
			Id linkId, Id retID, int size_descr, double halt, double hrs_week) {
		super();
		this.id = id;
		this.name = name;
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


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCenter(Coord mappedPosition) {
		this.mappedPosition = mappedPosition;
	}
	
	public double getCrowFlyDistance(Coord other) {
		return CoordUtils.calcDistance(this.mappedPosition, other);
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

	public double getAccessibility02() {
		return accessibility02;
	}

	public void setAccessibility02(double accessibility02) {
		this.accessibility02 = accessibility02;
	}

	public double getAccessibility10() {
		return accessibility10;
	}

	public void setAccessibility10(double accessibility10) {
		this.accessibility10 = accessibility10;
	}

	public double getAccessibility20() {
		return accessibility20;
	}

	public void setAccessibility20(double accessibility20) {
		this.accessibility20 = accessibility20;
	}
}
