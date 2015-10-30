/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityMatcher.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.nmviljoen.grain.facilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v2;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to read in facilities (producers and processors) and match them to the
 * closest node(s) in the path-dependent complex network.
 * 
 * @author jwjoubert
 */
public class FacilityMatcher {
	final private static Logger log = Logger.getLogger(FacilityMatcher.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FacilityMatcher.class.toString(), args);
		String facilitiesFile = args[0];
		String producerFile = args[1];
		String processorFile = args[2];
		String network = args[3];
		String output = args[4];
		String outputRange = args[5];
		
		/* Clear the output file. */
		File file = new File(output);
		if(file.exists()){
			log.warn("Output file " + output + " will be overwritten!");
			FileUtils.delete(file);
		}
		File fileRange = new File(outputRange);
		if(fileRange.exists()){
			log.warn("Output file " + outputRange + " will be overwritten!");
			FileUtils.delete(fileRange);
		}
		
		/* Write headers. */
		BufferedWriter bwOutput = IOUtils.getAppendingBufferedWriter(output);
		BufferedWriter bwRange = IOUtils.getAppendingBufferedWriter(outputRange);
		try{
			/* Write the output file's header. */
			bwOutput.write("Input,Id,lon,lat,within50,within100,within250,within500,within1000");
			bwOutput.newLine();
			
			/* Write the range file's header. */
			bwRange.write("oId,oLon,oLat,dId,dLon,dLat,Direction,Level");
			bwRange.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to output file(s)");
		} finally{
			try {
				bwOutput.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close output file(s)");
			}
		}
		
		FacilityMatcher fm = new FacilityMatcher();
		
		/* Parse the facilities resulting from clustering. */
		ActivityFacilities facilities = fm.parseFacilities(facilitiesFile);

		/* Match the facilities with the nodes in the network. */
		DigicorePathDependentNetworkReader_v2 nr = new DigicorePathDependentNetworkReader_v2();
		nr.parse(network);
		PathDependentNetwork pdn = nr.getPathDependentNetwork();
		pdn.writeNetworkStatisticsToConsole();
		ActivityFacilities matchedFacilities = fm.matchFacilitiesWithNetworkNodes(facilities, pdn);
		
		QuadTree<ActivityFacility> qtFacilities = fm.buildFacilitiesQT(matchedFacilities);
		
		/* Perform basic range analyses. */
		fm.matchRange(producerFile, 6, 5, 2, 1, qtFacilities, output);
		fm.matchRange(processorFile, 5, 4, 1, 2, qtFacilities, output);
		
