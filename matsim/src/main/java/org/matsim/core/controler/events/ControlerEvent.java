/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerEvent.java
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
 * Basic event class for all Events fired by the Controler
 *
 * @author dgrether
 */
public abstract class ControlerEvent {
	/**
	 * The Controler instance which fired this event
	 */
	protected final Controler controler;

	public ControlerEvent(final Controler controler) {
		this.controler = controler;
	}

	@Deprecated // use injection to access central objects.  kai/mz, nov'15   
	public Controler getControler() {
		return this.controler;
	}

}
