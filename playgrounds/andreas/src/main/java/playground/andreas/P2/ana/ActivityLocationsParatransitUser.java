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

package playground.andreas.P2.ana;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import playground.andreas.P2.helper.PConfigGroup;

/**
 * Writes all the coordinates of all activities right before and after a paratransit trip to file.
 * 
 * @author aneumann
 *
 */
public class ActivityLocationsParatransitUser implements IterationEndsListener {
	private final static Logger log = Logger.getLogger(ActivityLocationsParatransitUser.class);
	
	private final String pIdentifier;
	private boolean firstIteration = true;
	
	private List<Coord> activitiesOfParatransitUsers;

	public ActivityLocationsParatransitUser(PConfigGroup pConfig) {
		log.info("enabled");
		this.pIdentifier = pConfig.getPIdentifier();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.activitiesOfParatransitUsers = parsePopulation(event.getControler().getPopulation().getPersons().values());
		
		if (this.firstIteration) {
			// write it to main output
			writeResults(event.getControler().getControlerIO().getOutputFilename("actsFromParatransitUsers.txt"), this.activitiesOfParatransitUsers);
			this.firstIteration = false;
		} else {
			// write it somewhere
			writeResults(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "actsFromParatransitUsers.txt"), this.activitiesOfParatransitUsers);
		}
		
		this.activitiesOfParatransitUsers = null;
	}

	private List<Coord> parsePopulation(Collection<? extends Person> persons) {
		
		List<Coord> activitiesOfParatransitUsers = new LinkedList<Coord>();
		
		for (Person person : persons) {
			Coord lastCoord = null;
			boolean lastLegUsesParatransit = false;
			
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					
					if (!((Activity) pE).getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						if (lastLegUsesParatransit) {
							activitiesOfParatransitUsers.add(lastCoord);
						}
						lastCoord = ((Activity) pE).getCoord();
						lastLegUsesParatransit = false;
					}
					
				}

				if (pE instanceof Leg) {
					// check, if it is a paratransit user
					Leg leg = (Leg) pE;
					
					if (leg.getRoute() instanceof ExperimentalTransitRoute) {
						ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
						
						if (route.getRouteId().toString().contains(this.pIdentifier)) {
							// it's a paratransit user
							lastLegUsesParatransit = true;
						}
					}
				}
			}
			
			// add very last act as well
			if (lastLegUsesParatransit) {
				activitiesOfParatransitUsers.add(lastCoord);
			}
		}
		
		return activitiesOfParatransitUsers;
	}
	
	private void writeResults(String filename, List<Coord> activitiesOfParatransitUsers) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
			writer.write("x\ty");
			writer.newLine();
			
			for (Coord coord : activitiesOfParatransitUsers) {
				writer.write(coord.getX() + "\t" + coord.getY());
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
