package playground.anhorni.csestimation;

import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;

public class HomeSet {
	
	private EstimationPerson person;
	private TreeMap<Id, ShopLocation> shops = new TreeMap<Id, ShopLocation>();
	private final static Logger log = Logger.getLogger(HomeSet.class);
		
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
			for (Location loc : stores) {
				this.shops.put(loc.getId(), (ShopLocation)loc);
			}
		}
	}
	
	public double getAwarenessShare() {
		double awareCnt = 0.0;
		for (ShopLocation shop:this.shops.values()) {			
			if (person.getPersonLocations().getNullAwareOrnullVisitedStoresInQuerySet().contains(shop.getId())) {
				log.info("Person " + person.getId() + " contains NULL awareness stores");
				return -99.0;
			}
			else if (person.getPersonLocations().getAwareStoresInQuerySet().contains(shop.getId())) {
				awareCnt++;
			}
		}
		return awareCnt / this.shops.size();
	}
	public double getFrequentlyVisitedShare() {
		double frequentlyCnt = 0.0;
		for (ShopLocation shop:this.shops.values()) {			
			if (person.getPersonLocations().getNullAwareOrnullVisitedStoresInQuerySet().contains(shop.getId())) {
				log.info("Person " + person.getId() + " contains NULL frequently stores");
				return -99.0;
			}
			else if (person.getPersonLocations().getVisitedStoresInQuerySet().contains(shop.getId())) {
				frequentlyCnt++;
			}
		}
		return frequentlyCnt / this.shops.size();
	}	
	public TreeMap<Id, ShopLocation> getShops() {
		return shops;
	}
	public void setShops(TreeMap<Id, ShopLocation> shops) {
		this.shops = shops;
	}
}
