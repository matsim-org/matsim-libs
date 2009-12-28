package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Counter;

public class LocationModifier extends Modifier {

	private TreeMap<Id,ActivityFacilityImpl> shop_facilities=new TreeMap<Id,ActivityFacilityImpl>();
	private TreeMap<Id,ActivityFacilityImpl> leisure_facilities=new TreeMap<Id,ActivityFacilityImpl>();
	private QuadTree<ActivityFacilityImpl> shopFacQuadTree = null;
	private QuadTree<ActivityFacilityImpl> leisFacQuadTree = null;

	private final static Logger log = Logger.getLogger(LocationModifier.class);

	public LocationModifier(PopulationImpl plans, NetworkLayer network, ActivityFacilitiesImpl  facilities) {
		super(plans, network, facilities);
		this.initShopLeisure();
	}

	private void initShopLeisure(){
		
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_lt100sqm"));
		//this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilitiesForActivityType("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilitiesForActivityType("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilitiesForActivityType("leisure_sports"));

		this.shopFacQuadTree=this.builFacQuadTree(this.shop_facilities);
		this.leisFacQuadTree=this.builFacQuadTree(this.leisure_facilities);
		
		log.info("Total number of ch shop facilities:" + this.shop_facilities.size());
		log.info("Total number of ch leisure facilities:" + this.leisure_facilities.size());
		
	}

	@Override
	public void modify(){
		/* assign a random location around Bellevue with a radius of 30km
		 * -> tailored for WU project
		 */
		this.assignRandomLocation(new CoordImpl(683508.50, 246832.91), 30000);
	}

	private void assignRandomLocation(Coord coords, double radius) {

		log.info("running assignRandomLocation:");
		Iterator<? extends Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");

		ArrayList<ActivityFacilityImpl> zhShopFacilities = (ArrayList<ActivityFacilityImpl>)this.shopFacQuadTree.get(coords.getX(), coords.getY(), radius);
		ArrayList<ActivityFacilityImpl> zhLeisureFacilities = (ArrayList<ActivityFacilityImpl>)this.leisFacQuadTree.get(coords.getX(), coords.getY(), radius);
		log.info("Total number of zh shop facilities:" + zhShopFacilities.size());
		log.info("Total number of zh leisure facilities:" + zhLeisureFacilities.size());
		

		while (person_iter.hasNext()) {
			Person person = person_iter.next();
				counter.incCounter();

				Iterator<? extends Plan> plan_iter = person.getPlans().iterator();
				while (plan_iter.hasNext()) {
					Plan plan = plan_iter.next();

					if (this.shop_facilities.size()>0) {
						exchangeFacilities("s", zhShopFacilities, plan);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l", zhLeisureFacilities, plan);
					}

				}
		}
		log.info("assignRandomLocation done.");
	}

	private void exchangeFacilities(final String type, ArrayList<ActivityFacilityImpl>  exchange_facilities,
			final Plan plan) {

			final List<? extends PlanElement> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (act.getType().startsWith(type)) {

					ActivityFacilityImpl facility=exchange_facilities.get(
							MatsimRandom.getRandom().nextInt(exchange_facilities.size()));

					act.setFacility(facility);
					act.setLink(facility.getLink());
					act.setCoord(facility.getCoord());
				}
			}
			// loop over all <leg>s, remove route-information
			// routing is done after location choice
			for (int j = 1; j < actslegs.size(); j=j+2) {
				final LegImpl leg = (LegImpl)actslegs.get(j);
				leg.setRoute(null);
			}
		}

	private QuadTree<ActivityFacilityImpl> builFacQuadTree(TreeMap<Id,ActivityFacilityImpl> facilities_of_type) {
		Gbl.startMeasurement();
		System.out.println("      building facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (final ActivityFacilityImpl f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacilityImpl> quadtree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (final ActivityFacilityImpl f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
		return quadtree;
	}



	/* DEPR: ------------------------------------------------------------------------------------------
	 * private  void assignOneRandomLocOfArea() {

		log.info("running assignOneRandomLocOfArea:");
		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
				counter.incCounter();

				Iterator<Plan> plan_iter = person.getPlans().iterator();
				while (plan_iter.hasNext()) {
					Plan plan = plan_iter.next();

					if (this.shop_facilities.size()>0) {
						exchangeFacilities("s", this.shop_facilities, plan, -2, null);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l", this.leisure_facilities, plan, -2, null);
					}
				}
		}
		log.info("... done.");
	}

	private  void randomizeLocations() {

		log.info("running randomize locations:");
		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
				counter.incCounter();

				Iterator<Plan> plan_iter = person.getPlans().iterator();
				while (plan_iter.hasNext()) {
					Plan plan = plan_iter.next();

					if (this.shop_facilities.size()>0) {
						exchangeFacilities("s",this.shop_facilities, plan, -1, null);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l",this.leisure_facilities, plan, -1, null);
					}

				}
		}
		log.info("randomize locations done.");
	}

		private Facility findCloseFacility(String type, Plan plan,
			QuadTree<Facility> quadtree, TreeMap<Id,Facility> facilities_type ) {

		Facility facility=null;
		double homex=plan.getFirstActivity().getCoord().getX();
		double homey=plan.getFirstActivity().getCoord().getY();
		double radius=30000;
		int maxCounter=0;

		while (facility==null && maxCounter<10) {
			Object [] facility_array=quadtree.get(homex, homey, radius).toArray();

			if (facility_array.length>0) {
				int index=0;
				if (facility_array.length>1) {
					index=Gbl.random.nextInt(facility_array.length-1);
				}
				facility=(Facility)facility_array[index];
			}

			radius*=1.5;
			maxCounter++;
		}
		if (facility==null) {
			log.info(plan.getPerson().getId()+ ": no location found");
			facility=(Facility)facilities_type.values().toArray()[0];
		}
		return facility;
	}

	 */

}
