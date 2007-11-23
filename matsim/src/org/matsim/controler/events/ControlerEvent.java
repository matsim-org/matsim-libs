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

package org.matsim.controler.events;

import org.matsim.controler.Controler;


/**
 * Basic event class for all Events fired by the Controler
 * @author dgrether
 *
 */
public abstract class ControlerEvent {
	/**
	 * The Controler instance which fired this event
	 */
	private Controler controler;
	/**
	 * 
	 * @param c
	 */
	public ControlerEvent(Controler c) {
		this.controler = c;
	}
	/**
	 * 
	 * @return the Controler instance which fired the event
	 */
	public Controler getControler() {
		return this.controler;
	}
	
}
