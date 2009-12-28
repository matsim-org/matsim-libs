package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;

public class Hectare {
	
	private Coord coords;
	private List<Integer> shops = new Vector<Integer>();
	
	public Hectare(Coord coords) {
		super();
		this.coords = coords;
	}
	
	public Coord getCoords() {
		return this.coords;
	}

	public void addShop(int shop) {
		this.shops.add(shop);
	}
	
	public List<Integer> getShops() {
		return this.shops;
	}
}
