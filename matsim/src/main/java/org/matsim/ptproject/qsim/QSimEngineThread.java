/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineThread.java
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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.ParallelQSimEngine.ExtendedQueueNode;

public class QSimEngineThread extends Thread implements QSimEngine{
	
	private double time = 0.0;
	private boolean simulateAllNodes = false;
	private boolean simulateAllLinks = false;

	private boolean simulationRunning = true;
	
	private final CyclicBarrier startBarrier;
	private final CyclicBarrier separationBarrier;
	private final CyclicBarrier endBarrier;

	private ExtendedQueueNode[] queueNodes;
	private List<QLink> links = new ArrayList<QLink>();

	/** This is the collection of links that have to be activated in the current time step */
	/*package*/ final ArrayList<QLink> linksToActivate = new ArrayList<QLink>();
  private QSim qsim;
		
	public QSimEngineThread(boolean simulateAllNodes, boolean simulateAllLinks, CyclicBarrier startBarrier, CyclicBarrier separationBarrier, CyclicBarrier endBarrier, QSim sim)
	{
		this.simulateAllNodes = simulateAllNodes;
		this.simulateAllLinks = simulateAllLinks;
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
		this.qsim = sim;
	}

	public void setExtendedQueueNodeArray(ExtendedQueueNode[] queueNodes)
	{
		this.queueNodes = queueNodes;
	}

	public void setLinks(List<QLink> links)
	{
		this.links = links;
	}

	public void addLink(QLink link)
	{
		this.links.add(link);
	}

	public void setTime(final double t)
	{
		time = t;
	}

	public void afterSim() {
		this.simulationRunning = false;
	}

	public void simStep(double time) {
		// nothing to do here
	}
	
	@Override
	public void run()
	{
		/*
		 * The method is ended when the simulationRunning Flag is
		 * set to false.
		 */
		while(true)
		{
			try
			{
				/*
				 * The Threads wait at the startBarrier until they are 
				 * triggered in the next TimeStep by the run() method in
				 * the ParallelQSimEngine.
				 */
				startBarrier.await();

				/*
				 * Check if Simulation is still running.
				 * Otherwise print CPU usage and end Thread.
				 */
				if (!simulationRunning)
				{
					Gbl.printCurrentThreadCpuTime();
					return;
				}
				
				/*
				 * Move Nodes
				 */
				for (ExtendedQueueNode extendedQueueNode : queueNodes)
				{
					QNode node = extendedQueueNode.getQueueNode();
//					synchronized(node)
//					{
						if (node.isActive() || node.isSignalized() || simulateAllNodes)
						{
							node.moveNode(time, extendedQueueNode.getRandom());
						}
//					}
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
				
				/*
				 * The End of the Moving is synchronized with
				 * the endBarrier. If all Threads reach this Barrier
				 * the main Thread can go on.
				 */
				endBarrier.await();
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

	public void activateLink(QLink link)
	{
		if (!simulateAllLinks)
		{
			linksToActivate.add(link);
		}
	}

	public void activateLinks()
	{
		this.links.addAll(this.linksToActivate);
		this.linksToActivate.clear();
	}
	
	public List<QLink> getLinksToActivate()
	{
		return this.linksToActivate;
	}

	public int getNumberOfSimulatedLinks()
	{
		return this.links.size();
	}

  @Override
  public QSim getQSim() {
    return this.qsim;
  }

}
