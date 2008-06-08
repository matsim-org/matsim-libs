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
import org.matsim.utils.misc.Counter;

public class PlanAssignInitialLocations {

	private Plans plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	private final TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	private final TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();

	private final static Logger log = Logger.getLogger(PlanAssignInitialLocations.class);

	/**
	 * @param:
	 * - path to plans file
	 * - path to network file
	 * - path to facilities file
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
		*/
		String plansfilePath="./input/plans.xml.gz";
		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";


		PlanAssignInitialLocations randomizer=new PlanAssignInitialLocations();
		randomizer.run(plansfilePath, networkfilePath, facilitiesfilePath);
		randomizer.writePlans();
	}


	private void run(final String plansfilePath, final String networkfilePath, final String facilitiesfilePath) {
		this.init(plansfilePath, networkfilePath, facilitiesfilePath);
		//this.randomizeLocations();
		this.assignOneLocation();
	}

	private void init(final String plansfilePath, final String networkfilePath, final String facilitiesfilePath) {

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
						exchangeFacilities("s",this.shop_facilities, plan);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacilities("l",this.leisure_facilities, plan);
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
						exchangeFacility("s",this.shop_facilities, plan, 0);
					}

					if (this.leisure_facilities.size()>0) {
						exchangeFacility("l",this.leisure_facilities, plan, 0);
					}

				}
		}
		log.info("assignOneLocation done.");
	}


	private void writePlans() {
		new PlansWriter(this.plans, "./output/plans_randomized.xml.gz", "v4", 1.0).write();
		log.info("plans written to:  ./output/plans_randomized.xml.gz ");
	}


	private void exchangeFacilities(final String type, final TreeMap<Id,Facility>  exchange_facilities, final Plan plan) {

			final ArrayList<?> actslegs = plan.getActsLegs();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Act act = (Act)actslegs.get(j);
				if (act.getType().startsWith(type)) {

					final Facility facility=(Facility)exchange_facilities.values().toArray()[
					           Gbl.random.nextInt(exchange_facilities.size()-1)];
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

	private void exchangeFacility(final String type, final TreeMap<Id,Facility>  exchange_facilities, final Plan plan, final int index) {

		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				final Facility facility=(Facility)exchange_facilities.values().toArray()[index];
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

}
