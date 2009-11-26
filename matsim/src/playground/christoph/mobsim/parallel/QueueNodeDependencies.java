///* *********************************************************************** *
// * project: org.matsim.*
// * QueueNodeDependencies.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.christoph.mobsim.parallel;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
///*
// * Keep a QueueNode and all its depending in- and
// * outNodes in a single Object.
// * 
// * Every QueueNodeDependencies Objects gets its own
// * Random Object. I don't like this, but at the moment
// * its the only way i can think of that guarantees 
// * deterministic behavior... 
// */
//public class QueueNodeDependencies {
//	private QueueNode queueNode;
//	private List<QueueNodeDependencies> preProcessedInNodes;
//	private List<QueueNodeDependencies> postProcessedInNodes;
//	private List<QueueNodeDependencies> outNodes;
//	private int processedInNodes = 0;
//	private Random random;
//
//	// Has the Node already been moved in the current TimeStep?
//	private boolean moved = false;
//	
//	public QueueNodeDependencies(QueueNode queueNode, Random random)
//	{
//		this.queueNode = queueNode;
//		this.random = random;
//		
//		preProcessedInNodes = new ArrayList<QueueNodeDependencies>();
//		postProcessedInNodes = new ArrayList<QueueNodeDependencies>();
//		outNodes = new ArrayList<QueueNodeDependencies>();
//	}
//
//	public List<QueueNodeDependencies> getPreProcessedInNodes()
//	{
//		return this.preProcessedInNodes;
//	}
//	
//	public void addPreProcessedInNode(QueueNodeDependencies queueNodeDependencies)
//	{
//		preProcessedInNodes.add(queueNodeDependencies);
//	}
//	
//	public List<QueueNodeDependencies> getPostProcessedInNodes()
//	{
//		return this.postProcessedInNodes;
//	}
//	
//	public void addPostProcessedInNodes(QueueNodeDependencies queueNodeDependencies)
//	{
//		postProcessedInNodes.add(queueNodeDependencies);
//	}
//	
//	public QueueNode getQueueNode()
//	{
//		return this.queueNode;
//	}
//	
//	public List<QueueNodeDependencies> getOutNodes()
//	{
//		return this.outNodes;
//	}
//	
//	public void addOutNode(QueueNodeDependencies queueNodeDependencies)
//	{
//		outNodes.add(queueNodeDependencies);
//	}
//	
//	public Random getRandom()
//	{
//		return this.random;
//	}
//	
//	public boolean hasBeenMoved()
//	{
//		return moved;
//	}
//	
//	public void setMoved(boolean moved)
//	{
//		this.moved = moved;
//	}
//	
//	public void incNodeProcessed()
//	{
//		processedInNodes++;
//	}
//	
//	public int getProcessedInNodes()
//	{
//		return this.processedInNodes;
//	}
//		
//	public void resetProcessedInNodes()
//	{
//		processedInNodes = 0;
//	}
//	
//	/*
//	 * A QueueNode is moveable in the ParallelQueueSimulation
//	 * if there are no outNodes that need this Node processed
//	 * and all its depending inNodes have already been moved.
//	 */
//	public synchronized boolean isMoveable()
//	{
//		if (hasBeenMoved()) return false;
//		
//		for (QueueNodeDependencies outNode : outNodes)
//		{
//			if (outNode.postProcessedInNodes.contains(this))
//			{
//				if(!outNode.hasBeenMoved()) return false;
//			}
//		}
//		return processedInNodes == preProcessedInNodes.size();
//	}
//}
