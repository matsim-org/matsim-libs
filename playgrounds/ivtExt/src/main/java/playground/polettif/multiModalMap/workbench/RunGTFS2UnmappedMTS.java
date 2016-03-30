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


package playground.polettif.multiModalMap.workbench;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.multiModalMap.gtfs.GTFSReader;

public class RunGTFS2UnmappedMTS {
	
	public static void main(final String[] args) {
		final String gtfsPath = "C:/Users/polettif/Desktop/data/gtfs/zvv/";
		final String gtfsPath_sample = "C:/Users/polettif/Desktop/data/gtfs/google_sample/";
		final String mtsFile = "C:/Users/polettif/Desktop/output/test/zvv_unmappedSchedule.xml";
		final String mtsFile_sample = "C:/Users/polettif/Desktop/output/test/google_sample.xml";

		// Load Schedule
		GTFSReader gtfsReader = new GTFSReader(gtfsPath);
		gtfsReader.writeTransitSchedule(mtsFile);
	}
	
}