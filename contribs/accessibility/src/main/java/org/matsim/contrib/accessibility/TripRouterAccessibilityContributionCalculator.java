/**
 * 
 */
package org.matsim.contrib.accessibility;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public class TripRouterAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	
	private TripRouter tripRouter ;
	private TravelDisutility travelDisutility ;
	private String mode;
	private Network network ;
	
	public TripRouterAccessibilityContributionCalculator( String mode ) {
		this.mode = mode ;
	}

	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		// at this point, do nothing (inefficient)
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {
		Person person = null ; // I think that this is ok
		Facility<?> destinationFacility = new FakeFacility( destination.getNearestNode().getCoord() ) ;
		List<? extends PlanElement> plan = tripRouter.calcRoute(mode, origin, destinationFacility, departureTime, person) ;
		
		Vehicle vehicle = null ; // I think that this is ok
		double sum = 0. ;
		for ( Leg leg : TripStructureUtils.getLegs(plan) ) {
			if ( leg.getRoute() instanceof NetworkRoute ) {
				for ( Id<Link> linkId :  ((NetworkRoute) leg.getRoute()).getLinkIds() ) {
					Link link = network.getLinks().get( linkId ) ;
					sum += travelDisutility.getLinkTravelDisutility(link, departureTime, person, vehicle) ;
				}
			} else {
				leg.getRoute().getDistance() ;
				leg.getRoute().getTravelTime() ;
				// yyyyyy put together!
				throw new RuntimeException("not implemented") ;
				// yyyy money is missing
			}
		}
		return Math.exp(sum) ; // yyyyyy beta??
	}

}
