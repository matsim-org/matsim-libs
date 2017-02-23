/**
 * 
 */
package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.facilities.ActivityFacility;

/**
 * @author nagel
 *
 */
public class TransitAccessibilityContribution implements AccessibilityContributionCalculator {

	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination,
			Double departureTime) {
		// TODO Auto-generated method stub
		return 0;
	}

}
