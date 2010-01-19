package playground.anhorni.locationchoice.preprocess.plans.modifications;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.locationchoice.utils.QuadTreeRing;

import playground.anhorni.locationchoice.run.replanning.LeisureFacilityExtractor;

public class AssignInitialLeisureFacilities {
	
	private final QuadTreeRing<ActivityFacilityImpl> actTree;
	private final LeisureFacilityExtractor leisureFacilityExtractor;
	
	public AssignInitialLeisureFacilities(final QuadTreeRing<ActivityFacilityImpl> actTree) {
		this.leisureFacilityExtractor = new LeisureFacilityExtractor(actTree);
		this.leisureFacilityExtractor.setAssignInAnyCase(true);
		this.actTree = actTree;
	}
	
	public void run(Plan plan, String mode) {
		
		Activity actPre = (Activity)plan.getPlanElements().get(0);
		
		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
	
			if (act.getType().startsWith("leisure")) {
				
				ActivityFacilityImpl facility = null;
				
				// if distances are not yet assigned
				if (act.getType().equals("leisure")) {
					facility = this.actTree.get(actPre.getCoord().getX(), actPre.getCoord().getY());
				}
				else {
					if (i + 1 < plan.getPlanElements().size()) {
						final LegImpl leg = (LegImpl)plan.getPlanElements().get(i + 1);
						if (leg.getMode().toString().equals(mode)) {	
							String type = act.getType();
							String[] entries = type.split("_", -1);							
							int dist = Integer.parseInt(entries[1].trim());	
							
							facility = this.leisureFacilityExtractor.getFacility((CoordImpl)actPre.getCoord(), dist);
							
						}
					}
				}
				act.setFacilityId(facility.getId());
				act.setCoord(facility.getCoord());				
			}
			actPre = act;
		}
	}
}
