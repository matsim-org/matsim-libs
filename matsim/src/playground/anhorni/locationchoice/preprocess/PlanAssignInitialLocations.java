package playground.anhorni.locationchoice.preprocess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.misc.Counter;

public class PlanAssignInitialLocations {

	private Plans plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	private TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	private TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();
	private String outputpath="";

	private QuadTree<Facility> shopFacQuadTree = null;
	private QuadTree<Facility> leisFacQuadTree = null;

	private final static Logger log = Logger.getLogger(PlanAssignInitialLocations.class);

	/**
	 * @param:
	 * - path to plans file
	 * - path to network file
	 * - path to facilities file
	 * - type:
	 * 		0: assign one loc
	 * 		1: randomize locs
	 * 		2: assign one random loc of area
	 */
	public static void main(final String[] args) {

		/*
		if (args.length < 3 || args.length > 3 ) {
			System.out.println("Too few arguments. Exit");
			System.exit(1);
		}

		String plansfilePath=args[0];
		String networkfilePath=args[1];
		String facilitiesfilePath=args[2];
		int type=args[3];
		*/
		int type=2;

		String plansfilePath="./input/plans.xml.gz";
		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";


		PlanAssignInitialLocations randomizer=new PlanAssignInitialLocations();
		randomizer.run(plansfilePath, networkfilePath, facilitiesfilePath, type);
		randomizer.writePlans();
	}

	private void run(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath, final int type) {

		this.init(plansfilePath, networkfilePath, facilitiesfilePath, type);

		if (type==0) {
			this.randomizeLocations();
		}
		else if (type==1) {
			this.assignOneLocation();
		}
		else if (type==2) {
			this.assignOneRandomLocOfArea();
		}

	}

	private void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath, final int type) {

		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		this.facilities=new Facilities();
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_sports"));
		log.info("facilities reading done");


		this.plans=new Plans(false);
		final PlansReaderI plansReader = new MatsimPlansReader(this.plans);
		plansReader.readFile(plansfilePath);

		if (type==0) {
			this.outputpath="./output/plans_randomized.xml.gz";
		}
		else if (type==1) {
			this.outputpath="./output/plans_oneloc.xml.gz";
		}
		else if (type==2){
			this.outputpath="./output/plans_onelocinarea.xml.gz";
			this.shopFacQuadTree=this.builFacQuadTree(this.shop_facilities);
			this.leisFacQuadTree=this.builFacQuadTree(this.leisure_facilities);
		}

		log.info("plans reading done");
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

	private void writePlans() {
		new PlansWriter(this.plans, this.outputpath , "v4", 1.0).write();
		log.info("plans written to: " + this.outputpath);
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
					act.setFacility(facility);
					act.setLink(this.network.getNearestLink(facility.getCenter()));
					act.setCoord(facility.getCenter());
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
