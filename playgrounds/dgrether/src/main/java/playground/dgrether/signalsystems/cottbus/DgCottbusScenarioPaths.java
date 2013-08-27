/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusScenarioPaths
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public interface DgCottbusScenarioPaths {

	public static final String NETWORK_FILENAME = DgPaths.REPOS  + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
	public static final String LANES_FILENAME = DgPaths.REPOS  + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/lanes.xml";
	public static final String SIGNALS_FILENAME = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems_no_13.xml";
	public static final String SIGNAL_GROUPS_FILENAME = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_groups_no_13.xml";
	public static final String SIGNAL_CONTROL_FIXEDTIME_FILENAME = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_no_13.xml";
	public static final String SIGNAL_CONTROL_SYLVIA_FILENAME = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_sylvia_no_13.xml";
	
	
}
