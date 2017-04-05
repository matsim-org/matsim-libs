/**
 * 
 */
package org.matsim.contrib.accessibility;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

/**
 * @author nagel
 *
 */
public class TripRouterAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	
	private TripRouter tripRouter ;
	private String mode;
	private PlanCalcScoreConfigGroup scoreConfig;
	
	public TripRouterAccessibilityContributionCalculator( String mode, TripRouter tripRouter, PlanCalcScoreConfigGroup scoreConfig ) {
		this.mode = mode ;
		this.tripRouter = tripRouter;
		this.scoreConfig = scoreConfig;
	}

	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		// at this point, do nothing (inefficient)
	}

	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, final AggregationObject destination, Double departureTime) {
		Person person = null ; // I think that this is ok
		 Facility<?> destinationFacility = new Facility(){
			@Override public Coord getCoord() { return destination.getNearestNode().getCoord() ; }
			@Override public Id getId() { return null; }
			@Override public Map<String, Object> getCustomAttributes() { return null; }
			@Override public Id getLinkId() {
				for ( Id<Link> id : destination.getNearestNode().getInLinks().keySet() ) {
					return id ;
				} 
				return null ;
			}
		 } ;
		
		Gbl.assertNotNull(tripRouter);
		List<? extends PlanElement> plan = tripRouter.calcRoute(mode, origin, destinationFacility, departureTime, person) ;
		
//		Vehicle vehicle = null ; // I think that this is ok
		double sum = 0. ;
		List<Leg> legs = TripStructureUtils.getLegs(plan);
		Gbl.assertIf( !legs.isEmpty() );
		for ( Leg leg : legs ) {
//			if ( leg.getRoute() instanceof NetworkRoute ) {
//				for ( Id<Link> linkId :  ((NetworkRoute) leg.getRoute()).getLinkIds() ) {
//					Link link = network.getLinks().get( linkId ) ;
//					sum += travelDisutility.getLinkTravelDisutility(link, departureTime, person, vehicle) ;
//				}
//			} else {
				sum += leg.getRoute().getDistance() * scoreConfig.getModes().get( leg.getMode() ).getMarginalUtilityOfDistance() ;
				sum += leg.getRoute().getTravelTime() * scoreConfig.getModes().get( leg.getMode() ).getMarginalUtilityOfTraveling() ;
				sum += leg.getRoute().getTravelTime() * scoreConfig.getPerforming_utils_hr() / 3600. ;
			}
//		}
		if ( sum==0. ) {
			Logger.getLogger(this.getClass()).warn("sum=0");
		}
		
		return Math.exp(sum) ; // yyyyyy beta??
	}

}
