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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

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
	
	private PathDependentNetwork complexNetwork;
	
	private Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FreightChainGenerator.class.toString(), args);
		
		/* Parse all the arguments. */
		String complexNetworkFile = args[0];
		int numberOfPlans = Integer.parseInt(args[1]);
		String populationPrefix = args[2];
		String outputPlansFile = args[3];
		String attributeFile = args[4];
		int numberOfThreads = Integer.parseInt(args[5]);
		
		/* Read the path-dependent complex network. */
		DigicorePathDependentNetworkReader_v1 nr = new DigicorePathDependentNetworkReader_v1();
		nr.parse(complexNetworkFile);
		PathDependentNetwork pathDependentNetwork = nr.getPathDependentNetwork();
		pathDependentNetwork.writeNetworkStatisticsToConsole();
		
		/* Set up the freight chain generator. */
		FreightChainGenerator fcg = new FreightChainGenerator(pathDependentNetwork);
		fcg.generateFreightAgentPlans(pathDependentNetwork, populationPrefix, numberOfPlans, numberOfThreads);
		
		/* Write the population to file. */
		new PopulationWriter(fcg.getScenario().getPopulation(), null).write(outputPlansFile);
		
		/* Write the person attributes to file. */
		new ObjectAttributesXmlWriter(fcg.getScenario().getPopulation().getPersonAttributes()).writeFile(attributeFile);
		
		Header.printFooter();
	}
	
	
	public FreightChainGenerator(PathDependentNetwork network) {
		this.complexNetwork = network;
	}
	
	
	private void generateFreightAgentPlans(PathDependentNetwork network, String prefix, int numberOfPlans, int numberOfThreads){
		LOG.info("Creating a population of " + numberOfPlans + " freight activity chains...");
		
		/* Set up the multithreaded infrastructure. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<Plan>> listOfJobs = new ArrayList<Future<Plan>>();
		
		LOG.info("Creating plans using multi-threaded infrastructure.");
		Counter counter = new Counter("   plans # ");
		for(int i = 0; i < numberOfPlans; i++){
			DigicorePlanGenerator job = new DigicorePlanGenerator(this.complexNetwork, counter);
			Future<Plan> submit = threadExecutor.submit(job);
			listOfJobs.add(submit);
		}

		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();

		/* Aggregate the output plans. */
		PopulationFactory pf = sc.getPopulation().getFactory();
		try {
			int i = 0;
			for(Future<Plan> job : listOfJobs){
				Person vehicle = pf.createPerson( Id.create(prefix + "_" + i++, Person.class) );
				Plan plan;
				plan = job.get();

				/* Add the plan to the person, and the person to the population. */
				vehicle.addPlan(plan);
				sc.getPopulation().addPerson(vehicle);

				/* Indicate the subpopulation. */
				sc.getPopulation().getPersonAttributes().putAttribute(
						vehicle.getId().toString(), 
						sc.getConfig().plans().getSubpopulationAttributeName(), 
						"commercial"
				);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not aggregate multi-threaded plan output.");
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not aggregate multi-threaded plan output.");
		}
	}


	
	public Scenario getScenario(){
		return this.sc;
	}
	
	
	/**
	 * A private class the generate {@link Plan}s in a multi-threaded manner. 
	 *
	 * @author jwjoubert
	 */
	private class DigicorePlanGenerator implements Callable<Plan>{
		private final PathDependentNetwork network;
		private final Counter counter;
		
		/**
		 * @param network a path-dependent complex network as generated by the code
		 * 	      {@link PathDependentNetwork}.
		 */
		public DigicorePlanGenerator(PathDependentNetwork network, Counter counter) {
			this.network = network;
			this.counter = counter;
		}

		@Override
		public Plan call() throws Exception {
			PopulationFactory pf = sc.getPopulation().getFactory();
			
			/* Set up the plan. */
			PlanImpl plan = (PlanImpl) pf.createPlan();
			
			/* Generate the first activity. */
			Id<Node> previousId = Id.create("source", Node.class);
			Id<Node> currentId = network.sampleChainStartNode();
			Id<Node> nextId = network.getPathDependentNode(currentId).sampleBiasedNextPathDependentNode(previousId);
			Coord coord = network.getPathDependentNode(currentId).getCoord();
			ActivityImpl activity = new ActivityImpl("major", coord);
			activity.setFacilityId(Id.create(currentId.toString(), ActivityFacility.class));
			activity.setEndTime(ChainStartTime.getStartTimeInSeconds(RANDOM.nextDouble()));
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
				previousId = Id.create(currentId.toString(), Node.class);
				currentId = Id.create(nextId.toString(), Node.class);
				chainLength++;
				
				/* Generate the next node, given the current node. */
				nextId = network.getPathDependentNode(currentId).sampleBiasedNextPathDependentNode(previousId);
				String activityType = null;
				
				/*Uncomment the next line if you wish to enforce a maximum chain length when generating the chains.*/
//				if(chainLength == MAX_CHAIN_LENGTH || nextId == null || nextId.toString().equalsIgnoreCase("sink")){
				if(nextId == null || nextId.toString().equalsIgnoreCase("sink")){
					activityType = "major";
					chainEnd = true;
				} else{
					activityType = "minor";
				}
				
				/* Create and add the activity from the identified node. */
				Coord thisCoord = network.getPathDependentNode(currentId).getCoord();
				ActivityImpl thisActivity = new ActivityImpl(activityType, thisCoord);
				thisActivity.setFacilityId(Id.create(currentId, ActivityFacility.class));
				
				double duration = ActivityDuration.getDurationInSeconds(RANDOM.nextDouble());

				// FIXME Is this right i.t.o. last major activity?
				if(activityType.equalsIgnoreCase("minor")){
					thisActivity.setMaximumDuration(duration);
				}
				
				plan.addActivity(thisActivity);
				
			}
			counter.incCounter();
			return plan;
		}
		
	}
	
	

}
