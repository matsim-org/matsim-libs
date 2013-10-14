/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterXML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.events.implementations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.christoph.events.EventHandlerInstance;
import playground.christoph.events.MasterEventHandler;

public class EventWriterXML implements EventWriter, MasterEventHandler {

	private Thread writerThread;
	private WriterRunnable writerRunnable;
	
	private final Set<EventsWriterXMLInstance> instances = new LinkedHashSet<EventsWriterXMLInstance>();
	private final BlockingQueue<String[]> queue = new LinkedBlockingQueue<String[]>();
	
	public EventWriterXML(final String filename) {
		init(filename);
	}
	
	@Override
	public void closeFile() {
		this.writerRunnable.closeFile();
	}

	public void init(final String outfilename) {
		this.writerRunnable = new WriterRunnable(this.queue);
		this.writerRunnable.init(outfilename);
		writerThread = new Thread(this.writerRunnable);
		writerThread.setDaemon(true);
		writerThread.setName("EventsWriterXMLThread");
		writerThread.start();
	}

	@Override
	public void reset(final int iter) {
		for (BasicEventHandler instance : instances) instance.reset(iter);
		this.closeFile();
		this.instances.clear();
		this.queue.clear();
	}

	@Override
	public EventHandlerInstance createInstance() {
		EventsWriterXMLInstance instance = new EventsWriterXMLInstance(this.queue);
		instances.add(instance);
		return instance;
	}
	
	@Override
	public void finishEventsHandling() {
		for (EventsWriterXMLInstance instance : instances) instance.synchronize(Double.MAX_VALUE);

		// tell writer
		this.writerRunnable.finishWriting = true;
		
		// send an empty queue to ensure that the writing thread recognizes the finishWriting flag
		this.queue.add(new String[0]);
		
		try {
			this.writerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void synchronize(double time) {
		for (EventHandlerInstance instance : instances) instance.synchronize(time);
	}
	
	private static class WriterRunnable implements Runnable, EventWriter {

		private volatile boolean finishWriting = false;
		private BufferedWriter out = null;
		private BlockingQueue<String[]> queues;
		
		public WriterRunnable(BlockingQueue<String[]> queues) {
			this.queues = queues;
		}
		
		@Override
		public void run() {
			while (true) {				
				try {
					if (finishWriting && this.queues.size() == 0) {
						Gbl.printThreadCpuTime(Thread.currentThread());
						return;
					}
					
					String[] queue = this.queues.take();
					
					for (String eventString : queue) {
						if (eventString == null) continue;
						this.out.write(eventString.toString());
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		public void init(final String outfilename) {
			closeFile();
			try {
				this.out = IOUtils.getBufferedWriter(outfilename);
				this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void closeFile() {
			if (this.out != null) {
				try {
					this.out.write("</events>");
					// I added a "\n" to make it look nicer on the console.  Can't say if this may have unintended side
					// effects anywhere else.  kai, oct'12
					// fails signalsystems test (and presumably other tests in contrib/playground) since they compare
					// checksums of event files.  Removed that change again.  kai, oct'12
					this.out.close();
					this.out = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

}