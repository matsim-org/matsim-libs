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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.misc.v0_6.NullWriter;
import org.openstreetmap.osmosis.core.report.v0_6.EntityReporter;
import org.openstreetmap.osmosis.core.tee.v0_6.EntityTee;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlWriter;

import playground.dgrether.DgPaths;
import playground.dgrether.osm.OSMEntityCollector;
import playground.dgrether.visualization.KmlNetworkVisualizer;
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
		NetworkSink networkGenerator = createAndInitNetworkSink(sc, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N));
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
	
	
	private List<List<org.matsim.api.core.v01.network.Node>> detectAllJunctions(OSMEntityCollector signalizedOsmNodes, Network network){
		Set<org.matsim.api.core.v01.network.Node> visitedNodes = new HashSet<org.matsim.api.core.v01.network.Node>();
		
		List<List<org.matsim.api.core.v01.network.Node>> junctions = new ArrayList<List<org.matsim.api.core.v01.network.Node>>();
		
		for (Node osmNode : signalizedOsmNodes.getAllNodes().values()){
			log.info("processing potential junction node: " + osmNode.getId());
			org.matsim.api.core.v01.network.Node matsimNode = network.getNodes().get(new IdImpl(osmNode.getId()));
			if (matsimNode == null){
				log.warn("OSMNode  " + osmNode.getId() + " is tagged as signalized but is not contained in MATSim Network");
				continue;
			}

			if (visitedNodes.contains(matsimNode)){
				log.info("visited node twice " + matsimNode.getId());
				continue;
			}
			
			List<org.matsim.api.core.v01.network.Node> jn = detectJunction(matsimNode, visitedNodes, signalizedOsmNodes);
			if (jn.size() > 1) {
				junctions.add(jn);
			}
		}
		return junctions;
	}
	
	
	private List<org.matsim.api.core.v01.network.Node> detectJunction(org.matsim.api.core.v01.network.Node matsimNode, Set<org.matsim.api.core.v01.network.Node> visitedNodes, OSMEntityCollector signalizedOsmNodes){
		visitedNodes.add(matsimNode);
		List<org.matsim.api.core.v01.network.Node> junctionMatsimNodes = new LinkedList<org.matsim.api.core.v01.network.Node>();
		junctionMatsimNodes.add(matsimNode);
		
		ListIterator<org.matsim.api.core.v01.network.Node> it = junctionMatsimNodes.listIterator();
		while (it.hasNext()){
			org.matsim.api.core.v01.network.Node mNode = it.next();
			for (Link l : mNode.getOutLinks().values()){
				org.matsim.api.core.v01.network.Node toNode = l.getToNode();
				Long toNodeId = Long.decode(toNode.getId().toString());
				if (l.getLength() < 70.0 && signalizedOsmNodes.getAllNodes().containsKey(toNodeId)){
					it.add(toNode);
					visitedNodes.add(toNode);
//					log.info("  detected junction node: " + toNodeId);
				}
			}
		}
		return junctionMatsimNodes;
	}

	
	
	private void writeJunctionNodesToKml(List<List<org.matsim.api.core.v01.network.Node>> junctions, String kmlJunctionsOutFile) {
		CoordinateTransformation coordtransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N, TransformationFactory.WGS84);
		
		final Kml kml = new Kml();
		final Document document = new Document();
		kml.setFeature(document);

		int i = 0;
		
		int colorSteps = (255 / junctions.size()) - 1;
		
		for (List<org.matsim.api.core.v01.network.Node> junction : junctions){
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
		String baseInDirOsm = DgPaths.SHAREDSVN + "studies/countries/de/osm_berlinbrandenburg/workingset/";
		String baseInDirTest = DgPaths.STUDIESDG + "osmBerlinSzenario/";
		
		String baseInDir = baseInDirTest;
//		String baseInDir = baseInDirOsm;
		
		String bbOsmFile = baseInDirOsm + "berlinbrandenburg_filtered.osm";
		String testOsmFile = baseInDirTest + "testdata/map_filtered.osm";

		String osmFile = testOsmFile;
//		String osmFile = bbOsmFile;
		
		String baseOutDirTest = baseInDirTest;
		
		String baseOutDir = baseOutDirTest + "testdata/";
//		String baseOutDir = baseOutDirTest;
		
		String nodesOutOsmFile = baseOutDir + "potential_junction_nodes.osm";
		String nodesOutReportFile = baseOutDir + "nodes_filter_report.txt";
		String netOutFile = baseOutDir + "network.xml";
		String kmlJunctionsOutFile = baseOutDir + "junctions.kml";
		String kmlNetworkOutFile = baseOutDir + "network.kml";
		
		DgOsmBBJunctionDetectionTools tools = new DgOsmBBJunctionDetectionTools();
		
		OSMEntityCollector signalizedOsmNodes = tools.detectAndWriteSignalizedOsmNodes(osmFile, nodesOutOsmFile, nodesOutReportFile);

		Network network = tools.createMatsimNetwork(osmFile);
		new NetworkWriter(network).write(netOutFile);
		
		List<List<org.matsim.api.core.v01.network.Node>> junctionNodes = tools.detectAllJunctions(signalizedOsmNodes, network);
		
		tools.writeJunctionNodesToKml(junctionNodes, kmlJunctionsOutFile);
		
		//write network to kml, might exceed max memory
		new KmlNetworkVisualizer(network).write(kmlNetworkOutFile, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N, TransformationFactory.WGS84));

		signalizedOsmNodes.reset();
		log.info("done!");
	}

	
	
}
