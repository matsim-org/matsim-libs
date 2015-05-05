/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerStartupEvent.java
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

package org.matsim.core.controler.events;

import org.matsim.core.controler.Controler;

/**
 * ControlerEvent to notify all observers of the controler that the controler instance is setup
 *
 * @author dgrether
 */
public final class StartupEvent extends ControlerEvent {

	public StartupEvent(final Controler controler) {
		super(controler);
	}

}
