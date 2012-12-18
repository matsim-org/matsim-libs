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

	/**Design decision (jun'12):<ul>
	 * <li> The ControlerListeners should not get access to the Controler via the ControlerEvents.  Reason:
	 * The Controler is a much too powerful object, and it thus inhibits use of the ControlerListeners from objects which are
	 * not as powerful.
	 * <li> If you need access to internals of the controler inside the ControlerListener, put it into the constructor
	 * of the ControlerListener.  This will also clarify much more what you need.
	 * <li> NEW (jul'12): We think that it makes more sense to in fact attach some objects to the controler event.  Obvious
	 * candidates are getScenario(), getEvents(), getControlerIO(), some mobsim listener infrastructure.  A probable 
	 * transition would be to rename controlerEvent.getControler().getScenario() to 
	 * controlerEvent.getControlerListenerInfrastructure().getScenario().  For that reason, getControler() is no longer deprecated
	 * ... but you should try to restrict yourself to getControler().getScenario(), getControler().getEvents(), 
	 * getControler.getControlerIO().  kai/dominik, jul'12
	 * </ul>
	 * @return the Controler instance which fired the event
	 */
//	@Deprecated // jun'12.  See above  
	public Controler getControler() {
		return this.controler;
	}

}
