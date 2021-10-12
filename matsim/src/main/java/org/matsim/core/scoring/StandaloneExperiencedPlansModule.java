
/* *********************************************************************** *
 * project: org.matsim.*
 * StandaloneExperiencedPlansModule.java
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

 package org.matsim.core.scoring;


import org.matsim.core.controler.AbstractModule;

/**
 * In standard MATSim simulations, the functionality of the experienced-plans service
 * is integrated into {@link PlansScoringModule} so it all runs in the same EventHandler
 * and thus same thread. Thus this module must only be used when it is used outside of a
 * standard simulation.
 */
public final class StandaloneExperiencedPlansModule extends AbstractModule {

	private final boolean bindEventHandler;

	public StandaloneExperiencedPlansModule() {
		this(true);
	}

	/**
	 * @param bindEventHandler if <code>true</code>, binds {@link EventsToLegsAndActivities} as an active event handler.
	 *                         This is necessary if the experienced plans module is used without plans scoring, as
	 *                         by default {@link PlansScoringModule} makes sure that the necessary events are handled.
	 *                         If the parameter is omitted, a default value of <code>true</code> is used.
	 */
	public StandaloneExperiencedPlansModule(boolean bindEventHandler) {
		this.bindEventHandler = bindEventHandler;
	}

	@Override
	public void install() {
		bind(EventsToActivities.class).asEagerSingleton();
		bind(EventsToLegs.class).asEagerSingleton();
		bind(EventsToLegsAndActivities.class).asEagerSingleton();
		if (this.bindEventHandler) {
			addEventHandlerBinding().to(EventsToLegsAndActivities.class);
		}
		bind(ExperiencedPlansService.class).to(ExperiencedPlansServiceImpl.class).asEagerSingleton();
	}
}
