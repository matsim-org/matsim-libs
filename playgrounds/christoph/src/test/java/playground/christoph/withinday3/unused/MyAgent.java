/* *********************************************************************** *
 * project: org.matsim.*
 * MyAgent.java
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
package playground.christoph.withinday3.unused;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

/**
 * @author nagel
 *
 */
class MyAgent implements PersonDriverAgent {
	
	private PersonDriverAgent delegate ;
	private Mobsim mobsim ;
	
	MyAgent( Person person, Mobsim mobsim ) {
		this.mobsim = mobsim ;
		delegate = new DefaultAgentFactory(mobsim).createPersonAgent( person ) ;
	}

	public Id chooseNextLinkId() {
		return delegate.chooseNextLinkId();
	}

	public void endActivityAndAssumeControl(double now) {
		delegate.endActivityAndAssumeControl(now);
	}

	public void endLegAndAssumeControl(double now) {
		delegate.endLegAndAssumeControl(now);
	}

	public Leg getCurrentLeg() {
		return delegate.getCurrentLeg();
	}

	public Id getCurrentLinkId() {
		return delegate.getCurrentLinkId();
	}

	public PlanElement getCurrentPlanElement() {
		return delegate.getCurrentPlanElement();
	}

	public double getDepartureTime() {
		return delegate.getDepartureTime();
	}

	public Id getDestinationLinkId() {
		return delegate.getDestinationLinkId();
	}

	public Person getPerson() {
		return delegate.getPerson();
	}

	public QVehicle getVehicle() {
		return delegate.getVehicle();
	}

	public boolean initializeAndCheckIfAlive() {
		return delegate.initializeAndCheckIfAlive();
	}

	public void notifyMoveOverNode() {
		delegate.notifyMoveOverNode();
	}

	public void resetCaches() {
		delegate.resetCaches();
	}

	public void setVehicle(QVehicle veh) {
		delegate.setVehicle(veh);
	}

	public void teleportToLink(Id linkId) {
		delegate.teleportToLink(linkId);
	}


}
