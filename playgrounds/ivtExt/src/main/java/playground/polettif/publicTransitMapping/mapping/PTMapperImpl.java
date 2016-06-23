/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.mapping;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidateCreator;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidateCreatorStandard;
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.PseudoSchedule;
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.PseudoScheduleImpl;
import playground.polettif.publicTransitMapping.mapping.networkRouter.FastAStarRouter;
import playground.polettif.publicTransitMapping.mapping.networkRouter.Router;
import playground.polettif.publicTransitMapping.plausibility.StopFacilityHistogram;
import playground.polettif.publicTransitMapping.tools.MiscUtils;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.File;
import java.util.*;

/**
 * References an unmapped transit schedule to a network. Combines
 * routing of transit routes and referencing stopFacilities. Additional
 * stop facilities are created if a stopFacility has more than one
 * plausible link. Artificial links are added to the network if no
 * route can be found.
 *
 * @author polettif
 */
public class PTMapperImpl extends PTMapper {

	private final Map<String, Router> modeSeparatedRouters = new HashMap<>();

	public PTMapperImpl(PublicTransitMappingConfigGroup config, TransitSchedule schedule, Network network) {
		super(config, schedule, network);
	}

	public PTMapperImpl(String configPath) {
		super(configPath);
	}

	@Override
	public void run() {
		if(schedule == null) {
			throw new RuntimeException("No schedule defined!");
		} else if(network == null) {
			throw new RuntimeException("No network defined!");
		}

		setLogLevels();
		config.loadParameterSets();

		log.info("======================================");
		log.info("Mapping transit schedule to network...");

		/**
		 * Some schedule statistics
 		 */
		Set<String> scheduleTransportModes = new HashSet<>();
		Map<TransitLine, Integer> totalStopsPerTransitLine = new HashMap<>();
		int nStopFacilities = schedule.getFacilities().size();
		int nTransitRoutes = 0;
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				scheduleTransportModes.add(transitRoute.getTransportMode());
				MapUtils.addToInteger(transitLine, totalStopsPerTransitLine, transitRoute.getStops().size(), transitRoute.getStops().size());
				nTransitRoutes++;
			}
		}


		/** [1]
		 * Create a separate network for all schedule modes and
		 * initiate routers.
		 */
		log.info("===========================================");
		log.info("Creating mode separated network and routers");
		Map<String, Set<String>> modeRoutingAssignment = config.getModeRoutingAssignment();
		FastAStarRouter.setTravelCostType(config.getTravelCostType());
		for(String scheduleMode : scheduleTransportModes) {
			log.info("Initiating network and router for schedule mode \"" +scheduleMode+"\", network modes " + modeRoutingAssignment.get(scheduleMode));
			modeSeparatedRouters.put(scheduleMode, FastAStarRouter.createModeSeparatedRouter(network, modeRoutingAssignment.get(scheduleMode)));
		}

		/** [2]
		 * Load the closest links and create LinkCandidates. StopFacilities
		 * with no links within search radius are given a dummy loop link right
		 * on their coordinates. Each Link Candidate is a possible new stop facility
		 * after PseudoRouting.
		 */
		log.info("===========================");
		log.info("Creating link candidates...");
		LinkCandidateCreator linkCandidates = new LinkCandidateCreatorStandard(this.schedule, this.network, this.config, this.modeSeparatedRouters);
		linkCandidates.createLinkCandidates();

		/** [3]
		 * PseudoRouting
		 * Initiate and start threads, calculate PseudoTransitRoutes
		 * for all transit routes.
		 */
		log.info("==================================");
		log.info("Calculating pseudoTransitRoutes... ("+nTransitRoutes+" transit routes in "+schedule.getTransitLines().size()+" transit lines)");

		final PseudoSchedule pseudoSchedule = new PseudoScheduleImpl();

		// initiate pseudoRouting
		int numThreads = config.getNumOfThreads() > 0 ? config.getNumOfThreads() : 1;
		PseudoRouting[] pseudoRoutingThreads = new PseudoRouting[numThreads];
		for(int i = 0; i < numThreads; i++) {
			pseudoRoutingThreads[i] = new PseudoRoutingImpl(config, modeSeparatedRouters, linkCandidates);
		}
		// spread transit lines on threads
		int thr = 0;
		for(TransitLine transitLine : new LinkedList<>(MiscUtils.sortDescendingByValue(totalStopsPerTransitLine).keySet())) {
			pseudoRoutingThreads[thr++ % numThreads].addTransitLineToQueue(transitLine);
		}

		Thread[] threads = new Thread[numThreads];
		// start pseudoRouting
		for(int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(pseudoRoutingThreads[i]);
			threads[i].start();
		}
		for(Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		/** [4]
		 * Collect artificial links from threas and add them to network.
		 * Collect pseudoSchedules from threads.
		 */
		log.info("=====================================");
		log.info("Adding artificial links to network...");
		for(PseudoRouting prt : pseudoRoutingThreads) {
			prt.addArtificialLinks(network);
			pseudoSchedule.mergePseudoSchedule(prt.getPseudoSchedule());
		}


		/** [5]
		 * Replace the parent stop facilities in each transitRoute's routeProfile
		 * with child StopFacilities. Add the new transitRoutes to the schedule.
		 */
		log.info("========================================================================");
		log.info("Replacing parent StopFacilities in schedule with child StopFacilities...");
		pseudoSchedule.createAndReplaceFacilities(schedule);

		/** [6]
		 * The final routing should be done on the merged network, so a mode dependent
		 * router for each schedule mode is initialized using the same merged network.
		 */
		log.info("===========================================================================================");
		log.info("Initiating final routers to map transit routes with referenced facilities to the network...");
		Map<String, Router> finalRouters = new HashMap<>();
		for(String scheduleMode : scheduleTransportModes) {
			Set<String> routingTransportModes = new HashSet<>(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE_AS_SET);
			if(modeRoutingAssignment.get(scheduleMode) != null) routingTransportModes.addAll(modeRoutingAssignment.get(scheduleMode));
			log.info("Initiating network and router for schedule mode \"" +scheduleMode+"\", network modes " + routingTransportModes);

			finalRouters.put(scheduleMode, FastAStarRouter.createModeSeparatedRouter(network, routingTransportModes));
		}


		/** [7]
		 * Route all transitRoutes with the new referenced links. The shortest path
		 * between child stopFacilities is calculated and added to the schedule.
		 */
		log.info("=============================================");
		log.info("Creating link sequences for transit routes...");
		ScheduleTools.routeSchedule(this.schedule, this.network, finalRouters);

		/** [8]
		 * Now that all lines have been routed, it is possible that a route passes
		 * a link closer to a stop facility than its referenced link.
		 */
		PTMapperUtils.pullChildStopFacilitiesTogether(this.schedule, this.network);

		/** [9]
		 * After all lines are created, clean the schedule and network. Removing
		 * not used transit links includes removing artificial links that
		 * needed to be added to the network for routing purposes.
		 */
		log.info("=============================");
		log.info("Clean schedule and network...");
		cleanScheduleAndNetwork();

		/** [10]
		 * Validate the schedule
		 */
		log.info("======================");
		log.info("Validating schedule...");
		printValidateSchedule();

		/** [11]
		 * Write output files if defined in config
		 */
		writeOutputFiles();

		log.info("==================================================");
		log.info("= Mapping transit schedule to network completed! =");
		log.info("==================================================");

		/**
		 * Statistics
		 */
		printStatistics(nStopFacilities);
	}

	private void cleanScheduleAndNetwork() {
		// changing the freespeed of the artificial links (value is used in simulations)
		NetworkTools.resetLinkLength(network, PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE);
		PTMapperUtils.setFreeSpeedBasedOnSchedule(network, schedule, config.getScheduleFreespeedModes());

		// Remove unnecessary parts of schedule
		ScheduleCleaner.removeNotUsedTransitLinks(schedule, network, config.getModesToKeepOnCleanUp(), true);
		if(config.getRemoveNotUsedStopFacilities()) ScheduleCleaner.removeNotUsedStopFacilities(schedule);

		// change the network transport modes
		ScheduleTools.assignScheduleModesToLinks(schedule, network);
		if(config.getCombinePtModes()) {
			NetworkTools.replaceNonCarModesWithPT(network);
		} else if(config.getAddPtMode()) {
			ScheduleTools.addPTModeToNetwork(schedule, network);
		}
	}

	/**
	 * Write the schedule and network to output files (if defined in config)
	 */
	private void writeOutputFiles() {
		if(config.getOutputNetworkFile() != null && config.getOutputScheduleFile() != null) {
			log.info("Writing schedule and network to file...");
			try {
				ScheduleTools.writeTransitSchedule(schedule, config.getOutputScheduleFile());
				NetworkTools.writeNetwork(network, config.getOutputNetworkFile());
			} catch (Exception e) {
				log.error("Cannot write to output directory! Trying to write schedule and network file in working directory");
				long t = System.nanoTime() / 1000000;
				try {
					ScheduleTools.writeTransitSchedule(schedule, t + "schedule.xml.gz");
					NetworkTools.writeNetwork(network, t + "network.xml.gz");
				} catch (Exception e1) {
					throw new RuntimeException("Files could not be written in working directory");
				}
			}
			if(config.getOutputStreetNetworkFile() != null) {
				NetworkTools.writeNetwork(NetworkTools.filterNetworkByLinkMode(network, Collections.singleton(TransportMode.car)), config.getOutputStreetNetworkFile());
			}
		} else {
			log.info("");
			log.info("No output paths defined, schedule and network are not written to files.");
		}
	}

	/**
	 * Log the result of the schedule validator
	 */
	private void printValidateSchedule() {
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(schedule, network);
		if(validationResult.isValid()) {
			log.info("Schedule appears valid!");
		} else {
			log.warn("Schedule is NOT valid!");
		}
		if(validationResult.getErrors().size() > 0) {
			log.info("Validation errors:");
			for(String e : validationResult.getErrors()) {
				log.info(e);
			}
		}
		if(validationResult.getWarnings().size() > 0) {
			log.info("Validation warnings:");
			for(String w : validationResult.getWarnings()) {
				log.info(w);
			}
		}
	}

	/**
	 * Print some basic mapping statistics.
	 */
	private void printStatistics(int inputNStopFacilities) {
		int nArtificialLinks = 0;
		for(Link l : network.getLinks().values()) {
			if(l.getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				nArtificialLinks++;
			}
		}
		int withoutArtificialLinks = 0;
		int nRoutes = 0;
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				nRoutes++;

				boolean noArtificial = true;
				List<Id<Link>> linkIds = ScheduleTools.getTransitRouteLinkIds(transitRoute);
				for(Id<Link> linkId : linkIds) {
					if(!network.getLinks().get(linkId).getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
						noArtificial = false;
					}
				}
				if(noArtificial) {
					withoutArtificialLinks++;
				}
			}
		}

		StopFacilityHistogram histogram = new StopFacilityHistogram(schedule);

		log.info("");
		log.info("    Artificial Links:");
		log.info("       created  " + nArtificialLinks);
		log.info("    Stop Facilities:");
		log.info("       total input   " + inputNStopFacilities);
		log.info("       total output  " + schedule.getFacilities().size());
		log.info("       diff.         " + (schedule.getFacilities().size() - inputNStopFacilities));
		log.info("    Child Stop Facilities:");
		log.info("       median nr created   " + String.format("%.0f", histogram.median()));
		log.info("       average nr created  " + String.format("%.2f", histogram.average()));
		log.info("       max nr created      " + String.format("%.0f", histogram.max()));
		log.info("    Transit Routes:");
		log.info("       total routes in schedule         " + nRoutes);
		log.info("       routes without artificial links  " + withoutArtificialLinks);
		log.info("");
		log.info("    Run PlausibilityCheck for further analysis");
		log.info("");
		log.info("==================================================");
	}

	private static void setLogLevels() {
		Logger.getLogger(org.matsim.core.router.Dijkstra.class).setLevel(Level.ERROR); // suppress no route found warnings
		Logger.getLogger(org.matsim.core.network.NetworkImpl.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.network.filter.NetworkFilterManager.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessDijkstra.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessDijkstra.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessEuclidean.class).setLevel(Level.WARN);
		Logger.getLogger(org.matsim.core.router.util.PreProcessLandmarks.class).setLevel(Level.WARN);
	}
}
