/* *********************************************************************** *
 * project: org.matsim.*
 * Cottbus2KoehlerStrehler2010
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNet2MatsimNet;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.gexf.DgKSNetwork2Gexf;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.DgNet2Shape;
import playground.dgrether.utils.DgPopulationSampler;
import playground.dgrether.utils.zones.DgMatsimPopulation2Zones;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZoneODShapefileWriter;
import playground.dgrether.utils.zones.DgZoneUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Converts a MATSim Scenario to the traffic signal optimization model published in 
 * <ul>
 * <li>K&ouml;hler, E. &amp; Strehler, M.</li>
 * <li><i>Traffic Signal Optimization Using Cyclically Expanded Networks</i></li>
 * <li>Erlebach, T. &amp; L&uuml;bbecke, M. <i>(ed.)</i></li>
 * <li>Proc. 10th Workshop Algorithmic Approaches for Transportation Modelling, Optimization, and Systems</li>
 * <li><b>2010</b>(14), pp. 114-129</li>
 * </ul>
 * 
 * 
 * @author dgrether
 * @author tthunig
 */
public class Scenario2KoehlerStrehler2010 {
	
	public static final Logger log = Logger.getLogger(Scenario2KoehlerStrehler2010.class);
	
	private static final String smallNetworkFilename = "network_small.xml.gz";
	private static final String modelOutfile = "koehler_strehler_model.xml";
	
	private double matsimPopSampleSize = 1.0;
	private double ksModelCommoditySampleSize = 1.0;
	private String shapeFileDirectory = "shapes/";

	private Scenario fullScenario;
	private CoordinateReferenceSystem crs;
	
	
	public Scenario2KoehlerStrehler2010(Scenario scenario, CoordinateReferenceSystem crs) {
		this.fullScenario = scenario;
		this.crs = crs;
	}

	
	public void convert(String outputDirectory, String name, double boundingBoxOffset, int cellsX, int cellsY, double startTimeSec, double endTimeSec) throws IOException{
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = this.createShapeFileDirectory(outputDirectory);
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);
		Set<Id> signalizedLinks = this.getSignalizedLinkIds(this.fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData());
		Set<Id> signalizedNodes = this.getSignalizedNodeIds(this.fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), this.fullScenario.getNetwork());
		
		DgSignalsBoundingBox signalsBoundingBox = new DgSignalsBoundingBox(crs);
		SimpleFeature bb = signalsBoundingBox.calculateBoundingBoxForSignals(fullScenario.getNetwork(), fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), boundingBoxOffset);
		signalsBoundingBox.writeBoundingBox(bb, shapeFileDirectory);
		
		String smallNetworkFile = outputDirectory +  smallNetworkFilename;
		DgNetworkShrinker netShrinker = new DgNetworkShrinker();
		netShrinker.setSignalizedNodes(signalizedNodes);
		Network smallNetwork = netShrinker.createSmallNetwork(fullScenario.getNetwork(), bb, crs);
		
		//"clean" the small network
//		DgNetworkCleaner cleaner = new DgNetworkCleaner();
//		cleaner.run(smallNetwork);
//		
		//run a network simplifier to merge links with same attributes
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4)); //PASS1WAY: 1 in- and 1 outgoing link
		nodeTypesToMerge.add(new Integer(5)); //PASS2WAY: 2 in- and 2 outgoing links
		NetworkSimplifier nsimply = new NetworkSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
		nsimply.run(smallNetwork);
		
		this.writeNetwork(smallNetwork, smallNetworkFile);
		this.writeNetwork2Shape(fullScenario.getNetwork(), shapeFileDirectory + "network_full");
		this.writeNetwork2Shape(smallNetwork, shapeFileDirectory + "network_small");
		System.exit(0);
		
