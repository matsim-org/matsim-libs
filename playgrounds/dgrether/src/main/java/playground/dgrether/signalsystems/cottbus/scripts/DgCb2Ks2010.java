/* *********************************************************************** *
 * project: org.matsim.*
 * DgCb2Ks2010
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
package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.Scenario2KoehlerStrehler2010;
import playground.dgrether.signalsystems.cottbus.DgCottbusScenarioPaths;


/**
 * @author dgrether
 *
 */
public class DgCb2Ks2010 {

	public static void main(String[] args) throws Exception {
		int cellsX = 5;
		int cellsY = 5;
		double boundingBoxOffset = 50.0;
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
	//private static double startTime = 13.5 * 3600.0;
	//private static double endTime = 18.5 * 3600.0;
		double matsimPopSampleSize = 1.0;
		double ksModelCommoditySampleSize = 0.01;
		final String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/test_tt/";

		String networkFilename = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
		String lanesFilename = DgCottbusScenarioPaths.LANES_FILENAME;
		String signalSystemsFilename = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems.xml";
		String signalGroupsFilename = DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME;
		String signalControlFilename = DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME;
		//TODO change to run1712 when finished
		String populationFilename = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans_sample.xml";
//		String populationFile = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans.xml.gz";
		Scenario fullScenario = Scenario2KoehlerStrehler2010.loadScenario(networkFilename, populationFilename, lanesFilename, signalSystemsFilename, signalGroupsFilename, signalControlFilename);
		
		String name = "run 1292 output plans between 05:30 and 09:30";

		Scenario2KoehlerStrehler2010 matsim2KsConverter = new Scenario2KoehlerStrehler2010(fullScenario,  MGC.getCRS(TransformationFactory.WGS84_UTM33N));
		matsim2KsConverter.setMatsimPopSampleSize(matsimPopSampleSize);
		matsim2KsConverter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		matsim2KsConverter.convert(outputDirectory, name, boundingBoxOffset, cellsX, cellsY, startTime, endTime);
	}

}
