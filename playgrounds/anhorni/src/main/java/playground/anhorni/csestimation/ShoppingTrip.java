package playground.anhorni.csestimation;

import org.matsim.api.core.v01.Coord;



public class ShoppingTrip {
	private Coord startCoord;
	private Coord endCoord;
	private ShopLocation shop;
	private int startPLZ;
	private int endPLZ;
	
	public Coord getStart() {
		return startCoord;
	}
	public Coord getEnd() {
		return endCoord;
	}
	public ShopLocation getShop() {
		return shop;
	}
	public void setStart(Coord start) {
		this.startCoord = start;
	}
	public void setEnd(Coord end) {
		this.endCoord = end;
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
}
