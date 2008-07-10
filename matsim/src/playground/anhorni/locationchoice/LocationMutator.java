package playground.anhorni.locationchoice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.collections.QuadTree;


public abstract class LocationMutator extends PersonAlgorithm implements PlanAlgorithmI {

	// parameters of the specific selector ----------------------
	protected NetworkLayer network = null;
	protected Controler controler = null;
	private final Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	protected final TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	protected final TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();
	protected QuadTree<Facility> shopFacQuadTree = null;
	protected QuadTree<Facility> leisFacQuadTree = null;
	
	protected QuadTree<Facility> zhShopFacQuadTree = null;
	protected QuadTree<Facility> zhLeisureFacQuadTree = null;
	
	protected ArrayList<Facility> zhShopFacilities = null;
	protected ArrayList<Facility> zhLeisureFacilities = null;
	// ----------------------------------------------------------

	public LocationMutator(final NetworkLayer network, final Controler controler) {
		this.init(network, controler);
	}
	
	public LocationMutator(final NetworkLayer network) {
		this.init(network, null);
	}

	private void init(final NetworkLayer network, Controler controler) {
		this.network = network;
		this.controler = controler;

		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_lt100sqm"));
		
		// do not use shop_other for the moment
		// this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_sports"));
		
		this.shopFacQuadTree=this.builFacQuadTree(this.shop_facilities);
		this.leisFacQuadTree=this.builFacQuadTree(this.leisure_facilities);
		
		this.zhShopFacilities = (ArrayList<Facility>)this.shopFacQuadTree.get(683508.50, 246832.91, 30000);
		this.zhLeisureFacilities = (ArrayList<Facility>)this.leisFacQuadTree.get(683508.50, 246832.91, 30000);
		
		TreeMap<Id, Facility> treemapShop = new TreeMap<Id, Facility>();
		Iterator<Facility> sfac_it = this.zhShopFacilities.iterator();
		while (sfac_it.hasNext()) {
			Facility f = sfac_it.next();
			treemapShop.put(f.getId(), f);
		}
		
		TreeMap<Id, Facility> treemapLeisure = new TreeMap<Id, Facility>();
		Iterator<Facility> lfac_it = this.zhLeisureFacilities.iterator();
		while (lfac_it.hasNext()) {
			Facility f = lfac_it.next();
			treemapLeisure.put(f.getId(), f);
		}	
		this.zhShopFacQuadTree = this.builFacQuadTree(treemapShop);
		this.zhLeisureFacQuadTree = this.builFacQuadTree(treemapLeisure);
	}

	public void handlePlan(final Plan plan){
	}


	public void run(final Person person) {
		final int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			final Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {
		handlePlan(plan);
	}
	
	private QuadTree<Facility> builFacQuadTree(TreeMap<Id,Facility> facilities_of_type) {
		Gbl.startMeasurement();
		System.out.println("      building facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (final Facility f : facilities_of_type.values()) {
			if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
			if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
			if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
			if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Facility> quadtree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (final Facility f : facilities_of_type.values()) {
			quadtree.put(f.getCenter().getX(),f.getCenter().getY(),f);
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
		return quadtree;
	}

	public Controler getControler() {
		return controler;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}
}
