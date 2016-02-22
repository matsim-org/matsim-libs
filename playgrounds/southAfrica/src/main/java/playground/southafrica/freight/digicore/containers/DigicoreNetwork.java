/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreGraphJung.java
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

package playground.southafrica.freight.digicore.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;

import edu.uci.ics.jung.algorithms.util.WeightedChoice;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Class to describe the network graph created from consecutive activities.
 * 
 * @author jwjoubert
 */
public class DigicoreNetwork extends DirectedSparseGraph<Id<ActivityFacility>, Pair<Id<ActivityFacility>>>{
	private final Logger LOG = Logger.getLogger(DigicoreNetwork.class);
	
	private List<String> activityTypes;
	
	private Map< Tuple<Pair<Id<ActivityFacility>>, Pair<String>>, Integer> weights 
		= new LinkedHashMap<Tuple<Pair<Id<ActivityFacility>>,Pair<String>>, Integer>();
	
	/* Different weight maps. */
	private Map<Tuple<Id<ActivityFacility>, String>, Map<Id<ActivityFacility>, Integer>> nodeWeightMap = 
			new LinkedHashMap<Tuple<Id<ActivityFacility>,String>, Map<Id<ActivityFacility>,Integer>>();
	private Map<String, Map<Id<ActivityFacility>, Integer>> originWeightMaps = 
			new LinkedHashMap<String, Map<Id<ActivityFacility>,Integer>>();
	private Map<String, Map<Id<ActivityFacility>, Integer>> destinationWeightMaps = 
			new LinkedHashMap<String, Map<Id<ActivityFacility>,Integer>>();
	
	private Map<Id<ActivityFacility>,Coord> coord = new HashMap<Id<ActivityFacility>, Coord>();
	public int nodeCounter = 0;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public DigicoreNetwork() {
		this.activityTypes = new ArrayList<String>();
	}
	

	/**
	 * Adds a directed arc to the network graph, or increments the arc's weight if it 
	 * already exists. 
	 * @param origin the {@link DigicoreFacility} from which a vehicle travels.
	 * @param destination the {@link DigicoreFacility} to which the vehicle travels.
	 */
	public void addArc(DigicoreActivity origin, DigicoreActivity destination){
		if(origin == null || destination == null){
			throw new RuntimeException("Either the origin or destination facilities, or both, are NULL.");
		}
		if(origin.getFacilityId() == null || destination.getFacilityId() == null){
			throw new RuntimeException("Either the origin or destination facility Id, or both, are NULL.");
		}
		Pair<String> typePair = new Pair<String>(origin.getType(), destination.getType());
		Pair<Id<ActivityFacility>> idPair = new Pair<Id<ActivityFacility>>(origin.getFacilityId(), destination.getFacilityId());
		
		/* Check if both origin and destination vertices exist, add if not. */
		if(!this.containsVertex(origin.getFacilityId())){
			this.addVertex(origin.getFacilityId());
			this.coord.put(origin.getFacilityId(), origin.getCoord());
		}
		if(!this.containsVertex(destination.getFacilityId())){
			this.addVertex(destination.getFacilityId());
			this.coord.put(destination.getFacilityId(), destination.getCoord());
		}
		
		/* Check if both the origin and destination activity types are already recorded. */
		if(!this.activityTypes.contains(origin.getType())){
			this.activityTypes.add( origin.getType() );
		}
		if(!this.activityTypes.contains( destination.getType() )){
			this.activityTypes.add( destination.getType() );
		}
		
		/* Add the edge with weight 1 if it does not exist yet, otherwise 
		 * increment weight. These are the general network weights, and
		 * does not take activity pairs into account. */
		Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple = new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>( idPair, typePair );
		if(!this.containsEdge(idPair)){
			this.addEdge(idPair, origin.getFacilityId(), destination.getFacilityId(), EdgeType.DIRECTED);
			weights.put(tuple, 1);
		} else{
			if(!weights.containsKey(tuple)){
				weights.put(tuple, 1);
			} else{
				weights.put(tuple, weights.get(tuple) + 1);				
			}
		}
	}

	
	public Map<Id<ActivityFacility>, Coord> getCoordinates(){
		return this.coord;
	}
	
	
	public double getDensity(){
		if(this.getVertexCount() == 0){
			return 0.0;
		} else{
			return ((double)this.getEdgeCount()) / ( ((double)this.getVertexCount()) * ((double)this.getVertexCount()-1) );
		}
	}
	
		
	/**
	 * Determines the minimum and maximum edge weights. Returns [0,0] if the graph
	 * has no edges.
	 * @return
	 */
	public int[] getMinMaxEdgeWeights(){
		int[] mm = {0,0};
		if(this.getEdgeCount() > 0){
			int min = Integer.MAX_VALUE;
			int max = Integer.MIN_VALUE;
			for(Integer value : this.getWeights().values()){
				min = Math.min(min, value);
				max = Math.max(max, value);				
			}
			mm[0] = min;	
			mm[1] = max;
		}
		return mm;
	}
		

