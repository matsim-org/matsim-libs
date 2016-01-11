/* *********************************************************************** *
 * project: org.matsim.*
 * VolumeCounterControlerListener.java
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

/**
 * 
 */
package playground.ucsb.analysis;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author balmermi
 *
 */
public class VolumeCounterControlerListener implements IterationEndsListener,
		StartupListener {

	private VolumeCounter volumeCounter;
	
	/**
	 * 
	 */
	public VolumeCounterControlerListener() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.services.listener.StartupListener#notifyStartup(org.matsim.core.services.events.StartupEvent)
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		volumeCounter = new VolumeCounter(event.getServices().getScenario().getNetwork());
		event.getServices().getEvents().addHandler(volumeCounter);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.services.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.services.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		volumeCounter.writeVolumes(event.getServices().getControlerIO().getIterationFilename(event.getIteration(),"linkVolumes.txt.gz"));
	}
}
