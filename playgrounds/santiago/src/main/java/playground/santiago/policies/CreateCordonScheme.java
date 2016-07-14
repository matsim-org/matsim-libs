/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCordonScheme.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.santiago.policies;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author benjamin
 *
 */
public class CreateCordonScheme {
	private static final Logger log = Logger.getLogger(CreateCordonScheme.class);
	
	String netFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/network/network_merged_cl.xml.gz";
	
//	String cordonShapeFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/policies/cordon_triangle/triangleEPSG32719.shp";
//	String schemeName = "triangleCordon";
	
	String cordonShapeFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/policies/cordon_outer/cordonEPSG32719.shp";
	String schemeName = "outerCordon";
	
	String outFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/policies/" + schemeName + ".xml";

	Network net;
	Collection<SimpleFeature> featuresInCordon;
	Set<Id<Link>> cordonInLinks;
	Set<Id<Link>> cordonOutLinks;
	
	double morningStartTime = 7.5 * 3600.;
	double morningEndTime = 10.0 * 3600.;
	double afternoonStartTime = 18.0 * 3600.;
	double afternoonEndTime = 20.0 * 3600.;
	
	double amountIn = 6000.;
	double amountOut = 2650.;
	
	private void run() {
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(cordonShapeFile);
		featuresInCordon = shapeReader.getFeatureSet();
		
		net = NetworkUtils.createNetwork();
		new MatsimNetworkReader(net).readFile(netFile);
		
		fillCordonLinkSet();
		createLinkPricingFile();
	}

	private void fillCordonLinkSet() {
		cordonInLinks = new HashSet<Id<Link>>();
		cordonOutLinks = new HashSet<Id<Link>>();
		
		for(Link link : net.getLinks().values()){
			if(link.getAllowedModes().contains(TransportMode.pt)){
				continue;
			} else {
				Coord fromNode = link.getFromNode().getCoord();
				Coord toNode = link.getToNode().getCoord();

				if(isFeatureInShape(fromNode)){
					if(!isFeatureInShape(toNode)){
						cordonOutLinks.add(link.getId());
					}
				}
				if(!isFeatureInShape(fromNode)){
					if(isFeatureInShape(toNode)){
						cordonInLinks.add(link.getId());
					}
				}
			}
		}
	}

	private boolean isFeatureInShape(Coord coord) {
		boolean isInShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(SimpleFeature feature : featuresInCordon){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
	
	private void createLinkPricingFile() {
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		scheme.setName(schemeName);
		scheme.setType(scheme.TOLL_TYPE_LINK);
		scheme.setDescription("No description available");
		
		for(Id<Link> linkId : cordonOutLinks){
			scheme.addLink(linkId);
			scheme.addLinkCost(linkId, morningStartTime, morningEndTime, amountOut);
			scheme.addLinkCost(linkId, afternoonStartTime, afternoonEndTime, amountOut);
		}
		
		for(Id<Link> linkId : cordonInLinks){
			scheme.addLink(linkId);
			scheme.addLinkCost(linkId, morningStartTime, morningEndTime, amountIn);
			scheme.addLinkCost(linkId, afternoonStartTime, afternoonEndTime, amountIn);
		}
				
		RoadPricingWriterXMLv1 rpw = new RoadPricingWriterXMLv1(scheme);
		rpw.writeFile(outFile);
	}

	public static void main(String[] args) {
		CreateCordonScheme ccs = new CreateCordonScheme();
		ccs.run();
	}
}
