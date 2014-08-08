/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.sna.gis.CRSUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @author johannes
 *
 */
public class Bast2Counts {

	private static final Logger logger = Logger.getLogger(Bast2Counts.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws FactoryException 
	 */
	public static void main(String[] args) throws IOException, ParseException, FactoryException {
		String bastFile = "/home/johannes/gsv/counts/bast-geolocated2.csv";
		String netFile = "/home/johannes/gsv/osm/germany-network-cat5.simplified3.xml";
		String countsFile = "/home/johannes/gsv/counts/bast-counts.xml";
		
		logger.info("Loading bast counts...");
		BufferedReader reader = new BufferedReader(new FileReader(bastFile));
		
		String line = reader.readLine();
		String[] header = line.split("\t");
		Map<String, Integer> colIdices = new HashMap<String, Integer>();
		for(int i = 0; i < header.length; i++) {
			colIdices.put(header[i], i);
		}
		
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		
		List<CountsData> counts = new ArrayList<CountsData>();
		
		int xIdx = colIdices.get("GeoX");
		int yIdx = colIdices.get("GeoY");
		int valDirect1Idx = colIdices.get("DTV_Kfz_Mo-So_Ri1");
		int valDirect2Idx = colIdices.get("DTV_Kfz_Mo-So_Ri2");
		int nameIdx = colIdices.get("DZ_Name");
		
		int xDirect1 = colIdices.get("Fernziel_Ri1_long");
		int yDirect1 = colIdices.get("Fernziel_Ri1_lat");
		int xDirect2 = colIdices.get("Fernziel_Ri2_long");
		int yDirect2 = colIdices.get("Fernziel_Ri2_lat");
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t", -1);
			
			CountsData data = new CountsData();
			

			data.yCoord  = format.parse(tokens[xIdx]).doubleValue();
			data.xCoord = format.parse(tokens[yIdx]).doubleValue();
			data.name = tokens[nameIdx];
			data.name = data.name.replace("<", "-");
			data.name = data.name.replace(">", "-");
			
			if(tokens[xDirect1].length() > 0)
				data.xDirection1 = Double.parseDouble(tokens[xDirect1]);
			
			if(tokens[yDirect1].length() > 0)
				data.yDirection1 = Double.parseDouble(tokens[yDirect1]);
			
			if(tokens[xDirect2].length() > 0)
				data.xDirection2 = Double.parseDouble(tokens[xDirect2]);
			
			if(tokens[yDirect2].length() > 0)
				data.yDirection2 = Double.parseDouble(tokens[yDirect2]);
			
			
			try {
				data.valDirect1 = format.parse(tokens[valDirect1Idx]).doubleValue();
				data.valDirect2 = format.parse(tokens[valDirect2Idx]).doubleValue();
				counts.add(data);
			} catch (Exception e) {
				e.printStackTrace();
				logger.warn("No count data for " + data.name);
			}
		}
		
		reader.close();
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(netFile);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		
		logger.info("Linking bast counts...");
		GeometryFactory factory = new GeometryFactory(); //.getGeometryFactory(null);
		MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(4326), CRSUtils.getCRS(31467));
		
		Counts theCounts = new Counts();
		theCounts.setYear(2012);
		theCounts.setName("BaSt Zählstellen");
		
		int noreturn = 0;
		for(CountsData data : counts) {
			Point countPos = factory.createPoint(new Coordinate(data.xCoord, data.yCoord));
			countPos = CRSUtils.transformPoint(countPos, transform);
			
			Point direct1 = factory.createPoint(new Coordinate(data.xDirection1, data.yDirection1));
			Point direct2 = factory.createPoint(new Coordinate(data.xDirection2, data.yDirection2));
			
			Link link1 = network.getNearestLinkExactly(MatsimCoordUtils.pointToCoord(countPos));
			Node toNode = link1.getToNode();
			double dist1 = CoordUtils.calcDistance(toNode.getCoord(), MatsimCoordUtils.pointToCoord(direct1));
			double dist2 = CoordUtils.calcDistance(toNode.getCoord(), MatsimCoordUtils.pointToCoord(direct2));
			
			double returnVal;
			if(dist1 < dist2) {
				createCount(theCounts, link1, countPos, data.name, data.valDirect1);
				returnVal = data.valDirect2;
			} else {
				createCount(theCounts, link1, countPos, data.name, data.valDirect2);
				returnVal = data.valDirect1;
			}
			
			Link link2 = NetworkUtils.getConnectingLink(link1.getToNode(), link1.getFromNode());
			if(link2 == null) {
				link2 = getReturnLink(link1, network);
			}
			if(link2 != null) {
				createCount(theCounts, link2, countPos, data.name, returnVal);
			} else {
				noreturn++;
				logger.warn(String.format("No return link found for station %s", data.name));
			}
			
		}
		logger.warn(String.format("%s return links not found.", noreturn));
		
		logger.info("Wrting bast counts...");
		CountsWriter writer = new CountsWriter(theCounts);
		writer.write(countsFile);
		logger.info("Done.");
	}
	
	private static void createCount(Counts theCounts, Link link1, Point countPos, String name, double val) {
		Count count = theCounts.createAndAddCount(link1.getId(), name);
		if(count != null) {
			for(int i = 1; i < 25; i++) {
				count.createVolume(i, val);
			}
			count.setCoord(MatsimCoordUtils.pointToCoord(countPos));
		} else {
			logger.warn("Cannot add two counts for one link (" + name+").");
		}
	}
	
	private static Link getReturnLink(Link link, NetworkImpl network) {
		Collection<Node> fromNodes = getNearestNodes(link.getToNode(), network);
		Collection<Node> toNodes = getNearestNodes(link.getFromNode(), network);
		
		for(Node fromNode : fromNodes) {
			for(Node toNode : toNodes) {
				Link returnLink = NetworkUtils.getConnectingLink(fromNode, toNode);
				if(returnLink != null && returnLink != link) {
					return returnLink;
				}
			}
		}
		
		return null;
	}
	
	private static Collection<Node> getNearestNodes(Node node, NetworkImpl network) {
		double delta = 10;
		double step = 10;
		double minsize = 5;
		
		Collection<Node> nodes = network.getNearestNodes(node.getCoord(), delta);
		while(nodes.size() < minsize) {
			delta += step;
			nodes = network.getNearestNodes(node.getCoord(), delta);
		}
//		nodes.remove(node);
		
		return nodes;
	}
	
	private static class CountsData {
		
		private double xCoord;
		
		private double yCoord;
		
		private double valDirect1;
		
		private double valDirect2;
		
		private String name;
		
		private double xDirection1;
		
		private double yDirection1;
		
		private double xDirection2;
		
		private double yDirection2;
	}

}
