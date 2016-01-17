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
package playground.anhorni.surprice.analysis;

import org.apache.log4j.Logger;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;


public class ModeSharesControlerListener implements StartupListener, IterationEndsListener {
	private ModeSharesEventHandler handler;
	private String xy;
	private static final Logger log = Logger.getLogger(ModeSharesControlerListener.class);
	
	public ModeSharesControlerListener(String xy) {
		this.xy = xy;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		MatsimServices controler = event.getServices();
		handler = new ModeSharesEventHandler(controler, this.xy);
		controler.getEvents().addHandler(handler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String path = event.getServices().getControlerIO()
			.getIterationPath(event.getIteration());
		handler.printInfo(event.getIteration());
		handler.writeXYsGraphic(path+"/" + this.xy + "ByMode.png", 20);
		log.info("--------------------------------------");
	}
}

