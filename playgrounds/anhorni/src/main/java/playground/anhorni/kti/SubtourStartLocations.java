package playground.anhorni.kti;

import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;

public class SubtourStartLocations implements PlanAlgorithm {

	private TreeMap<Id, Integer> locationIds = null;

	//private final static Logger log = Logger.getLogger(SpatialSubtourAnalyzer.class);

	public void run(final Plan plan) {

		this.locationIds = new TreeMap<Id, Integer>();

		Id locationId = null;
		List<? extends PlanElement> actsLegs = plan.getPlanElements();
		for (int i=0; i < actsLegs.size(); i++) {
			if (actsLegs.get(i) instanceof Activity) {
				locationId = ((Activity) actsLegs.get(i)).getFacilityId();

				if (this.locationIds.get(locationId) == null) {
					this.locationIds.put(locationId, new Integer(0));
				}

				int cnt = this.locationIds.get(locationId).intValue() + 1;
				this.locationIds.put(locationId, cnt);
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
