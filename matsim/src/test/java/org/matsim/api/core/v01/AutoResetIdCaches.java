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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Auto-resets Id caches before each test is started. This helps to keep every single unit test independent of others
 * (so things like execution order etc. will not impact them).
 * <p>
 * It is configured in the junit-platform.properties file and in META-INF/services/org.junit.jupiter.api.extension.Extension file.
 * (Also see https://www.baeldung.com/junit-5-extensions)
 * Both files are located in the root of the matsim project in order to inherit the auto extension in all submodules.
 * <p>
 * IntelliJ does also recognize this configuration and will automatically enable the extension.
 * <p>
 * For some reason, the configuration files need to be placed in the matsim module (where this class is placed). Otherwise, it won't work.
 *
 * @author Michal Maciejewski (michalm)
 */
public class AutoResetIdCaches implements TestWatcher {
	private static final Logger log = LogManager.getLogger(AutoResetIdCaches.class);

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		resetIdCaches();
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
		resetIdCaches();
	}

	@Override
	public void testAborted(ExtensionContext context, Throwable cause) {
		resetIdCaches();
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		resetIdCaches();
	}

	private void resetIdCaches() {
		log.info("Resetting Id caches.");
		Id.resetCaches();
	}
}
