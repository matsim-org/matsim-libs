/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerStartupListener.java
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

package org.matsim.controler.listener;

import org.matsim.controler.events.ControlerStartupEvent;


/**
 * @author dgrether
 *
 */
public interface ControlerStartupListener extends ControlerListener {
	/**
	 * Notifies all observers that the controler is initialized and they should do the same
	 * @param controlerStartupEvent
	 */
	public void notifyStartup(ControlerStartupEvent controlerStartupEvent);

}
