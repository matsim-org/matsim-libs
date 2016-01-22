/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.anhorni.barbellscenario;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

public class TravelTimesControlerListener implements StartupListener, IterationEndsListener {
	private TravelTimeEventHandler handler;
	private int finalIteration = -1;
	private static final Logger log = Logger.getLogger(TravelTimesControlerListener.class);
	private List<Double> netTTs = new Vector<Double>();
	private List<Double> linkTTs = new Vector<Double>();
	
	public TravelTimesControlerListener(int finalIteration) {
		this.finalIteration = finalIteration;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		MatsimServices controler = event.getServices();
		handler = new TravelTimeEventHandler();
		controler.getEvents().addHandler(handler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {		
		int iteration = event.getIteration();
		
		if (iteration == finalIteration) {
			this.netTTs = handler.getNetTTs();
			this.linkTTs = handler.getLinkTTs();
		}
		log.info("--------------------------------------");
	}
	
	public List<Double> getNetTTs() {
		return netTTs;
	}

	public List<Double> getLinkTTs() {
		return linkTTs;
	}
}