	public Map<Tuple<Pair<Id<ActivityFacility>>, Pair<String>>, Integer> getWeights(){
		return this.weights;
	}
	
	
	public int getMultiplexEdgeWeight(Id<ActivityFacility> oId, String oType, Id<ActivityFacility> dId, String dType){
		Pair<Id<ActivityFacility>> idPair = new Pair<Id<ActivityFacility>>(oId, dId);
		Pair<String> typePair = new Pair<String>(oType, dType);
		Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple = new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(idPair, typePair);
		
		if(this.weights.containsKey(tuple)){
			return this.weights.get(tuple);
		} else {
			return 0;
		}
	}
	
	
	/**
	 * Gets the weight of the directed edge. All possible activity type
	 * combinations are added together. In a multiplex network, you may 
	 * want to get the weight for each activity type combination uniquely,
	 * then rather use the method {@link #getMultiplexEdgeWeight(Id, String, Id, String)}
	 * @param origin
	 * @param destination
	 * @return the weight of the directed edge, or 0 if the edge is not in the graph.
	 */
	public int getEdgeWeight(Id<ActivityFacility> oId, Id<ActivityFacility> dId){
		int totalWeight = 0;
		for(int i = 0; i < this.activityTypes.size(); i++){
			for(int j = 0; j < this.activityTypes.size(); j++){
				totalWeight += this.getMultiplexEdgeWeight(oId, this.activityTypes.get(i), dId, this.activityTypes.get(j));
			}
		}
		return totalWeight;
	}
	
	
	public List<String> getActivityTypes(){
		return this.activityTypes;				
	}
	
	
	public void printBasicNetworkStatistics(){		
		int[] minMax = this.getMinMaxEdgeWeights();
				
		LOG.info("---------------------  Graph statistics  -------------------");
		LOG.info("      Number of arcs: " + this.getEdgeCount());
		LOG.info("  Number of vertices: " + this.getVertexCount());
		LOG.info("             Density: " + String.format("%01.6f", this.getDensity()));
		LOG.info(" Minimum edge weight: " + minMax[0]);
		LOG.info(" Maximum edge weight: " + minMax[1]);
		LOG.info("------------------------------------------------------------");
	}
			
	
	/**
	 * Samples the first major activity in an activity chain.
	 * @param activityType
	 * @return
	 */
	public Id<ActivityFacility> sampleBiasedOriginNode(String activityType){
		return this.sampleBiasedOriginNode(activityType, MatsimRandom.getRandom());
	}
	
	
	/**
	 * Samples the first major activity in an activity chain.
	 * @param activityType
	 * @param random
	 * @return
	 */
	public Id<ActivityFacility> sampleBiasedOriginNode(String activityType, Random random) {
		Map<Id<ActivityFacility>, Integer> weightMap;
		if(originWeightMaps.get(activityType) == null){
			weightMap = new HashMap<Id<ActivityFacility>, Integer>();
			
			for(Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple : this.weights.keySet()){
				if(tuple.getSecond().getFirst().equalsIgnoreCase(activityType)){
					Id<ActivityFacility> major = tuple.getFirst().getFirst();
					if(!weightMap.containsKey(major)){
						weightMap.put(major, this.weights.get(tuple));					
					} else{
						weightMap.put(major, weightMap.get(major) + this.weights.get(tuple));
					}
				}
			}	
			originWeightMaps.put(activityType, weightMap);
		} else{
			weightMap = originWeightMaps.get(activityType);
		}
		
		WeightedChoice<Id<ActivityFacility>> vertexWeights = new WeightedChoice<Id<ActivityFacility>>(weightMap, random);
		return vertexWeights.nextItem();
	}

	
	/**
	 * Samples a next node from a given origin node and given a specific activity 
	 * type. This method creates its own random number generator. If you want
	 * deterministic behaviour, for example for tests cases, rather use the
	 * method {@link #sampleBiasedDestinationNode(Id, String, Random)}. 
	 * @param origin
	 * @param destinationType
	 * @return
	 */
	public Id<ActivityFacility> sampleBiasedDestinationNode(Id<ActivityFacility> origin, String destinationType) {
		return this.sampleBiasedDestinationNode(origin, destinationType, MatsimRandom.getRandom());
	}	
	
	
	/**
	 * Samples a destination node from a given origin and given a specific activity 
	 * type at the destination. 
	 * @param origin
	 * @param destinationType
	 * @param random
	 * @return
	 */
	public Id<ActivityFacility> sampleBiasedDestinationNode(Id<ActivityFacility> origin, String destinationType, Random random) {
		Tuple<Id<ActivityFacility>, String> weightMapTuple = new Tuple<Id<ActivityFacility>, String>(origin, destinationType);
		Map<Id<ActivityFacility>, Integer> weightMap;
		if(!nodeWeightMap.containsKey(weightMapTuple)){
			/* Build a weight map for the origin node, given the specific destination type. */
			weightMap = new HashMap<Id<ActivityFacility>, Integer>();
			
			for(Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple : this.weights.keySet()){
				if(tuple.getFirst().getFirst() == origin &&
						tuple.getSecond().getSecond().equalsIgnoreCase(destinationType)){
					Id<ActivityFacility> destinationId = tuple.getFirst().getSecond();
					weightMap.put(destinationId, weights.get(tuple));
				}
			}		
			nodeWeightMap.put(weightMapTuple, weightMap);
		} else{
			weightMap = nodeWeightMap.get(weightMapTuple);
		}
		
		if(weightMap.size() == 0){
			if(!destinationWeightMaps.containsKey(destinationType)){
				/* Build a destination weight map for the activity type. */
				for(Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple : this.weights.keySet()){
					if(tuple.getSecond().getSecond().equalsIgnoreCase(destinationType)){
						Id<ActivityFacility> node = tuple.getFirst().getSecond();
						if(!weightMap.containsKey(node)){
							weightMap.put(node, this.weights.get(tuple));					
						} else{
							weightMap.put(node, weightMap.get(node) + this.weights.get(tuple));
						}
					}
				}	
				destinationWeightMaps.put(destinationType, weightMap);
			} else{
				weightMap = destinationWeightMaps.get(destinationType);
			}
		}
		WeightedChoice<Id<ActivityFacility>> vertexWeights = new WeightedChoice<Id<ActivityFacility>>(weightMap, random);
		return vertexWeights.nextItem();			
	}
	
	
}

