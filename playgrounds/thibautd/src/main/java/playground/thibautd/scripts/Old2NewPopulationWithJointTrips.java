/* *********************************************************************** *
 * project: org.matsim.*
 * Old2NewPopulationWithJointTrips.java
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
package playground.thibautd.scripts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRoute;

import playground.thibautd.cliquessim.population.Clique;
import playground.thibautd.cliquessim.population.DriverRoute;
import playground.thibautd.cliquessim.population.JointActingTypes;
import playground.thibautd.cliquessim.population.JointPlan;
import playground.thibautd.cliquessim.population.PassengerRoute;
import playground.thibautd.cliquessim.utils.JointControlerUtils;

/**
 * Parses a population data file as it was before the inclusion
 * of passenger and driver routes, and dumps a file in the new
 * format.
 *
 * @author thibautd
 */
public class Old2NewPopulationWithJointTrips {
	public static void main(final String[] args) {
		Scenario sc = JointControlerUtils.createScenario( args[ 0 ] );
		String outFile = args[1];

		for (Clique cl : JointControlerUtils.getCliques( sc ).getCliques().values()) {
			for (JointPlan jp : cl.getPlans()) {
               // in the plan file, pu activities are numbered. If two pick ups have
               // the same number in the same joint plan, the following legs are
               // considered joint.
               // This structure "accumulates" the legs to join during the construction,
               // in order to be able to link all related legs.
               Map<String, Map<Id, Leg>> toLink = new HashMap<String, Map<Id, Leg>>();
               String actType;
               String currentJointEpisodeId = null;
               Leg currentLeg;
               Activity currentActivity;

               for (Map.Entry<Id, ? extends Plan> entry : jp.getIndividualPlans().entrySet()) {
				   Id id = entry.getKey();

				   for (PlanElement pe : entry.getValue().getPlanElements()) {
					   if (pe instanceof Activity) {
						   currentActivity = (Activity) pe;
						   actType = currentActivity.getType();

						   if (actType.matches(JointActingTypes.PICK_UP_REGEXP)) {
								   // the next leg will be to associate with this id
								   currentJointEpisodeId =
										   actType.split(JointActingTypes.PICK_UP_SPLIT_EXPR)[1];
								   currentActivity.setType(JointActingTypes.PICK_UP);
						   }
					   }
					   else {
						   currentLeg = (Leg) pe;

						   if (currentJointEpisodeId != null) {
							   // this leg is a shared leg, remember this.
							   if (!toLink.containsKey(currentJointEpisodeId)) {
								   toLink.put(currentJointEpisodeId, new HashMap<Id, Leg>());
							   }

							   toLink.get(currentJointEpisodeId).put( id , currentLeg );
							   currentJointEpisodeId = null;
						   }
					   }
				   }
				}

               // create the links that where encoded in the activity types names
               for (Map<Id, Leg> legsToLink : toLink.values()) {
                   for (Leg leg : legsToLink.values()) {
					   boolean isDriver;
					   if (leg.getMode().equals(TransportMode.car)) {
						   isDriver = true;
						   leg.setMode( JointActingTypes.DRIVER );
						   leg.setRoute(
								   new DriverRoute(
									   (NetworkRoute) leg.getRoute(),
									   Collections.EMPTY_SET ) );
					   }
					   else {
						   isDriver = false;
						   leg.setRoute( new PassengerRoute( null , null ) );
					   }

					   for (Map.Entry<Id, Leg> e : legsToLink.entrySet()) {
						   Leg linkedLeg = e.getValue();
						   Id id = e.getKey();
						   if (leg != linkedLeg) {
							   if (isDriver) {
								   ((DriverRoute) leg.getRoute()).addPassenger( id );
							   }
							   else if (linkedLeg.getMode().equals( TransportMode.car ) ||
									   linkedLeg.getMode().equals( JointActingTypes.DRIVER )) {
								   ((PassengerRoute) leg.getRoute()).setDriverId( id );
							   }
						   }
					   }       
					}
				}
			}
		}

		(new PopulationWriter( sc.getPopulation() , sc.getNetwork() )).write( outFile );
	}
}

