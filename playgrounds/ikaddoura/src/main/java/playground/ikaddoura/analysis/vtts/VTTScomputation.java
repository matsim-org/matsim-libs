/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.vtts;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

/**
* @author ikaddoura
*/

public class VTTScomputation implements StartupListener, AfterMobsimListener {

	private final VTTSHandler vttsHandler;

	public VTTScomputation(VTTSHandler vttsHandler) {
		this.vttsHandler = vttsHandler;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		event.getServices().getEvents().addHandler(vttsHandler);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		this.vttsHandler.computeFinalVTTS();
	}
	
}

