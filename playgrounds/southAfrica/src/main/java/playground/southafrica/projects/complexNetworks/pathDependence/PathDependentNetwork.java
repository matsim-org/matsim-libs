/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.pathDependence;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;

public class PathDependentNetwork {
	private final Logger LOG = Logger.getLogger(PathDependentNetwork.class);
	private Map<Id<Node>, PathDependentNode> network;
	private Double totalSourceWeight = null;
	private Random random;
	private String description = null;
	private long buildStartTime;
	private long buildEndTime;
	
	private static int sinkOnly = 0;
	private static int noSink = 0;
	private static int totalNextNodesSampled = 0;
	
	
	/**
	 * Instantiating a path-dependent network for the Digicore data. This 
	 * constructor creates its own {@link Random} object for sampling. If you
	 * require a deterministic outcome, rather use
	 * {@link PathDependentNetwork#PathDependentNetwork(long))}. 
	 * @param seed
	 */
	public PathDependentNetwork() {
		this(new Random().nextLong());
	}
	
	
	/**
	 * Instantiating a path-dependent network for the Digicore data. This 
	 * constructor requires a seed value, and is mainly used for test purposes,
	 * or when you require some deterministic outcome. When using this class in 
	 * applications, also consider using {@link PathDependentNetwork#PathDependentNetwork()}. 
	 * @param seed
	 */
	public PathDependentNetwork(long seed) {
		MatsimRandom.reset(seed);
		this.random = MatsimRandom.getRandom();
		this.network = new TreeMap<>();
	}
	
	
	public Map<Id<Node>, PathDependentNode> getPathDependentNodes(){
		return this.network;
	}
	
