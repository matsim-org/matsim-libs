package org.matsim.contrib.protobuf;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.misc.Counter;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by laemmel on 17/02/16.
 */
public class Example implements BasicEventHandler{

	private static final Logger log = Logger.getLogger(Example.class);

	private Counter counter = new Counter("number of events");


	@Override
	public void handleEvent(Event event) {
		counter.incCounter();
	}

	@Override
	public void reset(int iteration) {

	}

	public static void main(String [] args) throws IOException {

		String rawInput = "/Users/laemmel/Downloads/0.events.xml.gz";
		String output1 = "/tmp/f01.pbf";
		String output2 = "/tmp/f02.pbf";

		for (int i = 0; i < 10; i++) {
			long tm1, tm2, tm3, tm4;

			{

				log.info("Reading XML writing pbf");

//			Example e = new Example();
				long start = System.currentTimeMillis();
				EventsManager em = new EventsManagerImpl();
				FileOutputStream fos = new FileOutputStream(output1);
				ProtoEventsWriter pew = new ProtoEventsWriter(fos);
				em.addHandler(pew);
				MatsimEventsReader r = new MatsimEventsReader(em);

				r.readFile(rawInput);
				fos.close();
				long stop = System.currentTimeMillis();
				tm1 = stop - start;
			}
			{
				log.info("Reading pbf writing pbf");
				long start = System.currentTimeMillis();
				EventsManager em = new EventsManagerImpl();
				FileOutputStream fos = new FileOutputStream(output2);
				ProtoEventsWriter pew = new ProtoEventsWriter(fos);
				em.addHandler(pew);

				new ProtoEventsParser(em).parse(output1);
				fos.close();
				long stop = System.currentTimeMillis();
				tm2 = stop - start;
			}
			{
				log.info("Reading XML");
				long start = System.currentTimeMillis();
				EventsManager em = new EventsManagerImpl();
				Example e = new Example();
				em.addHandler(e);
				MatsimEventsReader r = new MatsimEventsReader(em);
				r.readFile(rawInput);
				log.info(e.counter.getCounter());
				long stop = System.currentTimeMillis();
				tm3 = stop - start;
			}

			{
				log.info("Reading pbf");
				long start = System.currentTimeMillis();
				EventsManager em = new EventsManagerImpl();
				Example e = new Example();
				em.addHandler(e);
				new ProtoEventsParser(em).parse(output1);
				log.info(e.counter.getCounter());
				long stop = System.currentTimeMillis();
				tm4 = stop - start;
			}
			log.info("XML --> pbf took: " + tm1);
			log.info("pbf --> pbf took: " + tm2);
			log.info("XML --> null took: " + tm3);
			log.info("pbf --> null took: " + tm4);

		}

	}
}
