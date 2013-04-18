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

package playground.andreas.utils.ana;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.opengis.feature.simple.SimpleFeature;

import playground.andreas.P2.stats.abtractPAnalysisModules.lineSetter.BVGLines2PtModes;
import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.act2mode.ActivityToModeAnalysis;
import playground.vsp.analysis.modules.boardingAlightingCount.BoardingAlightingCountAnalyzer;
import playground.vsp.analysis.modules.ptAccessibility.PtAccessibility;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesHandler;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.PtRoutes2PaxAnalysis;
import playground.vsp.analysis.modules.ptTravelStats.travelStatsAnalyzer;
import playground.vsp.analysis.modules.ptTripAnalysis.traveltime.TTtripAnalysis;
import playground.vsp.analysis.modules.stuckAgents.GetStuckEventsAndPlans;
import playground.vsp.analysis.modules.transitSchedule2Shp.TransitSchedule2Shp;
import playground.vsp.analysis.modules.transitVehicleVolume.TransitVehicleVolumeAnalyzer;

import com.vividsolutions.jts.geom.Geometry;

public class AnalysisRunner {

	/**
	 * 
	 * @param args OutputDir RunId iteration gridSize shapeFile quadrantSegments
	 */
	public static void main(String[] args) {
		
		String outputDir = args[0];
		String runId = args[1];
		int iteration = Integer.parseInt(args[2]);
		int gridSize = Integer.valueOf(args[3]);
		String shapeFile = args[4];
		int quadrantSegments = Integer.parseInt(args[5]);
		String fromCoordSystem = args[6];
		
		String targetCoordinateSystem = fromCoordSystem;
		CoordinateTransformation coordTransformation = null;
		
		if (args.length == 8) {
			// we want to transform
			String toCoordSystem = args[7];
			coordTransformation = TransformationFactory.getCoordinateTransformation(fromCoordSystem, toCoordSystem);
			targetCoordinateSystem = toCoordSystem;
		}
		
		String oldJavaIoTempDir = System.getProperty("java.io.tmpdir");
		String newJavaIoTempDir = outputDir + "/" + runId + "/tmp";
		System.out.println("Setting java tmpDir from " + oldJavaIoTempDir + " to " + newJavaIoTempDir);
		System.setProperty("java.io.tmpdir", newJavaIoTempDir);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		
//		String targetCoordinateSystem = TransformationFactory.WGS84_UTM35S; // Gauteng
//		String targetCoordinateSystem = TransformationFactory.WGS84_UTM33N; // Berlin
//		String targetCoordinateSystem = TransformationFactory.WGS84;		// World Mercator
		
		OutputDirectoryHierarchy dir = new OutputDirectoryHierarchy(outputDir + "/" + runId + "/", runId, true, true);
		
		new TransitScheduleReader(sc).readFile(dir.getIterationFilename(iteration, "transitSchedule.xml.gz"));
		new VehicleReaderV1(((ScenarioImpl) sc).getVehicles()).readFile(dir.getIterationFilename(iteration, "vehicles.xml.gz"));
		new MatsimNetworkReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_NETWORK));
		new MatsimFacilitiesReader((ScenarioImpl) sc).readFile(dir.getOutputFilename("output_facilities.xml.gz"));
		new MatsimPopulationReader(sc).readFile(dir.getIterationFilename(iteration, "plans.xml.gz"));
		
		BVGLines2PtModes bvgLines2PtModes = new BVGLines2PtModes();
		bvgLines2PtModes.setPtModesForEachLine(sc.getTransitSchedule(), "para");
		TagLinesInTransitSchedule.tagLinesInTransitSchedule(sc.getTransitSchedule(), bvgLines2PtModes.getLineId2ptModeMap());
		
		List<Integer> cluster = new ArrayList<Integer>(){{
			add(100);
			add(500);
			add(1000);
		}};
		
		SortedMap<String, List<String>> activityCluster = BVG3ActsScheme.createBVG3ActsScheme();		
		
		Set<String> ptModes = new HashSet<String>(){{
			add("pt");
		}};
		
		Set<String> networkModes = new HashSet<String>(){{
			add("car");
		}};
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		Map<String, Geometry> zones =  new HashMap<String, Geometry>();
		int i = 0;
		for (SimpleFeature f: features){
			if (f.getAttributes().size() < 3) {
				zones.put(Integer.toString(i), (Geometry) f.getAttribute(0));
			} else {
				zones.put((String)f.getAttribute(2), (Geometry) f.getAttribute(0));
			}
		}
		
		VspAnalyzer analyzer = new VspAnalyzer(dir.getOutputPath() + "_ana/", dir.getIterationFilename(iteration, Controler.FILENAME_EVENTS_XML));
		
		// works
		PtAccessibility ptAccessibility = new PtAccessibility(sc, cluster, quadrantSegments, activityCluster, targetCoordinateSystem, gridSize);
		analyzer.addAnalysisModule(ptAccessibility);
		
		GetStuckEventsAndPlans getStuckEventsAndPlans = new GetStuckEventsAndPlans(sc);
		analyzer.addAnalysisModule(getStuckEventsAndPlans);
		
		TransitVehicleVolumeAnalyzer transitVehicleVolumeAnalyzer = new TransitVehicleVolumeAnalyzer(sc, 3600., targetCoordinateSystem);
		analyzer.addAnalysisModule(transitVehicleVolumeAnalyzer);
		
		PtPaxVolumesAnalyzer ptPaxVolumesAnalyzer = new PtPaxVolumesAnalyzer(sc, 3600., targetCoordinateSystem);
		analyzer.addAnalysisModule(ptPaxVolumesAnalyzer);
		
		TTtripAnalysis ttTripAnalysis = new TTtripAnalysis(ptModes, networkModes, sc.getPopulation());	ttTripAnalysis.addZones(zones);
		analyzer.addAnalysisModule(ttTripAnalysis);
		
		TransitSchedule2Shp transitSchedule2Shp = new TransitSchedule2Shp(sc, targetCoordinateSystem);
		analyzer.addAnalysisModule(transitSchedule2Shp);
		
		ActivityToModeAnalysis activityToModeAnalysis = new ActivityToModeAnalysis(sc, null, 3600, targetCoordinateSystem);
		analyzer.addAnalysisModule(activityToModeAnalysis);
		
		BoardingAlightingCountAnalyzer boardingAlightingCountAnalyzes = new BoardingAlightingCountAnalyzer(sc, 3600, targetCoordinateSystem);
		boardingAlightingCountAnalyzes.setWriteHeatMaps(true, gridSize);
		analyzer.addAnalysisModule(boardingAlightingCountAnalyzes);

		analyzer.addAnalysisModule(new MyPtCount(sc.getNetwork()));

		PtRoutes2PaxAnalysis ptRoutes2PaxAnalysis = new PtRoutes2PaxAnalysis(sc.getTransitSchedule().getTransitLines(), ((ScenarioImpl) sc).getVehicles(), 3600.0, 24);
		analyzer.addAnalysisModule(ptRoutes2PaxAnalysis);
		
		travelStatsAnalyzer travelStatsAnalyzer = new travelStatsAnalyzer(sc, 3600.0);
		analyzer.addAnalysisModule(travelStatsAnalyzer);
		
		analyzer.run();
		
		System.out.println("Setting java tmpDir from " + newJavaIoTempDir + " to " + oldJavaIoTempDir);
		System.setProperty("java.io.tmpdir", oldJavaIoTempDir);
	}
}

class MyPtCount extends AbstractAnalyisModule{

	PtPaxVolumesHandler handler;
	private ArrayList<Id> links;
	private Network network;
	/**
	 * @param zones 
	 * @param network 
	 * @param name
	 */
	public MyPtCount(Network network) {
		super(MyPtCount.class.getSimpleName());
		this.handler = new PtPaxVolumesHandler(3600.);
		this.network = network;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		this.links = new ArrayList<Id>();
		for (Link link : this.network.getLinks().values()) {
			links.add(link.getId());
		}
		this.network = null;
	}

	@Override
	public void postProcessData() {
		
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder + "ptPaxVolumes.csv");
		try {
			//header
			writer.write("LinkId;total;");
			for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
					writer.write(String.valueOf(i) + ";");
			}
			writer.newLine();
			//content
			for(Id id: this.links){
				writer.write(id.toString() + ";");
				writer.write(this.handler.getPaxCountForLinkId(id) + ";");
				for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
					writer.write(this.handler.getPaxCountForLinkId(id, i) + ";");
				}
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}



