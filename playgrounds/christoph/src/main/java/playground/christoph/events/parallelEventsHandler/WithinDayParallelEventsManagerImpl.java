/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.christoph.events.parallelEventsHandler;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;

/**
 * 
 * ParallelEvents allows parallelization for events handling.
 * Usage: First create an object of this class. Before each iteration, call initProcessing.
 * After each iteration, call finishProcessing. This has already been incorporated into
 * the Controller.
 * 
   Usage via config.xml: 
   <module name="parallelEventHandling">
		<param name="numberOfThreads"        value="2" />
   </module>
	
	optionally you can also specify the estimated number of events per iteration:
    <param name="estimatedNumberOfEvents"        value="10000000" />
    (not really needed, but can make performance slightly faster in larger simulations).
 * @see http://www.matsim.org/node/238
 * @author rashid_waraich
 * 
 */
public class WithinDayParallelEventsManagerImpl extends EventsManagerImpl implements SimulationAfterSimStepListener{

	private int numberOfThreads;
	private EventsManagerImpl[] events = null;
	private WithinDayProcessEventThread[] eventsProcessThread = null;
	private int numberOfAddedEventsHandler = 0;
	private CyclicBarrier simStepBarrier = null;
	private CyclicBarrier iterationBarrier = null;
	// this number should be set in the following way:
	// if the number of events is estimated as x, then this number
	// could be set to x/10
	// the higher this parameter, the less locks are used, but
	// the more the time buffer between the simulation and events handling
	// for small simulations, the default value is ok and it even works
	// quite well for larger simulations with 10 million events
	private int preInputBufferMaxLength = 100000;

	
	/**
	 * @param numberOfThreads -
	 *            specify the number of threads used for the events handler
	 */
	public WithinDayParallelEventsManagerImpl(int numberOfThreads)
	{
		init(numberOfThreads);
	}

	/**
	 * 
	 * @param numberOfThreads
	 * @param estimatedNumberOfEvents
	 * Only use this constructor for larger simulations (20M+ events).
	 */
	public WithinDayParallelEventsManagerImpl(int numberOfThreads, int estimatedNumberOfEvents) {
		preInputBufferMaxLength = estimatedNumberOfEvents / 10;
		init(numberOfThreads);
	}
	
	@Override
	public void processEvent(final Event event) {
		for (int i = 0; i < eventsProcessThread.length; i++) {
			eventsProcessThread[i].processEvent(event);
		}
	}

	
	@Override
	public void addHandler(final EventHandler handler) {
		synchronized (this) {
			events[numberOfAddedEventsHandler].addHandler(handler);
			numberOfAddedEventsHandler = (numberOfAddedEventsHandler + 1)
					% numberOfThreads;
		}
	}
	
	
	@Override
	public void resetHandlers(final int iteration) {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].resetHandlers(iteration);
			}
		}
	}
	
	
	@Override
	public void resetCounter() {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].resetCounter();
			}
		}
	}
	
	
	@Override
	public void removeHandler(final EventHandler handler) {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].removeHandler(handler);
			}
		}
	}
	
	
	@Override
	public void clearHandlers() {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].clearHandlers();
			}
		}
	}
	
	
	@Override
	public void printEventHandlers() {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].printEventHandlers();
			}
		}
	}

	private void init(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		this.events = new EventsManagerImpl[numberOfThreads];
		this.eventsProcessThread = new WithinDayProcessEventThread[numberOfThreads];
		// the additional 1 is for the simulation barrier
		simStepBarrier = new CyclicBarrier(numberOfThreads + 1);
		iterationBarrier = new CyclicBarrier(numberOfThreads + 1);
		for (int i = 0; i < numberOfThreads; i++)
		{
			events[i] = (EventsManagerImpl) EventsUtils.createEventsManager();
		}
	}

	// When one simulation iteration is finish, it must call this method,
	// so that it can communicate to the threads, that the simulation is 
	// finished and that it can await the event handler threads.
	@Override
	public void finishProcessing()
	{
		for (int i = 0; i < eventsProcessThread.length; i++)
		{
			eventsProcessThread[i].closeIteration();
		}
		try
		{
			iterationBarrier.await();
		} 
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (BrokenBarrierException e)
		{
			e.printStackTrace();
		}
	}
	
	// create event handler threads
	// prepare for next iteration
	@Override
	public void initProcessing()
	{
		// reset this class, so that it can be reused for the next iteration
		for (int i = 0; i < numberOfThreads; i++)
		{
			eventsProcessThread[i] = new WithinDayProcessEventThread(events[i], preInputBufferMaxLength, simStepBarrier, iterationBarrier);
			new Thread(eventsProcessThread[i], "WithinDayParallelEvents-" + i).start();
		}
	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent event)
	{
		for (int i = 0; i < eventsProcessThread.length; i++)
		{
			eventsProcessThread[i].closeSimStep();
		}
		try
		{
			iterationBarrier.await();
		} 
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (BrokenBarrierException e)
		{
			e.printStackTrace();
		}
	}
}