	public void setDescription(String string){
		this.description = string;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	
	/**
	 * Method (only) used for test purposes. 
	 * @return
	 */
	public Random getRandom(){
		return this.random;
	}
	
	
	public void processActivityChain(DigicoreChain chain){
		/* Elements of the chain that we want to retain with the specific 
		 * source node. This is usable when we sample an activity chain later
		 * from the path-dependent complex network. */
		DigicoreActivity firstMajor = chain.get(0);
		int startHour = firstMajor.getEndTimeGregorianCalendar().get(Calendar.HOUR_OF_DAY);
		int numberOfActivities = chain.getNumberOfMinorActivities();
		
		/* Process the first activity pair, but only if both activities are 
		 * associated with a facility. */
		Id<Node> previousNodeId = Id.createNodeId("source");
		Id<Node> currentNodeId = Id.createNodeId(chain.get(0).getFacilityId());
		Id<Node> nextNodeId = Id.createNodeId(chain.get(1).getFacilityId());
		
		if(currentNodeId != null && nextNodeId != null){
			/* Add the current node to the network if it doesn't already exist. */
			if(!this.network.containsKey(currentNodeId)){
				this.network.put(currentNodeId, new PathDependentNode(currentNodeId, chain.get(0).getCoord()));
			} 
			PathDependentNode currentNode = this.network.get(currentNodeId);
			
			/* Add the next node to the network if it doesn't already exist. */
			if(!this.network.containsKey(nextNodeId)){
				this.network.put(nextNodeId, new PathDependentNode(nextNodeId, chain.get(1).getCoord()));
			} 
			
			/* Add the first path-dependent link to the network. */
			currentNode.addSourceLink(startHour, numberOfActivities, nextNodeId);
			
			/* Ensure that the next node reflects the fact that it can be
			 * reached from the currentNode. */
			this.network.get(nextNodeId).establishPathDependence(currentNodeId);
		}
		previousNodeId = currentNodeId == null ? Id.create("unknown", Node.class) : currentNodeId;

		/* Process the remainder of the (minor) activity pairs. */
		for(int i = 1; i < chain.size()-1; i++){
			currentNodeId = Id.create(chain.get(i).getFacilityId(), Node.class);
			nextNodeId = Id.create(chain.get(i+1).getFacilityId(), Node.class);

			if(currentNodeId != null && nextNodeId != null){
				/* Add the current node to the network if it doesn't already exist. */
				if(!this.network.containsKey(currentNodeId)){
					this.network.put(currentNodeId, new PathDependentNode(currentNodeId, chain.get(i).getCoord() ) );
				} 
				PathDependentNode currentNode = this.network.get(currentNodeId);
				
				/* Add the next node to the network if it doesn'talready exist. */
				if(!this.network.containsKey(nextNodeId)){
					this.network.put(nextNodeId, new PathDependentNode(nextNodeId, chain.get(i+1).getCoord() ) );
				} 
				PathDependentNode nextNode = this.network.get(nextNodeId);

				/* Add the path-dependent link to the network. */
				currentNode.addPathDependentLink(previousNodeId, nextNodeId);
				
				/* Ensure that the next node reflects the fact that it can be
				 * reached from the currentNode. */
				this.network.get(nextNodeId).establishPathDependence(currentNodeId);
			}
			previousNodeId = currentNodeId == null ? Id.create("unknown", Node.class) : currentNodeId;
		}

		/* Process the last activity pair. No link needs to be added, but we 
		 * just have to make sure it is indicated as sink. This is only 
		 * necessary if the node already exists in the network. If not, it means
		 * it was never part of a link that was added to the network anyway. */
		currentNodeId = Id.create(chain.get(chain.size()-1).getFacilityId(), Node.class);
		if(currentNodeId != null && this.network.containsKey(currentNodeId)){
			this.network.get(currentNodeId).setAsSink(previousNodeId);
		}
	}
	
	
	public int getNumberOfNodes() {
		return this.network.size();
	}
	
	
	public int getNumberOfEdges(){
		int totalNumberOfEdges = 0;
		for(PathDependentNode node : this.network.values()){
			totalNumberOfEdges += node.getOutDegree();
		}
		return totalNumberOfEdges;
	}
	
	
	public PathDependentNode getPathDependentNode(Id<Node> id){
		return this.network.get(id);
	}
	
	
	public double getPathDependentWeight(Id<Node> previousId, Id<Node> currentId, Id<Node> nextId){
		double d = 0.0;
		
		/* Sort out null Ids for sources and sinks. */
		if(previousId == null) previousId = Id.create("source", Node.class);
		if(nextId == null) nextId = Id.create("sink", Node.class);
		if(currentId == null){
			throw new IllegalArgumentException("Cannot have a 'null' Id for current node.");
		}
		
		if(this.network.containsKey(currentId)){
			d = this.network.get(currentId).getPathDependentWeight(previousId, nextId);
		}
		
		return d;
	}
	
	public double getWeight(Id<Node> fromId, Id<Node> toId){
		double d = 0.0;
		if(this.network.containsKey(fromId)){
			d = this.network.get(fromId).getWeight(toId);
		}
		return d;
	}
	
	
	public void buildNetwork(List<File> fileList) {
		LOG.info("Building network... number of vehicle files to process: " + fileList.size());
		Counter xmlCounter = new Counter("   vehicles completed: ");
		
		buildStartTime = System.currentTimeMillis();
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		for(File f : fileList){
			/* Read the vehicle file. */
			dvr.parse(f.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			/* Process vehicle's chains. */
			for(DigicoreChain dc : dv.getChains()){
				this.processActivityChain(dc);
			}
			xmlCounter.incCounter();
		}
		xmlCounter.printCounter();
		buildEndTime = System.currentTimeMillis();
		
		writeNetworkStatisticsToConsole();
	}

	
	public void addNewPathDependentNode(Id<Node> id, Coord coord){
		if(this.network.containsKey(id)){
			LOG.warn("Could not add a new node " + id.toString() + " - it already exists.");
		} else{
			this.network.put(id, new PathDependentNode(id, coord));
		}
	}
	
	
	public void setPathDependentEdgeWeight(Id<Node> previousId, Id<Node> currentId, Id<Node> nextId, double weight){
		if(!this.network.containsKey(currentId)){
			LOG.error("Oops... there doesn't seem to be a node " + currentId.toString() + " yet.");
		}
		this.network.get(currentId).setPathDependentEdgeWeight(previousId, nextId, weight);
	}
	
	
	/**
	 * <b><i>Warning:</i></b> This should only be used for tests!
	 * @param randomValue
	 * @return
	 */
	public Id<Node> sampleChainStartNode(double randomValue){
		Id<Node> id = null;
		
		/* Determine the total source weight, but only once for the network. */
		if(this.totalSourceWeight == null){
			this.totalSourceWeight = 0.0;
			for(PathDependentNode node : this.network.values()){
				this.totalSourceWeight += node.getSourceWeight();
			}
		}

		/* Given a random value, sample the next node. */
		double cumulativeWeight = 0.0;
		Iterator<PathDependentNode> iterator = this.network.values().iterator();
		while(id == null && iterator.hasNext()){
			PathDependentNode node = iterator.next();
			cumulativeWeight += node.getSourceWeight();
			if(cumulativeWeight/this.totalSourceWeight >= randomValue){
				id = node.getId();
			}
		}

		return id;
	}


	/**
	 * 
	 * @return
	 */
	public Id<Node> sampleChainStartNode(){
		Id<Node> node = null;
		while(node == null){
			node = sampleChainStartNode(random.nextDouble());
			if(node == null){
				LOG.warn("Redrawing a random value to find a start node.")
			}
		}
		return node;
	}
	
	/**
	 * <b><i>Warning:</i></b> This should only be used for tests!
	 * 
	 * @return
	 */
	public int sampleChainStartHour(Id<Node> startNode, double randomValue){
		PathDependentNode node = this.getPathDependentNode(startNode);
		
		Id<Node> sourceId = Id.createNodeId("source");
		if(!node.getPathDependence().containsKey(sourceId)){
			LOG.error("Cannot sample a chain's start hour from a node that is not considered a major activity.");
			throw new IllegalArgumentException("Illegal start node Id: " + startNode.toString());
		}

		/* Establish the number of times this node has been a source node, but 
		 * only once per node. */
		if(node.sourceCount == null){
			node.sourceCount = 0.0;
			for(String hour : node.hourMap.keySet()){
				node.sourceCount += (double)node.hourMap.get(hour); 
			}
		}
		
		double cumulativeWeight = 0.0;
		Integer hour = null;
		Iterator<String> iterator = node.hourMap.keySet().iterator();
		while(hour == null && iterator.hasNext()){
			String hourString = iterator.next();
			cumulativeWeight += node.hourMap.get(hourString);
			if( (cumulativeWeight/node.sourceCount) >= randomValue ){
				hour = Integer.parseInt(hourString);
			}
		}
		
		return hour;
	}
	

	/**
	 * 
	 * @param startNode
	 * @return
	 */
	public int sampleChainStartHour(Id<Node> startNode){
		return this.sampleChainStartHour(startNode, random.nextDouble());
	}
	
	
	public int sampleNumberOfMinorActivities(Id<Node> startNode, double randomValue){
		PathDependentNode node = this.getPathDependentNode(startNode);
		
		Id<Node> sourceId = Id.createNodeId("source");
		if(!node.getPathDependence().containsKey(sourceId)){
			LOG.error("Cannot sample a chain's number of activities from a node that is not considered a major activity.");
			throw new IllegalArgumentException("Illegal start node Id: " + startNode.toString());
		}

		/* Establish the number of times this node has been a source node, but 
		 * only once per node. */
		if(node.sourceCount == null){
			node.sourceCount = 0.0;
			for(String hour : node.activityCountMap.keySet()){
				node.sourceCount += (double)node.activityCountMap.get(hour); 
			}
		}
		
		double cumulativeWeight = 0.0;
		Integer numberOfActivities = null;
		Iterator<String> iterator = node.activityCountMap.keySet().iterator();
		while(numberOfActivities == null && iterator.hasNext()){
			String activityCountString = iterator.next();
			cumulativeWeight += node.activityCountMap.get(activityCountString);
			if( (cumulativeWeight/node.sourceCount) >= randomValue ){
				numberOfActivities = Integer.parseInt(activityCountString);
			}
		}
		
		return numberOfActivities;
	}
	
	
	/**
	 * 
	 * @param startNode
	 * @return
	 */
	public int sampleNumberOfMinorActivities(Id<Node> startNode){
		return this.sampleNumberOfMinorActivities(startNode, random.nextDouble());
	}
	
	
	public Id<Node> sampleBiasedNextPathDependentNode(Id<Node> previousNodeId, Id<Node> currentNodeId){
		return sampleBiasedNextPathDependentNode(previousNodeId, currentNodeId, random.nextDouble());
	}
	
	
	/**
	 * <b><i>Warning!!</i></b> This method is only meant for testing purposes. 
	 * You should use {@link #sampleBiasedNextPathDependentNode(Id)}.
	 * TODO Finish description.
	 * @param previousNodeId
	 * @param randomValue
	 * @return
	 */
	protected Id<Node> sampleBiasedNextPathDependentNode(Id<Node> previousNodeId, Id<Node> currentNodeId, double randomValue){
		PathDependentNode currentNode = this.getPathDependentNode(currentNodeId);		
//		previousNodeId = previousNodeId == null ? Id.create("source", Node.class) : previousNodeId;
		
//		/* Check that the path-dependent sequence exist. */
//		if(!pathDependence.containsKey(previousNodeId)){
//			LOG.error("There is no next node travelling from " + previousNodeId.toString() + " via " + this.getId().toString());
//			LOG.error("Null returned.");
//			return null;
//		}			
		
		/* Remove the 'sink' node from the choice set, and see if there are 
		 * possible next nodes. */
		Map<Id<Node>, Double> map = currentNode.getPathDependentNextNodes(previousNodeId);
		Id<Node> sinkId = Id.createNodeId("sink");
		Map<Id<Node>, Double> choiceMap = new HashMap<Id<Node>, Double>();
		for(Id<Node> id : map.keySet()){
			if(!id.equals(sinkId)){
				choiceMap.put(id, map.get(id));
			}
		}
		
		if(choiceMap.isEmpty()){
			for(Id<Node> otherPrevious : currentNode.getPathDependence().keySet()){
				map = currentNode.getPathDependentNextNodes(otherPrevious);
				for(Id<Node> possibleId : map.keySet()){
					/* Ignore 'sink' as a next node. */ 
					if(!possibleId.toString().equalsIgnoreCase(sinkId.toString())){
						PathDependentNode possibleNode = this.getPathDependentNode(possibleId);
						double weight = possibleNode.getTotalSinkWeight();
						if(weight > 0){
							choiceMap.put(possibleId, weight);
						}
					}
				}
			}
			sinkOnly++;
		}

		/* If this too fails, see if we can terminate the chain prematurely. */
		if(choiceMap.isEmpty()){
//			throw new RuntimeException("Ooops! Cannot sample a next node!!");
			return null;
		}
		
		/* Determine the total weighted out-degree. */
		double total = 0.0;
		for(Id<Node> id : choiceMap.keySet()){
			total += choiceMap.get(id);
		}
		
		/* Sample next node. */
		double cumulativeTotal = 0.0;
		Id<Node> nextId = null;
		Iterator<Id<Node>> iterator = choiceMap.keySet().iterator();
		
		while(nextId == null){
			Id<Node> id = iterator.next();
			cumulativeTotal += choiceMap.get(id);
			if( (cumulativeTotal / total) >= randomValue){
				nextId = id;
			}
		}
		
		totalNextNodesSampled++;
		return nextId;
	}

	
	public Id<Node> sampleEndOfChainNode(Id<Node> previousId, Id<Node> currentId){
		return this.sampleEndOfChainNode(previousId, currentId, random.nextDouble());
	}


	/**
	 * Should only be used directly for tests.
	 * 
	 * @param previousId
	 * @param currentId
	 * @param randomValue
	 * @return
	 */
	protected Id<Node> sampleEndOfChainNode(Id<Node> previousId, Id<Node> currentId, double randomValue){
		PathDependentNode currentNode = this.getPathDependentNode(currentId);
		
		/* Only consider those nodes who have a 'sink' in their choice set. */

		/* Given the path dependence, check if there is a next node that,
		 * in turn, will have a 'sink', i.e. end the chain. */
		Map<Id<Node>, Double> choiceMap = new HashMap<Id<Node>, Double>();	
		Map<Id<Node>, Double> map = currentNode.getPathDependentNextNodes(previousId);
		Id<Node> sinkId = Id.createNodeId("sink");
		for(Id<Node> possibleId : map.keySet()){
			/* Ignore 'sink' as a next node. */ 
			if(!possibleId.toString().equalsIgnoreCase(sinkId.toString())){
				PathDependentNode possibleNode = this.getPathDependentNode(possibleId);
				Map<Id<Node>, Double> possibleMap = possibleNode.getNextNodes(currentId);
				if(possibleMap.containsKey(sinkId)){
					/* Yes, this can be a possible node to choose as it can end a chain. */
					choiceMap.put(possibleId, possibleMap.get(sinkId));
				}
			}
		}
		
		/* If the first step was unsuccessful, ignore the path dependence, and
		 * check if any next node can be a 'sink', irrespective of the previous
		 * node in the path-dependence. */
		if(choiceMap.isEmpty()){
			LOG.warn("Check if this is calculated correctly.");
			/* Find another approach to get a next node that can end a chain. */
			for(Id<Node> otherPrevious : currentNode.getPathDependence().keySet()){
				map = currentNode.getPathDependentNextNodes(otherPrevious);
				for(Id<Node> possibleId : map.keySet()){
					/* Ignore 'sink' as a next node. */ 
					if(!possibleId.toString().equalsIgnoreCase(sinkId.toString())){
						PathDependentNode possibleNode = this.getPathDependentNode(possibleId);
						double weight = possibleNode.getTotalSinkWeight();
						if(weight > 0){
							choiceMap.put(possibleId, weight);
						}
					}
				}
			}
			noSink++;
		}

		/* If this too fails, see if we can terminate the chain prematurely. */
		if(choiceMap.isEmpty()){
//			throw new RuntimeException("Ooops! Cannot sample the end of an activity chain!!");
			return null;
		}
		
		
		double total = 0.0;
		for(Id<Node> id : choiceMap.keySet()){
			total += choiceMap.get(id);
		}
		
		double cumulativeWeight = 0.0;
		Id<Node> nextNode = null;
		Iterator<Id<Node>> iterator = choiceMap.keySet().iterator();
		while(nextNode == null && iterator.hasNext()){
			Id<Node> thisNode = iterator.next();
			cumulativeWeight += choiceMap.get(thisNode);
			if( (cumulativeWeight/total) >= randomValue ){
				nextNode = thisNode;
			}
		}

		totalNextNodesSampled++;
		return nextNode;
	}
	
	public double getSourceWeight(Id<Node> nodeId){
		if(this.network.containsKey(nodeId)){
			return this.getPathDependentNode(nodeId).getSourceWeight();
		} else{
			return 0.0;
		}
	}

	
	public void writeNetworkStatisticsToConsole(){
		LOG.info("---------------------  Graph statistics  -------------------");
		LOG.info("     Number of vertices: " + this.getNumberOfNodes());
		LOG.info("         Number of arcs: " + this.getNumberOfEdges());
		LOG.info("            Density (%): " + String.format("%01.6f", ( this.getNumberOfEdges() ) / Math.pow(getNumberOfNodes(), 2)*100.0 ) );
		LOG.info(" Network build time (s): " + String.format("%.2f", ((double)this.buildEndTime - (double)this.buildStartTime)/1000));
		LOG.info("------------------------------------------------------------");
	}
	
	
	/**
	 * TODO Fix
	 * @return
	 */
	public List<Tuple<Id<Node>, Id<Node>>> getEdges(){
		Map<Id<Node>, PathDependentNode> nodes = this.getPathDependentNodes();
		for(PathDependentNode node : nodes.values()){
			Id<Node> oId = node.getId();
		}
		
		return null;
	}
	
	public void reportSamplingStatus(){
		LOG.info("Sampling status:");
		LOG.info("  |_ next nodes sampled: " + totalNextNodesSampled);
		LOG.info("  |_ revised next nodes: " + sinkOnly);
		LOG.info("  |_ revised sink nodes: " + noSink);
	}


	public class PathDependentNode implements Identifiable<Node> {
		private final Id<Node> id;
		private final Coord coord;
		private Double sourceCount = null;
		private Double sinkCount = null;
		private Map<Id<Node>, Map<Id<Node>, Double>> pathDependence;
		private Map<String,Integer> hourMap;
		private Map<String, Integer> activityCountMap;
		
		public PathDependentNode(Id<Node> id, Coord coord) {
			this.id = id;
			this.coord = coord;
			this.pathDependence = new TreeMap<>();
			this.hourMap = new TreeMap<String, Integer>();
			this.activityCountMap = new TreeMap<String, Integer>();
		}

		@Override
		public Id<Node> getId() {
			return this.id;
		}
		
		public Coord getCoord(){
			return this.coord;
		}
		

		public void setAsSource(int startHour, int numberOfActivities){
			/* Set start hour. */
			String h = String.valueOf(startHour);
			if(!this.hourMap.containsKey(h)){
				this.hourMap.put(h, 1);
			} else{
				int oldValue = this.hourMap.get(h);
				this.hourMap.put(String.valueOf(startHour), oldValue + 1);
			}
			
			/* Set number of activities. */
			String n = String.valueOf(numberOfActivities);
			if(!this.activityCountMap.containsKey(n)){
				this.activityCountMap.put(n, 1);
			} else{
				int oldValue = this.activityCountMap.get(n);
				this.activityCountMap.put(n, oldValue + 1);
			}
		}
		
		
		public void setPathDependentEdgeWeight(Id<Node> previousId, Id<Node> nextId, double weight){
			if(!pathDependence.containsKey(previousId)){
				pathDependence.put(previousId, new TreeMap<Id<Node>, Double>());
			}
			this.pathDependence.get(previousId).put(nextId, weight);
		}
		
		
		public void setAsSink(Id<Node> previousId){
			Map<Id<Node>, Double> map = null;
			if(!pathDependence.containsKey(previousId)){
				map = new TreeMap<Id<Node>, Double>();
				pathDependence.put(previousId, map);
			} else{
				map = pathDependence.get(previousId);
			}
			
			Id<Node> sinkId = Id.create("sink", Node.class);
			if(!map.containsKey(sinkId)){
				map.put(sinkId, 1.0);
			} else{
				map.put(sinkId, map.get(sinkId) + 1.0);
			}
		}
		
		
		
		
		public void addSourceLink(int startHour, int numberOfActivities, Id<Node> nextNodeId){
			this.setAsSource(startHour, numberOfActivities);
			Id<Node> previousNodeId = Id.create("source", Node.class);
			
			addPathDependentLink(previousNodeId, nextNodeId);
		}
		
		public void addPathDependentLink(Id<Node> previousNodeId, Id<Node> nextNodeId){
			/* DEBUG Remove after problem sorted... why are there links from
			 * a node to itself if the network has already been cleaned?! */
			if(this.getId().toString().equalsIgnoreCase(nextNodeId.toString())){
				LOG.debug("Link from node to itself.");
			}		
			
			/* Add the path-dependency if it doesn't exist yet. */
			Map<Id<Node>, Double> map = null;
			if(!pathDependence.containsKey(previousNodeId)){
				map = new TreeMap<>();
				pathDependence.put(previousNodeId, map);
			} else{
				map = pathDependence.get(previousNodeId);
			}
			
			/* Increment the link weight. */
			if(!map.containsKey(nextNodeId)){
				map.put(nextNodeId, new Double(1.0));
			} else{
				map.put(nextNodeId, map.get(nextNodeId) + 1.0);
			}
		}
		
		
		public Id<Node> sampleNextNode(Id<Node> pastNode){
			//TODO Complete
			return null;
		}
		
		
		/**
		 * This method is mainly used for testing purposes.
		 * @param pastNode
		 * @return
		 */
		public Map<Id<Node>, Double> getNextNodes(Id<Node> pastNode){
			return this.pathDependence.get(pastNode);
		}
		
		
		/**
		 * Determining the node's (unweighted) in-degree. That is, the number of 
		 * other nodes that links into this node.
		 * @return
		 */
		public int getInDegree(){
			int inDegree = 0;
			for(Id<Node> id : this.pathDependence.keySet()){
				if(!id.equals(Id.create("source", Node.class)) && !id.equals(Id.create("unknown", Node.class))){
					inDegree++;
				}
			}
			return inDegree;
		}
		
		
		/**
		 * Determining the node's (unweighted) out-degree. That is, the number of
		 * other nodes that this node can link to. Path-dependence is <b>not</b>
		 * taken into account. If you want path-dependent out-degree, rather use
		 * {@link #getPathDependentOutDegree(Id)}.
		 */
		public int getOutDegree(){
			List<Id<Node>> outNodes = new ArrayList<>();
			for(Id<Node> inId : pathDependence.keySet()){
				for(Id<Node> outId : pathDependence.get(inId).keySet()){
					if(!outNodes.contains(outId)){
						outNodes.add(outId);
					}
				}
			}
			outNodes.remove(Id.create("sink", Node.class));			
			
			return outNodes.size();
		}
		
		
		private void establishPathDependence(Id<Node> previousId){
			if(!pathDependence.containsKey(previousId)){
				pathDependence.put(previousId, new TreeMap<Id<Node>, Double>());
			}
		}

		
		/**
		 * Determining the node's (unweighted) out-degree. That is, the number of
		 * other nodes that this node can link to. Path-dependence <b>is</b>
		 * taken into account. If you want the overall out-degree, rather use
		 * {@link #getOutDegree()}.
		 */
		public int getPathDependentOutDegree(Id<Node> inId){
			if(inId == null) inId = Id.create("source", Node.class);
			
			List<Id<Node>> outNodes = new ArrayList<>();
			for(Id<Node> outId : pathDependence.get(inId).keySet()){
				if(!outNodes.contains(outId)){
					outNodes.add(outId);
				}
			}
			outNodes.remove(Id.create("sink", Node.class));			
			
			return outNodes.size();
		}
		
		public Map<Id<Node>, Double> getPathDependentNextNodes(Id<Node> previousId){
			return this.pathDependence.get(previousId);
		}
		
		
		public Map<Id<Node>, Map<Id<Node>, Double>> getPathDependence(){
			return this.pathDependence;
		}
		
		
		/**
		 * 
		 * @param previousId
		 * @param nextId
		 * @return weight of the link, given the path dependence, or zero if the
		 * 		   particular link does not exist. 
		 */
		private double getPathDependentWeight(Id<Node> previousId, Id<Node> nextId){
			double weight = 0;
			if(pathDependence.containsKey(previousId)){
				Map<Id<Node>, Double> map = pathDependence.get(previousId);
				if(map.containsKey(nextId)){
					weight = map.get(nextId);
				}
			}
			return weight;
		}
		
		private double getWeight(Id<Node> nextId){
			double weight = 0.0;
			for(Map<Id<Node>, Double> map : pathDependence.values()){
				if(map.containsKey(nextId)){
					weight += map.get(nextId);
				}
			}
			return weight;
		}
		
		
		private double getSourceWeight(){
			double weight = 0.0;
			Id<Node> sourceId = Id.create("source", Node.class);
			if(pathDependence.containsKey(sourceId)){
				for(Double d : pathDependence.get(sourceId).values()){
					weight += d;
				}
			}
			
			return weight;
		}
		
		public Map<String, Integer> getStartTimeMap(){
			return this.hourMap;
		}
		
		
		public Map<String, Integer> getNumberOfActivityMap(){
			return this.activityCountMap;
		}
		
		
		public double getTotalSinkWeight(){
			double sinkWeight = 0.0;
			Id<Node> sink = Id.createNodeId("sink");
			Iterator<Id<Node>> iterator = this.pathDependence.keySet().iterator();
			while(iterator.hasNext()){
				Map<Id<Node>, Double> map = this.pathDependence.get(iterator.next());
				if(map.containsKey(sink)){
					sinkWeight += map.get(sink);
				}
			}
			
			return sinkWeight;
		}
	}
}