//		String smallNetworkClean = outputDirectory + "network_small_clean.xml.gz";
//		this.writeNetwork(smallNetwork, smallNetworkClean);
//		this.writeNetwork2Shape(smallNetwork, shapeFileDirectory + "network_small_clean");
//		System.exit(0);
		DgGrid grid = createGrid(signalsBoundingBox.getBoundingBox(),  crs, cellsX, cellsY);
		DgGridUtils.writeGrid2Shapefile(grid, crs, shapeFileDirectory + "grid.shp");

		//create some zones and map demand on the zones
		if (matsimPopSampleSize != 1.0){
			new DgPopulationSampler().samplePopulation(fullScenario.getPopulation(), matsimPopSampleSize);
		}
		List<DgZone> cells = DgZoneUtils.createZonesFromGrid(grid);
		cells = new DgMatsimPopulation2Zones().convert2Zones(this.fullScenario.getNetwork(), smallNetwork, this.fullScenario.getPopulation(), cells,
				signalsBoundingBox.getBoundingBox(), startTimeSec, endTimeSec);
		DgZoneUtils.writePolygonZones2Shapefile(cells, crs, shapeFileDirectory + "zones.shp");
		
		DgZoneODShapefileWriter zoneOdWriter = new DgZoneODShapefileWriter(cells, crs);
		zoneOdWriter.writeLineStringZone2ZoneOdPairsFromZones2Shapefile(shapeFileDirectory + "zone2dest_od_pairs.shp");
		zoneOdWriter.writeLineStringLink2LinkOdPairsFromZones2Shapefile(shapeFileDirectory + "link2dest_od_pairs.shp");
		
		
		Map<DgZone, Link> zones2LinkMap = DgZoneUtils.createZoneCenter2LinkMapping(cells, (NetworkImpl) smallNetwork);
		DgZoneUtils.writeLinksOfZones2Shapefile(cells, zones2LinkMap, crs, shapeFileDirectory + "links_for_zones.shp");
		
		
		//create koehler strehler network
		Config smallNetConfig = ConfigUtils.createConfig();
		smallNetConfig.network().setInputFile(smallNetworkFile);
		smallNetConfig.scenario().setUseLanes(true);
		smallNetConfig.network().setLaneDefinitionsFile(fullScenario.getConfig().network().getLaneDefinitionsFile());
		Scenario smallScenario = ScenarioUtils.loadScenario(smallNetConfig);
		smallScenario.addScenarioElement(this.fullScenario.getScenarioElement(SignalsData.class));
		
		DgMatsim2KoehlerStrehler2010NetworkConverter netConverter = new DgMatsim2KoehlerStrehler2010NetworkConverter(idConverter);
		netConverter.setSignalizedLinks(signalizedLinks);
		DgKSNetwork ksNet = netConverter.convertNetworkLanesAndSignals(smallScenario, startTimeSec, endTimeSec);
		DgKSNetwork2Gexf converter = new DgKSNetwork2Gexf();
		converter.convertAndWrite(ksNet, outputDirectory + "network_small.gexf");
		
		DgMatsim2KoehlerStrehler2010DemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010Zones2Commodities(zones2LinkMap, idConverter);
		DgCommodities commodities = demandConverter.convert(smallScenario, ksNet);
		
		if (ksModelCommoditySampleSize != 1.0){
			for (DgCommodity com : commodities.getCommodities().values()) {
				double flow = com.getFlow() * ksModelCommoditySampleSize;
				com.setSourceNode(com.getSourceNode(), flow);
			}
		}
		
		String description = "offset: " + boundingBoxOffset + "cellsX: " + cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTimeSec + " endTimeSec: " + endTimeSec;
		description += "matsimPopsampleSize: " + this.matsimPopSampleSize + " ksModelCommoditySampleSize: " + this.ksModelCommoditySampleSize;

		Network newMatsimNetwork = new DgKSNet2MatsimNet().convertNetwork(ksNet);
		DgKoehlerStrehler2010Router router = new DgKoehlerStrehler2010Router();
		List<Id> invalidCommodities = router.routeCommodities(newMatsimNetwork, commodities);
		for (Id id : invalidCommodities) {
			commodities.getCommodities().remove(id);
		}
		log.info("testing routing again...");
		router.routeCommodities(newMatsimNetwork, commodities);
		
		this.writeNetwork(newMatsimNetwork, outputDirectory + "matsimNetworkKsModel.xml.gz");
		this.writeNetwork2Shape(newMatsimNetwork, shapeFileDirectory + "matsimNetworkKsModel.shp");
		
		new DgKoehlerStrehler2010ModelWriter().write(ksNet, commodities, name, description, outputDirectory + modelOutfile);
		writeStats(cellsX, cellsY, boundingBoxOffset, startTimeSec, endTimeSec, ksNet, commodities);
		
		idPool.writeToFile(outputDirectory + "id_conversions.txt");
		
		OutputDirectoryLogging.closeOutputDirLogging();		
	}

	private String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectory;
		File outdir = new File(shapeDir);
		outdir.mkdir();
		return shapeDir;
	}

	public void writeStats(int cellsX, int cellsY, double boundingBoxOffset, double startTime, double endTime, DgKSNetwork ksNet, DgCommodities commodities){
		log.info("Cells:");
		log.info("  X " + cellsX + " Y " + cellsY);
		log.info("Bounding Box: ");
		log.info("  Offset: " + boundingBoxOffset);
		log.info("Time: " );
		log.info("  startTime: " + startTime + " " + Time.writeTime(startTime));
		log.info("  endTime: " + endTime  + " " + Time.writeTime(endTime));
		log.info("Network: ");
		log.info("  # Streets: " + ksNet.getStreets().size() );
		log.info("  # Crossings: " + ksNet.getCrossings().size());
		int noLights = 0;
		int noNodes = 0;
		for (DgCrossing c : ksNet.getCrossings().values()){
			noLights += c.getLights().size();
			noNodes += c.getNodes().size();
		}
		log.info("  # Lights: " + noLights);
		log.info("  # Nodes: " + noNodes);
		log.info("Commodities: ");
		log.info("  # Commodities: " + commodities.getCommodities().size());
	}
	

	
	public static Scenario loadScenario(String net, String pop, String lanesFilename, String signalsFilename,
			String signalGroupsFilename, String signalControlFilename){
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(net);
		c2.plans().setInputFile(pop);
		c2.network().setLaneDefinitionsFile(lanesFilename);
		c2.signalSystems().setSignalSystemFile(signalsFilename);
		c2.signalSystems().setSignalGroupsFile(signalGroupsFilename);
		c2.signalSystems().setSignalControlFile(signalControlFilename);
		Scenario scenario = ScenarioUtils.loadScenario(c2);
		return scenario;
	}
	
	private Set<Id> getSignalizedLinkIds(SignalSystemsData signals){
		Map<Id, Set<Id>> signalizedLinksPerSystem = DgSignalsUtils.calculateSignalizedLinksPerSystem(signals);
		Set<Id> signalizedLinks = new HashSet<Id>();
		for (Set<Id> signalizedLinksOfSystem : signalizedLinksPerSystem.values()){
			signalizedLinks.addAll(signalizedLinksOfSystem);
		}
		return signalizedLinks;
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
	
	public void writeNetwork(Network net, String outputFile){
		NetworkWriter netWriter = new NetworkWriter(net);
		netWriter.write(outputFile);
	}
	

	private void writeNetwork2Shape(Network net, String outputFilename){
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		new DgNet2Shape().write(net, outputFilename + "_links.shp", crs);
		new Nodes2ESRIShape(net, outputFilename + "_nodes.shp", crs).write();
	}

	
	public double getMatsimPopSampleSize() {
		return matsimPopSampleSize;
	}

	
	public void setMatsimPopSampleSize(double matsimPopSampleSize) {
		this.matsimPopSampleSize = matsimPopSampleSize;
	}

	
	public double getKsModelCommoditySampleSize() {
		return ksModelCommoditySampleSize;
	}

	
	public void setKsModelCommoditySampleSize(double ksModelCommoditySampleSize) {
		this.ksModelCommoditySampleSize = ksModelCommoditySampleSize;
	}
}
