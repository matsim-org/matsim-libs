package playground.anhorni.locationchoice;

import java.util.ArrayList;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;


public class RandomLocationMutator extends LocationMutator {


	public RandomLocationMutator(final NetworkLayer network) {
		super(network);
	}


	// plan == selected plan
	public void handlePlan(final Plan plan){

		if (this.shop_facilities.size()>0) {
			exchangeFacilities("s",this.zhShopFacilities, plan);
			
		}

		if (this.leisure_facilities.size()>0) {
			exchangeFacilities("l",this.zhLeisureFacilities, plan);
		}
	}


	public void exchangeFacilities(final String type, ArrayList<Facility>  exchange_facilities, final Plan plan) {
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				
				final Facility facility=(Facility)exchange_facilities.toArray()[
				           Gbl.random.nextInt(exchange_facilities.size()-1)];
				// plans: link, coords
				// facilities: coords
				// => use coords
				
				// facility needs to be set!!!
				
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
