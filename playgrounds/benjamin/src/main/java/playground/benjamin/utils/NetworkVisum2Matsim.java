/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.benjamin.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.EdgeType;
import org.matsim.visum.VisumNetworkReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class NetworkVisum2Matsim {

	private static final Logger log = Logger.getLogger(NetworkVisum2Matsim.class);


	private static final Collection<String> usedIds = new ArrayList<String>();
	private static final Collection<String> irrelevantIds = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99");
	private static final Collection<String> additionalIrrelevantIdsPeriphery = Arrays.asList(/*   /*"37","38","39","40","41","42","43",*/   /*"44", "45","46","47",*/   /*"48",*/   /*"49","50","51","52","53","54","55","56","57","58","59"*/   /*,"60","61","62","63","64","65","66","67","68","69","70","71","72","73"*/   /*,"74","75"*/   /*,"76","77","78","79"*/    /*,"80","81","82","83"*/);

	//some speed adaptions based on "OsmTransitMain"
	private static final Collection<String> innerCity30to40KmhIdsNeedingVmaxChange = Arrays.asList(/*"44",*/   /*"74", "75", "82", "83"*/   /*, "47", "50", "60", "61", "62" */);
	private static final Collection<String> innerCity45to60KmhIdsNeedingVmaxChange = Arrays.asList(/*"37", "38", "39", "40", "41", "42", "43",*/ /*  "45", "46", "48", "49"*/ /*"54", "55", "56", "57", "58",*/    /*"59", "72", "73", "80", "81"*/);
	private static final Collection<String> innerCity70to80KmhIdsNeedingVmaxChange = Arrays.asList();
	private static final Collection<String> innerCity100to140KmhIdsNeedingVmaxChange = Arrays.asList();

	private static String OutPath = "../../detailedEval/Net/";
	private static String InVisumNetFile = "../../detailedEval/Net/Analyse2005_Netz.net";
	private static String DetailedAreaShape = "../../detailedEval/Net/shapeFromVISUM/Landkreise_umMuenchen_Umrisse.shp";
	//	private static String DetailedAreaShape = "../../detailedEval/policies/zone30.shp";

	// OUTPUT FILES
	private static String OutNetworkFile = OutPath + "network-86-85-87-84_withLanes.xml";
	//	private static String OutNetworkFile = "../../detailedEval/policies/network-86-85-87-84_withLanes_zone30.xml.gz";

	private final MutableScenario scenario;
	private final Config config;
	private VisumNetwork visumNetwork;

	public NetworkVisum2Matsim() {
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();
	}

	private void prepareConfig() {
	}

	private void convertNetwork() {
		final Network network = (Network) scenario.getNetwork();
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();

		final Collection<SimpleFeature> featuresInShape = ShapeFileReader.getAllFeatures(DetailedAreaShape);

		VisumNetworkRowHandler nodeRowHandler = new VisumNetworkRowHandler() {

			@Override
			public void handleRow(Map<String, String> row) {
				Id<Node> id = Id.create(row.get("NR"), Node.class);
				Coord coord = new Coord(Double.parseDouble(row.get("XKOORD").replace(',', '.')), Double.parseDouble(row.get("YKOORD").replace(',', '.')));
				final Id<Node> id1 = id;
				final Coord coord1 = coord;
				NetworkUtils.createAndAddNode(network, id1, coord1);
			}

		};
		streamingVisumNetworkReader.addRowHandler("KNOTEN", nodeRowHandler);

		VisumNetworkRowHandler edgeRowHandler = new VisumNetworkRowHandler() {

			@Override
			public void handleRow(Map<String, String> row) {
				String nr = row.get("NR");
				Id<Link> id = Id.create(nr, Link.class);
				Id<Node> fromNodeId = Id.create(row.get("VONKNOTNR"), Node.class);
				Id<Node> toNodeId = Id.create(row.get("NACHKNOTNR"), Node.class);
				Node fromNode = network.getNodes().get(fromNodeId);
				Node toNode = network.getNodes().get(toNodeId);
				Link lastEdge = network.getLinks().get(id);
				if (lastEdge != null) {
					if (lastEdge.getFromNode().getId().equals(toNodeId) && lastEdge.getToNode().getId().equals(fromNodeId)) {
						id = Id.create(nr + 'R', Link.class);
					} else {
						throw new RuntimeException("Duplicate edge.");
					}
				}
				double length = Double.parseDouble(row.get("LAENGE").replace(',', '.')) * 1000;
				double freespeed = 0.0;
				String edgeTypeIdString = row.get("TYPNR");
				Id<EdgeType> edgeTypeId = Id.create(edgeTypeIdString, EdgeType.class);
				double capacity = getCapacity(edgeTypeId);
				int noOfLanes = getNoOfLanes(edgeTypeId);
				// kick out all irrelevant edge types
				if (isEdgeTypeRelevant(edgeTypeId)) {

					//					// definitions for zone30
					//					if(isEdgeInDetailedArea(fromNode, featuresInShape)){
					//						freespeed = 30 / 3.6;
					//						// setting all streets with noOfLanes = 1 to Type 75
					//						if(noOfLanes == 1){
					//							edgeTypeIdString =  "75";
					//							capacity = getCapacity(Id.create(edgeTypeIdString));
					//						}
					//						// setting all other streets to Type 83
					//						else{
					//							edgeTypeIdString =  "83";
					//							capacity = getCapacity(Id.create(edgeTypeIdString));
					//						}
					//						network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, noOfLanes, null, edgeTypeIdString);
					//						usedIds.add(edgeTypeIdString);
					//					}
					//					// kick out all edges in periphery that are irrelevant only there
					//					else {
					//						freespeed = getFreespeedTravelTime(edgeTypeId);
					//						network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, noOfLanes, null, edgeTypeIdString);
					//						usedIds.add(edgeTypeIdString);
					//					}

					// take all edges in detailed area
					if(isEdgeInDetailedArea(fromNode, featuresInShape)){

						if(innerCity30to40KmhIdsNeedingVmaxChange.contains(edgeTypeIdString)){
							freespeed = getFreespeedTravelTime(edgeTypeId) / 2;
						}
						if(innerCity45to60KmhIdsNeedingVmaxChange.contains(edgeTypeIdString)){
							freespeed = getFreespeedTravelTime(edgeTypeId) / 1.5;
						}
						else{
							freespeed = getFreespeedTravelTime(edgeTypeId);
						}
						final Id<Link> id1 = id;
						final Node fromNode1 = fromNode;
						final Node toNode1 = toNode;
						final double length1 = length;
						final double freespeed1 = freespeed;
						final double capacity1 = capacity;
						final double numLanes = noOfLanes;
						final String type = edgeTypeIdString;
						NetworkUtils.createAndAddLink(network,id1, fromNode1, toNode1, length1, freespeed1, capacity1, numLanes, null, type);
						usedIds.add(edgeTypeIdString);
					}
					else {
						if(isEdgeTypeRelevantForPeriphery(edgeTypeId)){
							freespeed = getFreespeedTravelTime(edgeTypeId);
							final Id<Link> id1 = id;
							final Node fromNode1 = fromNode;
							final Node toNode1 = toNode;
							final double length1 = length;
							final double freespeed1 = freespeed;
							final double capacity1 = capacity;
							final double numLanes = noOfLanes;
							final String type = edgeTypeIdString;
							NetworkUtils.createAndAddLink(network,id1, fromNode1, toNode1, length1, freespeed1, capacity1, numLanes, null, type);
							usedIds.add(edgeTypeIdString);
						}
					}

				}
			}
		};
		streamingVisumNetworkReader.addRowHandler("STRECKE", edgeRowHandler);
		streamingVisumNetworkReader.read(InVisumNetFile);
		network.setCapacityPeriod(16*3600);
	}

	private boolean isEdgeTypeRelevant(Id<EdgeType> edgeTypeId) {
		String idString = edgeTypeId.toString();
		if (irrelevantIds.contains(idString)) {
			return false;
		} else {
			return true;
		}
	}

	private boolean isEdgeInDetailedArea(Node fromNode, Collection<SimpleFeature> featuresInShape) {
		boolean isInDetailedArea = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(fromNode.getCoord().getX(), fromNode.getCoord().getY()));
		for (SimpleFeature ft : featuresInShape) {
			if (((Geometry) ft.getDefaultGeometry()).contains(geo)){
				isInDetailedArea = true;
				break;
			}
		}
		return isInDetailedArea;
	}

	private boolean isEdgeTypeRelevantForPeriphery(Id<EdgeType> edgeTypeId) {
		String idString = edgeTypeId.toString();
		if (additionalIrrelevantIdsPeriphery.contains(idString)) {
			return false;
		} else {
			return true;
		}
	}

	private double getCapacity(Id<EdgeType> edgeTypeId) {
		VisumNetwork.EdgeType edgeType = findEdgeType(edgeTypeId);
		double capacity = Double.parseDouble(edgeType.kapIV);
		return capacity;
	}

	private int getNoOfLanes(Id<EdgeType> edgeTypeId) {
		VisumNetwork.EdgeType edgeType = findEdgeType(edgeTypeId);
		int noOfLanes = Integer.parseInt(edgeType.noOfLanes);
		return noOfLanes;
	}

	private double getFreespeedTravelTime(Id<EdgeType> edgeTypeId) {
		VisumNetwork.EdgeType edgeType = findEdgeType(edgeTypeId);
		double v0 = Double.parseDouble(edgeType.v0IV) / 3.6;
		return v0;
	}

	private EdgeType findEdgeType(Id<EdgeType> edgeTypeId) {
		return visumNetwork.edgeTypes.get(edgeTypeId);
	}

	private void readVisumNetwork() {
		visumNetwork = new VisumNetwork();
		log.info("reading visum network.");
		new VisumNetworkReader(visumNetwork).read(InVisumNetFile);
	}

	private void writeNetwork() throws IOException,
	FileNotFoundException {
		Network network = scenario.getNetwork();
		log.info("writing network to file.");
		new NetworkWriter(network).write(OutNetworkFile);
	}

	public static void main(final String[] args) {
		convertVisumNetwork();
	}

	private static void convertVisumNetwork() {
		NetworkVisum2Matsim app = new NetworkVisum2Matsim();
		app.prepareConfig();
		app.readVisumNetwork();
		app.convertNetwork();
		app.cleanNetwork();
		app.dumpEdgeTypes();
		try {
			app.writeNetwork();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

	private void dumpEdgeTypes() {
		for (String usedEdgeId : usedIds) {
			System.out.print(usedEdgeId + " ");
		}
		for (EdgeType edgeType : visumNetwork.edgeTypes.values()) {
			if (usedIds.contains(edgeType.id.toString())) {
				System.out.println(edgeType.id + "   " + edgeType.v0IV + "  " + edgeType.kapIV);
			}
		}
	}

	private void cleanNetwork() {
		new org.matsim.core.network.algorithms.NetworkCleaner().run(scenario.getNetwork());
	}
}
