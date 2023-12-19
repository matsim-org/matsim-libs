
/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractMobsimModuleTest.java
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

 package org.matsim.core.mobsim;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

	public class AbstractMobsimModuleTest {
	 @Test
	 void testOverrides() {
		AbstractMobsimModule moduleA = new AbstractMobsimModule() {
			@Override
			protected void configureMobsim() {
				getConfig().createModule("testA");
				bind(String.class).toInstance("testAString");
			}
		};

		AbstractMobsimModule moduleB = new AbstractMobsimModule() {
			@Override
			protected void configureMobsim() {
				getConfig().createModule("testB");
				bind(String.class).toInstance("testBString");
			}
		};

		AbstractMobsimModule composite = AbstractMobsimModule.overrideMobsimModules(Collections.singleton(moduleA),
				Collections.singletonList(moduleB));

		Config config = ConfigUtils.createConfig();
		composite.setConfig(config);

		Injector injector = Guice.createInjector(composite);

		Assertions.assertTrue(config.getModules().containsKey("testA"));
		Assertions.assertTrue(config.getModules().containsKey("testB"));

		Assertions.assertEquals("testBString", injector.getInstance(String.class));
	}
}
