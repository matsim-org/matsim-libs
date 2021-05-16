/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.api.core.v01;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * Auto-resets Id caches before each test is started. This helps to keep every single unit test independent of others
 * (so things like execution order etc. will not impact them).
 * <p>
 * It is enabled in tests (run by maven) by registering them as listeners in the surefire and failsafe plugin configs:
 * <pre>
 * {@code
 * <property>
 * <name>listener</name>
 * <value>org.matsim.api.core.v01.IdCacheCleaner</value>
 * </property>
 * }
 * </pre>
 * <p>
 * IntelliJ does not support junit listeners out of the box. To enable them in IntelliJ, you can install a plugin
 * https://plugins.jetbrains.com/plugin/15718-junit-4-surefire-listener
 * <p>
 * Not sure about Eclipse, but it seems that an additional plugin would be required (see:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=538885)
 * <p>
 * Since IDEs have a limited support for registering jUnit RunListeners via pom.xml, there may be cases where we need
 * to explicitly reset the Id caches while setting up a test (to make them green in IDEs).
 *
 * @author Michal Maciejewski (michalm)
 */
public class AutoResetIdCaches extends RunListener {
	@Override
	public void testStarted(Description description) {
		Id.resetCaches();
	}
}
