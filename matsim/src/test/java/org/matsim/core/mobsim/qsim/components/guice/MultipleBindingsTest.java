
/* *********************************************************************** *
 * project: org.matsim.*
 * MultipleBindingsTest.java
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

 package org.matsim.core.mobsim.qsim.components.guice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

	/**
 * This test shows that Guice creates Singletons solely based on the *class
 * name*. So as shown in the example one can bind the same class to different
 * interfaces, even with different names. If the class is declared in the
 * Singleton scope, it will always be the *same* object that is used.
 */
public class MultipleBindingsTest {
	interface InterfaceA {
	}

	interface InterfaceB {
	}

	static class Implementation implements InterfaceA, InterfaceB {

	}

	 @Test
	 void testGuiceComponentNaming() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Implementation.class).asEagerSingleton();
				bind(Key.get(InterfaceA.class, Names.named("abc1"))).to(Implementation.class);
				bind(Key.get(InterfaceB.class, Names.named("abc2"))).to(Implementation.class);
			}
		});

		InterfaceA implA = injector.getInstance(Key.get(InterfaceA.class, Names.named("abc1")));
		InterfaceB implB = injector.getInstance(Key.get(InterfaceB.class, Names.named("abc2")));

		Assertions.assertSame(implA, implB);
	}
}
