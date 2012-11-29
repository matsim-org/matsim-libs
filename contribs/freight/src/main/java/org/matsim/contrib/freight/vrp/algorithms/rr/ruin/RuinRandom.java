/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.ruin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;

/**
 * In the random-ruin strategy, current solution is ruined randomly. I.e.
 * customer are removed randomly from current solution.
 * 
 * @author stefan schroeder
 * 
 */

public final class RuinRandom implements RuinStrategy {

	private Logger logger = Logger.getLogger(RuinRandom.class);

	private VehicleRoutingProblem vrp;

	private double fractionOfAllNodes2beRuined;

	private List<Job> unassignedJobs = new ArrayList<Job>();

	private Random random = RandomNumberGeneration.getRandom();
	
	private RouteAgentFactory routeAgentFactory;

	public void setRandom(Random random) {
		this.random = random;
	}

	public RuinRandom(VehicleRoutingProblem vrp, RouteAgentFactory routeAgentFactory) {
		super();
		this.vrp = vrp;
		this.routeAgentFactory = routeAgentFactory;
		logger.info("initialise random ruin");
		logger.info("done");
	}

	@Override
	public Collection<Job> ruin(Collection<VehicleRoute> vehicleRoutes) {
		clear();
		Collection<RouteAgent> routeAgents = makeRouteAgents(vehicleRoutes);
		int nOfJobs2BeRemoved = selectNuOfJobs2BeRemoved();
		LinkedList<Job> availableJobs = new LinkedList<Job>(vrp.getJobs().values());
		Set<RouteAgent> agents2update = new HashSet<RouteAgent>();
		for (int i = 0; i < nOfJobs2BeRemoved; i++) {
			Job job = pickRandomJob(availableJobs);
			unassignedJobs.add(job);
			availableJobs.remove(job);
			boolean removed = false;
			for (RouteAgent agent : routeAgents) {
				 removed = agent.removeJobWithoutTourUpdate(job);
				if (removed) {
					agents2update.add(agent);
					break;
				}
			}
			if(!removed) logger.warn("job " + job.getId() + " cannot be removed");
		}
		for(RouteAgent agent : agents2update){
			agent.updateTour();
		}
		return unassignedJobs;
	}

	private Collection<RouteAgent> makeRouteAgents(Collection<VehicleRoute> vehicleRoutes) {
		Collection<RouteAgent> agents = new ArrayList<RouteAgent>();
		for(VehicleRoute r : vehicleRoutes){
			agents.add(routeAgentFactory.createAgent(r));
		}
		return agents;
	}

	private Job pickRandomJob(LinkedList<Job> availableJobs) {
		int randomIndex = random.nextInt(availableJobs.size());
		return availableJobs.get(randomIndex);
	}

	public void setRuinFraction(double fractionOfAllNodes2beRuined) {
		this.fractionOfAllNodes2beRuined = fractionOfAllNodes2beRuined;
	}

	private int selectNuOfJobs2BeRemoved() {
		return (int) Math.ceil(vrp.getJobs().values().size() * fractionOfAllNodes2beRuined);
	}

	private void clear() {
		unassignedJobs.clear();
	}

}
