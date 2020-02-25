
/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractMobsimModule.java
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

 package org.matsim.core.mobsim.framework;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.matsim.core.config.Config;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.matsim.core.controler.AbstractModule;

public abstract class AbstractMobsimModule extends AbstractModule {
	private Optional<Config> config = Optional.empty();
	private Optional<AbstractMobsimModule> parent = Optional.empty();

	public final void setConfig(Config config) {
		this.config = Optional.of(config);
	}

	public final void setParent(AbstractMobsimModule parent) {
		this.parent = Optional.of(parent);
	}

	public final void install() {
		configureMobsim();
	}

	protected abstract void configureMobsim();

}
