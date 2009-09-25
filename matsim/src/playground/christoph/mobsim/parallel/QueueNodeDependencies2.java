///* *********************************************************************** *
// * project: org.matsim.*
// * QueueNodeDependencies2.java
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
// * Keep a QueueNode and all its depending Children
// */
//public class QueueNodeDependencies2 {
//	
//	private QueueNode queueNode;
//	private List<QueueNodeDependencies2> children;
//	private Random random;
//	
//	public QueueNodeDependencies2(QueueNode queueNode, Random random)
//	{
//		this.queueNode = queueNode;
//		this.random = random;
//		
//		children = new ArrayList<QueueNodeDependencies2>();
//	}
//
//	public List<QueueNodeDependencies2> getChildren()
//	{
//		return this.children;
//	}
//	
//	public void addChild(QueueNodeDependencies2 child)
//	{
//		children.add(child);
//	}
//	
//	public void removeChild(QueueNodeDependencies2 child)
//	{
//		children.remove(child);
//	}
//	
//	public QueueNode getQueueNode()
//	{
//		return this.queueNode;
//	}
//	
//	public int getNodeCount()
//	{
//		return this.children.size() + 1;
//	}
//	
//	public Random getRandom()
//	{
//		return this.random;
//	}
//}