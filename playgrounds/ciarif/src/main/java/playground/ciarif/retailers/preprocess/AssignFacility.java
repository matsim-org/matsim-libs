package playground.ciarif.retailers.preprocess;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;

import playground.ciarif.retailers.utils.ActivityDifferentiator;

public class AssignFacility {

	private final static Logger log = Logger.getLogger(ActivityDifferentiator.class);
	private ActivityImpl act;
	private Map<Id, ActivityFacility> facilities;

	public AssignFacility(final ActivityImpl act, Map<Id, ActivityFacility> map) {
		this.act = act;
		this.facilities = map;
	}

	public void run() {
		int rd = MatsimRandom.getRandom().nextInt(facilities.size());
		ActivityFacility facility =(ActivityFacility) (facilities.values().toArray())[rd];
		log.info("act type= " + act.getType());
		log.info("activity options= " + facility.getActivityOptions());
			if (facility.getActivityOptions().containsKey(act.getType().toString())) {
				act.setFacilityId(facility.getId());
				act.setCoord(facility.getCoord());
		}
			else {
				run();
				}
		
	}
	
	
	
}
