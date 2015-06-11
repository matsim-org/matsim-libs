/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.gregor.rtcadyts.run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.gregor.rtcadyts.debugging.ShapeFileDebugger;
import playground.gregor.rtcadyts.frames2counts.Frames2Counts;
import playground.gregor.rtcadyts.frames2counts.LinkInfo;
import playground.gregor.rtcadyts.io.SensorDataFrame;
import playground.gregor.rtcadyts.io.SensorDataReader;

public class MyRunRunner {

	public static void main(String [] args) throws IOException {
		List<SensorDataFrame> frames = new ArrayList<>();
		
		
		String inputDir = "/Users/laemmel/devel/cadytsA96/A96Daten/";
		File[] files = new File(inputDir).listFiles();
		for (File f : files) {
			if (f.isFile() && f.getAbsolutePath().endsWith("speed")){
				SensorDataFrame frame = SensorDataReader.handle(f);
				if (frame != null) {
					System.out.println(frame);
					frames.add(frame);
				}
			}
		}
		
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		CoordinateTransformation tr = new GeotoolsTransformation("EPSG:4326","EPSG:32632");
		OsmNetworkReader r = new OsmNetworkReader(sc.getNetwork(),tr);
		r.setKeepPaths(true);
		r.parse(inputDir+"/map.osm");
		
		Frames2Counts f2c = new Frames2Counts(sc,frames);
		f2c.run();
		Collection<LinkInfo> lis = f2c.getLinkInfos();
		ShapeFileDebugger shp = new ShapeFileDebugger(lis, "/Users/laemmel/devel/cadytsA96/debug/");
		shp.run();
		
	}
}
