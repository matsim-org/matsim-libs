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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.santiago.network.AddTollToTollways;

/**
 * @author benjamin
 *
 */
public class CreateCordonScheme {
	private static final Logger log = Logger.getLogger(CreateCordonScheme.class);
	
	String netFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/network/network_merged_cl.xml.gz";
	String gantriesShapeFile = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/toll/gantriesWithFares/gantriesAndFares2012.shp";
	
	String cordonShapeFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/policies/cordon_outer/modifiedCordon/modifiedCordonEPSG32719.shp";
	String schemeName = "outerCordonWithTolledTollways";
	RoadPricingSchemeImpl initialScheme;

	String outFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/policies/" + schemeName + ".xml";

	Network net;
	Collection<SimpleFeature> featuresInCordon;
	Set<Id<Link>> cordonInLinks;
	Set<Id<Link>> cordonOutLinks;
	//Be aware of this.
	boolean includeTolledTollways = true;
	//
	double morningStartTime = 7.5 * 3600.;
	double morningEndTime = 10.0 * 3600.;
	double afternoonStartTime = 18.0 * 3600.;
	double afternoonEndTime = 20.0 * 3600.;
	
	double amountIn;
	double amountOut;
	
	private void run() {
		
		//should always be called...
		if(includeTolledTollways){		
		AddTollToTollways att = new AddTollToTollways(gantriesShapeFile,netFile,schemeName);
		att.createNetworkFeatures();
		att.collectInformation();
		this.initialScheme = att.createGantriesFile();
		} else {
		this.initialScheme = new RoadPricingSchemeImpl();
		}
		
		//fares are different for each scheme (see page 5-13 for outerCordon, 5-14 for triangleCordon)
		if(schemeName.substring(0,1).equals("o")){
			this.amountIn=6000;
			this.amountOut=3600;
		} else if(schemeName.substring(0,1).equals("t")) {
			this.amountIn=6000;
			this.amountOut=2650;
		}
		
		
		
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(cordonShapeFile);
		featuresInCordon = shapeReader.getFeatureSet();
		
		net = NetworkUtils.createNetwork();
		new MatsimNetworkReader(net).readFile(netFile);
		
		fillCordonLinkSet();
		removeAndAddSomeLinksFromCordonLinkSet();
		createLinkPricingFile(initialScheme);
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
	
	//Be aware of this special method! It will not work with another net file
	private void removeAndAddSomeLinksFromCordonLinkSet(){
		if(schemeName.substring(0,1).equals("o")){
			//removing...
			cordonOutLinks.remove(Id.createLinkId("18442"));
			cordonInLinks.remove(Id.createLinkId("18441"));
			//adding...
			cordonOutLinks.add(Id.createLinkId("14132"));
			//done.
		} else if(schemeName.substring(0,1).equals("t")) {
		//Everything is ok.

		}
	}
	
	private void createLinkPricingFile(RoadPricingSchemeImpl initialScheme) {

		for(Id<Link> linkId : cordonOutLinks){
			initialScheme.addLink(linkId);
			initialScheme.addLinkCost(linkId, morningStartTime, morningEndTime, amountOut);
			initialScheme.addLinkCost(linkId, afternoonStartTime, afternoonEndTime, amountOut);
		}
		
		for(Id<Link> linkId : cordonInLinks){
			initialScheme.addLink(linkId);
			initialScheme.addLinkCost(linkId, morningStartTime, morningEndTime, amountIn);
			initialScheme.addLinkCost(linkId, afternoonStartTime, afternoonEndTime, amountIn);
		}
				
		RoadPricingWriterXMLv1 rpw = new RoadPricingWriterXMLv1(initialScheme);
		rpw.writeFile(outFile);
	}

	public static void main(String[] args) {
		CreateCordonScheme ccs = new CreateCordonScheme();
		ccs.run();
	}
}
