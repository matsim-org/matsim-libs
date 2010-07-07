package org.matsim.population.algorithms;

import java.util.List;

import org.junit.Ignore;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.world.Layer;
import org.matsim.world.Location;

@Ignore
public class TestsUtil {

	static PlanImpl createPlan(Layer layer, PersonImpl person, String mode,
			String facString, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		String[] locationIdSequence = facString.split(" ");
		for (int aa=0; aa < locationIdSequence.length; aa++) {
			Location location = layer.getLocation(new IdImpl(locationIdSequence[aa]));
			ActivityImpl act;
			if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
				act = plan.createAndAddActivity("actAtFacility" + locationIdSequence[aa]);
				act.setFacilityId(location.getId());
			} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
				act = plan.createAndAddActivity("actOnLink" + locationIdSequence[aa], location.getId());
			} else {
				throw new RuntimeException("Unknown tripStrucureAnalysisLayerOption");
			}
			act.setEndTime(10*3600);
			if (aa != (locationIdSequence.length - 1)) {
				plan.createAndAddLeg(mode);
			}
		}
		return plan;
	}

	/* Warning: This is NOT claimed to be correct. (It isn't.)
	 *
	 */
	static boolean equals(PlanElement o1, PlanElement o2) {
		if (o1 instanceof Leg) {
			if (o2 instanceof Leg) {
				Leg leg1 = (Leg) o1;
				Leg leg2 = (Leg) o2;
				if (leg1.getDepartureTime() != leg2.getDepartureTime()) {
					return false;
				}
				if (!leg1.getMode().equals(leg2.getMode())) {
					return false;
				}
				if (leg1.getTravelTime() != leg2.getTravelTime()) {
					return false;
				}
			} else {
				return false;
			}
		} else if (o1 instanceof Activity) {
			if (o2 instanceof Activity) {
				Activity activity1 = (Activity) o1;
				Activity activity2 = (Activity) o2;
				if (activity1.getEndTime() != activity2.getEndTime()) {
					return false;
				}
				if (activity1.getStartTime() != activity2.getStartTime()) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			throw new RuntimeException ("Unexpected PlanElement");
		}
		return true;
	}

	public static boolean equals(List<PlanElement> planElements,
			List<PlanElement> planElements2) {
		int nElements = planElements.size();
		if (nElements != planElements2.size()) {
			return false;
		} else {
			for (int i = 0; i < nElements; i++) {
				if (!equals(planElements.get(i), planElements2.get(i))) {
					return false;
				}
			}
		}
		return true;
	}

}
