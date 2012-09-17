/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplannerFactory.java
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

package org.matsim.withinday.replanning.replanners.interfaces;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.interfaces.Identifier;
import org.matsim.withinday.replanning.replanners.tools.ReplanningIdGenerator;

public abstract class WithinDayReplannerFactory<T extends Identifier> {

	private final WithinDayEngine replanningManager;
	private Id id;
	private AbstractMultithreadedModule abstractMultithreadedModule;
	private double replanningProbability = 1.0;
	private Set<T> identifiers = new HashSet<T>();
	
	public WithinDayReplannerFactory(WithinDayEngine replanningManager, 
			AbstractMultithreadedModule abstractMultithreadedModule, double replanningProbability) {
		this.replanningManager = replanningManager;
		this.abstractMultithreadedModule = abstractMultithreadedModule;
		this.replanningProbability = replanningProbability;
		this.id = ReplanningIdGenerator.getNextId();
	}
	
	public abstract WithinDayReplanner<? extends Identifier> createReplanner();
	
	/*
	 * This method should be called after a new Replanner instance
	 * has been created. Is there any way to force this???
	 */
	public final void initNewInstance(WithinDayReplanner<? extends Identifier> replanner) {
		replanner.setAbstractMultithreadedModule(this.abstractMultithreadedModule);
	}
	
	public final double getReplanningProbability() {
		return this.replanningProbability;
	}
	
	public final WithinDayEngine getReplanningManager() {
		return this.replanningManager;
	}
	
	public final Id getId() {
		return this.id;
	}
	
	public final boolean addIdentifier(T identifier) {
		return this.identifiers.add(identifier);
	}
	
	public final boolean removeIdentifier(T identifier) {
		return this.identifiers.remove(identifier);
	}
	
	public final Set<T> getIdentifers() {
		return Collections.unmodifiableSet(identifiers);
	}
}
