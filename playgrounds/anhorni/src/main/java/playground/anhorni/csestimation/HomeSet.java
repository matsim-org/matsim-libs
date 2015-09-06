package playground.anhorni.csestimation;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

public class HomeSet {
	
	private EstimationPerson person;
	private TreeMap<Double, ShopLocation> shops = new TreeMap<Double, ShopLocation>();
	private final static Logger log = Logger.getLogger(HomeSet.class);

	
	public void create(EstimationPerson person, QuadTree<Location> shopQuadTree) {
		this.person = person;
		
		int storeCnt = 0;
		double r = 0.0;
		
		Collection<Location> stores = new Vector<Location>();
		while (storeCnt < 10) {
			r += 0.1;
			stores = shopQuadTree.get(person.getHomeLocation().getCoord().getX(),
					person.getHomeLocation().getCoord().getY(), 
					r);
			storeCnt = stores.size();		
		}
		if (stores.size() > 10) {
			log.error("home set too large! " + stores.size() + " stores");
			System.exit(-1);
		}
		else {
			Coord hCoord = person.getHomeLocation().getCoord();
			for (Location loc : stores) {
				Coord sCoord = loc.getCoord();
				double dist = CoordUtils.calcDistance(hCoord, sCoord);
				this.shops.put(dist, (ShopLocation)loc);
			}
		}
		this.print();
	}
	
	public double getMaxDistanceAwareness() {
		Coord hCoord = person.getHomeLocation().getCoord();
		double maxDistAware = 0.0;
		for (ShopLocation shop:this.shops.values()) {	
			if (person.getPersonLocations().getAwareStoresInQuerySet().contains(shop.getId())) {
				Coord sCoord = shop.getCoord();
				double dist = CoordUtils.calcDistance(hCoord, sCoord);
				if (dist > maxDistAware) {
					maxDistAware = dist;
				}
			}
		}
		return maxDistAware;
	}
	
	public int getAwarenessCnt() {
		int awareCnt = 0;
		for (ShopLocation shop:this.shops.values()) {			
			if (person.getPersonLocations().getNullAwareOrnullVisitedStoresInQuerySet().contains(shop.getId())) {
				log.info("Person " + person.getId() + " contains NULL awareness stores");
				return -99;
			}
			else if (person.getPersonLocations().getAwareStoresInQuerySet().contains(shop.getId())) {
				awareCnt++;
			}
		}
		return awareCnt;
	}
	public int getFrequentlyVisitedCnt() {
		int frequentlyCnt = 0;
		for (ShopLocation shop:this.shops.values()) {			
			if (person.getPersonLocations().getNullAwareOrnullVisitedStoresInQuerySet().contains(shop.getId())) {
				log.info("Person " + person.getId() + " contains NULL frequently stores");
				return -99;
			}
			else if (person.getPersonLocations().getVisitedStoresInQuerySet().contains(shop.getId())) {
				frequentlyCnt++;
			}
		}
		return frequentlyCnt;
	}	
	public TreeMap<Double, ShopLocation> getShops() {
		return shops;
	}
	public void setShops(TreeMap<Double, ShopLocation> shops) {
		this.shops = shops;
	}
	private void print() {
		DecimalFormat formatter = new DecimalFormat("0.00");
		String str = "person " + this.person.getId().toString() + " " + person.getHomeLocation().getCoord().toString();
		for (ShopLocation shop:this.shops.values()) {
			str += " " + shop.getId().toString() + shop.getCoord().toString() + " d2h: " +
		formatter.format(CoordUtils.calcDistance(((Coord) person.getHomeLocation().getCoord()), shop.getCoord()));
		}
		log.info(str);
	}
}
