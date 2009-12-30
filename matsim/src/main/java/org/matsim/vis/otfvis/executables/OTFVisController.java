/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.executables;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisControlerListener;

/**
 * This class shows how the controller is overloaded to run a "live" simulation.
 * Now the live simulation is run via an listener as an first step to get this more modular.
 * @author dstrippgen
 *
 */
public class OTFVisController extends Controler {

  private static final Logger log = Logger.getLogger(OTFVisController.class);

	public static final int NOCONTROL = 0x00000000;
	
	public static final int STARTUP = 0x01000000;
	public static final int RUNNING = 0x02000000;
	public static final int PAUSED = 0x80000000; //Flag for indicating paused mode to "other clients"	
	public static final int REPLANNING = 0x04000000;
	public static final int CANCEL = 0x08000000;
	public static final int ALL_FLAGS = 0xff000000;

	private static OTFVisControlerListener listener;
	
	static {
		listener = new OTFVisControlerListener();
	}
	
	public static int getStatus(int flags) {
		return listener.getStatus(flags);
	}
	
	public static int getIteration(int flags) {
		return listener.getIteration(flags);
	}
	
	public OTFVisController(String[] args) {
		super(args);
		this.addControlerListener(listener);
	}

	public OTFVisController(String configFileName) {
		super(configFileName);
		this.addControlerListener(listener);
	}
	
	/**
	 * Do nothing as the listener is doing all the work
	 */
	@Override
	protected void runMobSim() {
	}


	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("running OTFVisController...");
		OTFVisController controller = new OTFVisController(args);
		controller.setOverwriteFiles(true);
		controller.run();
	}
}

