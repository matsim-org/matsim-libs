package playground.anhorni.locationchoice;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;


public class RandomLocationMutator extends PersonAlgorithm implements PlanAlgorithmI {

	// parameters of the specific selector ----------------------
	private final int nbrChanges=1;
	// ----------------------------------------------------------

	public RandomLocationMutator() {
	}

	// plan == selected plan
	public void handlePlan(final Plan plan){

		final Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		final TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
		final TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();


		shop_facilities.putAll(facilities.getFacilities("shop_retail_gt2500sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_get1000sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_get400sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_get100sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_other"));

		leisure_facilities.putAll(facilities.getFacilities("leisure_gastro"));
		leisure_facilities.putAll(facilities.getFacilities("leisure_culture"));
		leisure_facilities.putAll(facilities.getFacilities("leisure_sports"));


		for (int j=0; j<this.nbrChanges; j++) {

			final Facility shop_facility=(Facility)shop_facilities.entrySet().toArray()[
			           Gbl.random.nextInt(shop_facilities.size()-1)];

			final Link linkExchangeShop=shop_facility.getLink();
			System.out.println("link_id"+linkExchangeShop.getId());
			exchangeLink("s",linkExchangeShop, plan);

			final Facility leisure_facility=(Facility)leisure_facilities.entrySet().toArray()[
 			           Gbl.random.nextInt(leisure_facilities.size()-1)];

 			final Link linkExchangeLeisure=leisure_facility.getLink();
 			System.out.println("link_id"+linkExchangeLeisure.getId());
 			exchangeLink("l",linkExchangeShop, plan);
		}
	}


	public void exchangeLink(final String type, final Link link, final Plan plan) {
		// modify plan by randomly exchanging a link (facility) in the plan
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				act.setLink(link);
			}
		}

		// loop over all <leg>s, remove route-information
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
