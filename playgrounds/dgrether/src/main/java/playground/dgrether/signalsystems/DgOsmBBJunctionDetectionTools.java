/* *********************************************************************** *
 * project: org.matsim.*
 * DgOsmBBJunctionFilter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.referencing.GeodeticCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.misc.v0_6.NullWriter;
import org.openstreetmap.osmosis.core.report.v0_6.EntityReporter;
import org.openstreetmap.osmosis.core.tee.v0_6.EntityTee;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlWriter;

import playground.dgrether.osm.OSMEntityCollector;
import playground.mzilske.osm.NetworkSink;
import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Style;


/**
 * @author dgrether
 *
 */
public class DgOsmBBJunctionDetectionTools {
	
	private static final Logger log = Logger.getLogger(DgOsmBBJunctionDetectionTools.class);
	
	private GeodeticCalculator geoCalculator;
	private CoordinateReferenceSystem targetCRS;
	
	public DgOsmBBJunctionDetectionTools(){
		this.targetCRS = MGC.getCRS(TransformationFactory.WGS84);
		this.geoCalculator = new GeodeticCalculator(this.targetCRS);
		log.info("axis unit " + this.geoCalculator.getEllipsoid().getAxisUnit());
	}
	
	
	/**
	 * This is a copy from OsmTransitMain needed to create network
	 * Could also be placed in a NetworkSinkBuilder to avoid code duplication
	 */
	private NetworkSink createAndInitNetworkSink(Scenario scenario, CoordinateTransformation coordinateTransformation){
		NetworkSink networkGenerator = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		networkGenerator.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		networkGenerator.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		networkGenerator.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000, false);
		networkGenerator.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500, false);
		networkGenerator.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500, false);
		networkGenerator.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500, false);
		networkGenerator.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000, false);
		networkGenerator.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);
		return networkGenerator;
	}
	
	/**
	 * Parses an osm xml file and collects  all nodes tagged with "traffic_signals" in the returned OSMEntityCollector
	 */
	private OSMEntityCollector detectAndWriteSignalizedOsmNodes(String osmFile, String nodesOutOsmFile, String nodesOutReportFile){
		Set<String> emptyKeys = Collections.emptySet();
		Map<String, Set<String>> emptyKVs = Collections.emptyMap();
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("traffic_signals")));
		
		TagFilter tagFilterWays = new TagFilter("reject-ways", emptyKeys, emptyKVs);
		TagFilter tagFilterRelations = new TagFilter("reject-relations", emptyKeys, emptyKVs);
		TagFilter tagFilterNodes = new TagFilter("accept-node", emptyKeys, tagKeyValues);
		
		FastXmlReader reader = new FastXmlReader(new File(osmFile), true, CompressionMethod.None);
		reader.setSink(tagFilterWays);
		tagFilterWays.setSink(tagFilterRelations);
		tagFilterRelations.setSink(tagFilterNodes);

		EntityTee tee2 = new EntityTee(3);
		tagFilterNodes.setSink(tee2);
		XmlWriter writer = new XmlWriter(new File(nodesOutOsmFile), CompressionMethod.None);
		tee2.getSource(0).setSink(writer);
		tee2.getSource(1).setSink(new EntityReporter(new File(nodesOutReportFile)));
		OSMEntityCollector signalizedOsmNodes = new OSMEntityCollector();
		tee2.getSource(2).setSink(signalizedOsmNodes);
		reader.run();
		return signalizedOsmNodes;
	}
	
	/**
	 * Creates an Matsim Network from the osm file 
	 */
	private Network createMatsimNetwork(String osmFile){
		// get a Matsim Network
		// read the file again as NetworkSink modifies the data and the Entities are read-only if a EntityTee is used
		Scenario sc = new ScenarioImpl();
		NetworkSink networkGenerator = createAndInitNetworkSink(sc, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84));
		TagFilter transitRelationsFilter = createTransitRelationsFilter();
		TagFilter transitWaysFilter = createTransitWaysFilter();
		
		FastXmlReader reader = new FastXmlReader(new File(osmFile), true, CompressionMethod.None);
		reader.setSink(transitRelationsFilter);
		transitRelationsFilter.setSink(transitWaysFilter);
		transitWaysFilter.setSink(networkGenerator);
		//avoid null-pointers as NetworkSink needs no sink
		networkGenerator.setSink(new NullWriter());
		reader.run();

		NetworkCalcTopoType networkCalcTopoType = new NetworkCalcTopoType();
		networkCalcTopoType.run(sc.getNetwork());
		return sc.getNetwork();
	}
	
	private TagFilter createTransitWaysFilter() {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
//		tagKeyValues.put("route", new HashSet<String>(Arrays.asList("tram", "train", "bus")));
		Set<String> tagKeys = new HashSet<String>();
		tagKeys.add("railway");
		TagFilter transitFilter = new TagFilter("reject-way", tagKeys, tagKeyValues);
		return transitFilter;
	}

	private static TagFilter createTransitRelationsFilter() {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("route", new HashSet<String>(Arrays.asList("tram", "train", "bus")));
		Set<String> tagKeys = Collections.emptySet();
		TagFilter transitFilter = new TagFilter("reject-relation", tagKeys, tagKeyValues);
		return transitFilter;
	}
	
	
	private List<Set<org.matsim.api.core.v01.network.Node>> detectAllJunctions(OSMEntityCollector signalizedOsmNodes, Network network){
		Set<org.matsim.api.core.v01.network.Node> detectedJunctionNodes = new HashSet<org.matsim.api.core.v01.network.Node>();
		List<Set<org.matsim.api.core.v01.network.Node>> junctions = new ArrayList<Set<org.matsim.api.core.v01.network.Node>>();

		// get all matsim nodes for the osm nodes that are signalized
		Set<org.matsim.api.core.v01.network.Node> matsimNodes = new HashSet<org.matsim.api.core.v01.network.Node>();
		for (Node osmNode : signalizedOsmNodes.getAllNodes().values()){
			log.info("processing potential junction node: " + osmNode.getId());
			org.matsim.api.core.v01.network.Node matsimNode = network.getNodes().get(new IdImpl(osmNode.getId()));
			if (matsimNode == null){
				log.warn("OSMNode  " + osmNode.getId() + " is tagged as signalized but is not contained in MATSim Network");
				continue;
			}
			matsimNodes.add(matsimNode);
		}

		//detect the junctions
		for (org.matsim.api.core.v01.network.Node matsimNode : matsimNodes){
			if (detectedJunctionNodes.contains(matsimNode)){
//				log.info("visited node twice " + matsimNode.getId());
				continue;
			}
			Set<org.matsim.api.core.v01.network.Node> jn = detectJunction(matsimNode, detectedJunctionNodes, matsimNodes);
			detectedJunctionNodes.addAll(jn);
			junctions.add(jn);
		}
		return junctions;
	}
	
	
	private Set<org.matsim.api.core.v01.network.Node> detectJunction(org.matsim.api.core.v01.network.Node startNode, Set<org.matsim.api.core.v01.network.Node> detectedJunctionNodes, Set<org.matsim.api.core.v01.network.Node> matsimNodes){
		Set<org.matsim.api.core.v01.network.Node> junctionMatsimNodes = new HashSet<org.matsim.api.core.v01.network.Node>();
		junctionMatsimNodes.add(startNode);
		
		List<org.matsim.api.core.v01.network.Node> nodeBuffer = new LinkedList<org.matsim.api.core.v01.network.Node>();
		nodeBuffer.add(startNode);
		
		org.matsim.api.core.v01.network.Node fromNode;
		while (!nodeBuffer.isEmpty()){
			fromNode = nodeBuffer.remove(0);
			this.geoCalculator.setStartingGeographicPoint(fromNode.getCoord().getX(), fromNode.getCoord().getY());
			
			for (org.matsim.api.core.v01.network.Node toNode : matsimNodes){
				if (detectedJunctionNodes.contains(toNode) || junctionMatsimNodes.contains(toNode)){
					continue;
				}
				this.geoCalculator.setDestinationGeographicPoint(toNode.getCoord().getX(), toNode.getCoord().getY());
				if (this.geoCalculator.getOrthodromicDistance() < 60.0){
					nodeBuffer.add(toNode);
					junctionMatsimNodes.add(toNode);
				}
			}
			
			
		}
		
		return junctionMatsimNodes;
	}

	
	private void writeJunctionNodesToOsm(
			List<Set<org.matsim.api.core.v01.network.Node>> junctionNodes, String osmJunctionsOutFile) {

		DgOsmJunctionSource junctionSource = new DgOsmJunctionSource(junctionNodes);
		XmlWriter writer = new XmlWriter(new File(osmJunctionsOutFile), CompressionMethod.None);
		junctionSource.setSink(writer);
		junctionSource.run();
		writer.complete();
		
	}

	
	
	private void writeJunctionNodesToKml(List<Set<org.matsim.api.core.v01.network.Node>> junctions, String kmlJunctionsOutFile) {
		CoordinateTransformation coordtransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84);
		
		final Kml kml = new Kml();
		final Document document = new Document();
		kml.setFeature(document);

		int i = 0;
		
		int colorSteps = (255 / junctions.size()) - 1;
		
		for (Set<org.matsim.api.core.v01.network.Node> junction : junctions){
			i++;
			final Style style = new Style();
			document.getStyleSelector().add(style);
			style.setId("randomColorIcon" + i);
			final IconStyle iconstyle = new IconStyle();
			style.setIconStyle(iconstyle);
			String hexColor = Integer.toHexString(i * colorSteps);
			iconstyle.setColor("ff00" + hexColor + hexColor);
			iconstyle.setColorMode(ColorMode.NORMAL);
			iconstyle.setScale(0.7d);

			final Icon icon = new Icon();
			iconstyle.setIcon(icon);
			icon.setHref("http://maps.google.com/mapfiles/kml/pal2/icon18.png");

			for (org.matsim.api.core.v01.network.Node node : junction){
				final Placemark placemark = new Placemark();
				document.getFeature().add(placemark);
				placemark.setName(Integer.toString(i));
				placemark.setDescription(node.getId().toString());
				placemark.setStyleUrl("#randomColorIcon" + i);
				final Point point = new Point();
				placemark.setGeometry(point);
				List<Coordinate> coord  = new ArrayList<Coordinate>();
				point.setCoordinates(coord);
				Coord nodeCoord = coordtransform.transform(node.getCoord());
				coord.add(new Coordinate(nodeCoord.getX(), nodeCoord.getY()));
			}
		}
		

		try {
			kml.marshal(new File(kmlJunctionsOutFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String baseInDirOsm = DgOsmBBPaths.BASE_IN_DIR;
		String baseInDirTest = DgOsmBBPaths.BASE_OUT_DIR;
		
		boolean test = false;
		
		String baseInDir;
		if (test){
			baseInDir = baseInDirTest;
		}
		else {
			baseInDir = baseInDirOsm;
		}
		
		String bbOsmFile = baseInDirOsm + "berlinbrandenburg_filtered.osm";
		String testOsmFile = baseInDirTest + "testdata/map_filtered.osm";

		String osmFile;
		if (test){
			osmFile = testOsmFile;
		}
		else {
			osmFile = bbOsmFile;
		}
		
		String baseOutDirTest = baseInDirTest;
		
		String baseOutDir;
		if (test){
			baseOutDir = baseOutDirTest + "testdata/";
		}
		else {
			baseOutDir = baseOutDirTest;
		}
		
		String nodesOutOsmFile = baseOutDir + "potential_junction_nodes.osm";
		String nodesOutReportFile = baseOutDir + "nodes_filter_report.txt";
		String netOutFile = DgOsmBBPaths.NETWORK_GENERATED;
		String osmJunctionsOutFile = baseOutDir + "osm_bb_junctions.osm";
		String kmlJunctionsOutFile = baseOutDir + "junctions.kml";
		String kmlNetworkOutFile = baseOutDir + "network.kml";
		
		DgOsmBBJunctionDetectionTools tools = new DgOsmBBJunctionDetectionTools();
		
		OSMEntityCollector signalizedOsmNodes = tools.detectAndWriteSignalizedOsmNodes(osmFile, nodesOutOsmFile, nodesOutReportFile);

		Network network = tools.createMatsimNetwork(osmFile);
		new NetworkWriter(network).write(netOutFile);
		
		List<Set<org.matsim.api.core.v01.network.Node>> junctionNodes = tools.detectAllJunctions(signalizedOsmNodes, network);
		
		log.info("writing osm output...");
		
		tools.writeJunctionNodesToOsm(junctionNodes, osmJunctionsOutFile);
		
		log.info("writing kml output...");
		tools.writeJunctionNodesToKml(junctionNodes, kmlJunctionsOutFile);
		
		//write network to kml, might exceed max memory
//		new KmlNetworkVisualizer(network).write(kmlNetworkOutFile, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84));

		signalizedOsmNodes.reset();
		log.info("done!");
	}
	
}