		/* Evaluate the network reach, in stages. */
		double rangeThreshold = 50.0;
		fm.evaluateNetworkReach(qtFacilities, pdn, producerFile, rangeThreshold, 6, 5, 2, outputRange);
		
		
		Header.printFooter();
	}
	
	public FacilityMatcher() {

	}
	
	public ActivityFacilities matchFacilitiesWithNetworkNodes(ActivityFacilities facilities, PathDependentNetwork network){
		log.info("Matching the facilities with the nodes in the network...");
		log.info("          Facilities file: " + facilities.getName());
		log.info("   Path-dependent network: " + network.getDescription());
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
			boolean isNodeInNetwork = network.getPathDependentNodes().containsKey(Id.create(id.toString(), Node.class));
			if(isNodeInNetwork){
				sc.getActivityFacilities().addActivityFacility(facilities.getFacilities().get(id));
			}
		}
		
		log.info("Done matching.");
		log.info("----------------------------------------------");
		log.info("         Number of facilities: " + facilities.getFacilities().size());
		log.info("     Number of nodes matching: " + sc.getActivityFacilities().getFacilities().size());
		log.info("----------------------------------------------");
		
		return sc.getActivityFacilities();
	}
	
	
	public ActivityFacilities parseFacilities(String filename){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario ).parse(filename);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		return facilities;
	}
	
	
	/**
	 * Reads an {@link ActivityFacilities} file, and building a {@link QuadTree}
	 * from them.
	 * @param filename
	 * @return
	 */
	public QuadTree<ActivityFacility> buildFacilitiesQT(ActivityFacilities facilities){
		log.info("Populating the QuadTree of facilities.");
		/* Determine the extent of the facilities file. */
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
			ActivityFacility facility = facilities.getFacilities().get(id);
			Coord c = facility.getCoord();
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
			maxX = Math.max(maxX, c.getX());
			maxY = Math.max(maxY, c.getY());
		}
		
		/* Build the QuadTree. */
		QuadTree<ActivityFacility> qt = new QuadTree<ActivityFacility>(minX, minY, maxX, maxY);
		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
			ActivityFacility facility = facilities.getFacilities().get(id);
			Coord c = facility.getCoord();
			qt.put(c.getX(), c.getY(), facility);
		}
		
		log.info("Done populating the Quadtree of facilities (" + qt.size() + " found)");
		return qt;
	}
	
	public void matchRange(String filename, int xField, int yField, int idField, 
			int fileId, QuadTree<ActivityFacility> qt, String output){
		log.info("Evaluating the number of facilities within range from " + filename);
		
		/* Set up some counters. */
		int numberWithin0050 = 0;
		int numberWithin0100 = 0;
		int numberWithin0250 = 0;
		int numberWithin0500 = 0;
		int numberWithin1000 = 0;
		int producers = 0;
		
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(output);
		try{
			
			String line = br.readLine(); // Header.
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				Coord cWgs = null;
				try{
					cWgs = new Coord(Double.parseDouble(sa[xField]), Double.parseDouble(sa[yField]));
				} catch(NumberFormatException ee){
					log.debug("Ooops!!");
				} catch(ArrayIndexOutOfBoundsException eee){
					log.debug("Ooops!!");
				}
				Coord cAlbers = ct.transform(cWgs);
				
				boolean oddCoord = false;
				if(Math.abs(cWgs.getX()) > 90.0 || Math.abs(cWgs.getY()) > 90.0){
					oddCoord = true;
					log.debug("Why is there a coordinate out of range?!");
					log.debug(String.format("    %s: (%.4f; %.4f)", sa[idField], cWgs.getX(), cWgs.getY()));
				}
				
				Collection<ActivityFacility> within0050 = qt.getDisk(cAlbers.getX(), cAlbers.getY(), 50.0);
				Collection<ActivityFacility> within0100 = qt.getDisk(cAlbers.getX(), cAlbers.getY(), 100.0);
				Collection<ActivityFacility> within0250 = qt.getDisk(cAlbers.getX(), cAlbers.getY(), 250.0);
				Collection<ActivityFacility> within0500 = qt.getDisk(cAlbers.getX(), cAlbers.getY(), 500.0);
				Collection<ActivityFacility> within1000 = qt.getDisk(cAlbers.getX(), cAlbers.getY(), 1000.0);
				
				/* Update statistics. */
				numberWithin0050 += Math.min(1, within0050.size());
				numberWithin0100 += Math.min(1, within0100.size());
				numberWithin0250 += Math.min(1, within0250.size());
				numberWithin0500 += Math.min(1, within0500.size());
				numberWithin1000 += Math.min(1, within1000.size());
				
				/* Write the output. */
				String s = String.format("%d,%s,%.6f,%.6f,%d,%d,%d,%d,%d\n", 
						fileId,
						sa[idField],
						Double.parseDouble(sa[xField]),
						Double.parseDouble(sa[yField]),
						within0050.size(),
						within0100.size(),
						within0250.size(),
						within0500.size(),
						within1000.size() );
				bw.write(s);
				
				producers++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		
		/* Report ranges. */
		log.info("Total number of input locations: " + producers);
		log.info("------------------------------------------");
		log.info("   Facilities within...");
		log.info("        50m: " + numberWithin0050);
		log.info("       100m: " + (numberWithin0100 - numberWithin0050));
		log.info("       250m: " + (numberWithin0250 - numberWithin0100));
		log.info("       500m: " + (numberWithin0500 - numberWithin0250));
		log.info("      1000m: " + (numberWithin1000 - numberWithin0500));
		log.info("     >1000m: " + (producers - numberWithin1000));
		log.info("------------------------------------------");
		log.info("Done evaluating facilities within range.");
	}
	
	public void evaluateNetworkReach(QuadTree<ActivityFacility> qt, 
			PathDependentNetwork network, String producers, 
			double rangeThreshold, int xField, int yField, int idField,
			String output){
		log.info("Evaluating the network reach...");
		
		CoordinateTransformation ctWgsToAlbers = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		CoordinateTransformation ctAlbersToWgs = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		Counter counter = new Counter("   producers # ");

		BufferedReader br = IOUtils.getBufferedReader(producers);
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(output);
		try{
			String line = br.readLine(); // Header.
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				Coord cWgs = null;
				try{
					cWgs = new Coord(Double.parseDouble(sa[xField]), Double.parseDouble(sa[yField]));
				} catch(NumberFormatException ee){
					log.debug("Ooops!!");
				} catch(ArrayIndexOutOfBoundsException eee){
					log.debug("Ooops!!");
				}
				Coord cAlbers = ctWgsToAlbers.transform(cWgs);
				
				Collection<ActivityFacility> withinRange = qt.getDisk(cAlbers.getX(), cAlbers.getY(), rangeThreshold);
				Iterator<ActivityFacility> iterator = withinRange.iterator();
				List<String> outputStrings = new ArrayList<String>();
				while(iterator.hasNext()){
					ActivityFacility af = iterator.next();
					Id<Node> thisNodeId = Id.createNodeId(af.getId().toString());
					
					/* Only check for facilities that do occur in the path-dependent network. */
					if(network.getPathDependentNode(thisNodeId) != null){
						
						/* Upstream, level 1 */
						outputStrings.addAll(this.getUpstreamOutput(ctAlbersToWgs, network, thisNodeId, 1));
						
						/* Upstream, level 2 */
						Iterator<Id<Node>> level1IteratorUp = network.getConnectedInNodeIds(thisNodeId).iterator();
						while(level1IteratorUp.hasNext()){
							Id<Node> level1IdUp = level1IteratorUp.next();
							outputStrings.addAll(this.getUpstreamOutput(ctAlbersToWgs, network, level1IdUp, 2));
							
							/* Upstream, level 3 */
							Iterator<Id<Node>> level2IteratorUp = network.getConnectedInNodeIds(level1IdUp).iterator();
							while(level2IteratorUp.hasNext()){
								Id<Node> level2IdUp = level2IteratorUp.next();
								outputStrings.addAll(this.getUpstreamOutput(ctAlbersToWgs, network, level2IdUp, 3));
								
								/* Upstream, level 4 */
								Iterator<Id<Node>> level3IteratorUp = network.getConnectedInNodeIds(level2IdUp).iterator();
								while(level3IteratorUp.hasNext()){
									Id<Node> level3IdUp = level3IteratorUp.next();
									outputStrings.addAll(this.getUpstreamOutput(ctAlbersToWgs, network, level3IdUp, 4));
								}
							}
						}
						
						/* Downstream, level 1. */
						outputStrings.addAll(this.getDownstreamOutput(network, thisNodeId, 1));
						
						/* Downstream, level 2. */
						Iterator<Id<Node>> level1IteratorDown = network.getConnectedOutNodeIds(thisNodeId).iterator();
						while(level1IteratorDown.hasNext()){
							Id<Node> level1IdDown = level1IteratorDown.next();
							outputStrings.addAll(this.getDownstreamOutput(network, level1IdDown, 2));

							/* Downstream, level 3. */
							Iterator<Id<Node>> level2IteratorDown = network.getConnectedOutNodeIds(level1IdDown).iterator();
							while(level2IteratorDown.hasNext()){
								Id<Node> level2IdDown = level2IteratorDown.next();
								outputStrings.addAll(this.getDownstreamOutput(network, level2IdDown, 3));
								
								/* Downstream, level 4. */
								Iterator<Id<Node>> level3IteratorDown = network.getConnectedOutNodeIds(level2IdDown).iterator();
								while(level3IteratorDown.hasNext()){
									Id<Node> level3IdDown = level3IteratorDown.next();
									outputStrings.addAll(this.getDownstreamOutput(network, level3IdDown, 4));
								}
							}
						}
						
						/* Now write them all to file. */
						for(String s : outputStrings){
							bw.write(s);
							bw.newLine();
						}
					}
					
					counter.incCounter();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + producers);
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + producers);
			}
		}
		counter.printCounter();
		
		log.info("Done evaluating network reach.");
	}
	
	private List<String> getUpstreamOutput(
			CoordinateTransformation ct, PathDependentNetwork network, Id<Node> nodeId, int level){
		List<String> list = new ArrayList<String>();
		
		Coord thisCoord = ct.transform(network.getPathDependentNode(nodeId).getCoord());

		Iterator<Id<Node>> inIterator = network.getConnectedInNodeIds(nodeId).iterator();
		while(inIterator.hasNext()){
			Id<Node> inNodeId = inIterator.next();
			PathDependentNode inNode = network.getPathDependentNode(inNodeId);
			
			for(int i = 0; i < level; i++){
				
			}
			
			/*FIXME Remove after debugging. */
			if(inNode == null){
				log.debug("Oops!! Null node!!");
			}
			
			Coord inCoord = ct.transform(inNode.getCoord());
			String sIn = String.format("%s,%.6f,%.6f,%s,%.6f,%.6f,Up,%d", 
					inNodeId.toString(),
					inCoord.getX(),
					inCoord.getY(),
					nodeId.toString(),
					thisCoord.getX(),
					thisCoord.getY(),
					level);
			list.add(sIn);
		}
		
		return list;
	}

	private List<String> getDownstreamOutput(PathDependentNetwork network, Id<Node> nodeId, int level){
		List<String> list = new ArrayList<String>();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		Coord thisCoord = ct.transform(network.getPathDependentNode(nodeId).getCoord());
		
		Iterator<Id<Node>> outIterator = network.getConnectedOutNodeIds(nodeId).iterator();
		while(outIterator.hasNext()){
			Id<Node> outNodeId = outIterator.next();
			PathDependentNode outNode = network.getPathDependentNode(outNodeId);
			
			/*FIXME Remove after debugging. */
			if(outNode == null){
				log.debug("Oops!! Null node!!");
			}
			
			Coord outCoord = ct.transform(outNode.getCoord());
			String sOut = String.format("%s,%.6f,%.6f,%s,%.6f,%.6f,Down,%d", 
					nodeId.toString(),
					thisCoord.getX(),
					thisCoord.getY(),
					outNodeId.toString(),
					outCoord.getX(),
					outCoord.getY(),
					level);
			list.add(sOut);
		}
		
		return list;
	}
	
}
