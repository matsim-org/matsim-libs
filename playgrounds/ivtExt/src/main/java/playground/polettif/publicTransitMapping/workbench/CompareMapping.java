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

package playground.polettif.publicTransitMapping.workbench;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.gtfs.Gtfs2TransitSchedule;
import playground.polettif.publicTransitMapping.mapping.PTMapperImpl;
import playground.polettif.publicTransitMapping.plausibility.StopFacilityHistogram;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleShapeFileWriter;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;
//import playground.polettif.boescpa.converters.osm.ptMapping.PTMapperOnlyBusses;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CompareMapping {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		String inputGtfs = "data/gtfs/zvv/";
		String ct = "EPSG:2056";
		String unmappedMTS = "data/mts/fromGtfs/zvv_mostServices.xml.gz";
		String filteredUnmappedMTS = "data/mts/fromGtfs/zvv_mostServices_bus.xml.gz";
		String networkFile = "data/network/mm/zurich_detail.xml.gz";
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
// 		Gtfs2TransitSchedule.run(inputGtfs, Gtfs2TransitSchedule.ServiceParam.dayWithMostServices.toString(), ct, unmappedMTS, null, output+"gtfs.shp");

		/**
		 * Run PTMapper
		 */
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(unmappedMTS);
		Network network = NetworkTools.readNetwork(networkFile);

		// cleanup
		Set<String> modesToRemove = new HashSet<>();
		modesToRemove.add("rail");
		modesToRemove.add("tram");
		modesToRemove.add("gondola");
		modesToRemove.add("funicular");
		modesToRemove.add("ferry");
		ScheduleCleaner.removeTransitRouteByMode(schedule, modesToRemove);
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleTools.writeTransitSchedule(schedule, filteredUnmappedMTS);

		// ptm
		PublicTransitMappingConfigGroup ptmConfig = PublicTransitMappingConfigGroup.createDefaultConfig();
		ptmConfig.setTravelCostType(PublicTransitMappingConfigGroup.TravelCostType.travelTime);
		PublicTransitMappingConfigGroup.LinkCandidateCreatorParams lccParamBus = new PublicTransitMappingConfigGroup.LinkCandidateCreatorParams("bus");
		lccParamBus.setNetworkModesStr("car,bus");
		lccParamBus.setMaxNClosestLinks(4);
		lccParamBus.setLinkDistanceTolerance(1.5);
		lccParamBus.setMaxLinkCandidateDistance(40);
		ptmConfig.addParameterSet(lccParamBus);
		ptmConfig.setNumOfThreads(4);
		ptmConfig.setMaxTravelCostFactor(8);


		PublicTransitMappingConfigGroup.ModeRoutingAssignment mra = new PublicTransitMappingConfigGroup.ModeRoutingAssignment("bus");
		mra.setNetworkModesStr("bus,car");
		ptmConfig.addParameterSet(mra);

		ptmConfig.setOutputNetworkFile(output + "ptm_network.xml.gz");
		ptmConfig.setOutputScheduleFile(output + "ptm_schedule.xml.gz");

		PTMapperImpl.run(ptmConfig, schedule, network);

		// shapeFile
		ScheduleShapeFileWriter.run(schedule, network, ct, output);

		// stop facilities histogram
		StopFacilityHistogram histogram = new StopFacilityHistogram(schedule);
		histogram.createCsv(output+"gtfs_stopFacilities.csv");

		/**
		 * boescpa
		 */
//		TransitSchedule schedule2 = ScheduleTools.readTransitSchedule(filteredUnmappedMTS);
//		Network network2 = NetworkTools.readNetwork(networkFile);
//
//		PTMapperOnlyBusses boescpa = new PTMapperOnlyBusses(schedule2);
//		boescpa.routePTLines(network2);
//
//		ScheduleTools.writeTransitSchedule(schedule2, output+"boescpa/boescpa_schedule.xml.gz");
//		NetworkTools.writeNetwork(network2, output+"boescpa/boescpa_network.xml.gz");
//		ScheduleShapeFileWriter.run(schedule2, network2, ct, output+"boescpa/");

	}
}
