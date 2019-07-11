/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.locationchoice.frozenepsilons;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.locationchoice.frozenepsilons.DestinationChoiceContext.ActivityFacilityWithIndex;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
//import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ScaleEpsilon;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

final class BestReplyLocationChoicePlanAlgorithm implements PlanAlgorithm {
	private static final Logger log = Logger.getLogger( BestReplyLocationChoicePlanAlgorithm.class ) ;
	
	private final ActivityFacilities facilities;
	private final ObjectAttributes personsMaxDCScoreUnscaled;
	private final ScaleEpsilon scaleEpsilon;
//	private final ActTypeConverter actTypeConverter;
	private final DestinationSampler sampler;
	private final DestinationChoiceContext lcContext;
	private final MultiNodeDijkstra forwardMultiNodeDijkstra;
	private final BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final int iteration;
	private final Map<Id<ActivityFacility>, Id<Link>> nearestLinks;
	private final Map<String, Double> teleportedModeSpeeds;
	private final Map<String, Double> beelineDistanceFactors;
	private TreeMap<String, QuadTree<ActivityFacilityWithIndex>> quadTreesOfType;
	private final TripRouter tripRouter;
	private final FrozenTastesConfigGroup dccg;
	private final Scenario scenario;

	public BestReplyLocationChoicePlanAlgorithm(
		  TreeMap<String, QuadTree<ActivityFacilityWithIndex>> quad_trees,
		  ObjectAttributes personsMaxDCScoreUnscaled, DestinationChoiceContext lcContext,
		  DestinationSampler sampler, TripRouter tripRouter, MultiNodeDijkstra forwardMultiNodeDijkstra,
		  BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra, ScoringFunctionFactory scoringFunctionFactory,
		  int iteration, Map<Id<ActivityFacility>, Id<Link>> nearestLinks ) {
		this.facilities = lcContext.getScenario().getActivityFacilities();
		this.personsMaxDCScoreUnscaled = personsMaxDCScoreUnscaled;
		this.scaleEpsilon = lcContext.getScaleEpsilon();
//		this.actTypeConverter = lcContext.getConverter();
		this.sampler = sampler;
		this.lcContext = lcContext;
		this.forwardMultiNodeDijkstra = forwardMultiNodeDijkstra;
		this.backwardMultiNodeDijkstra = backwardMultiNodeDijkstra;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.iteration = iteration;
		this.nearestLinks = nearestLinks;
		
		// Cache maps since otherwise they would be created on the fly every time when accessing them via the config object.
		this.teleportedModeSpeeds = this.lcContext.getScenario().getConfig().plansCalcRoute().getTeleportedModeSpeeds();
		this.beelineDistanceFactors = this.lcContext.getScenario().getConfig().plansCalcRoute().getBeelineDistanceFactors();
		
		this.quadTreesOfType = quad_trees;
		this.tripRouter = tripRouter;

		scenario = this.lcContext.getScenario();
		this.dccg = ConfigUtils.addOrGetModule( scenario.getConfig(), FrozenTastesConfigGroup.class ) ;
	}

	@Override
	public final void run(final Plan plan) {
		final Person person = plan.getPerson();

		Long idExclusion = this.dccg.getIdExclusion();
		if (idExclusion != null && Long.parseLong(person.getId().toString()) > idExclusion) return;
		// if person is not in the analysis population
		// TODO: replace this now by subpopulation!
		// Isn't this done by the strategy manager logic?  kai, mar'19

		// ### this is where the work is done:
		this.handleActivities( plan, this.lcContext.getPersonIndex( person.getId() ) );
		// ###

	}

