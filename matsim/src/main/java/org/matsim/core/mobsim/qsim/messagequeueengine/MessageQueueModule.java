/* *********************************************************************** *
 * project: org.matsim.*
 * MessageQueueModule.java
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

package org.matsim.core.mobsim.qsim.messagequeueengine;

import org.matsim.core.mobsim.messagequeue.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.messagequeue.SteppableScheduler;

public class MessageQueueModule extends AbstractQSimModule {
	static public final String COMPONENT_NAME = "MessageQueueEngine";

	@Override
	protected void configureQSim() {
		bind(MessageQueue.class).asEagerSingleton();
		bind(SteppableScheduler.class).asEagerSingleton();
		bind(MessageQueueEngine.class).asEagerSingleton();

		this.addQSimComponentBinding( COMPONENT_NAME ).to( MessageQueueEngine.class ) ;

	}
}
