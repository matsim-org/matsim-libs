package playground.christoph.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.network.MyLinkImpl;

public class TravelTimeCollector implements TravelTime, AgentStuckEventHandler,
	LinkEnterEventHandler, LinkLeaveEventHandler,
	AgentArrivalEventHandler, AgentDepartureEventHandler,
	QueueSimulationBeforeSimStepListener, QueueSimulationAfterSimStepListener{

	private Network network;
	
	// Trips with no Activity on the current Link
	private Map<Id, TripBin> regulatActiveTrips;	// PersonId
	
	/*
	 * We may have to sort / order the Trips to get deterministic results!
	 */
	private List<TripBin> finishedTrips;	// LinkId
	
	/*
	 * For parallel Execution
	 */
	private static AtomicInteger threadLock;
	private UpdateMeanTravelTimesThread[] updateMeanTravelTimesThreads;
	private int numOfThreads = 8;
	
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
		finishedTrips = new ArrayList<TripBin>();
		
		for (Link link : this.network.getLinks().values())
		{
			MyLinkImpl myLink = (MyLinkImpl)link;
			myLink.cacheFreeSpeedTravelTime();
			myLink.setTravelTime(Math.ceil(myLink.getFreespeedTravelTime(Time.UNDEFINED_TIME)));
			myLink.updateMeanTravelTime(Time.UNDEFINED_TIME);
		}
	}
	

	public double getLinkTravelTime(Link link, double time)
	{
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
	public void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e)
	{
		Iterator<TripBin> iter = this.finishedTrips.iterator();
		
		while (iter.hasNext())
		{
			TripBin tripBin = iter.next();
			
			double travelTime = tripBin.leaveTime - tripBin.enterTime;
			
			MyLinkImpl myLink = (MyLinkImpl)network.getLinks().get(tripBin.linkId);
			myLink.addTravelTime(travelTime, e.getSimulationTime());
			
			iter.remove();
		}
	}
	
	// Update Link TravelTimes
	public void notifySimulationBeforeSimStep(QueueSimulationBeforeSimStepEvent e)
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
			// lock threadLocker until wait() statement listens for the notifies
			synchronized(threadLock) 
			{
				threadLock.set(this.numOfThreads);
							
				for (UpdateMeanTravelTimesThread updateMeanTravelTimesThread : updateMeanTravelTimesThreads) 
				{
					updateMeanTravelTimesThread.setTime(time);
					synchronized(updateMeanTravelTimesThread)
					{
						updateMeanTravelTimesThread.notify();
					}
				}
				threadLock.wait();
			}		
		} 
		catch (InterruptedException e)
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
		
		threadLock = new AtomicInteger(0);

		createArrays();
		
		updateMeanTravelTimesThreads = new UpdateMeanTravelTimesThread[numOfThreads];
						
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			UpdateMeanTravelTimesThread updateMeanTravelTimesThread = new UpdateMeanTravelTimesThread();
			updateMeanTravelTimesThread.setName("UpdateMeanTravelTimes" + i);
			updateMeanTravelTimesThread.setMyLinkImplsArray(this.parallelArrays[i]);			
			updateMeanTravelTimesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			updateMeanTravelTimesThreads[i] = updateMeanTravelTimesThread;
			
			updateMeanTravelTimesThread.start();
		}
	}
	/*
	 * The thread class that updates the Mean Travel Times in the MyLinksImpls.
	 */
	private static class UpdateMeanTravelTimesThread extends Thread
	{
		private double time = 0.0;
		private MyLinkImpl[] myLinks;
		
		public UpdateMeanTravelTimesThread()
		{
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
					 * threadLocker.decCounter() and wait() have to be
					 * executed in a synchronized block! Otherwise it could
					 * happen, that the replanning ends and the replanning of
					 * the next SimStep executes the notify command before
					 * wait() is called -> we have a DeadLock!
					 */
					synchronized(this)
					{
						int activeThreads = threadLock.decrementAndGet();
						if (activeThreads == 0)
						{
							synchronized(threadLock)
							{
								threadLock.notify();
							}
						}
						wait();
					}
					
					for (MyLinkImpl myLink : myLinks)
					{	
						myLink.updateMeanTravelTime(this.time);
					}
				}
				catch (InterruptedException e)
				{
					Gbl.errorMsg(e);
				}
			}
		}	// run()
		
	}	// ReplannerThread
	
}