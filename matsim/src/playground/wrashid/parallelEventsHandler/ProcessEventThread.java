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

package playground.wrashid.parallelEventsHandler;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;


public class ProcessEventThread implements Runnable {
	ArrayList<BasicEvent> preInputBuffer=null;
	ConcurrentListSPSC<BasicEvent> eventQueue = null;
	Events events;
	CyclicBarrier cb=null;
	private int preInputBufferMaxLength;

	public ProcessEventThread(Events events, int preInputBufferMaxLength) {
		this.events = events;
		this.preInputBufferMaxLength= preInputBufferMaxLength;
		eventQueue = new ConcurrentListSPSC<BasicEvent>();
		preInputBuffer= new ArrayList<BasicEvent>();
		cb=new CyclicBarrier(2);
		Thread t = new Thread(this);
		t.start();
	}
	
	
	public ProcessEventThread(Events events, int preInputBufferMaxLength, CyclicBarrier cb) {
		this.events = events;
		this.preInputBufferMaxLength= preInputBufferMaxLength;
		eventQueue = new ConcurrentListSPSC<BasicEvent>();
		preInputBuffer= new ArrayList<BasicEvent>();
		this.cb=cb;
		
		Thread t = new Thread(this);
		t.start();
	}
	
	
	
	

	
	// a different approach was tried, but it was not as efficient:
	// buffer the elements locally and then write them to eventQueue at once.
	// the problem with this is, that actually the current thread is allowed to 
	// be slow. But this second approach makes the processEventThread slow instead
	// of the main thread.
	
	// This second proposed approach would again make sense, if we use the main thread
	// after its completion as a worker thread.
	
	// This can be further improved: make a list of lists, so that no copying is needed
	// during synchronization
	public void processEvent(BasicEvent event) {
		// first approach (quick on office computer)
		//eventQueue.add(event);
		
		// second approach, lesser locking => faster on Satawal
		preInputBuffer.add(event);
		if (preInputBuffer.size()>preInputBufferMaxLength){
			eventQueue.add(preInputBuffer);
			preInputBuffer.clear();
		}
	}

	public void run() {
		// process events, until DummyEvent arrives
		BasicEvent nextEvent = null;
		while (true) {
			nextEvent = eventQueue.remove();
			if (nextEvent != null) {
				if (nextEvent instanceof DummyEvent) {
					break;
				} else {
					events.processEvent(nextEvent);
				}
			}
		}
		// inform main thread, that processing finished
		try {
			cb.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// schedule dummy event and flush buffered events
	public void close(){
		processEvent(new DummyEvent(0.0));
		eventQueue.add(preInputBuffer);
		preInputBuffer.clear();
	}

	// call to flush buffer
	public void awaitHandler() {
		close();
		
		try {
			cb.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	
}
