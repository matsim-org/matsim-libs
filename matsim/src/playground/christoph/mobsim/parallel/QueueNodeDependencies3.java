///* *********************************************************************** *
// * project: org.matsim.*
// * QueueNodeDependencies3.java
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
//package playground.christoph.mobsim.parallel;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.atomic.AtomicInteger;
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
//public class QueueNodeDependencies3 {
//	
//	private QueueNode queueNode;
//	
//	/*
//	 * Nodes that have to be processed before this Node can
//	 * be processed.
//	 */
//	private List<QueueNodeDependencies3> preProcessedNodes;
//	
//	/*
//	 * Nodes that can be processed after the current Node
//	 * has been processed.
//	 */
//	private List<QueueNodeDependencies3> postProcessedNodes;
//	
//	/*
//	 * Nodes that can be processed after this Node has been processed.
//	 * They are identified in a preprocessing Step so no further 
//	 * notification, etc. is needed during the simulation. 
//	 */
//	private List<QueueNodeDependencies3> postProcessableChildren; 
//	
//	/*
//	 * Nodes that cannot be processed after this Node has been processed
//	 * because they depend on other Nodes as well.
//	 */
//	private List<QueueNodeDependencies3> notPostProcessableChildren;
//		
//	private AtomicInteger processedNodes = new AtomicInteger(0);
//	private int nodesToProcess = 0;
//	private Random random;
//
//	// Has the Node already been moved in the current TimeStep?
////	private boolean moved = false;
//	
//	public QueueNodeDependencies3(QueueNode queueNode, Random random)
//	{
//		this.queueNode = queueNode;
//		this.random = random;
//		
//		preProcessedNodes = new ArrayList<QueueNodeDependencies3>();
//		postProcessedNodes = new ArrayList<QueueNodeDependencies3>();
//	}
//
//	public List<QueueNodeDependencies3> getPreProcessedNodes()
//	{
//		return this.preProcessedNodes;
//	}
//	
//	public void addPreProcessedNode(QueueNodeDependencies3 queueNodeDependencies)
//	{
//		if (!preProcessedNodes.contains(queueNodeDependencies))
//		{
//			preProcessedNodes.add(queueNodeDependencies);
//			nodesToProcess++;
//		}
//	}
//	
//	public List<QueueNodeDependencies3> getPostProcessedNodes()
//	{
//		return this.postProcessedNodes;
//	}
//	
//	public void addPostProcessedNode(QueueNodeDependencies3 queueNodeDependencies)
//	{
//		if (!postProcessedNodes.contains(queueNodeDependencies))
//		{
//			postProcessedNodes.add(queueNodeDependencies);
//		}
//	}
//	
//	public void removePostProcessedNode(QueueNodeDependencies3 queueNodeDependencies)
//	{
//		postProcessedNodes.remove(queueNodeDependencies);
//	}
//	
//	public QueueNode getQueueNode()
//	{
//		return this.queueNode;
//	}
//	
//	public Random getRandom()
//	{
//		return this.random;
//	}
//		
//	public int incProcessedNodes()
//	{
//		return processedNodes.incrementAndGet();
//	}
//	
//	public int getProcessedNodes()
//	{
//		return processedNodes.intValue();
//	}
//		
//	public void resetProcessedNodes()
//	{
//		processedNodes.set(0);
//	}
//	
//	public void identifyPostProcessableChildren()
//	{
//		postProcessableChildren = new ArrayList<QueueNodeDependencies3>();
//		notPostProcessableChildren = new ArrayList<QueueNodeDependencies3>();
//		
//		for(QueueNodeDependencies3 qnd : postProcessedNodes)
//		{
//			if(qnd.getPreProcessedNodes().size() == 1)
//			{
//				postProcessableChildren.add(qnd);
//			}
//			else
//			{
//				notPostProcessableChildren.add(qnd);
//			}
//		}
//	}
//	
//	/*
//	 * Returns a List that contains all Nodes that can be processed
//	 * after the current Node. 
//	 */
//	public List<QueueNodeDependencies3> getPostProcessableChildren()
//	{
//		List<QueueNodeDependencies3> list = new ArrayList<QueueNodeDependencies3>();
//		list.addAll(this.postProcessableChildren);
//		
//		for (QueueNodeDependencies3 child : postProcessableChildren)
//		{
//			list.addAll(child.getPostProcessableChildren());
//		}
//		
//		return list;
//	}
//	
//	public List<QueueNodeDependencies3> getNotPostProcessableChildren()
//	{
//		return this.notPostProcessableChildren;
//	}
//	
//	/*
//	 * Returns a List of all Nodes that have to be processed before the
//	 * current Node can be processed.
//	 */
//	public List<QueueNodeDependencies3> getPreProcessedParents()
//	{
//		List<QueueNodeDependencies3> list = new ArrayList<QueueNodeDependencies3>();
//		list.addAll(this.preProcessedNodes);
//		
//		for (QueueNodeDependencies3 parent : preProcessedNodes)
//		{
//			list.addAll(parent.getPreProcessedParents());
//		}
//		
//		return list;
//	}
//	
//	/*
//	 * Remove implicit Dependencies. An example would be a scenario
//	 * where a Nodes has two Parents. If one Parent depends also on
//	 * the other one we can eliminate one Dependency from the current
//	 * Node. 
//	 */
//	public int removeImplicitDependencies()
//	{
//		int count = 0;
//		Iterator<QueueNodeDependencies3> iter = preProcessedNodes.iterator();
//		while(iter.hasNext())
//		{
//			QueueNodeDependencies3 parent = iter.next();
//				
//			for (QueueNodeDependencies3 otherParent : preProcessedNodes)
//			{
//				if(otherParent == parent) continue;
//				
//				if (otherParent.getPreProcessedParents().contains(parent))
//				{
//					count++;
//					parent.getPostProcessedNodes().remove(this);
//					iter.remove();
//					break;
//				}
//			}
//		}
//		
//		return count;
//	}
//	
//	/*
//	 * A QueueNode is movable in the ParallelQueueSimulation
//	 * if there are no outNodes that need this Node processed
//	 * and all its depending inNodes have already been moved.
//	 */
//	protected boolean isMoveable()
//	{
//		return processedNodes.intValue() == nodesToProcess;
//	}
//}
