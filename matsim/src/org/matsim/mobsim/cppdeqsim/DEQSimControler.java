/* *********************************************************************** *
 * project: org.matsim.*
 * DEQSimControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.cppdeqsim;


import org.matsim.controler.Controler;

public class DEQSimControler extends Controler {

	public DEQSimControler(final String[] args) {
		super(args);
	}

	@Override
	protected void runMobSim() {
		/* remove eventswriter, as the deqsim writes the (binary) events */
		setWriteEventsInterval(0);

		DEQSim sim = new DEQSim(this.population, this.events);
		sim.setIterationStopWatch(this.stopwatch);
		sim.run();
	}

	public static void main(final String[] args) {
		final Controler controler = new DEQSimControler(args);
		controler.addControlerListener(new DEQSimControlerListener());
		controler.run();
		System.exit(0);
	}

}
