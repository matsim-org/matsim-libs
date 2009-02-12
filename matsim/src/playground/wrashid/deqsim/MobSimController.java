/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package playground.wrashid.deqsim;

import org.matsim.controler.Controler;
import org.matsim.mobsim.jdeqsim.util.Timer;



/**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 *
 * @author mrieser
 */
public class MobSimController extends Controler {

	public MobSimController(final String[] args) {
	    super(args);
	  }

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		final MobSimController controler = new MobSimController(args);
		controler.run();
		t.endTimer();
		t.printMeasuredTime("Time needed for MobSimController run: ");
		controler.events.printEventsCount();
	}
}
