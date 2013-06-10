/* *********************************************************************** *
 * project: org.matsim.*
 * DgNumberOfLineSwitchAnalysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.scripts;

import air.analysis.DgFlightLineSwitchEventHandler;


/**
 * @author dgrether
 *
 */
public class DgNumberOfLineSwitchAnalysis {

	public static void main(String[] args) {
		String events = "/home/dgrether/data/work/repos/runs-svn/run1854/ITERS/it.600/1854.600.events.xml.gz";
		new DgFlightLineSwitchEventHandler().calcLineswitch(events);
	}

}
