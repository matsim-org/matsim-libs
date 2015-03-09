/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.utils.CollectionUtils;

/**
 * @author johannes
 *
 */
public class MergeFacilitiesToLink {

	private static final Logger logger = Logger.getLogger(MergeFacilitiesToLink.class);
	
	private static final int numThreads=20;
	
	public static void main(String args[]) {
		Map<String, ActivityFacilities> facMap = new HashMap<String, ActivityFacilities>();
		
		
		for(int i = 0; i < args.length - 2; i++) {
			String filename = new File(args[i]).getName();
			String type = filename.split("\\.")[1];
		
			logger.info("Loading facilitites... " + type);
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
			
			facReader.readFile(args[i]);
			
			ActivityFacilities facilities = scenario.getActivityFacilities();
			
			facMap.put(type, facilities);
		}
		
		logger.info("Adding activity options...");
		for(Entry<String, ActivityFacilities> entry : facMap.entrySet()) {
			for(ActivityFacility fac : entry.getValue().getFacilities().values()) {
				fac.addActivityOption(entry.getValue().getFactory().createActivityOption(entry.getKey()));
			}
		}
		
		logger.info("Loading network...");
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(args[args.length - 2]);
		Network network = scenario.getNetwork();
		
		logger.info("Connecting facilities to links...");
		Map<Link, Set<ActivityFacility>> fac2link = linkFacilities(facMap, network);
		
		logger.info("Merging facilities...");
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		merge(facilities, fac2link);
		
		logger.info("Writing facilities...");
		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(args[args.length - 1]);
		
		logger.info("Done.");
	}
	
	private static Map<Link, Set<ActivityFacility>> linkFacilities(Map<String, ActivityFacilities> facMap, Network network) {
		Map<Link, Set<ActivityFacility>> fac2link = new HashMap<Link, Set<ActivityFacility>>();
				
		for(Entry<String, ActivityFacilities> entry : facMap.entrySet()) {
			logger.info("Connecting facilities..." + entry.getKey());
			
			ActivityFacilities facilities = entry.getValue();
			
			ProgressLogger.init(facilities.getFacilities().size(), 1, 10);
			
			ExecutorService executor = Executors.newFixedThreadPool(numThreads);
			
			LinkThread[] threads = new LinkThread[numThreads];
			for(int i = 0; i < numThreads; i++)
				threads[i] = new LinkThread();
			
			Future<?>[] futures = new Future[numThreads];
			/*
			 * split collection in approx even segments
			 */
			int n = Math.min(facilities.getFacilities().size(), threads.length);
			List<? extends ActivityFacility>[] segments = CollectionUtils.split(facilities.getFacilities().values(), n);
			/*
			 * submit tasks
			 */
			for(int i = 0; i < segments.length; i++) {
				threads[i].fac2link = fac2link;
				threads[i].network = network;
				threads[i].facilities = segments[i];
				
				futures[i] = executor.submit(threads[i]);
			}
			/*
			 * wait for threads
			 */
			for(int i = 0; i < segments.length; i++) {
				try {
					futures[i].get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		
		return fac2link;
	}
	
	private static class LinkThread implements Runnable {

		private Network network;
		
		private Collection<? extends ActivityFacility> facilities;
		
		private Map<Link, Set<ActivityFacility>> fac2link;
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			for (ActivityFacility facility : facilities) {
				Coord c = facility.getCoord();
				Link link = ((NetworkImpl) network).getNearestLinkExactly(c);
				putEntry(fac2link, link, facility);
				ProgressLogger.step();
			}
		}
	}
	
	private static synchronized void putEntry(Map<Link, Set<ActivityFacility>> fac2link, Link link, ActivityFacility facility) {
		Set<ActivityFacility> linkFacs = fac2link.get(link);
		if (linkFacs == null) {
			linkFacs = new HashSet<ActivityFacility>();
			fac2link.put(link, linkFacs);
		}
		linkFacs.add(facility);
	}
	
	private static void merge(ActivityFacilities facilities, Map<Link, Set<ActivityFacility>> fac2Link) {
		ProgressLogger.init(fac2Link.size(), 1, 10);
		long idCounter = 0;
		for(Entry<Link, Set<ActivityFacility>> entry : fac2Link.entrySet()) {
			Link link = entry.getKey();
			ActivityFacility newFac = facilities.getFactory().createActivityFacility(Id.create(idCounter++, ActivityFacility.class), link.getCoord());
			facilities.addActivityFacility(newFac);
			
			for(ActivityFacility origFac : entry.getValue()) {
				String type = origFac.getActivityOptions().keySet().iterator().next();
				ActivityOption opt = newFac.getActivityOptions().get(type);
				if(opt == null) {
					opt = facilities.getFactory().createActivityOption(type);
					newFac.addActivityOption(opt);
					opt.setCapacity(1);
				} else {
					opt.setCapacity(opt.getCapacity() + 1);
				}
			}
			ProgressLogger.step();
		}
	}
}
