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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;

public class PathDependentNetwork {
	private final Logger LOG = Logger.getLogger(PathDependentNetwork.class);
	private Map<Id, PathDependentNode> network;
	private Random random;
	private String description = null;
	private long buildStartTime;
	private long buildEndTime;
	
	
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
		this.network = new TreeMap<Id, PathDependentNetwork.PathDependentNode>();
	}
	
	
	public Map<Id, PathDependentNode> getPathDependentNodes(){
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
		/* Process the first activity pair, but only if both activities are 
		 * associated with a facility. */
		Id previousNodeId = null;
		Id currentNodeId = chain.get(0).getFacilityId();
		Id nextNodeId = chain.get(1).getFacilityId();
		
		if(currentNodeId != null && nextNodeId != null){
			/* Add the current node to the network if it doesn't already exist. */
			if(!this.network.containsKey(currentNodeId)){
				this.network.put(currentNodeId, new PathDependentNode(currentNodeId, chain.get(0).getCoord()));
			} 
			PathDependentNode currentNode = this.network.get(currentNodeId);
			
			/* Add the next node to the network if it doesn'talready exist. */
			if(!this.network.containsKey(nextNodeId)){
				this.network.put(nextNodeId, new PathDependentNode(nextNodeId, chain.get(1).getCoord()));
			} 
			
			/* Add the path-dependent link to the network. */
			currentNode.addPathDependentLink(null, nextNodeId);
			
			/* Ensure that the next node reflects the fact that it can be
			 * reached from the currentNode. */
			this.network.get(nextNodeId).establishPathDependence(currentNodeId);
		}
		previousNodeId = currentNodeId == null ? new IdImpl("unknown") : currentNodeId;

		/* Process the remainder of the (minor) activity pairs. */
		for(int i = 1; i < chain.size()-1; i++){
			currentNodeId = chain.get(i).getFacilityId();
			nextNodeId = chain.get(i+1).getFacilityId();

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
			previousNodeId = currentNodeId == null ? new IdImpl("unknown") : currentNodeId;
		}

		/* Process the last activity pair. No link needs to be added, but we 
		 * just have to make sure it is indicated as sink. This is only 
		 * necessary if the node already exists in the network. If not, it means
		 * it was never part of a link that was added to the network anyway. */
		currentNodeId = chain.get(chain.size()-1).getFacilityId();
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
	
	
	public PathDependentNode getPathDependentNode(Id id){
		return this.network.get(id);
	}
	
	
	public double getPathDependentWeight(Id previousId, Id currentId, Id nextId){
		double d = 0.0;
		
		/* Sort out null Ids for sources and sinks. */
		if(previousId == null) previousId = new IdImpl("source");
		if(nextId == null) nextId = new IdImpl("sink");
		if(currentId == null){
			throw new IllegalArgumentException("Cannot have a 'null' Id for current node.");
		}
		
		if(this.network.containsKey(currentId)){
			d = this.network.get(currentId).getPathDependentWeight(previousId, nextId);
		}
		
		return d;
	}
	
	public double getWeight(Id fromId, Id toId){
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

	
	public void addNewPathDependentNode(Id id, Coord coord){
		if(this.network.containsKey(id)){
			LOG.warn("Could not add a new node " + id.toString() + " - it already exists.");
		} else{
			this.network.put(id, new PathDependentNode(id, coord));
		}
	}
	
	
	public void setPathDependentEdgeWeight(Id previousId, Id currentId, Id nextId, double weight){
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
	public Id sampleChainStartNode(double randomValue){
		Id id = null;
		
		/* Determine the total source weight. */
		double totalWeight = 0.0;
		for(PathDependentNode node : this.network.values()){
			totalWeight += node.getSourceWeight();
		}
		
		/* Given a random value, sample the next node. */
		double cumulativeWeight = 0.0;
		Iterator<PathDependentNode> iterator = this.network.values().iterator();
		while(id == null){
			PathDependentNode node = iterator.next();
			cumulativeWeight += node.getSourceWeight();
			if(cumulativeWeight/totalWeight >= randomValue){
				id = node.getId();
			}
		}
		
		return id;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Id sampleChainStartNode(){
		return sampleChainStartNode(random.nextDouble());
	}
	
	
	public double getSourceWeight(Id nodeId){
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
		LOG.info("            Density (%): " + String.format("%01.6f", ( (double)this.getNumberOfEdges() ) / Math.pow((double)getNumberOfNodes(), 2)*100.0 ) );
		LOG.info(" Network build time (s): " + String.format("%.2f", ((double)this.buildEndTime - (double)this.buildStartTime)/1000));
		LOG.info("------------------------------------------------------------");
	}
	

	public class PathDependentNode implements Identifiable{
		private final Id id;
		private final Coord coord;
		private double source = 0.0;
		private double sink = 0.0;
		private Map<Id, Map<Id, Double>> pathDependence;
		
		public PathDependentNode(Id id, Coord coord) {
			this.id = id;
			this.coord = coord;
			this.pathDependence = new TreeMap<Id, Map<Id,Double>>();
		}

		@Override
		public Id getId() {
			return this.id;
		}
		
		public Coord getCoord(){
			return this.coord;
		}
		
		public void setAsSource(){
			source += 1.0;
		}
		
		public void setPathDependentEdgeWeight(Id previousId, Id nextId, double weight){
			if(!pathDependence.containsKey(previousId)){
				pathDependence.put(previousId, new TreeMap<Id, Double>());
			}
			this.pathDependence.get(previousId).put(nextId, weight);
		}
		
		
		public void setAsSink(Id previousId){
			sink += 1.0;
			Map<Id, Double> map = null;
			if(!pathDependence.containsKey(previousId)){
				map = new TreeMap<Id, Double>();
				pathDependence.put(previousId, map);
			} else{
				map = pathDependence.get(previousId);
			}
			
			Id sinkId = new IdImpl("sink");
			if(!map.containsKey(sinkId)){
				map.put(sinkId, 1.0);
			} else{
				map.put(sinkId, map.get(sinkId) + 1.0);
			}
		}
		
		
		public Id sampleBiasedNextPathDependentNode(Id previousNodeId){
			return sampleBiasedNextPathDependentNode(previousNodeId, random.nextDouble());
		}
		
		
		/**
		 * <b><i>Warning!!</i></b> This method is only meant for testing purposes. 
		 * You should use {@link #sampleBiasedNextPathDependentNode(Id)}.
		 * TODO Finish description.
		 * @param previousNodeId
		 * @param randomValue
		 * @return
		 */
		protected Id sampleBiasedNextPathDependentNode(Id previousNodeId, double randomValue){
			
			previousNodeId = previousNodeId == null ? new IdImpl("source") : previousNodeId;
			
			/* Check that the path-dependent sequence exist. */
			if(!pathDependence.containsKey(previousNodeId)){
				LOG.error("There is no next node travelling from " + previousNodeId.toString() + " via " + this.getId().toString());
				LOG.error("Null returned.");
				return null;
			}			
			
			/* Determine the total weighted out-degree. */
			Map<Id, Double> map = pathDependence.get(previousNodeId);
			double total = 0.0;
			for(Id id : map.keySet()){
				total += map.get(id);
			}
			
			/* Sample next node. */
			double cumulativeTotal = 0.0;
			Id nextId = null;
			Iterator<Id> iterator = map.keySet().iterator();
			
			while(nextId == null){
				Id id = iterator.next();
				cumulativeTotal += map.get(id);
				if( (cumulativeTotal / total) >= randomValue){
					nextId = id;
				}
			}
			
			if(nextId.toString().equalsIgnoreCase("sink")){
				return null;
			}
			
			return nextId;
			
		}
		
		
		
		public void addPathDependentLink(Id previousNodeId, Id nextNodeId){
			/* DEBUG Remove after problem sorted... why are there links from
			 * a node to itself if the network has already been cleaned?! */
			if(this.getId().toString().equalsIgnoreCase(nextNodeId.toString())){
				LOG.debug("Link from node to itself.");
			}		
			
			Id id = previousNodeId == null ? new IdImpl("source") : previousNodeId;
			if(previousNodeId == null){
				this.setAsSource();
			}
			
			/* Add the path-dependency if it doesn't exist yet. */
			Map<Id, Double> map = null;
			if(!pathDependence.containsKey(id)){
				map = new TreeMap<Id, Double>();
				pathDependence.put(id, map);
			} else{
				map = pathDependence.get(id);
			}
			
			/* Increment the link weight. */
			if(!map.containsKey(nextNodeId)){
				map.put(nextNodeId, new Double(1.0));
			} else{
				map.put(nextNodeId, map.get(nextNodeId) + 1.0);
			}
		}
		
		
		public Id sampleNextNode(Id pastNode){
			//TODO Complete
			return null;
		}
		
		
		/**
		 * This method is mainly used for testing purposes.
		 * @param pastNode
		 * @return
		 */
		public Map<Id, Double> getNextNodes(Id pastNode){
			return this.pathDependence.get(pastNode);
		}
		
		
		/**
		 * Determining the node's (unweighted) in-degree. That is, the number of 
		 * other nodes that links into this node.
		 * @return
		 */
		public int getInDegree(){
			int inDegree = 0;
			for(Id id : this.pathDependence.keySet()){
				if(!id.equals(new IdImpl("source")) && !id.equals(new IdImpl("unknown"))){
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
			List<Id> outNodes = new ArrayList<Id>();
			for(Id inId : pathDependence.keySet()){
				for(Id outId : pathDependence.get(inId).keySet()){
					if(!outNodes.contains(outId)){
						outNodes.add(outId);
					}
				}
			}
			outNodes.remove(new IdImpl("sink"));			
			
			return outNodes.size();
		}
		
		
		private void establishPathDependence(Id previousId){
			if(!pathDependence.containsKey(previousId)){
				pathDependence.put(previousId, new TreeMap<Id, Double>());
			}
		}

		
		/**
		 * Determining the node's (unweighted) out-degree. That is, the number of
		 * other nodes that this node can link to. Path-dependence <b>is</b>
		 * taken into account. If you want the overall out-degree, rather use
		 * {@link #getOutDegree()}.
		 */
		public int getPathDependentOutDegree(Id inId){
			if(inId == null) inId = new IdImpl("source");
			
			List<Id> outNodes = new ArrayList<Id>();
			for(Id outId : pathDependence.get(inId).keySet()){
				if(!outNodes.contains(outId)){
					outNodes.add(outId);
				}
			}
			outNodes.remove(new IdImpl("sink"));			
			
			return outNodes.size();
		}
		
		public Map<Id, Double> getPathDependentNextNodes(Id previousId){
			return this.pathDependence.get(previousId);
		}
		
		
		public Map<Id, Map<Id, Double>> getPathDependence(){
			return this.pathDependence;
		}
		
		
		/**
		 * 
		 * @param previousId
		 * @param nextId
		 * @return weight of the link, given the path dependence, or zero if the
		 * 		   particular link does not exist. 
		 */
		private double getPathDependentWeight(Id previousId, Id nextId){
			double weight = 0;
			if(pathDependence.containsKey(previousId)){
				Map<Id, Double> map = pathDependence.get(previousId);
				if(map.containsKey(nextId)){
					weight = map.get(nextId);
				}
			}
			return weight;
		}
		
		private double getWeight(Id nextId){
			double weight = 0.0;
			for(Map<Id, Double> map : pathDependence.values()){
				if(map.containsKey(nextId)){
					weight += map.get(nextId);
				}
			}
			return weight;
		}
		
		
		private double getSourceWeight(){
			double weight = 0.0;
			Id sourceId = new IdImpl("source");
			if(pathDependence.containsKey(sourceId)){
				for(Double d : pathDependence.get(sourceId).values()){
					weight += d;
				}
			}
			
			return weight;
		}
		
	}
}
