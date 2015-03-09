package playground.balac.retailers.preprocess;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacility;

import playground.balac.retailers.utils.ActivityDifferentiatorBalac;


public class AssignFacility {

	private final static Logger log = Logger.getLogger(ActivityDifferentiatorBalac.class);
	private ActivityImpl act;
	private Map<Id, ActivityFacility> facilities;

	public AssignFacility(ActivityImpl act,	Map<Id, ActivityFacility> map) {
		
		this.act = act;
		this.facilities = map;
	}

	public void run() {
			int rd = MatsimRandom.getRandom().nextInt(this.facilities.size());
			ActivityFacility facility = (ActivityFacility) (facilities.values().toArray())[rd];
			act.setFacilityId(facility.getId());
			act.setCoord(facility.getCoord());
		}
}
