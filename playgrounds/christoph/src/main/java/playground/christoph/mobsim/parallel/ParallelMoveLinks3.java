///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveLinks3.java
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
//import java.util.List;
//import java.util.ListIterator;
//import java.util.concurrent.BrokenBarrierException;
//import java.util.concurrent.CyclicBarrier;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueLink;
//
//public class ParallelMoveLinks3 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveLinks3.class);
//	
//	private MoveLinksThread[] moveLinksThreads;
//	private CyclicBarrier timeStepStartBarrier;
//	private CyclicBarrier timeStepEndBarrier;
//	private boolean simulateAllLinks = false;
//	private int numOfThreads;
//	
//	public List<List<QueueLink>> parallelSimLinksArray;
//	
//	public ParallelMoveLinks3(boolean simulateAllLinks)
//	{
//		this.simulateAllLinks = simulateAllLinks;
//	}
//
//	/*
//	 * The Threads are waiting at the TimeStepStartBarrier.
//	 * We trigger them by reaching this Barrier. Now the
//	 * Threads will start moving the Links. We wait until
//	 * all of them reach the TimeStepEndBarrier to move on.
//	 * We should not have any Problems with Race Conditions
//	 * because even if the Threads would be faster than this
//	 * Thread, means the reach the TimeStepEndBarrier before
//	 * this Method does, it should work anyway.
//	 */
//	public void run(double time)
//	{
//		try
//		{
//			// set current Time
//			for (MoveLinksThread moveLinksThread : moveLinksThreads) moveLinksThread.setTime(time);
//
//			this.timeStepStartBarrier.await();
//			
//			this.timeStepEndBarrier.await();
//		
//		} 
//		catch (InterruptedException e)
//		{
//			Gbl.errorMsg(e);
//		}
//      catch (BrokenBarrierException e)
//      {
//      	Gbl.errorMsg(e);
//      }
//	}
//		
//	public void init(List<List<QueueLink>> linkLists)
//	{
//		this.numOfThreads = linkLists.size();
//
//		this.timeStepStartBarrier = new CyclicBarrier(this.numOfThreads + 1);
//		this.timeStepEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
//		
//		moveLinksThreads = new MoveLinksThread[numOfThreads];
//		
//		// setup threads
//		for (int i = 0; i < numOfThreads; i++) 
//		{
//			MoveLinksThread moveLinkThread = new MoveLinksThread(simulateAllLinks);
//			moveLinkThread.setName("MoveLinks" + i);
//			moveLinkThread.handleLinks(linkLists.get(i));
//			moveLinkThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
//			moveLinkThread.setCyclicTimeStepStartBarrier(this.timeStepStartBarrier);
//			moveLinkThread.setCyclicTimeStepEndBarrier(this.timeStepEndBarrier);
//			moveLinksThreads[i] = moveLinkThread;
//			
//			moveLinkThread.start();
//		}
//		
//		/*
//		 * After initialization the Threads are waiting at the
//		 * TimeStepEndBarrier. We trigger this Barrier once so 
//		 * they wait at the TimeStepStartBarrier what has to be
//		 * their state if the run() method is called.
//		 */
//		try
//		{
//			this.timeStepEndBarrier.await();
//		} 
//		catch (InterruptedException e) 
//		{
//			Gbl.errorMsg(e);
//		} 
//		catch (BrokenBarrierException e)
//		{
//			Gbl.errorMsg(e);
//		}
//	}
//
//	/**
//	 * The thread class that really handles the links.
//	 */
//	private static class MoveLinksThread extends Thread
//	{
//		private double time = 0.0;
//		private boolean simulationRunning = true;
//		private boolean simulateAllLinks = false;
//		
//		private List<QueueLink> links = new ArrayList<QueueLink>();
//		
//		private CyclicBarrier timeStepStartBarrier;
//		private CyclicBarrier timeStepEndBarrier;
//		
//		public MoveLinksThread(boolean simulateAllLinks)
//		{
//			this.simulateAllLinks = simulateAllLinks;
//		}
//		
//		public void setTime(final double t)
//		{
//			time = t;
//		}
//		
//		public void handleLinks(List<QueueLink> links)
//		{
//			this.links = links;
//		}
//
//		public void setCyclicTimeStepStartBarrier(CyclicBarrier barrier)
//		{
//			this.timeStepStartBarrier = barrier;
//		}
//		
//		public void setCyclicTimeStepEndBarrier(CyclicBarrier barrier)
//		{
//			this.timeStepEndBarrier = barrier;
//		}
//						
//		@Override
//		public void run()
//		{
//			while (simulationRunning)
//			{
//				try
//				{
//					/*
//					 * The End of the Link Moving is synchronized with 
//					 * the TimeStepEndBarrier. If all Threads reach this Barrier
//					 * the main run() Thread can go on.
//					 * 
//					 * The Threads wait now at the TimeStepStartBarrier until
//					 * they are triggered again in the next TimeStep by the main run()
//					 * method.
//					 */
//					timeStepEndBarrier.await();
//						
//					timeStepStartBarrier.await();
//
//					
//					ListIterator<QueueLink> simLinks = this.links.listIterator();
//					QueueLink link;
//					boolean isActive;
//
//					while (simLinks.hasNext()) 
//					{
//						link = simLinks.next();
//						
//						/*
//						 * Synchronize on the QueueLink is only some kind of Workaround.
//						 * It is only needed, if the QueueSimulation teleports Vehicles
//						 * between different Threads. It would be probably faster, if the
//						 * QueueSimulation would contain a synchronized method to do the
//						 * teleportation instead of synchronize on EVERY QueueLink.
//						 */
//						synchronized(link)
//						{
//							isActive = link.moveLink(time);
//							
//							if (!isActive && !simulateAllLinks)
//							{
//								simLinks.remove();
//							}
//						}
//					}
//				}
//				catch (InterruptedException e)
//				{
//					log.error("Something is going wrong here...");
//					Gbl.errorMsg(e);
//				}
//	            catch (BrokenBarrierException e)
//	            {
//	            	Gbl.errorMsg(e);
//	            }
//			}	// while Simulation Running
//			
//		}	// run()
//		
//	}	// ReplannerThread
//	
//}