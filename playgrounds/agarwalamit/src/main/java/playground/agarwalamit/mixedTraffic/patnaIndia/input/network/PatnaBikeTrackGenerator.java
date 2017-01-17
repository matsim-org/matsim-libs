/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class PatnaBikeTrackGenerator {

	private final String railShapeFile = PatnaUtils.INPUT_FILES_DIR + "/raw/network/railPatna/railPatna_multiPart2SinglePart.shp"; // Bike is laid parallel to rail track.
	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final Network network = this.scenario.getNetwork();
	private final Map<Coord, String> coordId = new HashMap<>();
	private final CoordinateTransformation ct= PatnaUtils.COORDINATE_TRANSFORMATION;
	
	public static void main(String[] args) {
		String outNetworkFile = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
		
		PatnaBikeTrackGenerator track = new PatnaBikeTrackGenerator();
		track.process();
		new NetworkWriter(track.getNetwork()).write(outNetworkFile);
	}

	public void process(){
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(railShapeFile);

		for(SimpleFeature sf : features){
			// reads every feature here (corresponding to every line in attribute table)

			if(sf.getFeatureType() instanceof SimpleFeatureTypeImpl){

				if( sf.getAttribute("type").equals("platform")) continue;

				String osmId = (String)sf.getAttribute("osm_id") ;

				MultiLineString mls = (MultiLineString) sf.getAttributes().get(0);

				Coordinate[] coords = mls.getCoordinates();

				// number of coordinates must not be more than 2.
				if ( coords.length!=2 ) throw new RuntimeException("Number of coordinates are more than 2 in the MultiLineString "+sf.getAttributes().toString());

				addBiDirectionLinks(coords,osmId);
			}
		}
		// since the shape has many rail lines, some are deleted manually because only one line is enought to get the shape of bike track
		// manually edit few things.
		
		// joint nodes to create a link
		{
			Node n1 = this.network.getNodes().get(Id.createNodeId("428435458_node249"));
			Node n2 = this.network.getNodes().get(Id.createNodeId("395991040_node208"));
			createAndAddLink(new Node [] {n1,  n2}, "manuallyAdded");
			createAndAddLink(new Node [] {n2,  n1}, "manuallyAdded");
		}
		{
			Node n1 = this.network.getNodes().get(Id.createNodeId("97953615_node20"));
			Node n2 = this.network.getNodes().get(Id.createNodeId("428413768_node231"));
			createAndAddLink(new Node [] {n1,  n2}, "manuallyAdded");
			createAndAddLink(new Node [] {n2,  n1}, "manuallyAdded");
		}
		{
			Node n1 = this.network.getNodes().get(Id.createNodeId("129093865_node44"));
			Node n2 = this.network.getNodes().get(Id.createNodeId("428413768_node231"));
			createAndAddLink(new Node [] {n1,  n2}, "manuallyAdded");
			createAndAddLink(new Node [] {n2,  n1}, "manuallyAdded");
		}
	}
	
	public Network getNetwork(){
		return this.network;
	}

	private void addBiDirectionLinks(Coordinate[] coords, String osmId) {
		Node [] nodes = addOrGetNode(coords, osmId);
		createAndAddLink(nodes, osmId);

		//reverse link
		createAndAddLink(new Node [] {nodes[1], nodes[0]}, osmId);
	}
	
	private void createAndAddLink(Node [] nodes, String osmId) {
		int noOfLinks = this.network.getLinks().size();
		String id = osmId+"_link"+noOfLinks;
		double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());
		Set<String> modes = new HashSet<>();
		modes.add("bike");
		
		Id<Link> linkId = Id.createLinkId(id);
		Link l = network.getFactory().createLink(linkId, nodes[0], nodes[1]);
		l.setAllowedModes(modes );
		l.setLength(dist);
		l.setCapacity(900); // half of the one lane capacity
		l.setFreespeed(25/3.6); // lets keep this also lower than a normal car link
		l.setNumberOfLanes(1);
		this.network.addLink(l);
	}

	private Node [] addOrGetNode(Coordinate [] coords, String osmId) {
		Node [] nodes = new Node [coords.length];
		for ( int i=0; i<coords.length;i++) {
			Coord c = new Coord(coords[i].x, coords[i].y);
			Coord transformedCoord = ct.transform(c);
			String id = this.coordId.get(transformedCoord);

			if(id==null) {
				id = osmId+"_node"+ this.network.getNodes().size();
				Node n = this.network.getFactory().createNode(Id.createNodeId(id), transformedCoord);
				this.coordId.put(transformedCoord, id);
				this.network.addNode(n);
			} 
			nodes[i]=this.network.getNodes().get(Id.createNodeId(id));
		}
		return nodes;
	}
}