package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.ArrayList;
import java.util.Map;

/**
 * Interface to provide a means to compute the utility of a given
 * origin destination, for a given mode.
 * This includes travel time estimation and utility formulation.
 *
 * @author thibautd, dziemke
 */
// It might make sense to split this further, into travel time estimation
// and utility estimation. Ideally, one would actually not need any additional
// interface compared to what MATSim provides. Not there yet [td, june 15]
public interface AccessibilityContributionCalculator {

	void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities);

	/**
	 * Provided for performance purpose.
	 * The accessibility listener(s) aggregate first all "measuring points"
	 * according to the node they are associated to on the network,
	 * allowing to perform some preprocessing (for instance pre-computing
	 * a least-cost path tree).
	 *
	 * @param fromNodeId the Id of the node on the network to which will be associated the next
	 *                 examined opportunities.
	 * @param departureTime TODO
	 */
	void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime );

	/**
	 * estimates the contribution of a given opportunity to the accessibility metric,
	 * defined as an origin point (provided as a facility) and a group of facilities.
	 * "Contribution" is understood here as what is summed, that is, for the logsum,
	 * the exponential of the utility, scaled by the logit scale parameter.
	 *
	 * @param origin the origin point
	 * //@param destination the opportunities at the destination
	 * @param departureTime TODO
	 * @return the utility of the OD pair, to be included in the logsum
	 */
	double computeContributionOfOpportunity(ActivityFacility origin, Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime);
	// yyyy I am somewhat sceptic if we tryly need both "fromNode" (above) and origin.
	// yyyy And I am quite confident that we do not need the departure time twice.

    Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints();

    Map<Id<? extends BasicLocation>, AggregationObject> getAgregatedOpportunities();

	/*
	Needed for perallelization
	 */
	AccessibilityContributionCalculator duplicate();
}