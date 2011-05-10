/* *********************************************************************** *
 * project: org.matsim.*
 * JointReplanningControlerListener.java
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
package playground.thibautd.jointtripsoptimizer.run;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

import herbie.running.analysis.ModeSharesEventHandler;

/**
 * A controler listener to load any necessary EventsListener in the
 * controler.
 * @author thibautd
 */
public class JointReplanningControlerListener implements StartupListener, IterationEndsListener {
	private ModeSharesEventHandler handler;

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();
		handler = new ModeSharesEventHandler(controler);
		controler.getEvents().addHandler(handler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		String path = event.getControler().getControlerIO()
			.getIterationPath(event.getIteration());
		handler.printInfo(event.getIteration());
		handler.writeTraveledDistancesGraphic(path+"/graphicTest.png", 20);
	}
}

