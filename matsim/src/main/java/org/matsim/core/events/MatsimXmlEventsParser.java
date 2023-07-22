
/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimXmlEventsParser.java
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

package org.matsim.core.events;

import org.matsim.core.utils.io.MatsimXmlParser;

abstract class MatsimXmlEventsParser extends MatsimXmlParser {

	public MatsimXmlEventsParser() {
		super(ValidationType.NO_VALIDATION);
	}

	public abstract void addCustomEventMapper(String key, MatsimEventsReader.CustomEventMapper value );
}
