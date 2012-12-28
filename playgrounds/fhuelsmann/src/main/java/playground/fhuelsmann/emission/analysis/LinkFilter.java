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

package playground.fhuelsmann.emission.analysis;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


public class LinkFilter {
	
	private static final Logger logger = Logger.getLogger(LinkFilter.class);
	private Network network;
//	private Map<Id, double[]> nodeCollector = new TreeMap<Id,double[]>();
	
	private final Map<Id, Node> nodeCollector = new TreeMap<Id, Node>();
	
	public LinkFilter(Network network) {
	super();
	this.network = network;
}
	
	Network getRelevantNetwork(Collection<SimpleFeature> featuresInShape) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network filteredNetwork = scenario.getNetwork();
//		NetworkImpl filteredNetwork = scenario.getNetwork();
			
		for (Node node : network.getNodes().values()) {

			if (isNodeInShape(node, featuresInShape)) {
				Node fromtonode = filteredNetwork.getFactory().createNode(node.getId(),node.getCoord());
				filteredNetwork.addNode(fromtonode);
			}
		}
	
	 
		for(Link link : network.getLinks().values()){
		
			if (filteredNetwork.getNodes().containsKey(link.getFromNode().getId()) && filteredNetwork.getNodes().containsKey(link.getToNode().getId())
					&& !filteredNetwork.getLinks().containsKey(link.getId())){

				Link onelink = filteredNetwork.getFactory().createLink(link.getId(), link.getFromNode().getId(), link.getToNode().getId());
				filteredNetwork.addLink(onelink);
//				System.out.println("onelink " + onelink.getId());
			}
		}	
				
//		System.out.println("Result "+ filteredNetwork +"         " +filteredNetwork.getLinks());
		
		return filteredNetwork;	
	
	}

		
			
	public Map<Id, Node> getNodeCollector() {
		return nodeCollector;
	}
	
	private boolean isNodeInShape(Node node, Collection<SimpleFeature> featuresInShape) {
		boolean isInShape = false;

		Coord nodeCoord = node.getCoord();
		//System.out.println("nodeCoord   " + nodeCoord);
		GeometryFactory factory = new GeometryFactory();
		Coordinate coor = new Coordinate(nodeCoord.getX(), nodeCoord.getY());
		//System.out.println("coor   " + coor);
		Geometry geo = factory.createPoint(coor);
		//System.out.println("geo   " + geo);
		for(SimpleFeature feature : featuresInShape){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				//	logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
	
	
	Collection<SimpleFeature> readShape(String shapeFile) {
		final Collection<SimpleFeature> featuresInShape;
		featuresInShape = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return featuresInShape;
	}

}
