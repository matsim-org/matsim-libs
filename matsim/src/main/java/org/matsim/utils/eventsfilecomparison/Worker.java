package org.matsim.utils.eventsfilecomparison;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.EventsManagerFactoryImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.handler.BasicEventHandler;
import org.xml.sax.SAXException;

public class Worker extends Thread implements BasicEventHandler{
	
	private static final Logger log = Logger.getLogger(Worker.class);
	
	private final EventsManager e;
	private final String eFile;
	private double time = -1;
	private final EventsFileComparator eFC;
	
	private final Map<String,Counter> events = new HashMap<String,Counter>();
	private int numEvents = 0;
	private boolean abort = false;

	public Worker(String eFile1, EventsFileComparator eventsFileComparator) {
		this.e = new EventsManagerFactoryImpl().createEventsManager();
		this.e.addHandler(this);
		this.eFile = eFile1;
		this.eFC = eventsFileComparator;
	}

	public synchronized void abort() {
		this.abort = true;
		this.cont();
		
	}

	public synchronized boolean isAborted(boolean toggle) {
		if (toggle) {
			this.abort = this.abort == true ? false : true;
		}
		return this.abort;
	}
	public synchronized void cont() {
		if (this.isAlive() ) {
			this.notify();
		}
	}

	public String getEFile() {
		return this.eFile;
	}

	@Override
	public void run(){
		if (this.eFile.endsWith("xml.gz")) {
			
			try {
				new EventsReaderXMLv1(this.e).parse(this.eFile);
			} catch (SAXException e) {
				this.eFC.abort(-99);
				throw new RuntimeException(e);
			} catch (ParserConfigurationException e) {
				this.eFC.abort(-99);
				throw new RuntimeException(e);
			} catch (IOException e) {
				this.eFC.abort(-99);
				throw new RuntimeException(e);
			}
		} else if (this.eFile.endsWith("txt.gz")) {
			try {
				new EventsReaderTXTv1(this.e).readFile(this.eFile);
			} catch (Exception e) {
				this.eFC.abort(-99);
				e.printStackTrace();
				
			}
		} else {
			this.eFC.abort(-99);
			throw new RuntimeException("Unrecognized events file suffix.");
		}
		this.eFC.timeStepFinished(true);
	}

	public Map<String, Counter> getEventsMap() {
		return this.events;
	}
		
	public double getCurrentTime() {
		return this.time;
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
		
	@Override
	public void handleEvent(Event event) {
		if (this.isAborted(false)) {
			log.warn("Stopping thread with Thread.stop()");
			this.stop();
		}
		
		if (this.time != event.getTime()) {
			if (this.eFC.timeStepFinished(false)) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			this.time = event.getTime();
			this.numEvents = 0;
		}

		this.numEvents++;
		addEvent(event);
	}
	public int getNumEvents(){
		return this.numEvents;
	}

	private void addEvent(Event event){
		String lexString = toLexicographicSortedString(event);
		Counter counter = this.events.get(lexString);
		if (counter == null) {
			counter = new Counter();
			this.events.put(lexString, counter);
		}
		counter.increment();
	}
	private String toLexicographicSortedString(Event event) {
		
		List<String> strings = new ArrayList<String>();
		for (Entry<String, String> e : event.getAttributes().entrySet()) {
			StringBuilder tmp = new StringBuilder();
			tmp.append(e.getKey());
			tmp.append("=");
			tmp.append(e.getValue());
			strings.add(tmp.toString());
			
		}
		Collections.sort(strings);
		StringBuilder eventStr = new StringBuilder();
		for (String str : strings) {
			eventStr.append("_");
			eventStr.append(str);
			
		}
		return eventStr.toString();
	}



}



