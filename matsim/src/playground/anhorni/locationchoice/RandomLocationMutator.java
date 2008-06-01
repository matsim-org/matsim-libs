package playground.anhorni.locationchoice;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;


public class RandomLocationMutator extends PersonAlgorithm implements PlanAlgorithmI {

	// parameters of the specific selector ----------------------
	private NetworkLayer network=null;
	private final Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	private final TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	private final TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();
	// ----------------------------------------------------------

	public RandomLocationMutator(final NetworkLayer network) {
		this.init(network);
	}

	private void init(final NetworkLayer network) {
		this.network=network;

		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_sports"));
	}

	// plan == selected plan
	public void handlePlan(final Plan plan){

		if (this.shop_facilities.size()>0) {
			exchangeFacilities("s",this.shop_facilities, plan);
		}

		if (this.leisure_facilities.size()>0) {
			exchangeFacilities("l",this.leisure_facilities, plan);
		}
	}


	public void exchangeFacilities(final String type, TreeMap<Id,Facility>  exchange_facilities, final Plan plan) {
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				
				final Facility facility=(Facility)exchange_facilities.values().toArray()[
				           Gbl.random.nextInt(exchange_facilities.size()-1)];
				// plans: link, coords
				// facilities: coords
				// => use coords
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


	@Override
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
}
