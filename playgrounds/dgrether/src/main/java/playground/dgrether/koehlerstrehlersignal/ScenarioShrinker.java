/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioShrinker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.DgPopulationSampler;
import playground.dgrether.utils.zones.DgMatsimPopulation2Zones;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZoneODShapefileWriter;
import playground.dgrether.utils.zones.DgZoneUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 *
 */
public class ScenarioShrinker {
	
	private static final Logger log = Logger.getLogger(ScenarioShrinker.class);
	
	private static final String smallNetworkFilename = "network_small.xml.gz";
	private static final String simplifiedNetworkFilename = "network_small_simplified.xml.gz";
	private static final String simplifiedLanesFilename = "lanes_network_small.xml.gz";

	
	private double matsimPopSampleSize = 1.0;
	private String shapeFileDirectory = "shapes/";

	private Scenario fullScenario;
	private CoordinateReferenceSystem crs;

	public ScenarioShrinker(Scenario scenario, CoordinateReferenceSystem crs){
		this.fullScenario = scenario;
		this.crs = crs;
	}
	
	public void shrinkScenario(String outputDirectory, String name, double boundingBoxOffset, int cellsX, int cellsY, double startTimeSec, double endTimeSec) throws IOException{
		//Some initialization
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = this.createShapeFileDirectory(outputDirectory);
		Set<Id> signalizedNodes = this.getSignalizedNodeIds(this.fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), this.fullScenario.getNetwork());
		DgNetworkUtils.writeNetwork2Shape(fullScenario.getNetwork(), shapeFileDirectory + "network_full");
		
		//create the bounding box
		DgSignalsBoundingBox signalsBoundingBox = new DgSignalsBoundingBox(crs);
		SimpleFeature bb = signalsBoundingBox.calculateBoundingBoxForSignals(fullScenario.getNetwork(), fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), boundingBoxOffset);
		signalsBoundingBox.writeBoundingBox(bb, shapeFileDirectory);
		
		//Reduce the network size to the bounding box
		DgNetworkShrinker netShrinker = new DgNetworkShrinker();
		netShrinker.setSignalizedNodes(signalizedNodes);
		Network smallNetwork = netShrinker.createSmallNetwork(fullScenario.getNetwork(), bb, crs);
		
		DgNetworkUtils.writeNetwork(smallNetwork, outputDirectory +  smallNetworkFilename);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, shapeFileDirectory + "network_small");
		
		//"clean" the small network
		DgNetworkCleaner cleaner = new DgNetworkCleaner();
		cleaner.cleanNetwork(smallNetwork);
		String smallNetworkClean = outputDirectory + "network_small_clean.xml.gz";
		DgNetworkUtils.writeNetwork(smallNetwork, smallNetworkClean);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, shapeFileDirectory + "network_small_clean");

		//create a grid
		DgGrid grid = createGrid(signalsBoundingBox.getBoundingBox(),  crs, cellsX, cellsY);
		DgGridUtils.writeGrid2Shapefile(grid, crs, shapeFileDirectory + "grid.shp");

		//create some zones and map demand on the zones
		if (matsimPopSampleSize != 1.0){
			new DgPopulationSampler().samplePopulation(fullScenario.getPopulation(), matsimPopSampleSize);
		}
		
		//create some zones and match the population to them
		List<DgZone> zones = DgZoneUtils.createZonesFromGrid(grid);
		DgMatsimPopulation2Zones pop2zones = new DgMatsimPopulation2Zones();
		pop2zones.setUseLinkMappings(false);
		zones = pop2zones.convert2Zones(this.fullScenario.getNetwork(), smallNetwork, 
				this.fullScenario.getPopulation(), zones, signalsBoundingBox.getBoundingBox(), startTimeSec, endTimeSec);
		
		//write	 the matching to some files
		DgZoneUtils.writePolygonZones2Shapefile(zones, crs, shapeFileDirectory + "zones.shp");
		DgZoneODShapefileWriter zoneOdWriter = new DgZoneODShapefileWriter(zones, crs);
		zoneOdWriter.writeLineStringZone2ZoneOdPairsFromZones2Shapefile(shapeFileDirectory + "zone2dest_od_pairs.shp");
		zoneOdWriter.writeLineStringLink2LinkOdPairsFromZones2Shapefile(shapeFileDirectory + "link2dest_od_pairs.shp");
		//TODO write zones to file

		
		//run a network simplifier to merge links with same attributes
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS1WAY); //PASS1WAY: 1 in- and 1 outgoing link
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS2WAY); //PASS2WAY: 2 in- and 2 outgoing links
		NetworkSimplifier nsimply = new NetworkSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
		nsimply.simplifyNetworkLanesAndSignals(smallNetwork, this.fullScenario.getScenarioElement(LaneDefinitions20.class), this.fullScenario.getScenarioElement(SignalsData.class));

		
		String simplifiedNetworkFile = outputDirectory + simplifiedNetworkFilename;
		DgNetworkUtils.writeNetwork(smallNetwork, simplifiedNetworkFile);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, shapeFileDirectory + "network_small_simplified");

		LaneDefinitionsWriter20 lanesWriter = new LaneDefinitionsWriter20(this.fullScenario.getScenarioElement(LaneDefinitions20.class));
		lanesWriter.write(outputDirectory + simplifiedLanesFilename);
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter(outputDirectory);
		signalsWriter.writeSignalsData(this.fullScenario.getScenarioElement(SignalsData.class));
		
		OutputDirectoryLogging.closeOutputDirLogging();		
	}
		
	private String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectory;
		File outdir = new File(shapeDir);
		outdir.mkdir();
		return shapeDir;
	}
	
	public double getMatsimPopSampleSize() {
		return matsimPopSampleSize;
	}

	
	public void setMatsimPopSampleSize(double matsimPopSampleSize) {
		this.matsimPopSampleSize = matsimPopSampleSize;
	}
	


	private Set<Id> getSignalizedNodeIds(SignalSystemsData signals, Network network){
		Map<Id, Set<Id>> signalizedNodesPerSystem = DgSignalsUtils.calculateSignalizedNodesPerSystem(signals, network);
		Set<Id> signalizedNodes = new HashSet<Id>();
		for (Set<Id> signalizedNodesOfSystem : signalizedNodesPerSystem.values()){
			signalizedNodes.addAll(signalizedNodesOfSystem);
		}
		return signalizedNodes;
	}
	
	public DgGrid createGrid(Envelope boundingBox, CoordinateReferenceSystem crs, int xCells, int yCells){
		Envelope gridBoundingBox = new Envelope(boundingBox);
		//expand the grid size to avoid rounding errors 
		gridBoundingBox.expandBy(0.1);
		DgGrid grid = new DgGrid(xCells, yCells, gridBoundingBox);
		return grid;
	}
	
}
