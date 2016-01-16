/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.lib;

import org.matsim.core.controler.MatsimServices;

public class GlobalRegistry {

	public static MatsimServices controler = null;
	public static boolean isTestingMode = false;

	public static boolean doPrintGraficDataToConsole = false;
	public static int runNumber;

	public static Double readDoubleFromConfig(String moduleName, String paramName) {
		String doubleValue = null;

		try {
			doubleValue = controler.getConfig().getParam(moduleName, paramName);
		} catch (Exception e) {
			return null;
		}

		return new Double(doubleValue);

	}
}
