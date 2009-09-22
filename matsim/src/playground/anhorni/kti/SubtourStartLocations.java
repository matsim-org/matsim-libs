package playground.anhorni.kti;

import java.util.List;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class SubtourStartLocations implements PlanAlgorithm {

	private TreeMap<Id, Integer> locationIds = null;

	public void run(final PlanImpl plan) {

		this.locationIds = new TreeMap<Id, Integer>();

		Id locationId = null;
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
		for (int ii=0; ii < actsLegs.size(); ii++) {
			if (actsLegs.get(ii) instanceof ActivityImpl) {
				locationId = ((ActivityImpl) actsLegs.get(ii)).getFacilityId();
				
				if (this.locationIds.get(locationId) == null) {
					this.locationIds.put(locationId, new Integer(0));
				}
				
				int cnt = this.locationIds.get(locationId);
				this.locationIds.put(locationId, cnt++);
			}
		}
	}

	public TreeMap<Id, Integer> getSubtourStartLocations() {
		
		TreeMap<Id, Integer> subtourStartLocations = new TreeMap<Id, Integer>();
		
		for (Id facilityId : this.locationIds.keySet()) {
			if (this.locationIds.get(facilityId).intValue() > 1) {
				subtourStartLocations.put(facilityId, this.locationIds.get(facilityId).intValue() -1);
			}
		}	
		return subtourStartLocations;
	}
}
