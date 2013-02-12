package playground.anhorni.csestimation;

import org.matsim.api.core.v01.Coord;



public class ShoppingTrip {
	private Coord startCoord;
	private Coord endCoord;
	private ShopLocation shop;
	private int startPLZ;
	private int endPLZ;
	private String mode;
	
	public ShopLocation getShop() {
		return shop;
	}
	public void setShop(ShopLocation shop) {
		this.shop = shop;
	}
	public Coord getStartCoord() {
		return startCoord;
	}
	public Coord getEndCoord() {
		return endCoord;
	}
	public int getStartPLZ() {
		return startPLZ;
	}
	public int getEndPLZ() {
		return endPLZ;
	}
	public void setStartCoord(Coord startCoord) {
		this.startCoord = startCoord;
	}
	public void setEndCoord(Coord endCoord) {
		this.endCoord = endCoord;
	}
	public void setStartPLZ(int startPLZ) {
		this.startPLZ = startPLZ;
	}
	public void setEndPLZ(int endPLZ) {
		this.endPLZ = endPLZ;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
}
