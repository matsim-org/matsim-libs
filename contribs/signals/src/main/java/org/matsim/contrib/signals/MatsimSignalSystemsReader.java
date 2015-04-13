/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimSignalSystemsReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals;


/**
 * A place for constants to xml schemata
 * @author dgrether
 */
public class MatsimSignalSystemsReader {
	/**
	 * @deprecated use new file formats
	 */
	@Deprecated
	public static final String SIGNALSYSTEMS10 = "http://www.matsim.org/files/dtd/lightSignalSystems_v1.0.xsd";
	/**
	 * @deprecated use new file formats
	 */
	@Deprecated
	public static final String SIGNALSYSTEMS11 = "http://www.matsim.org/files/dtd/signalSystems_v1.1.xsd";

	public static final String SIGNALSYSTEMS20 = "http://www.matsim.org/files/dtd/signalSystems_v2.0.xsd";

	public static final String SIGNALGROUPS20 = "http://www.matsim.org/files/dtd/signalGroups_v2.0.xsd";

	public static final String SIGNALCONTROL20 = "http://www.matsim.org/files/dtd/signalControl_v2.0.xsd";
	
}
