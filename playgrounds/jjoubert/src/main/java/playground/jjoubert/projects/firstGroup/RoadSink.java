/* *********************************************************************** *
 * project: org.matsim.*
 * RoadSink.java
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

/**
 * 
 */
package playground.jjoubert.projects.firstGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * Class to help parse the roads, their road names and/or road references from
 * OpenStreetMap data.
 *  
 * @author jwjoubert
 */
public class RoadSink implements Sink {
	final private static Logger LOG = Logger.getLogger(RoadSink.class);

	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	final private String nodeFile;
	final private String wayFile;
	private Counter entityCounter = new Counter("  entity # ");
	private Counter wayCounter = new Counter("  way # ");
	private CoordinateTransformation ct;

	public RoadSink(String wayFile, String nodeFile) {
		this.nodeFile = nodeFile;
		this.wayFile = wayFile;
		this.ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, 
				TransformationFactory.HARTEBEESTHOEK94_LO29);
	}

	@Override
	public void initialize(Map<String, Object> arg0) {
		LOG.info("Initialising OSM sink...");
		this.nodeMap = new HashMap<>();
		this.wayMap = new HashMap<>();
		this.relationMap = new HashMap<>();
	}

	@Override
	public void complete() {
		entityCounter.printCounter();
		LOG.info("Completing OSM sink...");

		/* Report the size of the maps */
		LOG.info("    Number of nodes: " + nodeMap.size());
		LOG.info("     Number of ways: " + wayMap.size());
		LOG.info("Number of relations: " + relationMap.size());

		processWays(wayMap);
	}


	public void processWays(Map<Long,? extends EntityContainer> entityMap){
		LOG.info("Processing ways...");

		BufferedWriter nodeWriter = IOUtils.getBufferedWriter(nodeFile);
		BufferedWriter wayWriter = IOUtils.getBufferedWriter(wayFile);
		try{
			/* Headers */
			nodeWriter.write("wid,seq,nfid,flon,flat,ntid,tlon,tlat");
			nodeWriter.newLine();
			wayWriter.write("id,name,ref,length");
			wayWriter.newLine();

			/* Process ways */
			for(long id : wayMap.keySet()){
				WayContainer wc = wayMap.get(id);
				Collection<Tag> tags = wc.getEntity().getTags();
				tags.isEmpty();
				Map<String, Object> mTags = wc.getEntity().getMetaTags();
				mTags.isEmpty();

				String name = null;
				String ref = null;
				for(Tag tag : tags){
					if(tag.getKey().equalsIgnoreCase("name")){
						name = tag.getValue().replaceAll(",", "");
					}
					if(tag.getKey().equalsIgnoreCase("ref")){
						ref = tag.getValue().replaceAll(",", "");
					}
				}
				double dist = convertWayNodesToDistance(wc.getEntity().getWayNodes());

				/* Build the line. */
				String line = String.format(
						"%d,%s,%s,%.0f\n",
						id,
						(name == null ? "NA" : name),
						(ref == null ? "NA" : ref),
						dist);

				wayWriter.write(line);
				wayWriter.newLine();
				
				/* Write the way's nodes */
				printWayNodes(wc, nodeWriter);
				
				wayCounter.incCounter();
			}

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + wayFile);
		} finally{
			try {
				wayWriter.close();
				nodeWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close BufferedWriter");
			}
		}
		wayCounter.printCounter();
		LOG.info("Done processing ways.");
	}

	private double convertWayNodesToDistance(List<WayNode> nodes){
		double dist = 0.0;

		Node previousNode = findNode(nodes.get(0));
		Coord previousCoord = ct.transform(CoordUtils.createCoord(previousNode.getLongitude(), previousNode.getLatitude()));
		for(int i = 1; i < nodes.size(); i++){
			Node thisNode = findNode(nodes.get(i));
			Coord thisCoord = ct.transform(CoordUtils.createCoord(thisNode.getLongitude(), thisNode.getLatitude()));
			dist += CoordUtils.calcEuclideanDistance(previousCoord, thisCoord);
			previousNode = thisNode;
			previousCoord = thisCoord;
		}

		return dist;
	}
	
	private void printWayNodes(WayContainer container, BufferedWriter bw){
		int seq = 1;
		List<WayNode> wayNodes = container.getEntity().getWayNodes();
		Node fromNode = findNode(wayNodes.get(0));
		
		for(int i = 1; i < wayNodes.size(); i++){
			Node toNode = findNode(wayNodes.get(i));
			String line = String.format("%d,%d,%d,%.60f,%.60f,%d,%.60f,%.60f\n",
					container.getEntity().getId(),
					seq++,
					fromNode.getId(),
					fromNode.getLongitude(),
					fromNode.getLatitude(),
					toNode.getId(),
					toNode.getLongitude(),
					toNode.getLatitude());
			try {
				bw.write(line);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + bw.toString());
			}
			
			fromNode = toNode;
		}
	}
	
	
	private Node findNode(WayNode waynode){
		Node node = null;
		NodeContainer nc = nodeMap.get(waynode.getNodeId());
		if(nc == null){
			LOG.error("Could not find Node `" + waynode.getNodeId() + "' in the node map. ");
			LOG.error("OSM file should be parsed with osmosis with the argument `--un'");
			throw new RuntimeException("Aborting... as we NEED all the way nodes.");
		}
		node = nc.getEntity();
		
		return node;
	}


	/**
	 * Do nothing.
	 */
	@Override
	public void release() {
		LOG.info("Releasing OSM sink...");
	}

	/**
	 * Process the different containers by just adding them to the associated
	 * map for later processing. 
	 */
	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {

			@Override
			public void process(RelationContainer relationContainer) {
				relationMap.put(relationContainer.getEntity().getId(), relationContainer);
			}

			@Override
			public void process(WayContainer wayContainer) {
				wayMap.put(wayContainer.getEntity().getId(), wayContainer);
			}

			@Override
			public void process(NodeContainer nodeContainer) {
				nodeMap.put(nodeContainer.getEntity().getId(), nodeContainer);
			}

			@Override
			public void process(BoundContainer arg0) {
				// Ignore
			}
		});
		entityCounter.incCounter();
	}

}
