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

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.ImaginaryNode;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;

class ChoiceSet {
	private static final Logger log = LogManager.getLogger( ChoiceSet.class ) ;

	private final Network network;
	private final FrozenTastesConfigGroup dccg;
	private FrozenTastesConfigGroup.ApproximationLevel approximationLevel;
	private List<Id<ActivityFacility>> destinations = new LinkedList<>();
	private List<Id<ActivityFacility>> notYetVisited = new LinkedList<>();
	private final ActivityFacilities facilities;
	private final Scenario scenario;
	private final TimeInterpretation timeInterpretation;

	private MultiNodeDijkstra forwardMultiNodeDijkstra;
	private BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra;

	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		stb.append("destinations:") ;
		for ( Id<ActivityFacility> id : destinations ) {
			stb.append( " " ).append( id );
		}
		stb.append("\n") ;
		stb.append("notYetVisited:" ) ;
		for ( Id<ActivityFacility> id : notYetVisited ) {
			stb.append( " " ).append( id );
		}
		return stb.toString() ;
	}

	ChoiceSet( FrozenTastesConfigGroup.ApproximationLevel approximationLevel, Scenario scenario, TimeInterpretation timeInterpretation ) {
		this.approximationLevel = approximationLevel;
		this.facilities = scenario.getActivityFacilities();
		this.scenario = scenario;
		this.timeInterpretation = timeInterpretation;

		this.dccg = (FrozenTastesConfigGroup) this.scenario.getConfig().getModule( FrozenTastesConfigGroup.GROUP_NAME );

		this.network = scenario.getNetwork() ;
	}

	void addDestination(Id<ActivityFacility> facilityId) {
		this.destinations.add(facilityId);
		this.notYetVisited.add(facilityId);
	}

	Id<ActivityFacility> getWeightedRandomChoice( int actlegIndex,
								    ScoringFunctionFactory scoringFunction, Plan plan, TripRouter tripRouter, double pKVal,
								    MultiNodeDijkstra forwardMultiNodeDijkstra,
								    BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra, int iteration ) {
		this.forwardMultiNodeDijkstra = forwardMultiNodeDijkstra;
		this.backwardMultiNodeDijkstra = backwardMultiNodeDijkstra;

		List<ScoredAlternative> list;

		if (this.destinations.size() > 0) {
			Plan planTmp = PopulationUtils.createPlan( plan.getPerson() ) ;
			PopulationUtils.copyFromTo( plan, planTmp );
			// === this is where the work is done:
			list = this.createReducedChoiceSetWithPseudoScores(actlegIndex, this.facilities, scoringFunction, planTmp, tripRouter );
			// ===
		} else {
			// if we have no destinations defined so far, we can shorten this
			// currently handled activity which should be re-located
			Activity act = TripStructureUtils.getActivities( plan, ExcludeStageActivities ).get(actlegIndex);
			//			list = createEmptyChoiceMap( act.getFacilityId() );
			list = Collections.singletonList( new ScoredAlternative( 0., act.getFacilityId() ) ) ;
			// (the "0" is a dummy entry!)
		}

		Random random = new Random((long) (Long.MAX_VALUE / iteration * pKVal));

		//	a couple of random draws to come to the "chaotic" region:
		for (int i = 0; i < 10; i++) {
			random.nextDouble();
		}
		int randomIndex = random.nextInt( list.size() ) ;

		return list.get( randomIndex ).getAlternativeId() ;

	}

	/**
	 * the following two lines seem to be outdated:
	 * The "score", which is behind the "Double" in the TreeMap, is some pseudo score 0.6, 0.84, ..., see {ChoiceSet#generateReducedChoiceSet(ArrayList)}.
	 * Well, no, not any more, just setting all of them to 0.2.
	 *
	 * because of the return-typ from the constructPath method from the MultiNodeDijkstra and BackwardFastMultiNodeDijkstra class, we don't overwrite
	 * the whole path between activity we want to change and the activity before/after that. That means the score includes the new path and the old path,
	 * because it is the same in all destinations and the score is not used in the plan file, we decided to keep the information on the basis of a cleaner code
	 *
	 */
	private List<ScoredAlternative> createReducedChoiceSetWithPseudoScores(
		  int actlegIndex,
		  ActivityFacilities facilities,
		  ScoringFunctionFactory scoringFunction,
		  Plan planTmp,
		  TripRouter router ) {

		List<Activity> activities = TripStructureUtils.getActivities( planTmp, ExcludeStageActivities );

		// currently handled activity which should be re-located
		Activity activityToRelocate = activities.get(actlegIndex);

		// We need to calculate the multi node dijkstra stuff only in case localRouting is used.
		if (this.approximationLevel == FrozenTastesConfigGroup.ApproximationLevel.localRouting )
		{
			// we want to investigate multiple destinations for a given activity.  Thus, we need
			// (1) the Dijkstra tree from the activity before to all these destinations
			// (2) the (backwards) Dijkstra tree from all these destinations to the activity following the given activity.
			// The two trees will then be passed into the (preliminary) scoring of these destinations, where they will be evaluated for the
			// destination under consideration.

			// (0) collect all possible destinations and copy them into an "imaginary" node.
			List<InitialNode> destinationNodes = new ArrayList<>();
			for( Id<ActivityFacility> destinationId : this.destinations ){
				ActivityFacility destinationFacility = this.scenario.getActivityFacilities().getFacilities().get( destinationId );
				Link destinationLink = FacilitiesUtils.decideOnLink( destinationFacility, network );
				;
				Node toNode = Objects.requireNonNull( destinationLink ).getToNode();

				InitialNode initialToNode = new InitialNode( toNode, 0.0, 0.0 );
				destinationNodes.add( initialToNode );
			}
			ImaginaryNode destinationNode = MultiNodeDijkstra.createImaginaryNode( destinationNodes );

			// (1) forward tree
			{
				Activity previousActivity = activities.get(actlegIndex - 1);
				Node nextActNode = this.network.getLinks().get( PopulationUtils.decideOnLinkIdForActivity( previousActivity, scenario ) ).getToNode();

				forwardMultiNodeDijkstra.setSearchAllEndNodes( true );
				forwardMultiNodeDijkstra.calcLeastCostPath( nextActNode, destinationNode, timeInterpretation.decideOnActivityEndTimeAlongPlan(previousActivity, planTmp).seconds(), planTmp.getPerson(), null );
			}

			// (2) backward tree
			{
				Activity nextActivity = activities.get(actlegIndex + 1);
				Node nextActNode = this.network.getLinks().get( PopulationUtils.decideOnLinkIdForActivity( nextActivity, scenario ) ).getToNode();

				backwardMultiNodeDijkstra.setSearchAllEndNodes( true );
				backwardMultiNodeDijkstra.calcLeastCostPath( nextActNode, destinationNode,  timeInterpretation.decideOnActivityEndTimeAlongPlan(activityToRelocate, planTmp).seconds(), planTmp.getPerson(), null );
				// yy it is not clear to me how the dp time is interpreted for the backwards Dijkstra.  kai, mar'19
			}
			// ---
		}

		ArrayList<ScoredAlternative> list = new ArrayList<ScoredAlternative>();
		double largestValue = Double.NEGATIVE_INFINITY;
		Id<ActivityFacility> facilityIdWithLargestScore = activityToRelocate.getFacilityId();

		for (Id<ActivityFacility> destinationId : this.destinations) {

			activityToRelocate.setFacilityId( destinationId );
			final ActivityFacility activityFacility = facilities.getFacilities().get( destinationId );
			activityToRelocate.setCoord( activityFacility.getCoord() );
			activityToRelocate.setLinkId( FacilitiesUtils.decideOnLink( activityFacility, network ).getId() );

			switch ( dccg.getTravelTimeApproximationLevel() ){
				case completeRouting:
				case noRouting:
					throw new RuntimeException( "currently not implemented" );
				case localRouting:{
					Node movedActNode;
					{
						Id<Link> movedActLinkId = PopulationUtils.decideOnLinkIdForActivity( activityToRelocate, scenario );
						Link movedActLink = scenario.getNetwork().getLinks().get( movedActLinkId );
						movedActNode = movedActLink.getToNode();
					}
					{
						Link link;
						double startTime;
						Leg previousLeg = PopulationUtils.getPreviousLeg( planTmp, activityToRelocate );

						{
							Activity previousActivity = activities.get(actlegIndex - 1);
							Id<Link> linkId = PopulationUtils.decideOnLinkIdForActivity( previousActivity, scenario );
							link = scenario.getNetwork().getLinks().get( linkId );

							startTime = timeInterpretation.decideOnActivityEndTimeAlongPlan( previousActivity, planTmp ).seconds();
						}

						LeastCostPathCalculator.Path result = this.forwardMultiNodeDijkstra.constructPath( link.getToNode(), movedActNode, startTime );
						NetworkRoute linkNetworkRouteImpl = getNetworkRoute(activityToRelocate, link, result);
						Objects.requireNonNull(previousLeg).setRoute(linkNetworkRouteImpl);
						Objects.requireNonNull( previousLeg ).setTravelTime( result.travelTime );
					}
					{
						Link link;
						Leg nextLeg = PopulationUtils.getNextLeg( planTmp, activityToRelocate );
						{
							Activity nextAct = activities.get(actlegIndex + 1);
							Id<Link> linkId = PopulationUtils.decideOnLinkIdForActivity( Objects.requireNonNull( nextAct ), scenario );
							link = scenario.getNetwork().getLinks().get( linkId );
						}
						double startTime = timeInterpretation.decideOnActivityEndTimeAlongPlan( activityToRelocate, planTmp ).seconds();

						LeastCostPathCalculator.Path result = this.backwardMultiNodeDijkstra.constructPath( link.getToNode(), movedActNode, startTime );
						NetworkRoute linkNetworkRouteImpl = getNetworkRoute(activityToRelocate, link, result);
						Objects.requireNonNull(nextLeg).setRoute(linkNetworkRouteImpl);
						Objects.requireNonNull( nextLeg ).setTravelTime( result.travelTime );
					}
				}
				break ;
				default:
					throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
			}
			PlanTimesAdapter adapter = new PlanTimesAdapter( timeInterpretation );
			final double score = adapter.scorePlan( planTmp, scoringFunction, planTmp.getPerson() );

			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			list.add(new ScoredAlternative(score, destinationId));
		}

		Collections.sort( list );
		if ( !list.isEmpty() ) {
			return list.subList( 0,Math.min( list.size(), 5) ) ;
		} else  {
			// I don't think that this can happen.  But it was in the code before, and better safe than sorry.  kai, mar'19
			return Collections.singletonList( new ScoredAlternative( largestValue, facilityIdWithLargestScore ) )  ;
		}
	}

	private NetworkRoute getNetworkRoute(Activity activityToRelocate, Link link, LeastCostPathCalculator.Path result) {
		NetworkRoute linkNetworkRouteImpl = RouteUtils.createLinkNetworkRouteImpl(activityToRelocate.getLinkId(), link.getId());
		double distance = 0;
		if (result.links != null && !result.links.isEmpty()) {
			List<Id<Link>> linkIds = new ArrayList<>();
			for (Link linkInRoute : result.links) {
				linkIds.add(linkInRoute.getId());
				distance += linkInRoute.getLength();
			}
			linkNetworkRouteImpl.setLinkIds(activityToRelocate.getLinkId(), linkIds, link.getId());
		}
		linkNetworkRouteImpl.setDistance(distance);
		linkNetworkRouteImpl.setTravelTime(result.travelTime);
		linkNetworkRouteImpl.setTravelCost(result.travelCost);
		return linkNetworkRouteImpl;
	}


}
