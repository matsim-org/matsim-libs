package playground.anhorni.locationchoice.cs.helper;

import org.matsim.basic.v01.Id;
import org.matsim.utils.geometry.Coord;

public class ZHFacility  {
	
	private String shopID = null;
	private Id id = null;
	private Coord center = null;
	
	private Coord exactPosition = null;
	
	
	public ZHFacility(final Id id, Coord coord, String shopID, Coord exactPosition) {
		this.shopID = shopID;
		this.id = id;
		this.center = coord;
		this.exactPosition = exactPosition;
	}

	public String getShopID() {
		return shopID;
	}
	public void setShopID(String shopID) {
		this.shopID = shopID;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Coord getCenter() {
		return center;
	}

	public void setCenter(Coord center) {
		this.center = center;
	}
	
	public double getCrowFlyDistance(Coord other) {
		return this.center.calcDistance(other);
	}

	public Coord getExactPosition() {
		return exactPosition;
	}

	public void setExactPosition(Coord exactPosition) {
		this.exactPosition = exactPosition;
	}
}
