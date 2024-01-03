/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.population.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * @author mrieser
 */
public interface PopulationWriterHandler {

	void writeHeaderAndStartElement(BufferedWriter out) throws IOException;

	void startPlans(final Population plans, final BufferedWriter out) throws IOException;

	void writePerson(final Person person, final BufferedWriter out) throws IOException;

	void endPlans(final BufferedWriter out) throws IOException;

	void writeSeparator(final BufferedWriter out) throws IOException;

	default void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
		if (!converters.isEmpty()) {
			LogManager.getLogger(getClass()).warn(
					getClass().getName() +
							" does not support custom attributes." +
							" Please use a more recent file format" +
							" if you need this feature.");
		}
	}
}
