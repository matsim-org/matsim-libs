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
package playground.southafrica.freight.digicore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.algorithms.postclustering.ClusteredChainGenerator;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreChainElement;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesWriter;
import playground.southafrica.utilities.Header;

/**
 * Class to read the {@link DigicoreVehicles} and adapt each vehicle's activity 
 * chains ({@link DigicoreChain}s) by merging all consecutive activities 
 * ({@link DigicoreActivity}) that occur at the same facility, i.e. having the 
 * same facility {@link Id}. The output is written to a new (given) 
 * {@link DigicoreVehicles} container.
 * 
 * <br><br><b>Note:</b> Vehicle activities will only be associated with facilities
 * if the class {@link ClusteredChainGenerator} has been executed. That is, this
 * class must be run on the output vehicles container which, in turn, is 
 * produced by the class {@link ClusteredChainGenerator}.
 * 
 * @author jwjoubert
 */
public class DigicoreChainCleaner {
	final private static Logger LOG = Logger.getLogger(DigicoreChainCleaner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(DigicoreChainCleaner.class.toString(), args);
		String inputVehiclesFile = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);		
		String outputVehiclesFile = args[2];

		/* Read the vehicles container. */
		DigicoreVehicles vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(inputVehiclesFile);
		
		/* Execute the multi-threaded jobs */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<DigicoreVehicle>> listOfJobs = new ArrayList<>(vehicles.getVehicles().size());
		Counter threadCounter = new Counter("   vehicles completed: ");
		
		for(DigicoreVehicle vehicle : vehicles.getVehicles().values()){
			Callable<DigicoreVehicle> job = new CallableChainCleaner(vehicle, threadCounter);
			Future<DigicoreVehicle> submit = threadExecutor.submit(job);
			listOfJobs.add(submit);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		threadCounter.printCounter();

		/* Consolidate output. */
		LOG.info("Consolidate cleaned vehicles into single DigicoreVehicles container.");
		DigicoreVehicles newVehicles = new DigicoreVehicles(vehicles.getCoordinateReferenceSystem());
		String oldDescription = vehicles.getDescription();
		if(oldDescription == null){
			oldDescription = "";
		} else{
			oldDescription += oldDescription.endsWith(".") ? " " : ". ";
		}
		oldDescription += "Cleaned consecutive activities at same facility.";
		newVehicles.setDescription(oldDescription);
		
		for(Future<DigicoreVehicle> future : listOfJobs){
			DigicoreVehicle vehicle;
			try {
				vehicle = future.get();
				if(vehicle != null){
					newVehicles.addDigicoreVehicle(vehicle);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not get DigicoreVehicle after multithreaded run.");
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not get DigicoreVehicle after multithreaded run.");
			}
		}
		
		new DigicoreVehiclesWriter(newVehicles).write(outputVehiclesFile);
		
		Header.printFooter();
	}
	
	
	public static class CallableChainCleaner implements Callable<DigicoreVehicle> {
		private final DigicoreVehicle vehicle;
		private Counter counter;
		private int numberOfActivitiesChanged = 0;
		
		public CallableChainCleaner(DigicoreVehicle vehicle, Counter threadCounter) {
			this.vehicle = vehicle;
			this.counter = threadCounter;
		}

		@Override
		public DigicoreVehicle call() throws Exception {
			int chainIndex = 0;
			while(chainIndex < vehicle.getChains().size()){
				DigicoreChain chain = vehicle.getChains().get(chainIndex);
				
				cleanChain(chain);
				
				if(chain.size() < 2){
					/* It no longer is a valid chain, remove it. */
					vehicle.getChains().remove(chainIndex);
				} else{
					/* Move to the next chain. */
					chainIndex++;
				}
			}
			
			counter.incCounter();

			/* Write the vehicle to file, if it has at least one chain. */
			if(vehicle.getChains().size() > 0){
				LOG.info("   ==> " + vehicle.getId().toString() + " -> " + numberOfActivitiesChanged);
				return vehicle;
			} else{
				return null;
			}
		}

		/**
		 * This method has been revised in January 2017 to also account for 
		 * the {@link DigicoreTrace}s that are between two consecutive 
		 * {@link DigicoreActivity}s.
		 * 
		 * @param chain
		 * @return
		 */
		public DigicoreChain cleanChain(DigicoreChain chain){
			int activityIndex = 0;
			List<DigicoreChainElement> elements = chain;
			while(activityIndex < elements.size()-1){
				DigicoreActivity thisActivity = null;
				if(elements.get(activityIndex) instanceof DigicoreActivity){
					thisActivity = (DigicoreActivity) elements.get(activityIndex);
				}
				
				DigicoreActivity nextActivity = null;
				if(elements.get(activityIndex+2) instanceof DigicoreActivity){
					nextActivity = (DigicoreActivity) elements.get(activityIndex+2);
				}
				
				if( thisActivity.getFacilityId() != null &&
						nextActivity.getFacilityId() != null &&
						thisActivity.getFacilityId().toString().equalsIgnoreCase(nextActivity.getFacilityId().toString()) ){
					/* Merge the two activities. */
					numberOfActivitiesChanged++;
					thisActivity.setEndTime( nextActivity.getEndTime() );
					chain.remove( activityIndex + 1 ); /* Remove the trace. */
					chain.remove( activityIndex + 1 ); /* Remove the subsequent activity. */
					
				} else{
					/* Step over the current activity and the subsequent trace. */
					activityIndex += 2;
				}
			}
			return chain;
		}

	} 
	
	

}