	private void handleActivities( final Plan plan, final int personIndex ) {

		int actlegIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			actlegIndex++;
			if (pe instanceof Activity) {
				String actType = ((Activity) plan.getPlanElements().get(actlegIndex)).getType();
				if (actlegIndex > 0 && this.scaleEpsilon.isFlexibleType(actType)) {

					List<? extends PlanElement> actslegs = plan.getPlanElements();
					final Activity actToMove = (Activity) pe;
					final PlanElement prevAct = actslegs.get( actlegIndex - 2 );
					if ( ! ( prevAct instanceof Activity ) ) {
						log.warn("") ;
						log.warn( "prevAct is not an activity; agentId=" + plan.getPerson().getId() ) ;
						log.warn( "prevAct=" + prevAct ) ;
						log.warn("") ;
						for( PlanElement planElement : plan.getPlanElements() ){
							log.warn( planElement ) ;
						}
						log.warn("") ;
					}
					final Activity actPre = (Activity) prevAct;

					final Activity actPost = (Activity) actslegs.get(actlegIndex + 2);
					final Coord coordPre = PopulationUtils.decideOnCoordForActivity( actPre, scenario ) ;
					final Coord coordPost = PopulationUtils.decideOnCoordForActivity( actPost, scenario ) ;
					double distanceDirect = CoordUtils.calcEuclideanDistance( coordPre, coordPost );
					double maximumDistance = this.convertEpsilonIntoDistance(plan.getPerson(),
						  actToMove.getType() );

					double maxRadius = (distanceDirect +  maximumDistance) / 2.0;

					double x = (coordPre.getX() + coordPost.getX()) / 2.0;
					double y = (coordPre.getY() + coordPost.getY()) / 2.0;
					Coord center = new Coord(x, y);

					ChoiceSet cs = createChoiceSetFromCircle(plan, personIndex, this.dccg.getTravelTimeApproximationLevel(), actToMove, maxRadius, center );

					// === this is where the work is done:
					final Id<ActivityFacility> choice = cs.getWeightedRandomChoice(
							actlegIndex, this.scoringFunctionFactory, plan, this.tripRouter, this.lcContext.getPersonsKValuesArray()[personIndex],
							this.forwardMultiNodeDijkstra, this.backwardMultiNodeDijkstra, this.iteration);
					// yy This looks like method envy, i.e. the method should rather be in this class here.  kai, mar'19
					// ===

					this.setLocationOfActivityToFacilityId(actToMove, choice );

				}
			}
		}		
	}

	private ChoiceSet createChoiceSetFromCircle(Plan plan, int personIndex,
			final FrozenTastesConfigGroup.ApproximationLevel travelTimeApproximationLevel,
			final Activity actToMove, double maxRadius, Coord center) {

		ChoiceSet cs = new ChoiceSet(travelTimeApproximationLevel, scenario );

		final String convertedType = actToMove.getType();
		Gbl.assertNotNull(convertedType);
		final QuadTree<ActivityFacilityWithIndex> quadTree = this.quadTreesOfType.get( convertedType );
		Gbl.assertNotNull( quadTree );
		Collection<ActivityFacilityWithIndex> list = quadTree.getDisk(center.getX(), center.getY(), maxRadius );
		
		for (ActivityFacilityWithIndex facility : list) {
//			int facilityIndex = this.lcContext.getFacilityIndex(facility.getId());
			int facilityIndex = facility.getArrayIndex();
			if (this.sampler.sample(facilityIndex, personIndex)) { 
				
				// only add destination if it can be reached with the chosen mode
				String mode = PopulationUtils.getPreviousLeg( plan, actToMove ).getMode();
				
				Id<Link> linkId = null;
				// try to get linkId from facility, else get it from act. other options not allowed!
				if (facility.getLinkId() != null) {
					linkId = facility.getLinkId();
				}
				else {
					linkId = actToMove.getLinkId();
				}
				// TODO: solve this generic. for that we need info from the config, which modes are actually teleported.
				if (this.lcContext.getScenario().getNetwork().getLinks().get(linkId).getAllowedModes().contains(mode) || 
						mode.equals(TransportMode.bike) || 
						mode.equals(TransportMode.walk) ||
						mode.equals(TransportMode.transit_walk) ||
						mode.equals(TransportMode.other)) {
					cs.addDestination(facility.getId());
				}	
			}
		}
		
		return cs;
	}

	private void setLocationOfActivityToFacilityId( Activity act2, Id<ActivityFacility> facilityId ) {
		act2.setFacilityId( facilityId );
		ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
		act2.setLinkId( FacilitiesUtils.decideOnLink( facility, scenario.getNetwork() ).getId() );
		act2.setCoord( facility.getCoord() );
	}

	/**
	 * Conversion of the "frozen" logit model epsilon into a distance.
	 */
	private double convertEpsilonIntoDistance(Person person, String type) {
		double maxDCScore = 0.0;
		double scale = this.scaleEpsilon.getEpsilonFactor(type);		
		maxDCScore = (Double) this.personsMaxDCScoreUnscaled.getAttribute(person.getId().toString(), type);
		maxDCScore *= scale; // apply the scale factors given in the config file

		/* 
		 * here one could do a much more sophisticated calculation including time use and travel speed estimations (from previous iteration)
		 */
		double travelSpeedCrowFly = this.dccg.getTravelSpeed_car();
		double betaTime = this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car ).getMarginalUtilityOfTraveling();
//		if ( Boolean.getBoolean(this.scenario.getConfig().vspExperimental().getValue(VspExperimentalConfigKey.isUsingOpportunityCostOfTimeForLocationChoice)) ) {
		if ( this.scenario.getConfig().vspExperimental().isUsingOpportunityCostOfTimeForLocationChoice() ) {
			betaTime -= this.scenario.getConfig().planCalcScore().getPerforming_utils_hr() ;
			// needs to be negative (I think) since AH uses this as a cost parameter. kai, jan'13
		}
		double maxTravelTime = Double.MAX_VALUE;
		if (betaTime != 0.0) {
			if ( betaTime >= 0. ) {
				throw new RuntimeException("betaTime >= 0 in location choice; method not designed for this; aborting ...") ;
			}
			maxTravelTime = Math.abs(maxDCScore / (-1.0 * betaTime) * 3600.0); //[s] // abs used for the case when somebody defines beta > 0
			// yy maybe one can still used it with betaTime>0 (i.e. traveling brings utility), but taking the "abs" of it is almost certainly not the
			// correct way.  kai, jan'13
		}
		// distance linear
		double maxDistance = travelSpeedCrowFly * maxTravelTime; 

		// define a maximum distance choice set manually
		if ( this.dccg.getMaxDistanceDCScore() > 0.0) {
			maxDistance = this.dccg.getMaxDistanceDCScore();
		}
		return maxDistance;
	}
}
