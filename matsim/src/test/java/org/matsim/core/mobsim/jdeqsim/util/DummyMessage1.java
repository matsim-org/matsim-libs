
/* *********************************************************************** *
 * project: org.matsim.*
 * DummyMessage1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.mobsim.jdeqsim.Message;

public class DummyMessage1 extends Message {

	public Message messageToUnschedule=null;

	@Override
	public void handleMessage() {
		this.getReceivingUnit().getScheduler().unschedule(messageToUnschedule);
	}

	@Override
	public void processEvent() {
	}

}
