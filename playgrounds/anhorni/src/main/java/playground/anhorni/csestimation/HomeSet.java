package playground.anhorni.csestimation;

import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

public class HomeSet {
	
	private EstimationPerson person;
	private TreeMap<Double, ShopLocation> shops = new TreeMap<Double, ShopLocation>();
	private final static Logger log = Logger.getLogger(HomeSet.class);
	private WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();		
	
	public void create(EstimationPerson person, QuadTree<Location> shopQuadTree) {
		this.person = person;
		
		int storeCnt = 0;
		double r = 0.0;
		
		Collection<Location> stores = new Vector<Location>();
		while (storeCnt < 10) {
			r += 0.0000001;
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
			Coord hCoord = this.trafo.transform(person.getHomeLocation().getCoord());
			for (Location loc : stores) {
				Coord sCoord = this.trafo.transform(loc.getCoord());
				double dist = CoordUtils.calcDistance(hCoord, sCoord);
				this.shops.put(dist, (ShopLocation)loc);
			}
		}
	}
	
	public double getMaxDistanceAwareness() {
		Coord hCoord = this.trafo.transform(person.getHomeLocation().getCoord());
		double maxDistAware = 0.0;
		for (ShopLocation shop:this.shops.values()) {	
			if (person.getPersonLocations().getAwareStoresInQuerySet().contains(shop.getId())) {
				Coord sCoord = this.trafo.transform(shop.getCoord());
				double dist = CoordUtils.calcDistance(hCoord, sCoord);
				maxDistAware = dist;
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
}
