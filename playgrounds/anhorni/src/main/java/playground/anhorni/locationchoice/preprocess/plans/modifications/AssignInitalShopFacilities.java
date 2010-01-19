package playground.anhorni.locationchoice.preprocess.plans.modifications;

import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.locationchoice.utils.QuadTreeRing;


public class AssignInitalShopFacilities {
	
	private QuadTreeRing<ActivityFacilityImpl> actTree = null;
	
	public AssignInitalShopFacilities(final QuadTreeRing<ActivityFacilityImpl> actTree) {
		this.actTree = actTree;
	}
		
	public void run(PlanImpl plan, String type) {
		
		ActivityImpl actPre = (ActivityImpl)plan.getPlanElements().get(0);
		
		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
			
			if (act.getType().equals(type)) {
				ActivityImpl actAnchor = actPre;
				if (actPre.getType().startsWith("leisure")) {
					int j = i + 2;
					while (j < plan.getPlanElements().size() && actAnchor.getType().startsWith("leisure")) {
						actAnchor = (ActivityImpl)plan.getPlanElements().get(j);
						j +=2;
					}
				}
				ActivityFacilityImpl facility = this.actTree.get(actAnchor.getCoord().getX(), actAnchor.getCoord().getY());
				act.setFacilityId(facility.getId());
				act.setCoord(facility.getCoord());
			}
			actPre = act;
		}
	}
}
