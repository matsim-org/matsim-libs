/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.workbench;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

public class TransformToWGS84 {
	
	public static void main(final String[] args) {
		String base = "C:/Users/polettif/Desktop/output/PublicTransportMap/";
		String scheduleFile = base + "zurich_gtfs_schedule.xml";
		String networkFile = base + "zurich_gtfs_network.xml";

		ScheduleTools.transformScheduleFile(scheduleFile, TransformationFactory.CH1903_LV03_Plus, TransformationFactory.WGS84);
		NetworkTools.transformNetworkFile(networkFile, TransformationFactory.CH1903_LV03_Plus, TransformationFactory.WGS84);
	}
	
}