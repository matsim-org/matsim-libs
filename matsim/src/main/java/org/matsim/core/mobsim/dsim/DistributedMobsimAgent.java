 /* *********************************************************************** *
  * project: matsim
  * PlanAgent.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.dsim;

 import org.matsim.api.core.v01.Message;
 import org.matsim.core.mobsim.framework.MobsimAgent;

 /**
  * Extensions of the {@link MobsimAgent} interface that are used in distributed simulations.
  * This agent provides a method to convert the agent to a message that can be sent to other nodes.
  */
 public interface DistributedMobsimAgent extends MobsimAgent {

	 /**
	  * Convert the agent to a message that can be sent to other nodes. The message can be any java object, but should be as lightweight as possible.
	  */
	 Message toMessage();
 }
