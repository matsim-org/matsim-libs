/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.southafrica.population.freight;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v1;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.utilities.Header;

/**
 * Generates a given number of freight activity chains using a sequence-
 * dependent complex network.
 * @author jwjoubert
 */
public class FreightChainGenerator {
	private final static Logger LOG = Logger.getLogger(FreightChainGenerator.class);
	private final static int MAX_CHAIN_LENGTH = 20;
	private final static Random RANDOM = MatsimRandom.getRandom();
	private final static Double AVERAGE_SPEED = 50.0/3.6;
	
	private static Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FreightChainGenerator.class.toString(), args);
		
		String complexNetworkFile = args[0];
		int numberOfPlans = Integer.parseInt(args[1]);
		String populationPrefix = args[2];
		String outputPlansFile = args[3];
//		String networkFile = args[4];
		
		/* Read the path-dependent complex network. */
		DigicorePathDependentNetworkReader_v1 nr = new DigicorePathDependentNetworkReader_v1();
		nr.parse(complexNetworkFile);
		PathDependentNetwork network = nr.getPathDependentNetwork();
		network.writeNetworkStatisticsToConsole();
		
		generateFreightAgentPlans(network, populationPrefix, numberOfPlans);
		
		/* Write the population to file. */
		new PopulationWriter(sc.getPopulation(), null).write(outputPlansFile);
		
		Header.printFooter();
	}
	
	
	private static void generateFreightAgentPlans(PathDependentNetwork network, String prefix, int numberOfPlans){
		LOG.info("Creating a population of " + numberOfPlans + " freight activity chains...");
		PopulationFactory pf = sc.getPopulation().getFactory();
		
		for(int i = 0; i < numberOfPlans; i++){
			Person vehicle = new PersonImpl( new IdImpl(prefix + "_" + i) );
			
			/* Set up the plan. */
			PlanImpl plan = (PlanImpl) pf.createPlan();
			double cumulativeSeconds = 0.0;
			
			/* Generate the first activity. */
			Id previousId = new IdImpl("source");
			Id currentId = network.sampleChainStartNode();
			Id nextId = network.getPathDependentNode(currentId).sampleBiasedNextPathDependentNode(previousId);
			Coord coord = network.getPathDependentNode(currentId).getCoord();
			ActivityImpl activity = new ActivityImpl("major", coord);
			activity.setFacilityId(new IdImpl(currentId.toString()));
			activity.setEndTime(ChainStartTime.getStartTimeInSeconds(RANDOM.nextDouble()));
			cumulativeSeconds += activity.getEndTime();
			plan.addActivity(activity);
			
			/* Generate the consecutive activities until the next vertex Id is
			 * 'sink', indicating the activity chain ends there. */
			boolean chainEnd = false;
			int chainLength = 1;
			while(!chainEnd){
				/* Add the leg. */
				Leg leg = pf.createLeg("commercial");
				plan.addLeg(leg);
				
				/* Update the node sequence, but only if the current activity
				 * is not the end-of-chain activity. */
				previousId = new IdImpl(currentId.toString());
				currentId = new IdImpl(nextId.toString());
				chainLength++;
				
				/* Generate the next node, given the current node. */
				nextId = network.getPathDependentNode(currentId).sampleBiasedNextPathDependentNode(previousId);
				String activityType = null;
				if(chainLength == MAX_CHAIN_LENGTH || nextId == null || nextId.toString().equalsIgnoreCase("sink")){
					activityType = "major";
					chainEnd = true;
				} else{
					activityType = "minor";
				}
				
				/* Create and add the activity from the identified node. */
				Coord thisCoord = network.getPathDependentNode(currentId).getCoord();
				ActivityImpl thisActivity = new ActivityImpl(activityType, thisCoord);
				thisActivity.setFacilityId(currentId);
					/* Update the travel time */
				Coord previousCoord = network.getPathDependentNode(previousId).getCoord();
				cumulativeSeconds += CoordUtils.calcDistance(thisCoord, previousCoord) / AVERAGE_SPEED;
					/* Update the end time. */
				double duration = ActivityDuration.getDurationInSeconds(RANDOM.nextDouble());
				cumulativeSeconds += duration;
				// FIXME Is right right i.t.o. last major activity?
				if(activityType.equalsIgnoreCase("minor")){
					thisActivity.setMaximumDuration(duration);
					thisActivity.setEndTime(cumulativeSeconds);
				}
				
				plan.addActivity(thisActivity);
				
			}
			
			/* Add the plan to the person, and the person to the population. */
			vehicle.addPlan(plan);
			sc.getPopulation().addPerson(vehicle);
		}
	}
	
	

}
