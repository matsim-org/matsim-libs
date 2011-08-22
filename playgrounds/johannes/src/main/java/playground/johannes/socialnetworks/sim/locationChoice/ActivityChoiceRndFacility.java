/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChoice2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.locationChoice;

import gnu.trove.TIntArrayList;
import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author illenberger
 *
 */
public class ActivityChoiceRndFacility implements PlanAlgorithm {
	
	private static final Logger logger = Logger.getLogger(ActivityChoiceRndFacility.class);
	
	private static final String type = "leisure";
	
	private final Random random;
	
	private final ActivityMover mover;
	
	private Map<Person, Double> desiredArrivalTimes;
	
	private Map<Person, Double> desiredDurations;
	
	final private List<Id> linkIds;
	
	private Network network;
	
	private TObjectDoubleHashMap<Person> constants;
	
	public ActivityChoiceRndFacility(Network network, ActivityMover mover, Random random, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations, List<Id> linkIds, TObjectDoubleHashMap<Person> constants) {
		this.random = random;
		this.mover = mover;
		this.network = network;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
		this.linkIds =linkIds;
		this.constants = constants;	
	}
	

	@Override
	public void run(Plan plan) {
		TIntArrayList indices = new TIntArrayList(plan.getPlanElements().size());
		/*
		 * retrieve all potential activity indices
		 */
		for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
			Activity act = (Activity) plan.getPlanElements().get(i);
			if(type.equals(act.getType())) {
				indices.add(i);
			}
		}
		if (!indices.isEmpty()) {
			Link source = network.getLinks().get(((Activity)plan.getPlanElements().get(0)).getLinkId());
			/*
			 * randomly select one index
			 */
			int idx = indices.get(random.nextInt(indices.size()));
			/*
			 * randomly draw new location
			 */
//			Id link = linkIds.get(random.nextInt(linkIds.size()));
			Id link = null;
			double constant = constants.get(plan.getPerson());
			while (link == null) {
				link = linkIds.get(random.nextInt(linkIds.size()));
				if (link.equals(source.getId())) // home link is not allowed.
					link = null;
				else {
					Link target = network.getLinks().get(link);
					double d = CoordUtils.calcDistance(source.getCoord(), target.getCoord());

					if (random.nextDouble() > (13 / constant * Math.pow(d, -1))) {
						link = null;
					}
				}
			}
			/*
			 * move activity
			 */
			mover.moveActivity(plan, idx, link, desiredArrivalTimes.get(plan.getPerson()), desiredDurations.get(plan.getPerson()));
		}
	}
}
