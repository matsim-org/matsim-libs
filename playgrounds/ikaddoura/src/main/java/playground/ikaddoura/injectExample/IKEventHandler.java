/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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

package playground.ikaddoura.injectExample;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

/**
 * @author ikaddoura
 *
 */

public class IKEventHandler implements LinkEnterEventHandler {
	
	private int linkEnterCounter = 0;

	@Override
	public void reset(int iteration) {
		linkEnterCounter = 0;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEnterCounter++;
	}

	public int getCounter() {
		return linkEnterCounter;
	}
	
}
