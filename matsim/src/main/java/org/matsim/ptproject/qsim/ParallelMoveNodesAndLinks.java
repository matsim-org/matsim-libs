/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelMoveNodesAndLinks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class ParallelMoveNodesAndLinks {
	
	private final static Logger log = Logger.getLogger(ParallelMoveNodesAndLinks.class);
	
	private MoveThread[] moveThreads;
	private boolean simulateAllNodes = false;
	private boolean simulateAllLinks = false;
	private int numOfThreads;
	
	private ExtendedQueueNode[][] parallelNodesArrays;
	private List<List<QLink>> parallelSimLinksLists;
	
	private CyclicBarrier separationBarrier;	// separates moveNodes and moveLinks
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
		
	public ParallelMoveNodesAndLinks(boolean simulateAllNodes, boolean simulateAllLinks)
	{
		this.simulateAllNodes = simulateAllNodes;
		this.simulateAllLinks = simulateAllLinks;
	}

	/*
	 * The Threads are waiting at the startBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes and Links. We wait 
	 * until all of them reach the endBarrier to move
	 * on. We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the endBarrier before
	 * this Method does, it should work anyway.
	 */
	public void run(double time)
	{	
		try
		{
			// set current Time
			for (MoveThread moveThread : moveThreads) 
			{				
				moveThread.setTime(time);
			}
			
			this.startBarrier.await();
				
			this.endBarrier.await();		
		}
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
		catch (BrokenBarrierException e)
		{
	      	Gbl.errorMsg(e);
		}
	}
		
	public void initNodesAndLinks(QNode[] simNodesArray, List<QLink> allLinks, List<QLink> simActivateThis, int numOfThreads)
	{	
		this.numOfThreads = numOfThreads;

		createNodesArrays(simNodesArray);
		createLinkLists(allLinks);
		
		LinkReActivator linkReActivator = new LinkReActivator(simActivateThis, parallelSimLinksLists, numOfThreads);
		
		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.separationBarrier = new CyclicBarrier(numOfThreads, linkReActivator);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);
		
		moveThreads = new MoveThread[numOfThreads];
						
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			MoveThread moveThread = new MoveThread(simulateAllNodes);
			moveThread.setName("MoveNodesAndLinks" + i);
			
			moveThread.setStartBarrier(this.startBarrier);
			moveThread.setSeparationBarrier(this.separationBarrier);
			moveThread.setEndBarrier(this.endBarrier);
			
			moveThread.setExtendedQueueNodeArray(this.parallelNodesArrays[i]);
			moveThread.handleLinks(parallelSimLinksLists.get(i));
			moveThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			moveThreads[i] = moveThread;
			
			moveThread.start();
		}
		
		/*
		 * After initialization the Threads are waiting at the
		 * endBarrier. We trigger this Barrier once so 
		 * they wait at the startBarrier what has to be
		 * their state if the run() method is called.
		 */
		try
		{
			this.endBarrier.await();
		} 
		catch (InterruptedException e) 
		{
			Gbl.errorMsg(e);
		} 
		catch (BrokenBarrierException e)
		{
			Gbl.errorMsg(e);
		}
	}
		
	/*
	 * Create equal sized Nodes Arrays.
	 */
	private void createNodesArrays(QNode[] simNodesArray)
	{	
		List<List<ExtendedQueueNode>> nodes = new ArrayList<List<ExtendedQueueNode>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			nodes.add(new ArrayList<ExtendedQueueNode>());
		}
		
		int roundRobin = 0;
		for (QNode queueNode : simNodesArray)
		{
			ExtendedQueueNode extendedQueueNode = new ExtendedQueueNode(queueNode, MatsimRandom.getLocalInstance());
			nodes.get(roundRobin % numOfThreads).add(extendedQueueNode);
			roundRobin++;
		}
		
		/*
		 * Now we create Arrays out of our Lists because iterating over them
		 * is much faster.
		 */
		parallelNodesArrays = new ExtendedQueueNode[this.numOfThreads][];
		for (int i = 0; i < nodes.size(); i++)
		{
			List<ExtendedQueueNode> list = nodes.get(i);
			
			ExtendedQueueNode[] array = new ExtendedQueueNode[list.size()];
			list.toArray(array);
			parallelNodesArrays[i] = array;
		}
	}
	
	/*
	 * Create the Lists of QueueLinks that are handled on parallel Threads.
	 */
	private void createLinkLists(List<QLink> allLinks)
	{
		parallelSimLinksLists = new ArrayList<List<QLink>>();
		
		for (int i = 0; i < numOfThreads; i++)
		{
			parallelSimLinksLists.add(new ArrayList<QLink>());
		}
		
		/*
		 * If we simulate all Links, we have to add them initially to the Lists.
		 */
		if (simulateAllLinks) 
		{
			int roundRobin = 0;
			for(QLink link : allLinks)
			{
				parallelSimLinksLists.get(roundRobin % numOfThreads).add(link);
				roundRobin++;
			}
		}
	}

	
	/*
	 * We have a List for each Thread so we have to add up their size
	 * to get the Number of simulated Links.
	 */
	/*package*/ int getNumberOfSimulatedLinks()
	{
		int size = 0;
		for (List<?> list : this.parallelSimLinksLists)
		{
			size = size + list.size();
		}
		
		return size;
	}
	
	/*
	 * We do the load balancing between the Threads using some kind
	 * of round robin.
	 * 
	 * Additionally we should check from time to time whether the load
	 * is really still balanced. This is not guaranteed due to the fact
	 * that some Links get deactivated while others don't. If the number
	 * of Links is high enough statistically the difference shouldn't
	 * be to significant.
	 */	
	private static class LinkReActivator implements Runnable
	{
		private List<QLink> simActivateThis;
		private List<List<QLink>> parallelSimLinksLists;
		private int numOfThreads;
		private int distributor = 0;
		
		public LinkReActivator(List<QLink> simActivateThis, List<List<QLink>> parallelSimLinksLists, int numOfThreads)
		{
			this.simActivateThis = simActivateThis;
			this.parallelSimLinksLists = parallelSimLinksLists;
			this.numOfThreads = numOfThreads;
		}
		
		public void run()
		{
			if (!simActivateThis.isEmpty())
			{
				for(QLink link : simActivateThis)
				{
					parallelSimLinksLists.get(distributor % numOfThreads).add(link);
					distributor++;
				}
				simActivateThis.clear();
			}	
		}
		
	}
		
	/*
	 * Contains a QueueNode and MATSimRandom Object that is used when
	 * moving Vehicles over the Node.
	 * This is needed to ensure that the Simulation produces deterministic
	 * results that do not depend on the number of parallel Threads.
	 */
	private static class ExtendedQueueNode
	{
		private QNode queueNode;
		private Random random;
		
		public ExtendedQueueNode(QNode queueNode, Random random)
		{
			this.queueNode = queueNode;
			this.random = random;
		}
		
		public QNode getQueueNode()
		{
			return this.queueNode;
		}
		
		public Random getRandom()
		{
			return this.random;
		}
	}
	
	/*
	 * The thread class that really handles the Nodes.
	 */
	private static class MoveThread extends Thread
	{
		private double time = 0.0;
		private boolean simulateAllNodes = false;
		private boolean simulateAllLinks = false;
		
		private CyclicBarrier startBarrier;
		private CyclicBarrier separationBarrier;
		private CyclicBarrier endBarrier;
				
		private ExtendedQueueNode[] queueNodes;
		private List<QLink> links = new ArrayList<QLink>();
					
		public MoveThread(boolean simulateAllNodes)
		{
			this.simulateAllNodes = simulateAllNodes;
		}

		public void setStartBarrier(CyclicBarrier cyclicBarrier)
		{
			this.startBarrier = cyclicBarrier;
		}
		
		public void setSeparationBarrier(CyclicBarrier cyclicBarrier)
		{
			this.separationBarrier = cyclicBarrier;
		}
		
		public void setEndBarrier(CyclicBarrier cyclicBarrier)
		{
			this.endBarrier = cyclicBarrier;
		}
				
		public void setExtendedQueueNodeArray(ExtendedQueueNode[] queueNodes)
		{
			this.queueNodes = queueNodes;
		}
		
		public void handleLinks(List<QLink> links)
		{
			this.links = links;
		}
		
		public void setTime(final double t)
		{
			time = t;
		}
				
		@Override
		public void run()
		{	
			while(true)
			{
				try
				{
					/*
					 * The End of the Moving is synchronized with 
					 * the endBarrier. If all Threads reach this Barrier
					 * the main run() Thread can go on.
					 * 
					 * The Threads wait now at the startBarrier until
					 * they are triggered again in the next TimeStep by the main run()
					 * method.
					 */
					endBarrier.await();
						
					startBarrier.await();
										
					/*
					 * Move Nodes
					 */
					for (ExtendedQueueNode extendedQueueNode : queueNodes)
					{							
						QNode node = extendedQueueNode.getQueueNode();
//						synchronized(node)
//						{
							if (node.isActive() || node.isSignalized() || simulateAllNodes)
							{
								node.moveNode(time, extendedQueueNode.getRandom());
							}		
//						}
					}
					
					/*
					 * After moving the Nodes all we use a CyclicBarrier to synchronize
					 * the Threads. By using a Runnable within the Barrier we activate
					 * some Links. 
					 */
					this.separationBarrier.await();
					
					/*
					 * Move Links
					 */
					ListIterator<QLink> simLinks = this.links.listIterator();
					QLink link;
					boolean isActive;
					
					while (simLinks.hasNext()) 
					{
						link = simLinks.next();
						
						/*
						 * Synchronize on the QueueLink is only some kind of Workaround.
						 * It is only needed, if the QueueSimulation teleports Vehicles
						 * between different Threads. It would be probably faster, if the
						 * QueueSimulation would contain a synchronized method to do the
						 * teleportation instead of synchronize on EVERY QueueLink.
						 */
						synchronized(link)
						{
							isActive = link.moveLink(time);
							
							if (!isActive && !simulateAllLinks)
							{
								simLinks.remove();
							}
						}
					}
				}
				catch (InterruptedException e)
				{
					Gbl.errorMsg(e);
				}
	            catch (BrokenBarrierException e)
	            {
	            	Gbl.errorMsg(e);
	            }
			}
		}	// run()
		
	}	// ReplannerThread
	
}