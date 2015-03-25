/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonEndActivityAndEvacuateReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import playground.christoph.evacuation.withinday.replanning.replanners.old.EndActivityAndEvacuateReplanner;
import playground.christoph.evacuation.withinday.replanning.replanners.old.EndActivityAndEvacuateReplannerFactory;

public class MarathonEndActivityAndEvacuateReplannerFactory extends WithinDayDuringActivityReplannerFactory {

	private Scenario scenario;
	private EndActivityAndEvacuateReplannerFactory factory;
	
	public MarathonEndActivityAndEvacuateReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			EndActivityAndEvacuateReplannerFactory factory) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.factory = factory;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		
		EndActivityAndEvacuateReplanner delegate = (EndActivityAndEvacuateReplanner) factory.createReplanner();
		
		WithinDayDuringActivityReplanner replanner = new MarathonEndActivityAndEvacuateReplanner(super.getId(), 
				scenario, this.getWithinDayEngine().getActivityRescheduler(), delegate);
		return replanner;
	}

}