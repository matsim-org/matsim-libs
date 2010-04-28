/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCollector.java
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

package playground.christoph.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.network.MyLinkImpl;
import playground.christoph.network.SubLink;

public class TravelTimeCollector implements TravelTime, AgentStuckEventHandler,
	LinkEnterEventHandler, LinkLeaveEventHandler,
	AgentArrivalEventHandler, AgentDepartureEventHandler,
	SimulationBeforeSimStepListener, SimulationAfterSimStepListener{

	private Network network;

	// Trips with no Activity on the current Link
	private Map<Id, TripBin> regulatActiveTrips;	// PersonId

	/*
	 * We may have to sort / order the Trips to get deterministic results!
	 */
	private LinkedList<TripBin> finishedTrips;	// LinkId

	/*
	 * For parallel Execution
	 */
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private UpdateMeanTravelTimesThread[] updateMeanTravelTimesThreads;
	private int numOfThreads = 2;

	private MyLinkImpl[][] parallelArrays;

	public TravelTimeCollector(Network network)
	{
		this.network = network;

		init();
		init(this.numOfThreads);
	}

	private void init()
	{
		regulatActiveTrips = new HashMap<Id, TripBin>();
		finishedTrips = new LinkedList<TripBin>();

		for (Link link : this.network.getLinks().values())
		{
			MyLinkImpl myLink = (MyLinkImpl)link;
			myLink.cacheFreeSpeedTravelTime();
			myLink.setTravelTime(Math.ceil(myLink.getFreespeedTravelTime()));
			myLink.updateMeanTravelTime(Time.UNDEFINED_TIME);
		}
	}

	public double getLinkTravelTime(Link link, double time)
	{
		if (link instanceof SubLink) link = ((SubLink) link).getParentLink();

		MyLinkImpl myLink = (MyLinkImpl)link;
		return myLink.getTravelTime();
	}

	public void reset(int iteration)
	{
		// TODO Auto-generated method stub
	}

	public void handleEvent(LinkEnterEvent event)
	{
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();

		TripBin tripBin = new TripBin();
		tripBin.enterTime = time;
		tripBin.personId = personId;
		tripBin.linkId = linkId;

		this.regulatActiveTrips.put(personId, tripBin);
	}

	public void handleEvent(LinkLeaveEvent event)
	{
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();

		TripBin tripBin = this.regulatActiveTrips.remove(personId);
		if (tripBin != null)
		{
			tripBin.leaveTime = time;

			this.finishedTrips.add(tripBin);
		}
	}

	/*
	 * We don't have to count Stuck Events. The MobSim creates
	 * LeaveLink Events before throwing Stuck Events.
	 */
	public void handleEvent(AgentStuckEvent event)
	{

	}

	/*
	 * If an Agent performs an Activity on a Link we have
	 * to remove his current Trip. Otherwise we would have a
	 * Trip with the Duration of the Trip itself and the Activity.
	 */
	public void handleEvent(AgentArrivalEvent event)
	{
		Id personId = event.getPersonId();

		TripBin tripBin = this.regulatActiveTrips.remove(personId);
	}

	public void handleEvent(AgentDepartureEvent event)
	{

	}

	// Add Link TravelTimes
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e)
	{
		TripBin tripBin;
		while ((tripBin = finishedTrips.pollFirst()) != null)
		{
			double travelTime = tripBin.leaveTime - tripBin.enterTime;

			MyLinkImpl myLink = (MyLinkImpl)network.getLinks().get(tripBin.linkId);
			myLink.addTravelTime(travelTime, e.getSimulationTime());
		}
	}

	// Update Link TravelTimes
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e)
	{
//		for (Link link : this.network.getLinks().values())
//		{
//			MyLinkImpl myLink = (MyLinkImpl)link;
//			myLink.updateMeanTravelTime(e.getSimulationTime());
//		}

		// parallel Execution
		this.run(e.getSimulationTime());
	}

	private class TripBin {
		Id personId;
		Id linkId;
		double enterTime;
		double leaveTime;
	}

	/*
	 * ----------------------------------------------------------------
	 * Methods for parallel Execution
	 * ----------------------------------------------------------------
	 */

	/*
	 * The Threads are waiting at the TimeStepStartBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes. We wait until
	 * all of them reach the TimeStepEndBarrier to move on.
	 * We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the TimeStepEndBarrier before
	 * this Method does, it should work anyway.
	 */
	private void run(double time)
	{
		try
		{
			// set current Time
			for (UpdateMeanTravelTimesThread updateMeanTravelTimesThread : updateMeanTravelTimesThreads)
			{
				updateMeanTravelTimesThread.setTime(time);
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

	/*
	 * Create equal sized Arrays.
	 */
	private void createArrays()
	{
		List<List<MyLinkImpl>> links = new ArrayList<List<MyLinkImpl>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			links.add(new ArrayList<MyLinkImpl>());
		}

		int roundRobin = 0;
		for (Link link : this.network.getLinks().values())
		{
			MyLinkImpl myLink = (MyLinkImpl)link;
			links.get(roundRobin % numOfThreads).add(myLink);
			roundRobin++;
		}

		/*
		 * Now we create Arrays out of our Lists because iterating over them
		 * is much faster.
		 */
		parallelArrays = new MyLinkImpl[this.numOfThreads][];
		for (int i = 0; i < links.size(); i++)
		{
			List<MyLinkImpl> list = links.get(i);

			MyLinkImpl[] array = new MyLinkImpl[list.size()];
			list.toArray(array);
			parallelArrays[i] = array;
		}
	}

	public void init(int numOfThreads)
	{
		this.numOfThreads = numOfThreads;

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		createArrays();

		updateMeanTravelTimesThreads = new UpdateMeanTravelTimesThread[numOfThreads];

		// setup threads
		for (int i = 0; i < numOfThreads; i++)
		{
			UpdateMeanTravelTimesThread updateMeanTravelTimesThread = new UpdateMeanTravelTimesThread();
			updateMeanTravelTimesThread.setName("UpdateMeanTravelTimes" + i);
			updateMeanTravelTimesThread.setMyLinkImplsArray(this.parallelArrays[i]);
			updateMeanTravelTimesThread.setStartBarrier(this.startBarrier);
			updateMeanTravelTimesThread.setEndBarrier(this.endBarrier);
			updateMeanTravelTimesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			updateMeanTravelTimesThreads[i] = updateMeanTravelTimesThread;

			updateMeanTravelTimesThread.start();
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
	 * The thread class that updates the Mean Travel Times in the MyLinksImpls.
	 */
	private static class UpdateMeanTravelTimesThread extends Thread
	{
		private CyclicBarrier startBarrier;
		private CyclicBarrier endBarrier;

		private double time = 0.0;
		private MyLinkImpl[] myLinks;

		public UpdateMeanTravelTimesThread()
		{
		}

		public void setStartBarrier(CyclicBarrier cyclicBarrier)
		{
			this.startBarrier = cyclicBarrier;
		}

		public void setEndBarrier(CyclicBarrier cyclicBarrier)
		{
			this.endBarrier = cyclicBarrier;
		}

		public void setMyLinkImplsArray(MyLinkImpl[] myLinks)
		{
			this.myLinks = myLinks;
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

					for (MyLinkImpl myLink : myLinks)
					{
						myLink.updateMeanTravelTime(this.time);
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