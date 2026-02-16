package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.matsim.contrib.accessibility.AccessibilityUtils.extractLeg;


public class TeleportedModeContributionCalculator implements AccessibilityContributionCalculator {


	private final String mode;
	private final double betaTT_h;
	private final double betaDist_m;
	private final double asc;
	TripRouter tripRouter ;
	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;
	private ScoringConfigGroup scoringConfigGroup;

	TeleportedModeContributionCalculator(String mode, TripRouter tripRouter, ScoringConfigGroup scoringConfigGroup) {

		this.tripRouter = tripRouter;
		this.mode = mode;
		this.scoringConfigGroup = scoringConfigGroup;

		this.betaTT_h = scoringConfigGroup.getModes().get(mode).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaDist_m = scoringConfigGroup.getModes().get(mode).getMarginalUtilityOfDistance();
		this.asc = scoringConfigGroup.getModes().get(mode).getConstant();

	}

	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		this.aggregatedMeasurePoints = new ConcurrentHashMap<>();
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			Id<ActivityFacility> facilityId = measuringPoint.getId();
			if(!aggregatedMeasurePoints.containsKey(facilityId)) {
				aggregatedMeasurePoints.put(facilityId, new ArrayList<>());
			}
			aggregatedMeasurePoints.get(facilityId).add(measuringPoint);
		}


		this.aggregatedOpportunities = new ConcurrentHashMap<>();
		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {
			AggregationObject opportunityAsAggObj = new AggregationObject(opportunity.getId(), null, null, opportunity, 0.);
			aggregatedOpportunities.put(opportunity.getId(), opportunityAsAggObj);
		}
	}

	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
		//		nothing to do here...
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		// initialize sum of utilities
		double expSum = 0.;

		for (AggregationObject destination : aggregatedOpportunities.values()) {

			Facility opportunity = (Facility) destination.getNearestBasicLocation();

			List<? extends PlanElement> planElements = tripRouter.calcRoute(mode, origin, opportunity, departureTime, null, null);
			Leg walkLeg = extractLeg(planElements, mode);
			double teleportTime_h = walkLeg.getTravelTime().seconds() / 3600;
			double teleportDist_m = walkLeg.getRoute().getDistance();
			double utilityTeleport = teleportTime_h * betaTT_h + teleportDist_m * betaDist_m + asc;
			expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() * utilityTeleport);
		}

		return expSum;


	}

	@Override
	public Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
		return aggregatedMeasurePoints;
	}

	@Override
	public Map<Id<? extends BasicLocation>, AggregationObject> getAgregatedOpportunities() {
		return aggregatedOpportunities;
	}

	@Override
	public AccessibilityContributionCalculator duplicate() {
		TeleportedModeContributionCalculator copy = new TeleportedModeContributionCalculator(mode, tripRouter, scoringConfigGroup);
		copy.aggregatedOpportunities = this.aggregatedOpportunities;
		copy.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		return copy;
	}
}
