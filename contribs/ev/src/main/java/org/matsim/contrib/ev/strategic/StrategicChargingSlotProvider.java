package org.matsim.contrib.ev.strategic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

import com.google.common.base.Preconditions;

/**
 * This is the ChargingSlotProvider implementation of the startegic charging
 * package. It examines an agent's plan and tries to find the selected "charging
 * plan". If the charging plan can be found, the respective charging slots (if
 * still avaialble after mode choice, etc.) are generated throghout the day.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingSlotProvider implements ChargingSlotProvider {
	private final ChargingInfrastructure infrastructure;
	private final ChargingSlotFinder candidateFinder;

	public StrategicChargingSlotProvider(ChargingInfrastructure infrastructure, ChargingSlotFinder candidateFinder) {
		this.infrastructure = infrastructure;
		this.candidateFinder = candidateFinder;
	}

	@Override
	public List<ChargingSlot> findSlots(Person person, Plan plan, ElectricVehicle vehicle) {
		ChargingPlans chargingPlans = ChargingPlans.get(plan);
		ChargingPlan selectedPlan = chargingPlans.getSelectedPlan();

		if (selectedPlan == null) {
			return Collections.emptyList();
		}

		// find the charging activities that are compatible with the possible slots of
		// the current plan configuration
		List<ChargingSlot> slots = new LinkedList<>();

		List<ActivityBasedCandidate> activityBased = candidateFinder.findActivityBased(person, plan);
		List<LegBasedCandidate> legBased = candidateFinder.findLegBased(person, plan);

		List<Activity> activities = TripStructureUtils.getActivities(plan,
				StageActivityHandling.ExcludeStageActivities);

		for (ChargingPlanActivity chargingActivity : selectedPlan.getChargingActivities()) {
			if (!chargingActivity.isEnroute()) {
				for (ActivityBasedCandidate candidate : activityBased) {
					if (activities.indexOf(candidate.startActivity()) == chargingActivity.getStartActivityIndex()) {
						if (activities.indexOf(candidate.endActivity()) == chargingActivity.getEndActivityIndex()) {
							// we found a matching candidate
							Charger charger = infrastructure.getChargers().get(chargingActivity.getChargerId());
							slots.add(new ChargingSlot(candidate.startActivity(),
									candidate.endActivity(), charger));
						}
					}
				}
			} else {
				boolean foundMatch = false;

				for (LegBasedCandidate candidate : legBased) {
					if (activities.indexOf(candidate.followingActivity()) == chargingActivity
							.getFollowingActivityIndex()) {
						// we found a matching candidate
						Charger charger = infrastructure.getChargers().get(chargingActivity.getChargerId());
						slots.add(new ChargingSlot(candidate.leg(), chargingActivity.getDuration(), charger));

						Preconditions.checkState(!foundMatch);
						foundMatch = true;
					}
				}
			}
		}

		return slots;
	}
}
