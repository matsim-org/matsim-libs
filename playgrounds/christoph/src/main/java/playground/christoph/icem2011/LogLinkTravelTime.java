/* *********************************************************************** *
 * project: org.matsim.*
 * LogLinkTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.icem2011;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;

public class LogLinkTravelTime implements SimulationBeforeSimStepListener, BeforeMobsimListener, AfterMobsimListener {

	private static final Logger log = Logger.getLogger(LogLinkTravelTime.class);
	
	private Collection<Link> links;
	private TravelTime expectedTravelTime;
	private TravelTime measuredTravelTime;
	
	private String delimiter = ",";
	private Charset charset = Charset.forName("UTF-8");

	private int nextWrite = 0;
	private int writeInterval = 60;
	
	private Map<Id, StringBuffer> data = null;
	
	public LogLinkTravelTime(Collection<Link> links, TravelTime expectedTravelTime, TravelTime measuredTravelTime) {
		this.links = links;
		this.expectedTravelTime = expectedTravelTime;
		this.measuredTravelTime = measuredTravelTime;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		data = new HashMap<Id, StringBuffer>();
		
		for (Link link : links) {
			data.put(link.getId(), new StringBuffer());
		}
	}
	
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
	
		double time = e.getSimulationTime();
		if (time >= nextWrite) {
			nextWrite += writeInterval;

			for (Link link : links) {
				double ett = expectedTravelTime.getLinkTravelTime(link, time);
				double mtt = measuredTravelTime.getLinkTravelTime(link, time);
				data.get(link.getId()).append(time + delimiter + ett + delimiter + mtt + "\n");
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			log.info("Writing expected travel time files...");
			Counter counter = new Counter("Writing expected travel time files: "); 
			for (Link link : links) {
				String file = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "expectedLinkTravelTimes_" + link.getId() + ".txt");
		
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
				BufferedWriter bw = new BufferedWriter(osw);
				
				bw.write("time" + delimiter + "expected travel time" + delimiter + "measured travel time" + "\n");
				bw.write(data.get(link.getId()).toString());
				
				bw.close();
				osw.close();
				fos.close();
				counter.incCounter();
			}
			counter.printCounter();
			log.info("done.");
		} catch (IOException e1) {
			Gbl.errorMsg(e1);
		}
	}
}