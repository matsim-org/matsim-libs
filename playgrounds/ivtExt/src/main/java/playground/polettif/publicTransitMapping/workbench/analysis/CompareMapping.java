/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.workbench.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.boescpa.converters.osm.ptMapping.PTMapperOnlyBusses;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.gtfs.Gtfs2TransitSchedule;
import playground.polettif.publicTransitMapping.mapping.PTMapperImpl;
import playground.polettif.publicTransitMapping.plausibility.StopFacilityHistogram;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleShapeFileWriter;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CompareMapping {

	static String ct = "EPSG:2056";
	static Map<String, Set<Id<TransitStopFacility>>> parentStops;

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		String inputGtfs = "data/gtfs/zvv/";
		String serviceParam = Gtfs2TransitSchedule.ServiceParam.dayWithMostServices.toString();
		String fullUnmappedMTS = "data/mts/fromGtfs/zvv_"+serviceParam+".xml.gz";
		String singleUnmappedMTS = "data/mts/fromGtfs/zvv_"+serviceParam+"_bus.xml.gz";
		String combinedUnmappedMTS = "data/mts/fromGtfs/zvv_"+serviceParam+"_bus_combined.xml.gz";

		String detailNetworkFile = "data/network/mm/zurich_detail.xml.gz";
		String combinedNetworkFile = "data/network/mm/zurich.xml.gz";
		String output = args[0];

		/**
		 * Stop Histogram hafas
		 */
//		TransitSchedule mappedHafas = ScheduleTools.readTransitSchedule("output/2016-06-24/ch_schedule.xml.gz");
//		StopFacilityHistogram hafasHist = new StopFacilityHistogram(mappedHafas);
//		hafasHist.createCsv(output+"hafas_stopFacilities.csv");

		/**
		 * Convert GTFS to unmapped schedule
		 */
// 		Gtfs2TransitSchedule.run(inputGtfs, serviceParam, ct, fullUnmappedMTS, null, args[1]);

		// single
		TransitSchedule s = ScheduleTools.readTransitSchedule(fullUnmappedMTS);
		removeAllRoutesExceptBus(s);
		ScheduleCleaner.removeNotUsedStopFacilities(s);
		ScheduleTools.writeTransitSchedule(s, singleUnmappedMTS);

		/**
		 * Combine stop locations
		 */
		// combine
		TransitSchedule scheduleParentStopsForHist = ScheduleTools.readTransitSchedule(fullUnmappedMTS);
		combineChildStops(scheduleParentStopsForHist);
		StopFacilityHistogram gtfsHist = new StopFacilityHistogram(scheduleParentStopsForHist);
		gtfsHist.createCsv(output+"gtfs_original_stopFacilities.csv");
		gtfsHist.createPng(output+"gtfs_original_stopFacilities.png");
		TransitSchedule scheduleParentStops = ScheduleTools.readTransitSchedule(fullUnmappedMTS);
		setParentStopAsRouteStop(scheduleParentStops);
		ScheduleTools.writeTransitSchedule(scheduleParentStops, combinedUnmappedMTS);

		/**
		 * Run PTMapper
		 */
		runPolettif(output+"single/", singleUnmappedMTS, detailNetworkFile, 6, 40);
		runPolettif(output+"combined/", combinedUnmappedMTS, detailNetworkFile, 15, 80);

		/**
		 * boescpa
		 */
		runBoesch(output+"boesch/single/", singleUnmappedMTS, detailNetworkFile);
		runBoesch(output+"boesch/combined/", combinedUnmappedMTS, detailNetworkFile);
	}

	private static void runBoesch(String output, String scheduleFile, String networkFile) {
		TransitSchedule schedule2 = ScheduleTools.readTransitSchedule(scheduleFile);
		Network network2 = NetworkTools.readNetwork(networkFile);

		PTMapperOnlyBusses boescpa = new PTMapperOnlyBusses(schedule2);
		boescpa.routePTLines(network2);

		ScheduleTools.writeTransitSchedule(schedule2, output+"boesch_schedule.xml.gz");
		NetworkTools.writeNetwork(network2, output+"boesch_network.xml.gz");
		ScheduleShapeFileWriter.run(schedule2, network2, ct, output);
	}

	private static void runPolettif(String output, String scheduleFile, String networkFile, int maxNLinks, double maxDistance) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(scheduleFile);
		Network network = NetworkTools.readNetwork(networkFile);

		PublicTransitMappingConfigGroup ptmConfig = PublicTransitMappingConfigGroup.createDefaultConfig();
		ptmConfig.setTravelCostType(PublicTransitMappingConfigGroup.TravelCostType.travelTime);
		PublicTransitMappingConfigGroup.LinkCandidateCreatorParams lccParamBus = new PublicTransitMappingConfigGroup.LinkCandidateCreatorParams("bus");
		lccParamBus.setNetworkModesStr("car,bus");
		lccParamBus.setMaxNClosestLinks(maxNLinks);
		lccParamBus.setLinkDistanceTolerance(1.5);
		lccParamBus.setMaxLinkCandidateDistance(maxDistance);
		ptmConfig.addParameterSet(lccParamBus);
		ptmConfig.setNumOfThreads(4);
		ptmConfig.setMaxTravelCostFactor(8);


		PublicTransitMappingConfigGroup.ModeRoutingAssignment mra = new PublicTransitMappingConfigGroup.ModeRoutingAssignment("bus");
		mra.setNetworkModesStr("bus,car");
		ptmConfig.addParameterSet(mra);

		ptmConfig.setOutputNetworkFile(output + "ptm_network.xml.gz");
		ptmConfig.setOutputScheduleFile(output + "ptm_schedule.xml.gz");

		new PTMapperImpl(ptmConfig, schedule, network).run();

		// shapeFile
		ScheduleShapeFileWriter.run(schedule, network, ct, output);

		// stop facilities histogram
		StopFacilityHistogram histogram = new StopFacilityHistogram(schedule);
		histogram.createCsv(output+"stopFacilities.csv");
		histogram.createPng(output+"stopFacilities.png");
	}

	/**
	 * combines child stops back to the parent stop from gtfs
	 */
	private static void combineChildStops(TransitSchedule schedule) {
		parentStops = new HashMap<>();

		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			if(stopFacility.getId().toString().contains("Parent")) {
				MapUtils.getSet(stopFacility.getName(), parentStops).add(stopFacility.getId());
			}
		}
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop routeStop : transitRoute.getStops()) {
					TransitStopFacility sf = routeStop.getStopFacility();
					Set<Id<TransitStopFacility>> possibleParentStopId;
					try {
						possibleParentStopId = parentStops.get(sf.getName().split(", ")[1]);
					} catch (Exception e) {
						possibleParentStopId = parentStops.get(sf.getName());
					}
					if(possibleParentStopId != null) {
						TransitStopFacility parentStopFacility = null;

						// get closer parent stop
						double minDist = Double.MAX_VALUE;
						for(Id<TransitStopFacility> id : possibleParentStopId) {
							TransitStopFacility checkParent = schedule.getFacilities().get(id);
							double dist = CoordUtils.calcEuclideanDistance(checkParent.getCoord(), sf.getCoord());
							if(dist < minDist) {
								parentStopFacility = checkParent;
								minDist = dist;
							}
						}
						if(parentStopFacility != null) {
							Id<TransitStopFacility> childStopId = Id.create(parentStopFacility.getId() + ".link:" + sf.getId(), TransitStopFacility.class);
							TransitStopFacility childStop = schedule.getFactory().createTransitStopFacility(childStopId, sf.getCoord(), false);
							if(!schedule.getFacilities().containsKey(childStopId)) {
								schedule.addStopFacility(childStop);
							}
							routeStop.setStopFacility(childStop);
						}
					}
				}
			}
		}
		removeAllRoutesExceptBus(schedule);
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
	}

	private static void setParentStopAsRouteStop(TransitSchedule schedule) {
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop routeStop : transitRoute.getStops()) {
					TransitStopFacility sf = routeStop.getStopFacility();
					Set<Id<TransitStopFacility>> possibleParentStopId;
					try {
						possibleParentStopId = parentStops.get(sf.getName().split(", ")[1]);
					} catch (Exception e) {
						possibleParentStopId = parentStops.get(sf.getName());
					}
					if(possibleParentStopId != null) {
						TransitStopFacility parentStopFacility = null;

						// get closer parent stop
						double minDist = Double.MAX_VALUE;
						for(Id<TransitStopFacility> id : possibleParentStopId) {
							TransitStopFacility checkParent = schedule.getFacilities().get(id);
							double dist = CoordUtils.calcEuclideanDistance(checkParent.getCoord(), sf.getCoord());
							if(dist < minDist) {
								parentStopFacility = checkParent;
								minDist = dist;
							}
						}
						if(parentStopFacility != null) {
							routeStop.setStopFacility(parentStopFacility);
						}
					}
				}
			}
		}
		removeAllRoutesExceptBus(schedule);
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
	}

	private static void removeAllRoutesExceptBus(TransitSchedule scheduleToClean) {
		Set<String> modesToRemove = new HashSet<>();
		modesToRemove.add("rail");
		modesToRemove.add("tram");
		modesToRemove.add("gondola");
		modesToRemove.add("funicular");
		modesToRemove.add("ferry");
		ScheduleCleaner.removeTransitRouteByMode(scheduleToClean, modesToRemove);
	}
}
