package playground.anhorni.locationchoice.preprocess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.misc.Counter;

public class LocationModifier extends Modifier {

	private TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	private TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();
	private QuadTree<Facility> shopFacQuadTree = null;
	private QuadTree<Facility> leisFacQuadTree = null;

	private final static Logger log = Logger.getLogger(LocationModifier.class);

	public LocationModifier() {
		super();
		this.initShopLeisure();
	}

	private void initShopLeisure(){
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_sports"));

		this.shopFacQuadTree=this.builFacQuadTree(this.shop_facilities);
		this.leisFacQuadTree=this.builFacQuadTree(this.leisure_facilities);

	}

	@Override
	public void modify(int option){
		if (option==0) {
			this.randomizeLocations();
		}
		else if (option==1) {
			this.assignOneLocation();
		}
		else if (option==2) {
			this.assignOneRandomLocOfArea();
		}
		else {
			log.error("option must be 0,1 or 2");
		}
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
						exchangeFacilities("s",this.shop_facilities, plan, -1);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l",this.leisure_facilities, plan, -1);
					}

				}
		}
		log.info("randomize locations done.");
	}

	private  void assignOneLocation() {

		log.info("running assignOneLocation:");
		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
				counter.incCounter();

				Iterator<Plan> plan_iter = person.getPlans().iterator();
				while (plan_iter.hasNext()) {
					Plan plan = plan_iter.next();

					if (this.shop_facilities.size()>0) {
						exchangeFacilities("s",this.shop_facilities, plan, 0);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l",this.leisure_facilities, plan, 0);
					}

				}
		}
		log.info("assignOneLocation done.");
	}

	private  void assignOneRandomLocOfArea() {

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
						exchangeFacilities("s", this.shop_facilities, plan, -2);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l", this.leisure_facilities, plan, -2);
					}
				}
		}
		log.info("... done.");
	}

	private void exchangeFacilities(final String type, final TreeMap<Id,Facility>  exchange_facilities,
			final Plan plan, int index) {

			final ArrayList<?> actslegs = plan.getActsLegs();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Act act = (Act)actslegs.get(j);
				if (act.getType().startsWith(type)) {

					Facility facility=null;
					if (index==-1) {
						facility=(Facility)exchange_facilities.values().toArray()[
					           Gbl.random.nextInt(exchange_facilities.size()-1)];
					}
					else if (index==-2) {
						if (type.equals("s")) {
							facility=findCloseFacility(type, plan, this.shopFacQuadTree, exchange_facilities);
						}
						if (type.equals("l")) {
							facility=findCloseFacility(type, plan, this.leisFacQuadTree, exchange_facilities);
						}
					}
					else {
						facility=(Facility)exchange_facilities.values().toArray()[index];
					}
					//act.setFacility(facility);
					Link nearestLink=this.network.getNearestLink(facility.getCenter());
					act.setLink(nearestLink);
					act.setCoord(nearestLink.getCenter());
				}
			}

			// loop over all <leg>s, remove route-information
			// routing is done after location choice
			for (int j = 1; j < actslegs.size(); j=j+2) {
				final Leg leg = (Leg)actslegs.get(j);
				leg.setRoute(null);
			}
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
}
